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
image='ghcr.io/coffiiness/calfit-backend@'+digest
s['calls'].append([tool]+args)
def emit(value=None,code=0):
    p.write_text(json.dumps(s))
    if value is not None: print(json.dumps(value) if isinstance(value,(dict,list)) else value)
    sys.exit(code)
if tool=='curl':
    assert not any(x in args for x in ['--request','-X','--data','--data-binary']), 'GET only'
    url=args[-1]; base='https://api.github.com/repos/coffiiness/BE/'
    assert url.startswith(base)
    endpoint=url[len(base):]
    n=s.setdefault('requests',{}).get(endpoint,0)+1; s['requests'][endpoint]=n
    if case in ['api-error','rate-limit','network-error']: emit(code=22 if case!='network-error' else 7)
    if case=='api-after-pull' and n>1: emit(code=22)
    if endpoint.startswith('deployments?'):
        payload={'schemaVersion':1,'repository':'coffiiness/BE','image':'ghcr.io/coffiiness/calfit-backend',
                 'commitSha':sha,'digest':digest,'imageId':new,'workflowRunId':100,'workflowRunAttempt':1}
        if case=='bad-digest': payload['digest']='sha256:not-a-digest'
        if case=='injection': payload['commitSha']='$(touch /tmp/calfit-must-never-execute)'
        if case=='wrong-repository': payload['repository']='someone/BE'
        if case=='bad-payload': payload='echo malicious'
        d={'id':55,'environment':'oracle-production','task':'deploy','production_environment':True,
           'repository_url':base.rstrip('/'),'creator':{'login':'github-actions[bot]','type':'Bot'},
           'sha':sha,'ref':sha,'payload':payload}
        if case=='wrong-creator': d['creator']['login']='someone'
        if case=='request-changed' and n>1: d['id']=56
        emit([] if case=='no-request' else [d])
    if endpoint=='git/ref/heads/main': emit({'object':{'sha':'f'*40 if case=='stale-main' or (case=='main-advanced' and n>1) else sha}})
    if endpoint=='actions/workflows/ci.yml': emit({'id':20,'name':'Backend CI/CD','path':'.github/workflows/ci.yml'})
    if endpoint=='actions/runs/100':
        run={'id':100,'workflow_id':20,'run_attempt':1,'name':'Backend CI/CD','path':'.github/workflows/ci.yml',
             'event':'push','head_branch':'main','head_sha':sha,'repository':{'full_name':'coffiiness/BE'},
             'head_repository':{'full_name':'coffiiness/BE'},'status':'completed','conclusion':'success'}
        if case=='failed-workflow': run['conclusion']='failure'
        if case=='in-progress': run['status']='in_progress'; run['conclusion']=None
        if case=='wrong-event': run['event']='pull_request'
        if case=='wrong-run-sha': run['head_sha']='f'*40
        if case=='wrong-workflow': run['workflow_id']=99
        if case=='new-attempt' and n>1: run['run_attempt']=2
        emit(run)
    if endpoint.startswith('actions/runs/100/attempts/1/jobs?'):
        names=['Quality Gate and verified bootJar (AMD64)','Build and verify the ARM64 runtime','Publish the tested image to GHCR']
        jobs=[{'name':name,'run_id':100,'head_sha':sha,'status':'completed','conclusion':'success'} for name in names]
        if case=='skipped-quality': jobs[0]['conclusion']='skipped'
        emit({'total_count':3,'jobs':jobs})
    if endpoint.startswith('deployments/55/statuses?'): emit([{'state':'failure'}] if case=='cancelled-request' else [])
    if endpoint.startswith('compare/'): emit({'status':'behind' if case=='main-backwards' else 'diverged' if case=='main-diverged' else 'ahead'})
    raise AssertionError('Unexpected API path: '+endpoint)
if tool=='docker':
    assert args[:2]==['--host','unix:///var/run/docker.sock']
    assert 'DOCKER_CONTEXT' not in os.environ and 'DOCKER_HOST' not in os.environ
    args=args[2:]
    assert not any(x in args for x in ['down','prune','rm','build','login','push'])
    if args[0]=='compose':
        assert os.environ['BACKEND_IMAGE']==image and os.environ['JPA_DDL_AUTO']=='validate'
        assert 'COMPOSE_FILE' not in os.environ
        action=args[args.index('-f')+2:]
        if action[0]=='config':
            emit({'services':{'core-api':{'image':image,'platform':'linux/arm64','environment':{
                'SPRING_PROFILES_ACTIVE':'prod','SPRING_JPA_HIBERNATE_DDLAUTO':'update' if case=='bad-config' else 'validate'}},
                'mysql':{'image':'mysql:8.4.13'}},'volumes':{'mysql_data':{'external':True,'name':'calfit_oracle_mysql84'}}})
        if action[:2]==['ps','-q']: emit('mysql-container' if action[2]=='mysql' else 'new-api' if s.get('applied') else 'old-api')
        if action[0]=='up':
            assert action==['up','-d','--no-deps','--no-build','--pull','never','--wait','--wait-timeout','600','core-api']
            s['applied']=True
            emit(code=1 if case=='replace-failure' else 0)
    if args[0]=='pull':
        assert args==['pull','--platform','linux/arm64',image]
        emit(code=1 if case=='pull-failure' else 0)
    if args[:2]==['image','tag']:
        assert args[2]==old; s['rollbackTag']=args[3]; emit()
    if args[:2]==['image','inspect']:
        if '--format' in args: emit(old)
        emit([{'Id':'sha256:'+'e'*64 if case=='wrong-image-id' else new,'Os':'linux',
              'Architecture':'amd64' if case=='wrong-architecture' else 'arm64',
              'RepoDigests':[] if case=='wrong-repodigest' else [image],
              'Config':{'Labels':{'org.opencontainers.image.source':'https://github.com/coffiiness/BE',
                  'org.opencontainers.image.revision':'f'*40 if case=='wrong-revision' else sha,
                  'com.calfit.java.runtime':'17.0.21.1+2'}}}])
    if args[0]=='inspect':
        container=args[1]; fmt=args[args.index('--format')+1]
        if fmt=='{{.Image}}': emit(new if container=='new-api' else old)
        if fmt=='{{.State.Running}}': emit('true')
        if fmt=='{{.State.Health.Status}}': emit('unhealthy' if case=='unhealthy' and container=='new-api' else 'healthy')
        if fmt=='{{.State.StartedAt}}': emit('changed' if case=='mysql-changed' and s.get('applied') else 'unchanged')
        if '.Mounts' in fmt: emit('unexpected-volume' if case=='wrong-volume' else 'calfit_oracle_mysql84')
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
                self.assertEqual(json.loads((self.state / 'pending.json').read_text())['status'], 'failed')
                self.assertTrue((self.state / 'backups/55/recovery.env').exists())
                before = len(self.calls())
                self.assertNotEqual(self.run_script().returncode, 0)
                self.assertEqual(len(self.calls()), before)
                shutil.rmtree(self.state / 'backups')
                (self.state / 'pending.json').unlink()

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


if __name__ == '__main__':
    unittest.main(verbosity=2)
