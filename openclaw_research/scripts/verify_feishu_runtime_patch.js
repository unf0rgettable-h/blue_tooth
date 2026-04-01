#!/usr/bin/env node
"use strict";

const fs = require("fs");

const filePath = process.argv[2];

if (!filePath) {
  console.error("Usage: verify_feishu_runtime_patch.js <file>");
  process.exit(2);
}

const text = fs.readFileSync(filePath, "utf8");

const checks = [
  {
    label: "faster print frequency",
    ok: text.includes('print_frequency_ms: { default: 30 }')
  },
  {
    label: "larger print step",
    ok: text.includes('print_step: { default: 3 }')
  },
  {
    label: "explicit fast print strategy",
    ok: text.includes('print_strategy: "fast"')
  },
  {
    label: "reply text normalization helper",
    ok: text.includes("function normalizeVisibleReplyText(text)")
  },
  {
    label: "dispatch timing state",
    ok: text.includes("let dispatchStartedAt = 0;")
  },
  {
    label: "elapsed-aware card note",
    ok: text.includes("function resolveCardNote(agentId, identity, prefixCtx, elapsedMs)")
  },
  {
    label: "old Agent footer removed",
    ok: !text.includes("Agent: ${identity?.name?.trim() || agentId}")
  },
  {
    label: "old Provider footer removed",
    ok: !text.includes("Provider: ${prefixCtx.provider}")
  },
  {
    label: "normalized final dedupe set",
    ok: text.includes("const deliveredFinalFingerprints =")
  }
];

let failed = false;

for (const check of checks) {
  if (!check.ok) {
    failed = true;
    console.error(`FAIL: ${check.label}`);
  } else {
    console.log(`OK: ${check.label}`);
  }
}

process.exit(failed ? 1 : 0);
