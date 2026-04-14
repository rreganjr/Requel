# Agents Runbook

## Purpose
Document how we use AI-driven agents alongside the Requel codebase so contributors have a predictable, low-friction way to ask for help, run changes, and keep architectural principles intact.

## Agent Roles
- **Coding agent (primary):** Works inside the Codex CLI, edits files, and runs local commands. Treat it like a pair-programmer with commit-level discipline.
- **Research agent (optional):** Can browse externally for API changes or library behaviour; only invoke when specs are unclear or time-sensitive.
- **Planning helper (built-in):** Maintains short task plans when the work spans multiple steps; skip for trivial edits.

## Operating Guardrails
- Default workspace: repo root. Avoid `cd` in commands; set `workdir` explicitly.
- Edit policy: prefer `apply_patch` for code/doc changes; never use destructive git commands unless explicitly requested.
- Execution policy: use `rg` for search; keep commands minimal and reproducible. Run only the tests necessary to prove the change.
- Data boundaries: keep domain code persistence-ignorant—no repository access from entity constructors or JAXB hooks (see `doc/unmarshalling_plan.md`).
- Style boundaries: follow the DDD terminology and aggregate boundaries described in `doc/unmarshalling_plan.md`.

## Typical Workflows
1. **Diagnose & patch:** grep with `rg`, open the relevant file, propose an edit, apply via `apply_patch`, then run the smallest confirming test.
2. **Doc edits:** update or add markdown under `doc/`; keep summaries concise and actionable.
3. **Import/DDD tasks:** when touching import logic, honour the AggregateAssembler/ImportUnitOfWork pattern; avoid adding new `afterUnmarshal` repository calls.

## Commit Messages
- the top line should be a link to the ticket in github like `https://github.com/rreganjr/Requel/issues/38`
- followed by a one line summary referencing what work in the plan from the doc folder we worked on
- followed by file oriented details, note only use the filename not the full path
- the more complex the logic the more details we should include about the change
- if we are fixing a bug include what was not working, what was wrong with the code and how the code change solved the problem.

## When to Escalate or Ask
- Unclear bounded context ownership for a change.
- Import pipeline changes that risk reintroducing attached-entity flushes.
- Cross-module moves that might break the annotation ↔ project decoupling goals.

## Outputs to Expect
- Short, self-contained summaries with file/line references.
- Suggested next steps (tests, follow-up refactors) when relevant.

