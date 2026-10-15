Follow-on to #268. Renaming a glossary term changes the term and nothing else, so text written against the old name silently stops matching it.

### Why

Found while tuning #268. The vague-word check now flags "event" inside the term "Dry-run event" (the better term is "Dry-run webinar"). Accepting that fix, or renaming the term by hand, updates `terms.name` only. `EditGlossaryTermCommandImpl` keeps the term's referers and synonyms, but the stories and steps that say "dry-run event" keep saying it. On the next analysis the phrase no longer matches any term, so #268's glossary check raises it again as a candidate: the rename re-creates the finding it was meant to clear.

### Work

- **Keep the old name as a synonym.** Renaming a term creates (or keeps) a term with the old name whose canonical term is the renamed one, so existing text still matches the glossary and reads as the non-preferred word. Skip it when the old name is already a term or synonym, or differs only in case.
- **Offer to rewrite the referers' text.** After a rename, list the referers whose Name or Text contains the old name (word-boundary, case-insensitive) and let the user replace it with the new name in each, individually or all at once, through the normal Edit* commands and their authorization. Nothing is rewritten without the user choosing it.
- MCP/CLI: `EditGlossaryTerm` reports the synonym it created and the referers that still use the old name, so a gateway caller can do the same rewrite.

### AC

- Renaming "Dry-run event" to "Dry-run webinar" leaves a "Dry-run event" term whose canonical term is "Dry-run webinar"; re-analysis raises no glossary candidate for "dry-run event".
- A rename whose old name already exists as a term or synonym creates no duplicate.
- The rename result lists every referer whose text contains the old name; choosing "replace" edits exactly those entities, each through its Edit* command, and a user without Edit on one of them gets that one refused and the rest applied.
- Case-only renames create no synonym and offer no rewrite.

### Not in scope

- Rewriting text that uses a term without being a referer of it.
- Flagging text that uses a synonym instead of the canonical term; that is #258's terminology-consistency policy (child 7).
