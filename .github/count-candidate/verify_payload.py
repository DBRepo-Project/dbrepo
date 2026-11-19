import hashlib
import io
import sys
from zipfile import ZipFile


def digest(archive, name):
    with archive.open(name) as stream:
        return hashlib.file_digest(stream, "sha256").digest()


def changed_files(original, candidate, prefix=""):
    with ZipFile(original) as before, ZipFile(candidate) as after:
        old = {entry.filename for entry in before.infolist() if not entry.is_dir()}
        new = {entry.filename for entry in after.infolist() if not entry.is_dir()}
        for name in sorted(old ^ new):
            yield prefix + name
        for name in sorted(old & new):
            if digest(before, name) == digest(after, name):
                continue
            if name.endswith(".jar"):
                yield from changed_files(
                    io.BytesIO(before.read(name)), io.BytesIO(after.read(name)), prefix + name + "!/"
                )
            else:
                yield prefix + name


if __name__ == "__main__":
    endpoints = "BOOT-INF/classes/at/ac/tuwien/ifs/dbrepo/endpoints/"
    services = "BOOT-INF/lib/services-1.14.1.jar!/at/ac/tuwien/ifs/dbrepo/"
    expected = {
        endpoints + "TableEndpoint.class",
        endpoints + "ViewEndpoint.class",
        services + "mapper/MariaDbMapper.class",
        services + "service/impl/ViewServiceMariaDbImpl.class",
    }
    actual = set(changed_files(sys.argv[1], sys.argv[2]))
    if actual != expected:
        raise SystemExit(f"Unexpected or missing payload differences: {sorted(actual ^ expected)[:30]}")
    print("Changed files in the released application payload:")
    print("\n".join(sorted(actual)))
