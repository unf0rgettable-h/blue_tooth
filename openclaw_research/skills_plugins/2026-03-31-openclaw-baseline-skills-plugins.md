# OpenClaw Baseline Skills/Plugins Inspection

Date: 2026-03-31
Target host: `agentops@118.145.222.97`
Scope: read-only remote inspection plus a local recommendation artifact only. No remote install/config changes were applied from this session.

## Executive Summary

The remote host is running OpenClaw `2026.3.28` as a user-scoped install under `~agentops/.openclaw`, not under `/opt/research-lobster/openclaw/`.

The gateway service is active as `openclaw-gateway.service` and uses the packaged runtime:

- Binary entrypoint: `/home/agentops/.openclaw/lib/node_modules/openclaw/openclaw.mjs`
- Node runtime: `/home/agentops/.openclaw/tools/node-v24.14.1/bin/node`
- Main config: `/home/agentops/.openclaw/openclaw.json`
- Shared workspace root from config: `/opt/research-lobster/workspace`

The current config is intentionally minimal:

- `plugins.entries` only contains `feishu`
- `tools.web.search.provider` is unset
- `tools.web.fetch` is unset
- `commands.nativeSkills` is `auto`

Important constraint observed from the live system:

- `memory-core` is already bundled and loaded
- `memory-lancedb` is disabled
- Per instruction, no memory plugin or memory backend changes should be made now

## Current Remote State

OpenClaw state observed on 2026-03-31:

- Gateway unit: `openclaw-gateway.service`
- Gateway status: active/running
- Unit start time observed: 2026-03-31 00:51:54 CST
- `openclaw` is not in `PATH`, but the packaged CLI works via the absolute Node + `.mjs` paths
- Workspace directory `/opt/research-lobster/workspace` exists but is empty
- Feishu channel is enabled in config, but `openclaw status` reports it as `SETUP / not configured`
- `FEISHU_APP_SECRET` is not available in the current command path used for inspection

Plugins:

- `plugins list`: `42/82 loaded`
- `plugins doctor`: no plugin issues detected
- Search-related bundled plugins present but disabled by default:
  - `duckduckgo`
  - `firecrawl`
  - `tavily`
  - `exa`
  - `brave`
- Browser plugin is already loaded

Skills:

- `skills list`: `9/54 ready`
- Ready now:
  - `feishu-doc`
  - `feishu-drive`
  - `feishu-perm`
  - `feishu-wiki`
  - `healthcheck`
  - `node-connect`
  - `skill-creator`
  - `tmux`
  - `weather`
- Missing requirements on this Linux host block most research-adjacent bundled skills:
  - `blogwatcher`: missing `blogwatcher`
  - `summarize`: missing `summarize`
  - `nano-pdf`: missing `nano-pdf`
  - `obsidian`: missing `obsidian-cli`
  - `session-logs`: missing `rg`
  - `clawhub`: missing `clawhub`

## Safe Baseline Recommendation

The only clearly non-controversial, low-risk baseline plugin I recommend enabling next is:

1. `duckduckgo`

Reasoning:

- It is bundled already on this host
- It requires no API key
- It directly improves research usefulness by enabling `web_search`
- OpenClaw docs describe it as key-free and zero-config at the credential layer
- It does not touch memory features

Required caveat:

- Enabling the `duckduckgo` plugin alone is not enough
- `tools.web.search.provider` must also be set to `duckduckgo`
- If provider is left unset, OpenClaw falls back to Brave auto-selection and will fail without a Brave key

## Not Recommended For Baseline Right Now

These are useful, but not baseline-safe enough for this phase:

- `firecrawl`
  - Needs `FIRECRAWL_API_KEY`
  - Adds a paid external dependency and scraping policy surface
- `tavily`
  - Needs `TAVILY_API_KEY`
  - Adds external SaaS dependency
- `exa`
  - Needs `EXA_API_KEY`
  - Adds external SaaS dependency
- `brave`
  - Needs `BRAVE_API_KEY`
  - Adds external account/billing decision
- `blogwatcher`
  - Reasonable future skill, but requires adding Go tooling or a dedicated binary install
- `summarize`
  - Research-useful, but the packaged skill expects a separate `summarize` CLI and additional model/API choices
- `nano-pdf`
  - Useful later for PDF workflows, but not necessary for a safe baseline and requires extra Python/UV tooling
- `obsidian`
  - Not justified on this host without a clear vault path and operator workflow

## Optional Low-Risk Ops Add-On

If the operator wants a small operational improvement, install `ripgrep` only:

- It enables the bundled `session-logs` skill
- It is a standard package on Ubuntu
- It does not affect memory behavior

This is operationally safe, but it is not core to research capability, so I treated it as optional.

## Recommended Future Set

If a later phase explicitly approves more tooling and secrets, the next research-oriented set should be:

1. `duckduckgo` plugin plus `tools.web.search.provider=duckduckgo`
2. `ripgrep` for `session-logs`
3. One paid/structured search provider, but only after an explicit product choice:
   - `tavily` for AI-oriented search/extraction
   - `firecrawl` for JS-heavy scraping
   - `exa` for semantic search with content extraction
