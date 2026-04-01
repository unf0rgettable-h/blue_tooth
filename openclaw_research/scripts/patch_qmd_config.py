from pathlib import Path
import json


home = Path.home()
env_path = home / ".openclaw" / "openclaw.env"
cfg_path = home / ".openclaw" / "openclaw.json"

vals = {}
if env_path.exists():
    for line in env_path.read_text(encoding="utf-8").splitlines():
        if "=" in line and not line.startswith("#"):
            k, v = line.split("=", 1)
            vals[k] = v

vals["HF_ENDPOINT"] = "https://hf-mirror.com"
env_path.write_text("".join(f"{k}={v}\n" for k, v in vals.items()), encoding="utf-8")

cfg = json.loads(cfg_path.read_text(encoding="utf-8"))
memory = cfg.setdefault("memory", {})
memory["backend"] = "qmd"
qmd = memory.setdefault("qmd", {})
qmd["command"] = str(home / ".npm-global" / "bin" / "qmd")
qmd["includeDefaultMemory"] = True
qmd.setdefault("update", {})["interval"] = "5m"
qmd.setdefault("limits", {})["maxResults"] = 6
qmd["scope"] = {
    "default": "deny",
    "rules": [
        {"action": "allow", "match": {"chatType": "direct"}},
    ],
}

cfg_path.write_text(json.dumps(cfg, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")
print("patched config and env")
