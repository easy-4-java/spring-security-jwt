"""Run verify after removing checkout artifacts; expose only this invocation's reports."""
import argparse
import json
import os
from pathlib import Path
import shutil
import subprocess


def clean_target(root):
    target = root / "target"
    if target.is_symlink():
        target.unlink()
    elif target.exists():
        shutil.rmtree(target)


def run_verify(root, command):
    clean_target(root)
    # A failed Maven invocation must retain its exit status even if it wrote reports.
    try:
        status = subprocess.run(command, cwd=root, check=False).returncode
    except OSError as error:
        print(error)
        status = 127
    reports = {
        "surefire": any((root / "target/surefire-reports").glob("TEST-*.xml")),
        "jacoco": (root / "target/site/jacoco/jacoco.xml").is_file(),
    }
    output = os.environ.get("GITHUB_OUTPUT")
    if output:
        with open(output, "a", encoding="utf-8") as stream:
            for name, present in reports.items():
                stream.write(f"{name}={str(present).lower()}\n")
    for name, present in reports.items():
        if present:
            directory = root / ("target/surefire-reports" if name == "surefire" else "target/site/jacoco")
            (directory / "ci-origin.json").write_text(json.dumps({
                "sha": os.environ.get("GITHUB_SHA"),
                "run_id": os.environ.get("GITHUB_RUN_ID"),
                "run_attempt": os.environ.get("GITHUB_RUN_ATTEMPT"),
                "verify_exit_code": status,
            }) + "\n", encoding="utf-8")
    return status if status >= 0 else 128 - status


if __name__ == "__main__":
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--clean-only", action="store_true")
    args = parser.parse_args()
    root = Path.cwd()
    if args.clean_only:
        clean_target(root)
    else:
        raise SystemExit(run_verify(root, ["mvn", "-B", "--no-transfer-progress", "-s", ".mvn/ci-settings.xml", "clean", "verify"]))