4. One document-processing CLI only after a clear use case:
   - `summarize`
   - `nano-pdf`

## Helper Script

Local helper script created:

- `openclaw_safe_baseline_actions.sh`

Behavior:

- Dry-run by default
- Uses the existing SSH key
- Contains only the minimal future actions I consider safe:
  - enable `duckduckgo`
  - set `tools.web.search.provider=duckduckgo`
  - set conservative DuckDuckGo plugin defaults
  - restart the user gateway service
  - verify the resulting config/plugin state
- Includes an optional `WITH_RIPGREP=1` branch for `apt-get install ripgrep`

The script was not executed.

## Remote Commands Run During Inspection

Connection and identity:

- `ssh -i /home/unf0rgettable/.ssh/agentops_118_145_222_97 agentops@118.145.222.97 'whoami; hostname; pwd'`

Filesystem and install layout:

- `ls -la /opt`
- `ls -la /opt/research-lobster`
- `find /opt/research-lobster -maxdepth 4`
- `find /home/agentops -maxdepth 5 ...`

Config inspection:

- `sed -n '1,240p' /home/agentops/.openclaw/openclaw.json`
- `openclaw config get tools.web.search.provider`
- `openclaw config get tools.web.fetch`
- `openclaw config get plugins.entries`

Package and docs inspection:

- `sed -n '1,220p' /home/agentops/.openclaw/lib/node_modules/openclaw/package.json`
- `sed -n '1,220p' /home/agentops/.openclaw/lib/node_modules/openclaw/README.md`
- `find /home/agentops/.openclaw/lib/node_modules/openclaw/skills -maxdepth 2 -type f`
- `sed -n '1,260p' /home/agentops/.openclaw/lib/node_modules/openclaw/docs/tools/web.md`
- `sed -n '1,240p' /home/agentops/.openclaw/lib/node_modules/openclaw/docs/tools/duckduckgo-search.md`
- `sed -n '1,240p' /home/agentops/.openclaw/lib/node_modules/openclaw/docs/tools/firecrawl.md`
- `sed -n '1,240p' /home/agentops/.openclaw/lib/node_modules/openclaw/docs/tools/tavily.md`
- `sed -n '1,240p' /home/agentops/.openclaw/lib/node_modules/openclaw/docs/tools/exa-search.md`
- `sed -n '1,240p' /home/agentops/.openclaw/lib/node_modules/openclaw/docs/tools/brave-search.md`

CLI capability checks:

- `node /home/agentops/.openclaw/lib/node_modules/openclaw/openclaw.mjs --help`
- `node /home/agentops/.openclaw/lib/node_modules/openclaw/openclaw.mjs skills --help`
- `node /home/agentops/.openclaw/lib/node_modules/openclaw/openclaw.mjs plugins --help`
- `node /home/agentops/.openclaw/lib/node_modules/openclaw/openclaw.mjs skills list`
- `node /home/agentops/.openclaw/lib/node_modules/openclaw/openclaw.mjs skills check`
- `node /home/agentops/.openclaw/lib/node_modules/openclaw/openclaw.mjs skills info blogwatcher`
- `node /home/agentops/.openclaw/lib/node_modules/openclaw/openclaw.mjs skills info summarize`
- `node /home/agentops/.openclaw/lib/node_modules/openclaw/openclaw.mjs skills info nano-pdf`
- `node /home/agentops/.openclaw/lib/node_modules/openclaw/openclaw.mjs skills info obsidian`
- `node /home/agentops/.openclaw/lib/node_modules/openclaw/openclaw.mjs skills info session-logs`
- `node /home/agentops/.openclaw/lib/node_modules/openclaw/openclaw.mjs skills info clawhub`
- `node /home/agentops/.openclaw/lib/node_modules/openclaw/openclaw.mjs skills info healthcheck`
- `node /home/agentops/.openclaw/lib/node_modules/openclaw/openclaw.mjs plugins list`
- `node /home/agentops/.openclaw/lib/node_modules/openclaw/openclaw.mjs plugins doctor`
- `node /home/agentops/.openclaw/lib/node_modules/openclaw/openclaw.mjs plugins marketplace`
- `node /home/agentops/.openclaw/lib/node_modules/openclaw/openclaw.mjs plugins inspect duckduckgo`
- `node /home/agentops/.openclaw/lib/node_modules/openclaw/openclaw.mjs plugins inspect firecrawl`
- `node /home/agentops/.openclaw/lib/node_modules/openclaw/openclaw.mjs plugins inspect tavily`
- `node /home/agentops/.openclaw/lib/node_modules/openclaw/openclaw.mjs plugins inspect exa`

Host capability and service checks:

- `command -v npm node go uv python3 jq rg apt-get systemctl`
- `sudo -n true`
- `systemctl --user list-unit-files | grep -i openclaw`
- `systemctl --user --no-pager --full status openclaw-gateway.service`
- `node /home/agentops/.openclaw/lib/node_modules/openclaw/openclaw.mjs status`
- `sed -n '1,220p' /home/agentops/.openclaw/logs/config-health.json`
