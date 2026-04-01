#!/usr/bin/env bash
set -euo pipefail

profile_file="${HOME}/.profile"
bin_dir="${HOME}/bin"
wrapper_path="${bin_dir}/openclaw"
marker_start="# >>> OPENCLAW LOCAL TOOLS >>>"
marker_end="# <<< OPENCLAW LOCAL TOOLS <<<"

mkdir -p "${bin_dir}"

if ! grep -Fq "${marker_start}" "${profile_file}"; then
  cat >>"${profile_file}" <<'EOF'

# >>> OPENCLAW LOCAL TOOLS >>>
if [ -d "$HOME/.npm-global/bin" ] ; then
    PATH="$HOME/.npm-global/bin:$PATH"
fi

if [ -d "$HOME/.openclaw/tools/node/bin" ] ; then
    PATH="$HOME/.openclaw/tools/node/bin:$PATH"
fi

export PATH
# <<< OPENCLAW LOCAL TOOLS <<<
EOF
fi

cat >"${wrapper_path}" <<'EOF'
#!/usr/bin/env bash
set -euo pipefail

export PATH="$HOME/.openclaw/tools/node/bin:$HOME/.npm-global/bin:$PATH"

if [ -f "$HOME/.openclaw/openclaw.env" ]; then
  set -a
  # shellcheck disable=SC1090
  . "$HOME/.openclaw/openclaw.env"
  set +a
fi

exec "$HOME/.openclaw/bin/openclaw" "$@"
EOF

chmod 700 "${wrapper_path}"

sudo loginctl enable-linger "$(id -un)"
systemctl --user daemon-reload
systemctl --user restart openclaw-gateway.service

printf 'wrapper=%s\n' "${wrapper_path}"
printf 'linger=%s\n' "$(loginctl show-user "$(id -un)" -p Linger --value)"
printf 'gateway=%s\n' "$(systemctl --user is-active openclaw-gateway.service)"
