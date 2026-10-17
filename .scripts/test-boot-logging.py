#!/usr/bin/env python3
"""Check logging from packaged Boot jars without starting application services."""
import json
import os
import shutil
from pathlib import Path
import subprocess
import sys
import tempfile
import zipfile

probe = Path(__file__).with_name("LoggingRuntimeSmoke.java")
for filename in sys.argv[1:]:
    with zipfile.ZipFile(filename) as archive, tempfile.TemporaryDirectory() as temporary:
        # Retain the packaged classpath order, including the AWS bundle's embedded SLF4J.
        entries = [json.loads(line.removeprefix("- "))
                   for line in archive.read("BOOT-INF/classpath.idx").decode().splitlines()]
        selected = [entry for entry in entries
                    if Path(entry).name.startswith(("slf4j-api-", "logback-", "bundle-"))]
        assert any(Path(entry).name.startswith("slf4j-api-") for entry in selected), filename
        classpath = []
        for entry in selected:
            target = Path(temporary) / Path(entry).name
            with archive.open(entry) as source, target.open("wb") as output:
                shutil.copyfileobj(source, output)
            classpath.append(str(target))
        subprocess.run(["java", "-cp", os.pathsep.join(classpath), str(probe)], check=True)
        print("Packaged logging passed:", filename, flush=True)
