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
