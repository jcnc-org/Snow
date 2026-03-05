---
name: llvm-authority-kb
description: Maintain Snow’s local authoritative knowledge base for LLVM/ABI/third-party APIs. Use when adding external API assumptions, pinning versions, updating references, or validating provenance metadata.
---

# LLVM Authority Knowledge Base

## Location

- `docs/knowledge/llvm-21.1.8-authority.md`
- `docs/knowledge/runtime-abi-authority.md`
- `docs/knowledge/third-party-api-authority.md`
- `docs/knowledge/update-sop.md`

## Required Provenance Fields

Each authority entry must include:

- `Source:` official URL
- `Version:` pinned version (for LLVM: `21.1.8`)
- `Retrieved:` explicit date
- `Why it matters:` concise Snow-specific rationale

## Policy

- Prefer primary sources (official LLVM and ABI docs).
- Store concise curated notes; do not vendor huge snapshots.
- Keep usage guidance tied to concrete compiler modules.
- Update authority docs in the same change set when external API assumptions change.

## Validation

Run:

```powershell
powershell -ExecutionPolicy Bypass -File tools/check_knowledge_base.ps1
```

The check must hard-fail on missing provenance fields or mismatched pinned versions.
