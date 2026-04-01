#!/usr/bin/env bash
set -euo pipefail

HOST="${HOST:-118.145.222.97}"
USER_NAME="${USER_NAME:-agentops}"
KEY_PATH="${KEY_PATH:-/home/unf0rgettable/.ssh/agentops_118_145_222_97}"
EXECUTE="${EXECUTE:-0}"
WITH_RIPGREP="${WITH_RIPGREP:-0}"

REMOTE_NODE="/home/agentops/.openclaw/tools/node-v24.14.1/bin/node"
REMOTE_OPENCLAW="/home/agentops/.openclaw/lib/node_modules/openclaw/openclaw.mjs"
REMOTE_SERVICE="openclaw-gateway.service"

run_remote() {
  local script="$1"
  if [[ "${EXECUTE}" == "1" ]]; then
    ssh -i "${KEY_PATH}" -o BatchMode=yes "${USER_NAME}@${HOST}" "${script}"
  else
    printf '[dry-run] ssh -i %q -o BatchMode=yes %q@%q %q\n' \
      "${KEY_PATH}" "${USER_NAME}" "${HOST}" "${script}"
  fi
}

printf 'Target: %s@%s\n' "${USER_NAME}" "${HOST}"
printf 'Mode: %s\n' "$( [[ "${EXECUTE}" == "1" ]] && echo execute || echo dry-run )"
printf 'Optional ripgrep install: %s\n' "${WITH_RIPGREP}"

run_remote "test -x '${REMOTE_NODE}' && test -f '${REMOTE_OPENCLAW}'"

run_remote "'${REMOTE_NODE}' '${REMOTE_OPENCLAW}' plugins enable duckduckgo"
run_remote "'${REMOTE_NODE}' '${REMOTE_OPENCLAW}' config set tools.web.search.provider duckduckgo"
run_remote "'${REMOTE_NODE}' '${REMOTE_OPENCLAW}' config set plugins.entries.duckduckgo.config.webSearch.safeSearch moderate"
run_remote "'${REMOTE_NODE}' '${REMOTE_OPENCLAW}' config set plugins.entries.duckduckgo.config.webSearch.region us-en"

if [[ "${WITH_RIPGREP}" == "1" ]]; then
  run_remote "sudo apt-get update && sudo apt-get install -y ripgrep"
fi

run_remote "systemctl --user restart '${REMOTE_SERVICE}'"

run_remote "'${REMOTE_NODE}' '${REMOTE_OPENCLAW}' config get tools.web.search.provider"
run_remote "'${REMOTE_NODE}' '${REMOTE_OPENCLAW}' config get plugins.entries.duckduckgo"
run_remote "'${REMOTE_NODE}' '${REMOTE_OPENCLAW}' plugins list | grep -i duckduckgo"
run_remote "systemctl --user --no-pager --full status '${REMOTE_SERVICE}' | sed -n '1,40p'"

cat <<'EOF'

This script was generated as a guarded helper only.

Default behavior:
- does not change the remote host
- prints the exact SSH commands it would run

To execute the recommended baseline actions:
  EXECUTE=1 bash openclaw_safe_baseline_actions.sh

To also install ripgrep for the optional session-logs skill:
  EXECUTE=1 WITH_RIPGREP=1 bash openclaw_safe_baseline_actions.sh

Notes:
- Executing this script will modify /home/agentops/.openclaw/openclaw.json on the remote host.
- It does not install or enable any memory plugin/backend.
- It does not touch Feishu credentials or other channel configs.
EOF
