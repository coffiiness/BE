"""Isolated Linux tests. All curl/docker calls are mocks; no real endpoints/daemon.

Run with Python 3 stdlib, bash, jq, flock. No Gradle/Docker engine required.
"""
import fcntl
import json
import os
from pathlib import Path
import shutil
import subprocess
import tempfile
import unittest

SHA = 'a' * 40
DIGEST = 'sha256:' + 'd' * 64
NEW = 'sha256:' + 'b' * 64
OLD = 'sha256:' + 'c' * 64
IMAGE = 'ghcr.io/coffiiness/calfit-backend@' + DIGEST
SCRIPT = Path(__file__).with_name('deploy-oracle.sh').resolve()

# This dispatcher has no networking or subprocess calls. Unknown calls fail closed.
MOCK = r'''
import json, os, sys
from pathlib import Path
p=Path(os.environ['MOCK_STATE']); s=json.loads(p.read_text()); args=sys.argv[2:]
tool=sys.argv[1]; case=s['case']; sha='a'*40; digest='sha256:'+'d'*64
old='sha256:'+'c'*64; new='sha256:'+'b'*64
run_id=100; deployment_id=55
if s.get('release')==2: sha='2'*40; digest='sha256:'+'1'*64; new='sha256:'+'3'*64; run_id=101; deployment_id=56
if s.get('identity_fixture'):
    digest=s['identity_fixture']['digest']; new=s['identity_fixture']['config']
config=new
if s.get('store')=='containerd': new=digest
if s.get('old_store')=='containerd': old='sha256:'+'7'*64
image='ghcr.io/coffiiness/calfit-backend@'+digest
s['calls'].append([tool]+args)
def emit(value=None,code=0):
    p.write_text(json.dumps(s))
    if value is not None: print(json.dumps(value) if isinstance(value,(dict,list)) else value)
    sys.exit(code)
if tool=='curl':
    assert not any(x in args for x in ['--request','-X','--data','--data-binary']), 'GET only'
    url=args[-1]; base='https://api.github.com/repos/coffiiness/BE/'
    if url=='http://127.0.0.1:8080/health':
        emit('503' if case=='host-health-failure' and s.get('active_image')==new else '200')
    if url=='https://api.calfit.co.kr/health':
        emit('503' if case=='external-http-failure' else '200',code=7 if case=='external-network-failure' else 0)
    assert url.startswith(base)
    endpoint=url[len(base):]
    n=s.setdefault('requests',{}).get(endpoint,0)+1; s['requests'][endpoint]=n
    if case in ['api-error','rate-limit','network-error']: emit(code=22 if case!='network-error' else 7)
    if case=='api-after-pull' and n>1: emit(code=22)
    if endpoint.startswith('deployments?'):
        payload={'schemaVersion':1,'repository':'coffiiness/BE','image':'ghcr.io/coffiiness/calfit-backend',
                 'commitSha':sha,'digest':digest,'imageId':config,'workflowRunId':run_id,'workflowRunAttempt':1}
        if case=='bad-digest': payload['digest']='sha256:not-a-digest'
        if case=='injection': payload['commitSha']='$(touch /tmp/calfit-must-never-execute)'
        if case=='wrong-repository': payload['repository']='someone/BE'
        if case=='bad-payload': payload='echo malicious'
        d={'id':deployment_id,'environment':'oracle-production','task':'deploy','production_environment':True,
           'repository_url':base.rstrip('/'),'creator':{'login':'github-actions[bot]','type':'Bot'},
           'sha':sha,'ref':sha,'payload':payload}
        if case=='wrong-creator': d['creator']['login']='someone'
        if case=='request-changed' and n>1: d['id']=56
        emit([] if case=='no-request' else [d])
    if endpoint=='git/ref/heads/main': emit({'object':{'sha':'f'*40 if case=='stale-main' or (case=='main-advanced' and n>1) else sha}})
    if endpoint=='actions/workflows/ci.yml': emit({'id':20,'name':'Backend CI/CD','path':'.github/workflows/ci.yml'})
    if endpoint==f'actions/runs/{run_id}':
        run={'id':run_id,'workflow_id':20,'run_attempt':1,'name':'Backend CI/CD','path':'.github/workflows/ci.yml',
             'event':'push','head_branch':'main','head_sha':sha,'repository':{'full_name':'coffiiness/BE'},
             'head_repository':{'full_name':'coffiiness/BE'},'status':'completed','conclusion':'success'}
        if case=='failed-workflow': run['conclusion']='failure'
        if case=='in-progress': run['status']='in_progress'; run['conclusion']=None
        if case=='wrong-event': run['event']='pull_request'
        if case=='wrong-run-sha': run['head_sha']='f'*40
        if case=='wrong-workflow': run['workflow_id']=99
        if case=='new-attempt' and n>1: run['run_attempt']=2
        emit(run)
    if endpoint.startswith(f'actions/runs/{run_id}/attempts/1/jobs?'):
        names=['Quality Gate and verified bootJar (AMD64)','Build and verify the ARM64 runtime','Publish the tested image to GHCR']
        jobs=[{'name':name,'run_id':run_id,'head_sha':sha,'status':'completed','conclusion':'success'} for name in names]
        if case=='skipped-quality': jobs[0]['conclusion']='skipped'
        emit({'total_count':3,'jobs':jobs})
    if endpoint.startswith(f'deployments/{deployment_id}/statuses?'): emit([{'state':'failure'}] if case=='cancelled-request' else [])
    if endpoint.startswith('compare/'): emit({'status':'behind' if case=='main-backwards' else 'diverged' if case=='main-diverged' else 'ahead'})
    raise AssertionError('Unexpected API path: '+endpoint)
if tool=='docker':
    assert args[:2]==['--host','unix:///var/run/docker.sock']
    assert 'DOCKER_CONTEXT' not in os.environ and 'DOCKER_HOST' not in os.environ
    args=args[2:]
    assert not any(x in args for x in ['down','prune','rm','build','login','push'])
    if args[0]=='compose':
        target=os.environ.get('BACKEND_IMAGE')
        assert target is None or target==image or target.startswith('calfit-backend:rollback-deployment-')
        assert os.environ['JPA_DDL_AUTO']=='validate'
        assert 'COMPOSE_FILE' not in os.environ
        action=args[args.index('-f')+2:]
        if action[0]=='config':
            if target is None:
                target='calfit-backend:oracle-arm64'
                indexes=[i+1 for i,x in enumerate(args) if x=='--env-file']
                if len(indexes)>1:
                    target=Path(args[indexes[-1]]).read_text().strip().split('=',1)[1]
            emit({'services':{'core-api':{'image':target,'platform':'linux/arm64','environment':{
                'SPRING_PROFILES_ACTIVE':'prod','SPRING_JPA_HIBERNATE_DDLAUTO':'update' if case=='bad-config' else 'validate'}},
                'mysql':{'image':'mysql:8.4.13'}},'volumes':{'mysql_data':{'external':True,'name':'calfit_oracle_mysql84'}}})
        if action[:2]==['ps','-q']: emit('mysql-container' if action[2]=='mysql' else 'new-api' if s.get('active_image',old)==new else 'old-api')
        if action[0]=='up':
            assert action==['up','-d','--no-deps','--no-build','--pull','never','--wait','--wait-timeout','600','core-api']
            rollback=target!=image
            s['active_image']=old if rollback else new
            s['applied']=True
            if rollback: s['rollback_count']=s.get('rollback_count',0)+1
            if (case=='process-kill' and not rollback) or (case=='rollback-kill' and rollback):
                p.write_text(json.dumps(s))
                import signal
                os.kill(os.getppid(),signal.SIGKILL)
                sys.exit(0)
            emit(code=1 if (not rollback and case in ['replace-failure','rollback-failure','rollback-unhealthy','missing-rollback-image','rollback-kill']) or (rollback and case=='rollback-failure') else 0)
    if args[0]=='pull':
        assert args==['pull','--platform','linux/arm64',image]
        emit(code=1 if case=='pull-failure' else 0)
    if args[:2]==['manifest','inspect']:
        assert args==['manifest','inspect',image], 'Manifest must be fetched by the exact registry digest'
        if case in ['manifest-error','unknown-manifest-digest']: emit(code=1)
        manifest={'schemaVersion':2,'mediaType':'application/vnd.oci.image.manifest.v1+json',
                  'config':{'digest':config}}
        if case=='docker-schema2': manifest['mediaType']='application/vnd.docker.distribution.manifest.v2+json'
        if case=='wrong-config-digest': manifest['config']['digest']='sha256:'+'9'*64
        if case=='missing-config-digest': manifest['config']={}
        if case=='manifest-index': manifest['mediaType']='application/vnd.oci.image.index.v1+json'
        emit(manifest)
    if args[:2]==['image','tag']:
        assert args[2]==old; s['rollbackTag']=args[3]; emit()
    if args[:2]==['image','inspect']:
        if '--format' in args:
            if case=='missing-rollback-image' and s.get('applied') and args[2].startswith('calfit-backend:rollback-deployment-'): emit(code=1)
            emit('sha256:'+'8'*64 if case=='stale-version' and not args[2].startswith('calfit-backend:rollback-deployment-') else old)
        emit([{'Id':'sha256:'+'e'*64 if case=='wrong-image-id' else new,'Os':'linux',
              'Architecture':'amd64' if case=='wrong-architecture' else 'arm64',
              'RepoDigests':[] if case=='wrong-repodigest' else [image],
              'Config':{'Labels':{'org.opencontainers.image.source':'https://github.com/coffiiness/BE',
                  'org.opencontainers.image.revision':'f'*40 if case=='wrong-revision' else sha,
                  'com.calfit.java.runtime':'17.0.21.1+2'}}}])
    if args[0]=='inspect':
        container=args[1]; fmt=args[args.index('--format')+1]
        if fmt=='{{.Image}}': emit(new if container=='new-api' else old)
        if fmt=='{{.State.Running}}': emit('false' if case=='not-running' and container=='new-api' else 'true')
        if fmt=='{{.State.Health.Status}}': emit('unhealthy' if (case=='unhealthy' and container=='new-api') or (case=='rollback-unhealthy' and container=='old-api' and s.get('rollback_count')) else 'healthy')
        if fmt=='{{.State.StartedAt}}': emit('changed' if case=='mysql-changed' and s.get('applied') else 'unchanged')
        if '.Mounts' in fmt: emit('unexpected-volume' if case=='wrong-volume' else 'calfit_oracle_mysql84')
    if args[0]=='exec':
        assert args[1] in ['old-api','new-api'] and args[-1]=='http://127.0.0.1:8081/actuator/health'
        emit({'status':'DOWN' if case=='actuator-down' and args[1]=='new-api' else 'UP'})
    raise AssertionError('Unexpected Docker command: '+repr(args))
raise AssertionError('Unexpected mock tool')
'''


