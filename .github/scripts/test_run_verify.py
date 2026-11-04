"""Synthetic report-provenance tests; these do not stand in for Maven tests."""
import importlib.util
import os
from pathlib import Path
import sys
import tempfile
import unittest
from unittest.mock import patch

spec = importlib.util.spec_from_file_location("run_verify", Path(__file__).with_name("run_verify.py"))
verify = importlib.util.module_from_spec(spec)
spec.loader.exec_module(verify)


class ReportProvenanceTest(unittest.TestCase):
    def exercise(self, exit_code, generate):
        with tempfile.TemporaryDirectory() as temporary:
            root = Path(temporary)
            for path in ["target/surefire-reports/TEST-old.xml", "target/site/jacoco/jacoco.xml"]:
                old = root / path
                old.parent.mkdir(parents=True, exist_ok=True)
                old.write_text("old Mac / Jackson 2.17 report")
            output = root / "outputs"
            command = [sys.executable, "-c", (
                "from pathlib import Path; "
                "assert not Path('target').exists(); "
                + ("p=Path('target/surefire-reports/TEST-current.xml'); p.parent.mkdir(parents=True); p.write_text('<testsuite tests=\"1\"/>'); "
                   "p=Path('target/site/jacoco/jacoco.xml'); p.parent.mkdir(parents=True); p.write_text('<report/>'); " if generate else "")
                + f"raise SystemExit({exit_code})"
            )]
            with patch.dict(os.environ, {"GITHUB_OUTPUT": str(output), "GITHUB_SHA": "current-sha"}):
                self.assertEqual(exit_code, verify.run_verify(root, command))
            expected = str(generate).lower()
            self.assertEqual(f"surefire={expected}\njacoco={expected}\n", output.read_text())
            self.assertFalse((root / 'target/surefire-reports/TEST-old.xml').exists())
            for path in (root / 'target').rglob('*'):
                if path.is_file():
                    self.assertNotIn('Jackson 2.17', path.read_text())
            if generate:
                self.assertIn('current-sha', (root / 'target/surefire-reports/ci-origin.json').read_text())

    def test_success_uploads_only_current_reports(self):
        self.exercise(0, True)

    def test_failure_retains_failure_and_current_reports(self):
        self.exercise(1, True)

    def test_failure_before_tests_does_not_upload_old_reports(self):
        self.exercise(2, False)

    def test_skipped_project_build_has_no_reports_or_output(self):
        with tempfile.TemporaryDirectory() as temporary:
            root = Path(temporary)
            (root / 'target/surefire-reports').mkdir(parents=True)
            (root / 'target/surefire-reports/TEST-old.xml').write_text('old Mac / Jackson 2.17')
            verify.clean_target(root)
            self.assertFalse((root / 'target').exists())
            self.assertFalse((root / 'outputs').exists())


if __name__ == '__main__':
    unittest.main()
