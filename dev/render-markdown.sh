#!/usr/bin/env bash
set -euo pipefail

source_file="${1:-}"
if [[ -z "$source_file" || ! -f "$source_file" ]]; then
  echo "Usage: task md -- <markdown-file>" >&2
  exit 2
fi

task --taskfile /home/will/Taskfile.yml md -- "$source_file"
base_name="${source_file##*/}"
html_file="/home/will/tmp/${base_name%.md}.html"
if [[ ! -s "$html_file" ]]; then
  echo "Markdown renderer did not create $html_file" >&2
  exit 1
fi

# The shared renderer writes into ~/tmp. Resolve repository links from the
# Markdown directory so screenshot/source links stay usable in the review page.
python3 - "$source_file" "$html_file" <<'PYLINKS'
from pathlib import Path
from urllib.parse import urlsplit, urlunsplit, unquote
import html
import re
import sys
source=Path(sys.argv[1]).resolve()
page=Path(sys.argv[2])
def rebase(match):
    value=html.unescape(match[2]);parts=urlsplit(value)
    if parts.scheme or parts.netloc or not parts.path or parts.path.startswith('/'):
        return match[0]
    target=(source.parent/unquote(parts.path)).resolve()
    if not target.is_file():return match[0]
    if target.suffix=='.md':
        rendered=page.parent/(target.stem+'.html')
        if rendered.exists() and rendered.stat().st_mtime>=target.stat().st_mtime:target=rendered
    absolute=urlsplit(target.as_uri())
    return match[1]+html.escape(urlunsplit((absolute.scheme,absolute.netloc,absolute.path,parts.query,parts.fragment)),quote=True)+match[3]
page.write_text(re.sub(r'(<a\b[^>]*\bhref=")([^"]+)(")',rebase,page.read_text()))
PYLINKS

fallback_pattern='<pre[^>]*>\s*<code[^>]*>\s*(flowchart|graph|sequenceDiagram|classDiagram|stateDiagram|erDiagram|journey|gantt|pie|mindmap|timeline|gitGraph|C4Context|C4Container|C4Component|requirementDiagram|quadrantChart|block-beta|packet-beta|kanban|sankey-beta|xychart-beta|architecture-beta)([[:space:]]|&)'
fixture="$(mktemp)"
trap 'rm -f "$fixture"' EXIT
printf '%s\n' '<pre><code>flowchart TD' 'A --> B' '</code></pre>' '<pre><code>pie title Example' '"A" : 1' '</code></pre>' > "$fixture"
if ! rg -U -q "$fallback_pattern" "$fixture"; then
  echo "Mermaid fallback detector self-check failed." >&2
  exit 1
fi

if rg -U -q "$fallback_pattern" "$html_file"; then
  echo "Mermaid fallback detected in $html_file; the diagram rendered as raw text." >&2
  echo "Fix the Mermaid renderer or replace the diagram with a local SVG before review." >&2
  exit 1
fi

xdg-open "$html_file"
