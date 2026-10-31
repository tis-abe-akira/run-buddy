#!/usr/bin/env bash
# Mutation check for TransactionalPortRulesTest: proves the ArchUnit rule catches what it claims to.
#
# In a throwaway git worktree of HEAD, it plants code that breaks the rule (mutants) and code that
# must stay allowed, runs the rule's test, and checks the report:
#   - every mutant is reported by name ("killed"), so the rule still catches violations
#   - the allowed code is not reported, so the rule does not cry wolf
#   - the failure comes from the rule, not from a compile error or another test
# Exit 0 = the rule works. Anything else = the rule (or this check) needs attention.
#
# It checks the committed HEAD, not uncommitted changes. Usage: backend/scripts/archunit-mutation-check.sh
set -euo pipefail

repo="$(git -C "$(dirname "$0")" rev-parse --show-toplevel)"
work="$(mktemp -d)"
cleanup() { git -C "$repo" worktree remove --force "$work" >/dev/null 2>&1 || rm -rf "$work"; }
trap cleanup EXIT

git -C "$repo" worktree add --detach --quiet "$work" HEAD
backend="$work/backend"
main="$backend/src/main/java/com/example/runbuddy"

# Adds a member just before the interface's closing brace.
add_member() {
  local file="$1" member="$2"
  MEMBER="$member" perl -0pi -e 's/\}\s*\z/\n$ENV{MEMBER}\n}\n/' "$file"
}

# --- Mutants: each must be reported ---------------------------------------------------------
# 1, 2. A default method on each existing repository port (the getOrEmpty bug of ADR-0007).
add_member "$main/plan/MonthlyPlanRepository.java" '    default void mutantDefaultMethod() {
    }'
add_member "$main/activity/RunResultRepository.java" '    default void mutantDefaultMethod() {
    }'
# 3. A new port, implemented by an adapter that is @Transactional on a method only: the rule must
#    find ports it was never told about.
mkdir -p "$main/mutant"
cat > "$main/mutant/MutantPort.java" <<'JAVA'
package com.example.runbuddy.mutant;

public interface MutantPort {

    String find();

    default String mutantDefaultMethod() {
        return find();
    }
}
JAVA
cat > "$main/mutant/MutantAdapter.java" <<'JAVA'
package com.example.runbuddy.mutant;

import org.springframework.transaction.annotation.Transactional;

public class MutantAdapter implements MutantPort {

    @Override
    @Transactional(readOnly = true)
    public String find() {
        return "";
    }
}
JAVA

# --- Allowed code: must NOT be reported -----------------------------------------------------
# A static method does not go through the proxy, so it is allowed.
add_member "$main/plan/MonthlyPlanRepository.java" '    static void allowedStaticMethod() {
    }'

# --- Run the rule ---------------------------------------------------------------------------
echo "Running TransactionalPortRulesTest against the mutants..."
set +e
(cd "$backend" && ./mvnw -B -q test -Dtest=TransactionalPortRulesTest -Dsurefire.failIfNoSpecifiedTests=false) \
  > "$work/mvn.log" 2>&1
status=$?
set -e

report="$backend/target/surefire-reports/com.example.runbuddy.architecture.TransactionalPortRulesTest.txt"
fail() { echo "::error::$1"; echo "--- Maven output ---"; tail -60 "$work/mvn.log"; exit 1; }

[ -f "$report" ] || fail "The test did not run (compile error?), so the mutants prove nothing."
[ "$status" -ne 0 ] || fail "The rule passed with mutants in place: it no longer catches default methods."
grep -q "Tests run: 3, Failures: 1, Errors: 0" "$report" \
  || fail "Expected exactly the rule test to fail. Report: $(head -5 "$report")"

survived=0
for mutant in \
  com.example.runbuddy.plan.MonthlyPlanRepository.mutantDefaultMethod \
  com.example.runbuddy.activity.RunResultRepository.mutantDefaultMethod \
  com.example.runbuddy.mutant.MutantPort.mutantDefaultMethod; do
  if grep -qF "$mutant() is a default method" "$report"; then
    echo "killed:   $mutant"
  else
    echo "SURVIVED: $mutant"
    survived=1
  fi
done

if grep -qF "allowedStaticMethod" "$report"; then
  echo "FALSE ALARM: allowedStaticMethod was reported"
  survived=1
else
  echo "allowed:  com.example.runbuddy.plan.MonthlyPlanRepository.allowedStaticMethod"
fi

[ "$survived" -eq 0 ] || fail "The rule missed a mutant or reported allowed code (see above)."
echo "All mutants killed: TransactionalPortRulesTest catches what it claims to."
