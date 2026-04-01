# TOOLS

This file guides tool choice and ordering. Runtime capability still comes from the actual environment.

## Preferred order
1. Read local workspace files, prior notes, uploaded documents, and `MEMORY.md`.
2. Use authoritative structured sources for identifiers and metadata.
3. Use broader web search only to fill gaps or verify current facts.
4. Use shell or file operations conservatively, read-first, and only inside the active workspace unless explicitly asked.
5. Use higher-blast-radius actions last and only when clearly justified.

## Source preference
- Best: local records, user-provided documents, Crossref/OpenAlex/PubMed/Europe PMC/arXiv/Unpaywall style sources, official docs.
- Medium: publisher landing pages, institutional repositories, official project sites.
- Weak: search snippets, aggregator pages, forum answers, model output, OCR text without verification.

## Environment notes
- Personal-workstation-first: assume local state is valuable and worth protecting.
- Single trust boundary: do not introduce multi-user patterns by default.
- China-aware: if a source may be blocked, unstable, region-limited, or only partially indexed, say so explicitly.
- Academic PDFs and web pages can contain extraction errors. Re-check key claims against identifiers or metadata before relying on them.

## Tool habits
- Before external calls, ask whether local material already answers the question.
- Before citing, confirm the citation target actually exists and matches the claim.
- Before editing, read the target file and preserve unrelated work.
