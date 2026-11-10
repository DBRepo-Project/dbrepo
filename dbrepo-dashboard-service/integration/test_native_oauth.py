import os
from pathlib import Path
import subprocess
import sys


def test_native_oauth(tmp_path):
    environment = dict(os.environ, DBREPO_NATIVE_FIXTURE_DIR=str(tmp_path))
    log = tmp_path / "result.log"
    with log.open("w") as output:
        process = subprocess.Popen([sys.executable, str(Path(__file__).with_name("native_oauth.py"))],
                                   env=environment, stdout=output, stderr=subprocess.STDOUT)
        try:
            assert process.wait(timeout=600) == 0, log.read_text()
        finally:
            if process.poll() is None:
                process.terminate()
                try:
                    process.wait(timeout=60)
                except subprocess.TimeoutExpired:
                    process.kill()
                    process.wait(timeout=5)
    assert '"refresh": true' in log.read_text()
    assert '"collisions_fail_closed": true' in log.read_text()
