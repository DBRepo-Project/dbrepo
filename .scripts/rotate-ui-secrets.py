#!/usr/bin/env python3
"""Replace only the three UI session secrets in an existing protected .env file."""
import argparse
import base64
import os
from pathlib import Path
import secrets


def rotate(path):
    path = Path(path)
    if path.is_symlink():
        raise ValueError("Use the actual environment file, not a symlink")
    text = path.read_text()
    keys = ("NUXT_OIDC_TOKEN_KEY", "OIDC_AUTH_SESSION_SECRET", "OIDC_SESSION_SECRET")
    aliases = ("OIDC_TOKEN_KEY", "NUXT_OIDC_AUTH_SESSION_SECRET", "NUXT_OIDC_SESSION_SECRET")
    lines = [line for line in text.splitlines() if line.split("=", 1)[0].strip() not in keys + aliases]
    values = ("data:;base64," + base64.b64encode(secrets.token_bytes(32)).decode(),
              secrets.token_urlsafe(48), secrets.token_urlsafe(48))
    lines.extend(f"{key}={value}" for key, value in zip(keys, values))
    # Create beside the original so replacement is atomic and never opens the file to other users.
    temporary = path.with_name(path.name + ".ui-rotation-" + secrets.token_hex(8))
    try:
        with os.fdopen(os.open(temporary, os.O_WRONLY | os.O_CREAT | os.O_EXCL, 0o600), "w") as stream:
            stream.write("\n".join(lines) + "\n")
        os.replace(temporary, path)
    finally:
        temporary.unlink(missing_ok=True)


if __name__ == "__main__":
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("env_file", type=Path)
    args = parser.parse_args()
    rotate(args.env_file)
    print("UI secrets rotated. Recreate the UI service; existing sessions must sign in again.")
