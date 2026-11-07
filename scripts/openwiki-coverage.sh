#!/usr/bin/env bash
# Lists what openwiki/INSTRUCTIONS.md asks the wiki to cover but no wiki page mentions (ADR-0015).
#
# The brief names what matters: classes in backticks (`MonthlyPlanRepository`), class members
# (`AdvisorTools.upcomingRaces`, where the member must appear) and ADRs (ADR-0007). Each must appear
# in at least one generated page. Exits 1 and prints the missing terms when any is missing, so it can
# run after every wiki update, by hand, from Claude Code or in CI.
#
# Usage: scripts/openwiki-coverage.sh [wiki-directory]   (default: openwiki)
set -euo pipefail

wiki="${1:-openwiki}"
brief="$wiki/INSTRUCTIONS.md"
[ -f "$brief" ] || { echo "no $brief" >&2; exit 2; }

pages=$(find "$wiki" -name '*.md' ! -path "$brief" ! -path "$wiki/.*" | sort)
[ -n "$pages" ] || { echo "no generated pages under $wiki/" >&2; exit 2; }

# Backticked ClassName or ClassName.member, and ADR-NNNN anywhere in the brief.
terms=$( { grep -oE '`[A-Z][A-Za-z0-9]+(\.[a-z][A-Za-z0-9]*)?`' "$brief" | tr -d '`'
           grep -oE 'ADR-[0-9]{4}' "$brief"; } | sort -u)

missing=()
while IFS= read -r term; do
  # For Class.member, the member is what the page has to explain.
  needle="${term##*.}"
  # shellcheck disable=SC2086
  grep -qF -- "$needle" $pages || missing+=("$term")
done <<< "$terms"

total=$(wc -l <<< "$terms")
if [ "${#missing[@]}" -eq 0 ]; then
  echo "OpenWiki covers all $total terms named in $brief."
  exit 0
fi
echo "OpenWiki does not mention ${#missing[@]} of $total terms named in $brief:"
printf '  %s\n' "${missing[@]}"
exit 1
