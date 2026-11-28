"""Validate the manual migration only in a labeled, disposable, local tmpfs MySQL.

Never accepts a network DB address or production credentials. No existing table,
database or volume is deleted. Creates a new schema-only QA copy; refuses reuse.
"""
import argparse
import json
from pathlib import Path
import subprocess


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--container", required=True)
    parser.add_argument("--docker-config", required=True)
    args = parser.parse_args()
    docker = ["docker", "--config", args.docker_config]
    if args.container != "calfit-membership-qa-20261128":
        raise ValueError("Only the explicitly created single-workspace QA container is allowed")
    info = json.loads(subprocess.check_output(docker + ["inspect", args.container], text=True))[0]
    assert info["Config"]["Labels"].get("calfit.qa") == "single-active-workspace"
    assert "/var/lib/mysql" in info["HostConfig"].get("Tmpfs", {})
    assert not any(m["Type"] in ("volume", "bind") for m in info["Mounts"])
    assert not info["HostConfig"].get("Privileged")
    source, target = "calfit_membership_qa", "calfit_membership_qa_migration"

    def sql(query, database=source, succeeds=True):
        # Synthetic password belongs only to this freshly created QA container.
        command = docker + ["exec", "-i", "-e", "MYSQL_PWD=synthetic-membership-root",
                            args.container, "mysql", "-uroot", "--batch", "--raw",
                            "--skip-column-names", database]
        result = subprocess.run(command, input=query, text=True, capture_output=True, encoding="utf-8")
        if succeeds and result.returncode:
            raise AssertionError(result.stderr)
        if not succeeds:
            assert result.returncode != 0, "Expected migration to reject duplicate ACTIVE memberships"
            assert "Duplicate ACTIVE memberships" in result.stderr, result.stderr
        return result.stdout

    assert sql("SELECT VERSION();").strip() == "8.4.13"
    before = sql("SELECT COUNT(*),SUM(status='DELETED') FROM workspace_members;").strip()
    sql(f"CREATE DATABASE {target};")  # No IF NOT EXISTS: never reuses an existing schema.
    tables = sql("SHOW TABLES;").splitlines()
    for table in tables:
        if table == "google_calendar_oauth_states":
            continue
        assert table.replace("_", "").isalnum()
        ddl = sql(f"SHOW CREATE TABLE `{table}`;").split("\t", 1)[1].rstrip()
        sql("SET FOREIGN_KEY_CHECKS=0;\n" + ddl + ";", target)
    sql("ALTER TABLE workspace_members DROP INDEX uq_workspace_members_active_user, DROP COLUMN active_user_id;", target)
    # Only synthetic migration fixtures in the newly created copy.
    sql("""INSERT INTO users(id,email,password,name,role,status)
           VALUES(9223372036854000,'schema-only@mock.invalid','synthetic','Schema QA','MEMBER','ACTIVE');
           INSERT INTO workspaces(workspace_id,name,status) VALUES('schema-qa','Schema QA','ACTIVE');
           INSERT INTO workspace_members(user_id,tenant_id,member_type,status)
           VALUES(9223372036854000,'schema-qa','HR','ACTIVE'),
                 (9223372036854000,'schema-qa','HR','ACTIVE');""", target)
    root = Path(__file__).resolve().parents[2]
    migration = (root / "deploy/oracle/sql/single-active-workspace-apply.sql").read_text(encoding="utf-8")
    sql(migration, target, succeeds=False)
    assert sql("SELECT COUNT(*) FROM information_schema.columns WHERE table_schema=DATABASE() AND table_name='workspace_members' AND column_name='active_user_id';", target).strip() == "0"
    sql("DROP PROCEDURE calfit_require_single_active_membership;", target)
    # Simulate an approved resolution by soft deletion, retaining BOTH synthetic histories.
    sql("UPDATE workspace_members SET status='DELETED' WHERE user_id=9223372036854000;", target)
    sql("INSERT INTO workspace_members(user_id,tenant_id,member_type,status) VALUES(9223372036854000,'schema-qa','HR','ACTIVE');", target)
    sql(migration, target)
    assert sql("SELECT COUNT(*),SUM(status='DELETED'),COUNT(active_user_id) FROM workspace_members;", target).strip() == "3\t2\t1"
    column = sql("SELECT extra,generation_expression FROM information_schema.columns WHERE table_schema=DATABASE() AND table_name='workspace_members' AND column_name='active_user_id';", target)
    assert "VIRTUAL GENERATED" in column and "ACTIVE" in column
    assert sql("SELECT non_unique FROM information_schema.statistics WHERE table_schema=DATABASE() AND table_name='workspace_members' AND index_name='uq_workspace_members_active_user';", target).strip() == "0"
    assert sql("SELECT COUNT(*),SUM(status='DELETED') FROM workspace_members;").strip() == before
    print("PASS: duplicate guard, migration, generated column, UNIQUE, 2 DELETED histories, original QA rows preserved")
    print(f"Preserved migration schema for JPA validate: {target}")


if __name__ == "__main__":
    main()