class DeployTest(unittest.TestCase):
    def setUp(self):
        self.tmp = tempfile.TemporaryDirectory(prefix='calfit-deploy-mock-')
        self.addCleanup(self.tmp.cleanup)
        self.root = Path(self.tmp.name) / 'BE'
        self.root.mkdir()
        self.state = self.root / '.oracle-deploy'
        self.state.mkdir()
        self.env_bytes = b'FAKE_SYNTHETIC_ENV_ONLY=true\n'
        (self.root / '.env.prod').write_bytes(self.env_bytes)
        self.old_version = b'BACKEND_IMAGE=calfit-backend:existing-local\n'
        (self.root / '.env.image').write_bytes(self.old_version)
        (self.root / 'docker-compose.prod.yml').write_text('mock compose\n')
        self.mock_state = Path(self.tmp.name) / 'mock.json'
        self.set_case('new')
        self.bin = Path(self.tmp.name) / 'bin'
        self.bin.mkdir()
        dispatcher = self.bin / 'mock.py'
        dispatcher.write_text(MOCK)
        for command in ('curl', 'docker'):
            wrapper = self.bin / command
            wrapper.write_text('#!/bin/sh\nexec python3 "' + str(dispatcher) + '" ' + command + ' "$@"\n')
            wrapper.chmod(0o700)
        uname = self.bin / 'uname'
        uname.write_text('#!/bin/sh\necho aarch64\n')
        uname.chmod(0o700)
        self.env = dict(os.environ, PATH=str(self.bin) + ':' + os.environ['PATH'], MOCK_STATE=str(self.mock_state),
                        BACKEND_IMAGE='evil:override', JPA_DDL_AUTO='update', COMPOSE_FILE='/must-not-load.yml',
                        DOCKER_HOST='tcp://must-not-contact:2375', DOCKER_CONTEXT='must-not-contact')

    def set_case(self, case):
        self.mock_state.write_text(json.dumps({'case': case, 'calls': []}))

    def configure_store(self, store, old_store='classic', identity_fixture=None):
        record = json.loads(self.mock_state.read_text())
        record.update(store=store, old_store=old_store)
        if identity_fixture:
            record['identity_fixture'] = identity_fixture
        self.mock_state.write_text(json.dumps(record))

    def run_script(self, *flags):
        result = subprocess.run(['bash', str(SCRIPT), '--root', str(self.root), *flags],
                                env=self.env, text=True, capture_output=True, timeout=30)
        self.assertEqual((self.root / '.env.prod').read_bytes(), self.env_bytes)
        return result

    def calls(self):
        return json.loads(self.mock_state.read_text())['calls']

    def previous(self, **overrides):
        current = dict(status='success', digest='sha256:' + 'e' * 64, commitSha='9' * 40,
                       workflowRunId=99, workflowRunAttempt=1, deploymentId=54)
        current.update(overrides)
        (self.state / 'current.json').write_text(json.dumps(current))

    def test_new_deployment_and_recovery_preserved(self):
        self.previous()
        result = self.run_script()
        self.assertEqual(result.returncode, 0, result.stdout + result.stderr)
        self.assertEqual(json.loads((self.state / 'current.json').read_text())['digest'], DIGEST)
        self.assertEqual((self.root / '.env.image').read_text(), 'BACKEND_IMAGE=' + IMAGE + '\n')
        backup = self.state / 'backups' / '55'
        self.assertEqual((backup / 'env.image.previous').read_bytes(), self.old_version)
        self.assertEqual(json.loads((backup / 'recovery.json').read_text())['previousImageId'], OLD)
        self.assertTrue((self.state / 'digests' / (DIGEST[7:] + '.json')).is_file())
        self.assertFalse((self.state / 'pending.json').exists())

    def test_first_deployment_without_version_file(self):
        (self.root / '.env.image').unlink()
        result = self.run_script()
        self.assertEqual(result.returncode, 0, result.stdout + result.stderr)
        self.assertTrue((self.state / 'backups/55/env.image.absent').exists())

    def test_dry_run_never_reads_production_env_or_calls_docker(self):
        (self.root / '.env.prod').unlink()
        result = subprocess.run(['bash', str(SCRIPT), '--root', str(self.root), '--dry-run'],
                                env=self.env, text=True, capture_output=True, timeout=30)
        self.assertEqual(result.returncode, 0, result.stderr)
        self.assertIn('DRY-RUN', result.stdout)
        self.assertFalse(any(c[0] == 'docker' for c in self.calls()))
        self.assertEqual((self.root / '.env.image').read_bytes(), self.old_version)
        self.assertFalse((self.state / 'current.json').exists())

    def test_same_digest(self):
        self.previous(digest=DIGEST)
        result = self.run_script()
        self.assertEqual(result.returncode, 0, result.stderr)
        self.assertIn('already deployed', result.stdout)
        self.assertFalse(any(c[0] == 'docker' for c in self.calls()))

    def test_previously_applied_digest_never_reapplied(self):
        (self.state / 'digests').mkdir()
        (self.state / 'digests' / (DIGEST[7:] + '.json')).write_text('{}')
        result = self.run_script()
        self.assertEqual(result.returncode, 0, result.stderr)
        self.assertIn('history', result.stdout)
        self.assertFalse(any(c[0] == 'docker' for c in self.calls()))

    def test_invalid_api_metadata_keeps_existing_service(self):
        cases = ['bad-digest', 'injection', 'wrong-repository', 'bad-payload', 'wrong-creator',
                 'stale-main', 'failed-workflow', 'in-progress', 'wrong-event', 'wrong-run-sha',
                 'wrong-workflow', 'skipped-quality', 'cancelled-request', 'api-error',
                 'rate-limit', 'network-error']
        for case in cases:
            with self.subTest(case=case):
                self.set_case(case)
                result = self.run_script()
                self.assertNotEqual(result.returncode, 0)
                self.assertFalse(any(c[0] == 'docker' for c in self.calls()))
                self.assertEqual((self.root / '.env.image').read_bytes(), self.old_version)

    def test_no_request(self):
        self.set_case('no-request')
        self.assertEqual(self.run_script().returncode, 0)
        self.assertFalse(any(c[0] == 'docker' for c in self.calls()))

    def test_old_run_or_deployment_rejected(self):
        for overrides in [dict(workflowRunId=101), dict(workflowRunId=100, workflowRunAttempt=1), dict(deploymentId=56)]:
            with self.subTest(overrides=overrides):
                self.previous(**overrides)
                self.set_case('new')
                self.assertNotEqual(self.run_script().returncode, 0)
                self.assertFalse(any(c[0] == 'docker' for c in self.calls()))

    def test_main_history_cannot_move_back_or_diverge(self):
        self.previous()
        for case in ['main-backwards', 'main-diverged']:
            with self.subTest(case=case):
                self.set_case(case)
                self.assertNotEqual(self.run_script().returncode, 0)
                self.assertFalse(any(c[0] == 'docker' for c in self.calls()))

    def test_pull_identity_or_race_failure_keeps_old_version(self):
        cases = ['bad-config', 'wrong-volume', 'pull-failure', 'wrong-image-id', 'wrong-architecture',
                 'wrong-revision', 'wrong-repodigest', 'main-advanced', 'request-changed', 'new-attempt', 'api-after-pull']
        for case in cases:
            with self.subTest(case=case):
                self.set_case(case)
                result = self.run_script()
                self.assertNotEqual(result.returncode, 0, case)
                self.assertFalse(json.loads(self.mock_state.read_text()).get('applied'), case)
                self.assertEqual((self.root / '.env.image').read_bytes(), self.old_version)
                self.assertFalse((self.state / 'current.json').exists())

    def test_failed_replacement_or_health_never_records_success(self):
        for case in ['replace-failure', 'unhealthy', 'mysql-changed']:
            with self.subTest(case=case):
                self.set_case(case)
                result = self.run_script()
                self.assertNotEqual(result.returncode, 0)
                self.assertFalse((self.state / 'current.json').exists())
                self.assertTrue((self.state / 'backups/55/recovery.env').exists())
                if case == 'mysql-changed':
                    self.assertEqual(json.loads((self.state / 'pending.json').read_text())['status'], 'rollback_failed')
                    before = len(self.calls())
                    self.assertNotEqual(self.run_script().returncode, 0)
                    self.assertEqual(len(self.calls()), before)
                    (self.state / 'pending.json').unlink()
                else:
                    self.assertFalse((self.state / 'pending.json').exists())
                    self.assertEqual((self.root / '.env.image').read_bytes(), self.old_version)
                    before = len([c for c in self.calls() if c[0] == 'docker'])
                    self.assertEqual(self.run_script().returncode, 0)
                    self.assertEqual(len([c for c in self.calls() if c[0] == 'docker']), before)
                    shutil.rmtree(self.state / 'failures')
                shutil.rmtree(self.state / 'backups')

    def test_duplicate_execution_uses_real_flock(self):
        with (self.state / 'lock').open('w') as handle:
            fcntl.flock(handle, fcntl.LOCK_EX | fcntl.LOCK_NB)
            result = self.run_script()
            self.assertEqual(result.returncode, 0, result.stderr)
            self.assertIn('holds the lock', result.stdout)
            self.assertEqual(self.calls(), [])

    def test_corrupt_local_history_fails_closed(self):
        self.previous(workflowRunId='invalid')
        self.assertNotEqual(self.run_script().returncode, 0)
        self.assertFalse(any(c[0] == 'docker' for c in self.calls()))

    def test_process_interrupted_after_atomic_image_selection_requires_recovery(self):
        (self.state / 'pending.json').write_text('{"status":"in_progress"}')
        result = self.run_script()
        self.assertNotEqual(result.returncode, 0)
        self.assertEqual(self.calls(), [])
        self.assertEqual((self.root / '.env.image').read_bytes(), self.old_version)

    def test_internal_probe_failures_roll_back(self):
        for case in ['not-running', 'actuator-down', 'host-health-failure']:
            with self.subTest(case=case):
                self.set_case(case)
                result = self.run_script()
                self.assertNotEqual(result.returncode, 0, result.stdout + result.stderr)
                self.assertEqual(json.loads(self.mock_state.read_text())['active_image'], OLD)
                self.assertEqual((self.root / '.env.image').read_bytes(), self.old_version)
                self.assertEqual(json.loads((self.state / 'failures/requests/55.json').read_text())['status'], 'rolled_back')
                shutil.rmtree(self.state / 'backups')
                shutil.rmtree(self.state / 'failures')

    def test_external_only_failure_does_not_roll_back(self):
        for case in ['external-network-failure', 'external-http-failure']:
            with self.subTest(case=case):
                self.set_case(case)
                result = self.run_script()
                self.assertEqual(result.returncode, 0, result.stdout + result.stderr)
                self.assertEqual(json.loads((self.state / 'current.json').read_text())['externalHealth'], 'degraded')
                self.assertEqual(json.loads(self.mock_state.read_text()).get('rollback_count', 0), 0)
                self.assertEqual(json.loads(self.mock_state.read_text())['active_image'], NEW)
                shutil.rmtree(self.state / 'backups')
                shutil.rmtree(self.state / 'digests')
                (self.state / 'current.json').unlink()
                (self.root / '.env.image').write_bytes(self.old_version)

    def test_first_local_rollback_restores_absent_version_file(self):
        (self.root / '.env.image').unlink()
        self.set_case('replace-failure')
        result = self.run_script()
        self.assertNotEqual(result.returncode, 0)
        self.assertFalse((self.root / '.env.image').exists())
        self.assertEqual(json.loads(self.mock_state.read_text())['active_image'], OLD)
        self.assertFalse((self.state / 'pending.json').exists())
        self.assertFalse((self.state / 'current.json').exists())

    def test_ghcr_rollback_preserves_previous_success_exactly(self):
        old_ghcr = b'BACKEND_IMAGE=ghcr.io/coffiiness/calfit-backend@sha256:' + b'e' * 64 + b'\n'
        (self.root / '.env.image').write_bytes(old_ghcr)
        self.previous()
        previous = (self.state / 'current.json').read_bytes()
        self.set_case('actuator-down')
        result = self.run_script()
        self.assertNotEqual(result.returncode, 0)
        self.assertEqual((self.state / 'current.json').read_bytes(), previous)
        self.assertEqual((self.root / '.env.image').read_bytes(), old_ghcr)
        self.assertEqual(len([c for c in self.calls() if c[0] == 'docker' and 'pull' in c]), 1)

    def test_rollback_failure_and_bad_recovery_health_block_next_run(self):
        for case in ['rollback-failure', 'rollback-unhealthy']:
            with self.subTest(case=case):
                self.set_case(case)
                self.assertNotEqual(self.run_script().returncode, 0)
                self.assertEqual(json.loads((self.state / 'pending.json').read_text())['status'], 'rollback_failed')
                self.assertFalse((self.state / 'current.json').exists())
                before = len(self.calls())
                self.assertNotEqual(self.run_script().returncode, 0)
                self.assertEqual(len(self.calls()), before)
                (self.state / 'pending.json').unlink()
                shutil.rmtree(self.state / 'backups')
                (self.root / '.env.image').write_bytes(self.old_version)

    def test_failed_digest_blocked_but_new_release_is_allowed(self):
        self.set_case('replace-failure')
        self.assertNotEqual(self.run_script().returncode, 0)
        before = len([c for c in self.calls() if c[0] == 'docker'])
        self.assertEqual(self.run_script().returncode, 0)
        self.assertEqual(len([c for c in self.calls() if c[0] == 'docker']), before)
        self.set_case('new')
        record = json.loads(self.mock_state.read_text())
        record['release'] = 2
        self.mock_state.write_text(json.dumps(record))
        result = self.run_script()
        self.assertEqual(result.returncode, 0, result.stdout + result.stderr)
        self.assertEqual(json.loads((self.state / 'current.json').read_text())['deploymentId'], 56)
        self.assertTrue((self.state / 'failures/requests/55.json').exists())

    def test_stale_previous_version_is_rejected_before_replacement(self):
        self.set_case('stale-version')
        self.assertNotEqual(self.run_script().returncode, 0)
        self.assertFalse(json.loads(self.mock_state.read_text()).get('applied'))
        self.assertEqual((self.root / '.env.image').read_bytes(), self.old_version)

    def test_killed_deployment_is_recovered_before_new_api_poll(self):
        self.set_case('process-kill')
        result = self.run_script()
        self.assertNotEqual(result.returncode, 0)
        self.assertTrue((self.state / 'pending.json').exists())
        record = json.loads(self.mock_state.read_text())
        record['case'] = 'api-error'
        record['calls'] = []
        self.mock_state.write_text(json.dumps(record))
        result = self.run_script()
        self.assertEqual(result.returncode, 0, result.stdout + result.stderr)
        self.assertFalse(any(c[0] == 'curl' and c[-1].startswith('https://api.github.com') for c in self.calls()))
        self.assertEqual((self.root / '.env.image').read_bytes(), self.old_version)
        self.assertFalse((self.state / 'pending.json').exists())
        self.assertEqual(json.loads(self.mock_state.read_text())['active_image'], OLD)

    def test_interrupted_success_finalization_restores_old_journal(self):
        self.previous()
        previous = (self.state / 'current.json').read_bytes()
        self.set_case('process-kill')
        self.assertNotEqual(self.run_script().returncode, 0)
        pending = json.loads((self.state / 'pending.json').read_text())
        pending['status'] = 'success'
        (self.state / 'current.json').write_text(json.dumps(pending))
        (self.state / 'digests' / (DIGEST[7:] + '.json')).write_text(json.dumps(pending))
        self.set_case('new')
        result = self.run_script()
        self.assertEqual(result.returncode, 0, result.stdout + result.stderr)
        self.assertEqual((self.state / 'current.json').read_bytes(), previous)
        self.assertFalse((self.state / 'digests' / (DIGEST[7:] + '.json')).exists())
        self.assertTrue((self.state / 'failures/requests/55.json').exists())

    def test_process_killed_during_rollback_is_recoverable(self):
        self.set_case('rollback-kill')
        self.assertNotEqual(self.run_script().returncode, 0)
        self.assertEqual(json.loads((self.state / 'pending.json').read_text())['status'], 'rolling_back')
        self.set_case('new')
        result = self.run_script()
        self.assertEqual(result.returncode, 0, result.stdout + result.stderr)
        self.assertFalse((self.state / 'pending.json').exists())
        self.assertEqual((self.root / '.env.image').read_bytes(), self.old_version)

    def test_prepared_interruption_keeps_existing_container(self):
        self.set_case('process-kill')
        self.assertNotEqual(self.run_script().returncode, 0)
        pending = json.loads((self.state / 'pending.json').read_text())
        pending['phase'] = 'prepared'
        (self.state / 'pending.json').write_text(json.dumps(pending))
        self.set_case('new')  # old container still running at the prepared checkpoint
        result = self.run_script()
        self.assertEqual(result.returncode, 0, result.stdout + result.stderr)
        self.assertFalse(any(c[0] == 'docker' and 'up' in c for c in self.calls()))
        self.assertEqual((self.root / '.env.image').read_bytes(), self.old_version)
        self.assertFalse((self.state / 'pending.json').exists())

    def test_manual_recover_after_blocked_rollback(self):
        self.set_case('rollback-failure')
        self.assertNotEqual(self.run_script().returncode, 0)
        self.assertEqual(json.loads((self.state / 'pending.json').read_text())['status'], 'rollback_failed')
        self.set_case('new')
        result = self.run_script('--recover')
        self.assertEqual(result.returncode, 0, result.stdout + result.stderr)
        self.assertFalse((self.state / 'pending.json').exists())
        self.assertFalse(any(c[0] == 'curl' and c[-1].startswith('https://api.github.com') for c in self.calls()))

    def test_missing_rollback_cache_never_pulls_old_image(self):
        self.set_case('missing-rollback-image')
        self.assertNotEqual(self.run_script().returncode, 0)
        self.assertEqual(json.loads((self.state / 'pending.json').read_text())['status'], 'rollback_failed')
        self.assertEqual(len([c for c in self.calls() if c[0] == 'docker' and 'pull' in c]), 1)

    def test_dry_pending_and_recover_without_pending_do_not_access_env(self):
        (self.root / '.env.prod').unlink()
        (self.state / 'pending.json').write_text('{"status":"rollback_failed"}')
        result = subprocess.run(['bash', str(SCRIPT), '--root', str(self.root), '--dry-run', '--recover'],
                                env=self.env, capture_output=True, text=True, timeout=30)
        self.assertEqual(result.returncode, 0, result.stderr)
        self.assertEqual(self.calls(), [])
        (self.state / 'pending.json').unlink()
        result = subprocess.run(['bash', str(SCRIPT), '--root', str(self.root), '--recover'],
                                env=self.env, capture_output=True, text=True, timeout=30)
        self.assertEqual(result.returncode, 0, result.stderr)
        self.assertEqual(self.calls(), [])

    def test_containerd_success_separates_config_manifest_and_local_id(self):
        self.configure_store('containerd')
        result = self.run_script()
        self.assertEqual(result.returncode, 0, result.stdout + result.stderr)
        current = json.loads((self.state / 'current.json').read_text())
        self.assertEqual(current['imageId'], NEW)
        self.assertEqual(current['configDigest'], NEW)
        self.assertEqual(current['digest'], DIGEST)
        self.assertEqual(current['manifestDigest'], DIGEST)
        self.assertEqual(current['localImageId'], DIGEST)
        self.assertEqual(json.loads(self.mock_state.read_text())['active_image'], DIGEST)
        self.assertFalse((self.state / 'pending.json').exists())

    def test_classic_success_keeps_config_based_local_id(self):
        result = self.run_script()
        self.assertEqual(result.returncode, 0, result.stdout + result.stderr)
        current = json.loads((self.state / 'current.json').read_text())
        self.assertEqual(current['imageId'], NEW)
        self.assertEqual(current['configDigest'], NEW)
        self.assertEqual(current['localImageId'], NEW)
        self.assertEqual(current['manifestDigest'], DIGEST)

    def test_observed_oracle_digests_with_containerd_store(self):
        identity = {
            'digest': 'sha256:a5161e6a1e0226856451573fc9f0babd6bf96b4aec1b10e5314a32f473201fdc',
            'config': 'sha256:0ed0373e7ae777ac1231f558d212140c3924220eb550a3ad9fb31803f4240f4f',
        }
        self.configure_store('containerd', identity_fixture=identity)
        result = self.run_script()
        self.assertEqual(result.returncode, 0, result.stdout + result.stderr)
        current = json.loads((self.state / 'current.json').read_text())
        self.assertEqual(current['configDigest'], identity['config'])
        self.assertEqual(current['imageId'], identity['config'])
        self.assertEqual(current['localImageId'], identity['digest'])

    def test_manifest_errors_and_unrelated_local_ids_fail_before_replacement(self):
        for store in ['classic', 'containerd']:
            for case in ['wrong-config-digest', 'missing-config-digest', 'manifest-index',
                         'manifest-error', 'unknown-manifest-digest', 'wrong-image-id',
                         'wrong-repodigest', 'wrong-architecture', 'wrong-revision']:
                with self.subTest(store=store, case=case):
                    self.set_case(case)
                    self.configure_store(store)
                    result = self.run_script()
                    self.assertNotEqual(result.returncode, 0, result.stdout + result.stderr)
                    self.assertFalse(json.loads(self.mock_state.read_text()).get('applied'))
                    self.assertEqual((self.root / '.env.image').read_bytes(), self.old_version)
                    self.assertFalse((self.state / 'pending.json').exists())

    def test_docker_schema2_manifest_is_supported(self):
        self.set_case('docker-schema2')
        result = self.run_script()
        self.assertEqual(result.returncode, 0, result.stdout + result.stderr)

    def test_containerd_rollback_uses_previous_local_id_and_preserves_state(self):
        old_local = 'sha256:' + '7' * 64
        self.previous(imageId=OLD, localImageId=old_local, configDigest=OLD,
                      manifestDigest=old_local, digest=old_local)
        previous = (self.state / 'current.json').read_bytes()
        self.set_case('actuator-down')
        self.configure_store('containerd', old_store='containerd')
        result = self.run_script()
        self.assertNotEqual(result.returncode, 0)
        self.assertEqual((self.state / 'current.json').read_bytes(), previous)
        self.assertEqual((self.root / '.env.image').read_bytes(), self.old_version)
        self.assertEqual(json.loads(self.mock_state.read_text())['active_image'], old_local)
        recovery = json.loads((self.state / 'backups/55/recovery.json').read_text())
        self.assertEqual(recovery['previousImageId'], old_local)
        failed = json.loads((self.state / 'failures/requests/55.json').read_text())
        self.assertEqual(failed['imageId'], NEW)
        self.assertEqual(failed['localImageId'], DIGEST)
        self.assertFalse((self.state / 'pending.json').exists())
        self.assertEqual(len([c for c in self.calls() if c[0] == 'docker' and 'pull' in c]), 1)

    def test_containerd_pending_identity_survives_interruption_and_recovery(self):
        self.set_case('process-kill')
        self.configure_store('containerd')
        self.assertNotEqual(self.run_script().returncode, 0)
        pending = json.loads((self.state / 'pending.json').read_text())
        self.assertEqual(pending['imageId'], NEW)
        self.assertEqual(pending['configDigest'], NEW)
        self.assertEqual(pending['manifestDigest'], DIGEST)
        self.assertEqual(pending['localImageId'], DIGEST)
        record = json.loads(self.mock_state.read_text())
        record.update(case='new', calls=[])
        self.mock_state.write_text(json.dumps(record))
        result = self.run_script('--recover')
        self.assertEqual(result.returncode, 0, result.stdout + result.stderr)
        self.assertEqual(json.loads(self.mock_state.read_text())['active_image'], OLD)
        self.assertFalse((self.state / 'pending.json').exists())


if __name__ == '__main__':
    unittest.main(verbosity=2)
