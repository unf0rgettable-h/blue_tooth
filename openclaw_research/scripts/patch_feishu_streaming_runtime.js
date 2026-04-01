#!/usr/bin/env node
"use strict";

const fs = require("fs");

const filePath = process.argv[2];

if (!filePath) {
  console.error("Usage: patch_feishu_streaming_runtime.js <file>");
  process.exit(2);
}

let text = fs.readFileSync(filePath, "utf8");

const replacements = [
  {
    label: "streaming defaults",
    from: `\t\t\t\tstreaming_config: {\n\t\t\t\t\tprint_frequency_ms: { default: 50 },\n\t\t\t\t\tprint_step: { default: 1 }\n\t\t\t\t}\n`,
    to: `\t\t\t\tstreaming_config: {\n\t\t\t\t\tprint_frequency_ms: { default: 30 },\n\t\t\t\t\tprint_step: { default: 3 },\n\t\t\t\t\tprint_strategy: "fast"\n\t\t\t\t}\n`
  },
  {
    label: "card note helpers",
    from: `function resolveCardHeader(agentId, identity) {\n\tconst name = identity?.name?.trim() || agentId;\n\tconst emoji = identity?.emoji?.trim();\n\treturn {\n\t\ttitle: emoji ? \`\${emoji} \${name}\` : name,\n\t\ttemplate: identity?.theme ?? "blue"\n\t};\n}\n/** Build a card note footer from agent identity and model context. */\nfunction resolveCardNote(agentId, identity, prefixCtx) {\n\tconst parts = [\`Agent: \${identity?.name?.trim() || agentId}\`];\n\tif (prefixCtx.model) parts.push(\`Model: \${prefixCtx.model}\`);\n\tif (prefixCtx.provider) parts.push(\`Provider: \${prefixCtx.provider}\`);\n\treturn parts.join(" | ");\n}\n`,
    to: `function resolveCardHeader(agentId, identity) {\n\tconst name = identity?.name?.trim() || agentId;\n\tconst emoji = identity?.emoji?.trim();\n\treturn {\n\t\ttitle: emoji ? \`\${emoji} \${name}\` : name,\n\t\ttemplate: identity?.theme ?? "blue"\n\t};\n}\nfunction formatElapsedSeconds(elapsedMs) {\n\tif (!Number.isFinite(elapsedMs) || elapsedMs < 0) return null;\n\tif (elapsedMs < 1e3) return "<1s";\n\tconst seconds = elapsedMs / 1e3;\n\treturn seconds >= 10 ? \`\${Math.round(seconds)}s\` : \`\${seconds.toFixed(1)}s\`;\n}\nfunction normalizeVisibleReplyText(text) {\n\treturn String(text ?? "").replace(/\\[\\[reply_to_current\\]\\]/g, "").replace(/\\s+/g, " ").trim();\n}\n/** Build a card note footer from model context and elapsed time only. */\nfunction resolveCardNote(agentId, identity, prefixCtx, elapsedMs) {\n\tconst parts = [];\n\tif (prefixCtx.model) parts.push(prefixCtx.model);\n\tconst elapsed = formatElapsedSeconds(elapsedMs);\n\tif (elapsed) parts.push(elapsed);\n\treturn parts.join(" | ");\n}\n`
  },
  {
    label: "dispatcher timing and dedupe state",
    from: `\tlet streaming = null;\n\tlet streamText = "";\n\tlet lastPartial = "";\n\tlet reasoningText = "";\n\tconst deliveredFinalTexts = /* @__PURE__ */ new Set();\n`,
    to: `\tlet streaming = null;\n\tlet streamText = "";\n\tlet lastPartial = "";\n\tlet reasoningText = "";\n\tlet dispatchStartedAt = 0;\n\tconst deliveredFinalFingerprints = /* @__PURE__ */ new Set();\n`
  },
  {
    label: "start note",
    from: `\t\t\t\tconst cardHeader = resolveCardHeader(agentId, identity);\n\t\t\t\tconst cardNote = resolveCardNote(agentId, identity, prefixContext.prefixContext);\n`,
    to: `\t\t\t\tconst cardHeader = resolveCardHeader(agentId, identity);\n\t\t\t\tconst cardNote = resolveCardNote(agentId, identity, prefixContext.prefixContext);\n`
  },
  {
    label: "close note elapsed",
    from: `\t\tif (streaming?.isActive()) {\n\t\t\tlet text = buildCombinedStreamText(reasoningText, streamText);\n\t\t\tif (mentionTargets?.length) text = buildMentionedCardContent(mentionTargets, text);\n\t\t\tconst finalNote = resolveCardNote(agentId, identity, prefixContext.prefixContext);\n\t\t\tawait streaming.close(text, { note: finalNote });\n\t\t}\n`,
    to: `\t\tif (streaming?.isActive()) {\n\t\t\tlet text = buildCombinedStreamText(reasoningText, streamText);\n\t\t\tif (mentionTargets?.length) text = buildMentionedCardContent(mentionTargets, text);\n\t\t\tconst elapsedMs = dispatchStartedAt > 0 ? Date.now() - dispatchStartedAt : void 0;\n\t\t\tconst finalNote = resolveCardNote(agentId, identity, prefixContext.prefixContext, elapsedMs);\n\t\t\tawait streaming.close(text, finalNote ? { note: finalNote } : void 0);\n\t\t}\n`
  },
  {
    label: "final fingerprint on chunked reply",
    from: `\t\tif (params.infoKind === "final") deliveredFinalTexts.add(params.text);\n`,
    to: `\t\tif (params.infoKind === "final") deliveredFinalFingerprints.add(normalizeVisibleReplyText(params.text));\n`
  },
  {
    label: "reply start clears fingerprint set and starts timer",
    from: `\t\tonReplyStart: async () => {\n\t\t\tdeliveredFinalTexts.clear();\n\t\t\tif (streamingEnabled && renderMode === "card") startStreaming();\n\t\t\tawait typingCallbacks?.onReplyStart?.();\n\t\t},\n`,
    to: `\t\tonReplyStart: async () => {\n\t\t\tdeliveredFinalFingerprints.clear();\n\t\t\tdispatchStartedAt = Date.now();\n\t\t\tif (streamingEnabled && renderMode === "card") startStreaming();\n\t\t\tawait typingCallbacks?.onReplyStart?.();\n\t\t},\n`
  },
  {
    label: "normalized skip duplicate final",
    from: `\t\t\tconst text = reply.text;\n\t\t\tconst hasText = reply.hasText;\n\t\t\tconst hasMedia = reply.hasMedia;\n\t\t\tconst skipTextForDuplicateFinal = info?.kind === "final" && hasText && deliveredFinalTexts.has(text);\n`,
    to: `\t\t\tconst text = reply.text;\n\t\t\tconst hasText = reply.hasText;\n\t\t\tconst hasMedia = reply.hasMedia;\n\t\t\tconst finalFingerprint = hasText ? normalizeVisibleReplyText(text) : "";\n\t\t\tconst skipTextForDuplicateFinal = info?.kind === "final" && hasText && finalFingerprint && deliveredFinalFingerprints.has(finalFingerprint);\n`
  },
  {
    label: "record fingerprint after streaming close",
    from: `\t\t\t\t\tif (info?.kind === "final") {\n\t\t\t\t\t\tstreamText = mergeStreamingText(streamText, text);\n\t\t\t\t\t\tawait closeStreaming();\n\t\t\t\t\t\tdeliveredFinalTexts.add(text);\n\t\t\t\t\t}\n`,
    to: `\t\t\t\t\tif (info?.kind === "final") {\n\t\t\t\t\t\tstreamText = mergeStreamingText(streamText, text);\n\t\t\t\t\t\tawait closeStreaming();\n\t\t\t\t\t\tif (finalFingerprint) deliveredFinalFingerprints.add(finalFingerprint);\n\t\t\t\t\t}\n`
  },
  {
    label: "simplified note for non-stream card send",
    from: `\t\t\t\t\tconst cardHeader = resolveCardHeader(agentId, identity);\n\t\t\t\t\tconst cardNote = resolveCardNote(agentId, identity, prefixContext.prefixContext);\n`,
    to: `\t\t\t\t\tconst cardHeader = resolveCardHeader(agentId, identity);\n\t\t\t\t\tconst elapsedMs = dispatchStartedAt > 0 ? Date.now() - dispatchStartedAt : void 0;\n\t\t\t\t\tconst cardNote = resolveCardNote(agentId, identity, prefixContext.prefixContext, elapsedMs);\n`
  },
  {
    label: "reset dispatch timer on idle",
    from: `\t\tonIdle: async () => {\n\t\t\tawait closeStreaming();\n\t\t\ttypingCallbacks?.onIdle?.();\n\t\t},\n`,
    to: `\t\tonIdle: async () => {\n\t\t\tawait closeStreaming();\n\t\t\tdispatchStartedAt = 0;\n\t\t\ttypingCallbacks?.onIdle?.();\n\t\t},\n`
  }
];

for (const replacement of replacements) {
  if (text.includes(replacement.to)) {
    continue;
  }
  if (!text.includes(replacement.from)) {
    console.error(`Patch anchor not found: ${replacement.label}`);
    process.exit(1);
  }
  text = text.replace(replacement.from, replacement.to);
}

fs.writeFileSync(filePath, text);
console.log(`Patched ${filePath}`);
