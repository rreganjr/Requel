#!/usr/bin/env bash
# Check markdown links under doc/ (plus the three root documents) and report prose mentions
# of doc/ paths that do not resolve.
#
# Two passes, deliberately different:
#
#   links  --  [text](target). These must resolve. A broken one fails the run (exit 1).
#   prose  --  a bare or backticked doc/....md inside a sentence. Informational, never fatal:
#              it may be an instruction, a conditional, or a reference to another branch, and
#              work artifacts are never retro-edited to make old prose resolve. Known-and-
#              intended ones are listed in scripts/doc-link-allow.tsv.
#
# No gh, no token, no network -- unlike gen-doc-index.sh, which calls this at the end.
#
#   ./scripts/check-doc-links.sh            # report and exit non-zero on a broken link
#   ./scripts/check-doc-links.sh --quiet    # print only when something is wrong
set -euo pipefail
root="$(git rev-parse --show-toplevel)"; cd "$root"

QUIET=0
for a in "$@"; do
  case "$a" in
    --quiet) QUIET=1 ;;
    -h|--help) sed -n '2,15p' "$0"; exit 0 ;;
    *) echo "!! unknown argument: $a" >&2; exit 2 ;;
  esac
done

python3 - "$QUIET" <<'PY'
import os, re, subprocess, sys
from urllib.parse import unquote

quiet = sys.argv[1] == '1'

ROOT_DOCS = ['README.md', 'RELEASE.md', 'CLAUDE.md']
ALLOW = 'scripts/doc-link-allow.tsv'

fence = re.compile(r'^\s{0,3}(```|~~~)')
code_span = re.compile(r'`[^`\n]*`')
link = re.compile(r'!?\[[^\]]*\]\(([^)\s]+)(?:\s+"[^"]*")?\)')
# Prose: only paths that literally start with doc/. Anything wider drowns the list in
# bare-name citations of files that do exist (AUTH_ARCH.md), template placeholders
# (<issue>-<slug>-plan.md) and shell fragments (ls doc/*.md).
prose = re.compile(r'(?<![\w/])(doc/[\w./+-]+\.md)')

def files():
    for dp, dn, fn in os.walk('doc'):
        dn.sort()
        for f in sorted(fn):
            if f.endswith('.md'):
                yield os.path.join(dp, f)
    for f in ROOT_DOCS:
        if os.path.exists(f):
            yield f

def normalize(target):
    """Strip a fragment, the ./Requel/ prefix that Codex-style citations carry, and a
    trailing :<line> or :<line>:<col>."""
    t = unquote(target).split('#')[0]
    if t.startswith('./Requel/'):
        t = t[len('./Requel/'):]
    return re.sub(r':\d+(:\d+)?$', '', t)

def resolve(citing, target):
    """Return the resolved path, or None."""
    d = os.path.dirname(citing)
    for c in (os.path.join(d, target), target):
        c = os.path.normpath(c)
        if os.path.exists(c):
            return c
    return None

def resolves(citing, target):
    return resolve(citing, target) is not None

# A link that resolves only in the working tree is broken for everyone who clones. Reported
# informationally rather than fatally, because a generated file can legitimately be ahead of
# the index mid-change.
tracked = set(subprocess.run(['git', 'ls-files', '-z'], capture_output=True, text=True,
                             check=True).stdout.split('\0'))

allow, allow_seen = {}, set()
if os.path.exists(ALLOW):
    for line in open(ALLOW, encoding='utf-8'):
        line = line.rstrip('\n')
        if not line.strip() or line.lstrip().startswith('#'):
            continue
        parts = [p for p in line.split('\t') if p.strip()]
        if len(parts) < 2:
            continue
        allow[(parts[0].strip(), parts[1].strip())] = (parts[2].strip() if len(parts) > 2 else '')

broken, mentions, untracked, suppressed = [], [], [], 0
nlinks = nfiles = 0

for p in files():
    nfiles += 1
    infence = False
    with open(p, encoding='utf-8', errors='replace') as fh:
        for i, line in enumerate(fh, 1):
            if fence.match(line):
                infence = not infence
                continue
            if infence:
                continue

            # links: an inline code span is a rendering of a link, not a link
            for m in link.finditer(code_span.sub(lambda s: ' ' * len(s.group(0)), line)):
                raw = m.group(1)
                if raw.startswith(('http://', 'https://', 'mailto:', '#')):
                    continue
                nlinks += 1
                t = normalize(raw)
                if not t:
                    continue
                hit = resolve(p, t)
                if hit is None:
                    broken.append((p, i, raw))
                elif os.path.isfile(hit) and hit not in tracked:
                    untracked.append((p, i, hit))

            # prose: a backticked path in running text is exactly the case this pass is for
            targets = {m.group(1) for m in link.finditer(line)}
            for m in prose.finditer(line):
                t = m.group(1)
                if t in targets or resolves(p, t) or os.path.exists(t):
                    continue
                if (p, t) in allow:
                    allow_seen.add((p, t))
                    suppressed += 1
                    continue
                mentions.append((p, i, t))

stale = sorted(k for k in allow if k not in allow_seen)
out = []
out.append('>> %d markdown links in %d files' % (nlinks, nfiles))
if broken:
    out.append('!! %d broken markdown link(s):' % len(broken))
    out += ['     %s:%d -> %s' % b for b in broken]
if mentions:
    out.append('-- %d unresolvable prose mention(s) (informational):' % len(mentions))
    out += ['     %s:%d -> %s' % m for m in mentions]
if untracked:
    out.append('-- %d link(s) resolve only in the working tree (untracked or ignored — '
               'broken for anyone who clones):' % len(untracked))
    out += ['     %s:%d -> %s' % u for u in untracked]
if suppressed:
    out.append('-- %d prose mention(s) suppressed by %s' % (suppressed, ALLOW))
if stale:
    out.append('-- %d allowlist entr(y/ies) matched nothing and can be removed:' % len(stale))
    out += ['     %s -> %s' % s for s in stale]
if not broken and not mentions and not untracked:
    out.append('>> no broken links')

if broken or not quiet:
    print('\n'.join(out), file=sys.stderr)
sys.exit(1 if broken else 0)
PY
