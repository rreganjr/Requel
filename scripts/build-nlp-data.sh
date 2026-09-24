#!/usr/bin/env bash
#
# build-nlp-data.sh — package the NLP data tree into the versioned data jar that the
# build fetches from the nlp-data-<N> GitHub Release (issue #193). See doc/guides/NLP_DATA.md.
#
# Usage:  scripts/build-nlp-data.sh <source-dir> <N>
#
#   <source-dir>  a directory holding an nlp/ tree, laid out exactly as it is read from the
#                 classpath (nlp/dictionary/..., nlp/jwnl/wn30/..., ...). To change the data,
#                 unzip the current requel-nlp-data-<N>.jar into a directory, edit it, and point
#                 this script at that directory.
#   <N>           the new data version, a positive integer. Never reuse a published one.
#
# Writes tmp/requel-nlp-data-<N>.jar and prints its sha256, which goes into the root pom's
# requel.nlp-data.sha256 property. Entries are sorted and carry a fixed timestamp, so the jar's
# contents depend only on the input files. .gz files are stored, everything else is deflated.
# .DS_Store files are skipped. Needs python3 only (no JDK `jar` tool).
#
set -euo pipefail

usage() { sed -n '3,17p' "$0" | sed 's/^# \{0,1\}//' >&2; exit 2; }
[[ $# -eq 2 ]] || usage
SRC="$1"
N="$2"
[[ "$N" =~ ^[1-9][0-9]*$ ]] || { echo "ERROR: <N> must be a positive integer, got '$N'." >&2; exit 2; }
[[ -d "$SRC/nlp" ]] || { echo "ERROR: $SRC/nlp is not a directory." >&2; exit 2; }

ROOT="$(cd "$(dirname "$0")/.." && pwd)"
OUT="$ROOT/tmp/requel-nlp-data-$N.jar"
[[ -e "$OUT" ]] && { echo "ERROR: $OUT already exists; remove it or pick a new <N>." >&2; exit 1; }
mkdir -p "$ROOT/tmp"

python3 - "$SRC" "$N" "$OUT" <<'PY'
import os, sys, zipfile

src, n, out = sys.argv[1], sys.argv[2], sys.argv[3]
EPOCH = (1980, 1, 1, 0, 0, 0)

files, dirs = [], {"META-INF/"}
for base, subdirs, names in os.walk(os.path.join(src, "nlp")):
    subdirs.sort()
    rel_base = os.path.relpath(base, src).replace(os.sep, "/")
    dirs.add(rel_base + "/")
    for name in names:
        if name == ".DS_Store":
            continue
        files.append(rel_base + "/" + name)
files.sort()

def entry(name, is_dir):
    info = zipfile.ZipInfo(name, EPOCH)
    info.create_system = 3  # unix, so external_attr permissions are honoured
    info.external_attr = ((0o40755 << 16) | 0x10) if is_dir else (0o100644 << 16)
    return info

manifest = ("Manifest-Version: 1.0\r\n"
            "Created-By: scripts/build-nlp-data.sh\r\n"
            f"Requel-NLP-Data-Version: {n}\r\n\r\n")

with zipfile.ZipFile(out, "w") as jar:
    # The manifest first, as the JDK jar tool writes it.
    jar.writestr(entry("META-INF/", True), b"")
    info = entry("META-INF/MANIFEST.MF", False)
    info.compress_type = zipfile.ZIP_DEFLATED
    jar.writestr(info, manifest)
    # Directory entries, so ClassLoader.getResource("nlp/.../") resolves inside the jar.
    for d in sorted(dirs - {"META-INF/"}):
        jar.writestr(entry(d, True), b"")
    for f in files:
        info = entry(f, False)
        info.compress_type = zipfile.ZIP_STORED if f.endswith(".gz") else zipfile.ZIP_DEFLATED
        with open(os.path.join(src, f), "rb") as fh:
            jar.writestr(info, fh.read())

print(f"files:   {len(files)}")
print(f"dirs:    {len(dirs) - 1}")
PY

SIZE=$(wc -c < "$OUT" | tr -d ' ')
if command -v sha256sum >/dev/null; then SHA=$(sha256sum "$OUT" | cut -d' ' -f1); else SHA=$(shasum -a 256 "$OUT" | cut -d' ' -f1); fi
echo "jar:     ${OUT#"$ROOT"/}"
echo "bytes:   $SIZE"
echo "sha256:  $SHA"
