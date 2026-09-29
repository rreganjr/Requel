# #268 Legacy lexical assistant precision (with #269 folded in) — implementation plan

Issue: https://github.com/rreganjr/Requel/issues/268 (child 1 of epic #267)
Folded in: https://github.com/rreganjr/Requel/issues/269 (child 2), closed with retro 0 when this merges
Branch: `268-lexical-precision`, cut from `release/2.0` @ `99b96f33`

## Summary

The four lexical assistants bury the roundtable project's real findings: 65 of its 74 open issues
are lexical, and every one of them is must-resolve. This ticket makes them precise enough to leave
on. Spelling stops flagging acronyms, hyphenated compounds and the project's own glossary and actor
vocabulary. Glossary candidates become one issue per phrase per project, only for phrases that
recur or are proper nouns, never for a phrase the glossary already defines and never for a verb
phrase the parser misread. The vague-word check is redesigned from data gathered first by a
harness, not replaced by a list. Every lexical finding becomes advisory. Each lexical assistant
can be switched off per project, and a new "re-run analysis" command lets a project be re-analyzed
after its glossary or settings change.

#269's root cause was already fixed by #314 (the `Sentencizer` span bug cut sentences mid-word).
What is left of it — a quoted-evidence check, rebuilding quoted phrases from the entity's own text,
and a dash/non-ASCII regression test — rides here.

Project import is the last path still running the old `project-jpa` `LexicalAssistant`; it moves
to the SPI, so there is one lexical implementation.

## Review against the tree (`release/2.0` @ `99b96f33`)

**Baseline, project 621 (roundtable), open issues** (`tmp/268-counts.sql`, output in
`tmp/268-before.txt`, not committed):

| Kind | Open | Must-resolve |
|---|---|---|
| glossary-candidate | 50 | 50 |
| non-lexical | 9 | 9 |
| spelling | 8 | 8 |
| vague-word | 6 | 6 |
| complexity | 1 | 1 |

**Two lexical implementations, but only one entry point still uses the old one.** Every edit
command that analyzes (goal, story, actor, use case, scenario, step) implements
`AnalysisRequestSource`, and `AnalysisInvokingCommandHandler` checks that before the legacy
`AnalyzableEditCommand.invokeAnalysis()`. `EditScenarioCommandImpl` inherits it from
`EditScenarioStepCommandImpl`, so scenario edits are already on the SPI (the review said
otherwise; that was wrong). The only live caller of the old path is
`ImportProjectStreamingCommandImpl.invokeAnalysis()` → `AssistantFacade.analyzeProject` →
`ProjectAssistant` → `LexicalAssistant` (749 lines, `setMustBeResolved(true)` in four places).
`AssistantFacade` itself is threaded through ~40 command constructors, so removing the class is
not this ticket (see Out of scope).

**Issues the old path writes can't be cleaned up by a re-run.** `reconcileStaleFindings` only
touches issues that have an `assistant_findings` row and an `ASSISTANT:` source. Issues written by
the old `LexicalAssistant` (import) have neither, so no SPI re-run ever removes them. Project 621
has none (`tmp/268-sources.sql`: all 65 open lexical issues are SPI-written with a finding row,
since it was built through the gateway), but any project imported before this ticket does.

**`Zzz` survived because a failed assistant is silent, and a failed property is worse.** The
audit log (`tmp/268-zzz-runs.sql`) shows Scenario 749 created as "Zzz isolation probe no steps"
at 02:22:20, renamed at 02:22:48 and edited again at 02:35:14; all three runs are `SUCCEEDED`. The
renamed text is many sentences of uneven length, which the pre-#314 `Sentencizer` turns into a
`StringIndexOutOfBoundsException` (a later, shorter span gives `substring(begin > end)`); the
app log from that morning isn't available, so this is the explanation that fits, not a captured
stack trace. `LexicalSpellingAssistant` has no try/catch, so it throws; `AssistantRunWorker` isolates
per-assistant failures by logging and dropping that assistant's result, so the run still reads
`SUCCEEDED`, and with no result there is no reconcile — the Name's `Zzz` finding stayed `ACTIVE`.
#314 removed this trigger, but two things stay wrong:

- A failed assistant leaves no trace on the run: `error_summary` is empty.
- The other three lexical assistants catch per property (`log.warn(...); skipping`) and return
  what they have. Their result then looks complete, so `reconcileStaleFindings` auto-resolves
  every earlier finding on the failed property. An NLP failure on the Text deletes its findings.

**Nothing can re-run analysis over a project.** The only project-wide analysis is the one import
triggers. Adding a glossary term therefore never clears the spelling findings it should, and the
before/after measurement has no trigger.

**The executor can't take one run per entity.** `assistantTaskExecutor` is core 5 / max 10 /
queue 25, and `AssistantDispatcherImpl.dispatch` marks a rejected run FAILED. Dispatching every
entity of a 70-entity project individually would fail about half of them.

**Vague words have no list.** `LexicalVagueWordAssistant` flags every token whose WordNet sense
has information content below `0.50` (named entities and the root `entity#n#1` sense on proper
nouns excepted). That is why the copula "be" and generic verbs fire.

**Glossary candidates.**
- The existing-term check looks up the phrase with its determiner still on
  (`findGlossaryTermForProjectOrDomain(projectOrDomain, termText)`); only the actor check strips
  it. So `the room`, `a room` and `a test room` are raised although **Room** and **Test room**
  are terms.
- Subject-less sentences (acceptance-criteria style) parse their verb as a noun: `archives the
  room`, `downloads the resulting MP4`, `Alarms route`, `Hosts or co-hosts the Zoom`,
  `force - stops the stream`, `an event unassisted`.
- A single capitalized word at the start of a sentence is tagged NNP: `Archive`, `Recover`,
  `Verification`, `Disclosure`, `Admin`.
- Phrases are rebuilt by joining tokens with spaces (`NLPTextImpl.getText`), so `force-stops`
  becomes `force - stops`, which is not in the entity text.
- Pre-#320 data already has issues attached to several entities (`a roundtable session` on 3),
  because the old lookups ignored the annotatable. Current code creates them per entity.

**Ignores are per entity.** `ignored_findings` rows carry `target_type`/`target_id` and a key
`assistant:type:id:finding-type[:property]:subject`. `FindingResolutionTrackingCommandHandler`
records one row per finding behind the resolved issue. `target_type` is a free `varchar(80)`, so
a `Project` target fits without a schema change.

**Detaching the last entity deletes the issue.** `RemoveAnnotationFromAnnotatableCommandImpl`
deletes an issue left with no annotatables, so a shared, project-wide issue cleans itself up when
the last entity stops using the phrase.

**No per-project assistant settings exist.** AI gating is the global `requel.ai.*` properties;
`SimpleAssistantRegistry.findAssistantsFor` filters by target type only.

**Glossary terms are never analyzed.** `EditGlossaryTermCommandImpl.invokeAnalysis()` is a TODO,
and the command is not an `AnalysisRequestSource`, although `ProjectEntityTargetLoader` already
supports `GlossaryTerm`.

## Locked decisions

1. **#269 folds in.** Close #269 with retro 0 and a comment naming #314 as the root-cause fix.
2. **One lexical implementation.** Import dispatches through the SPI; the old `LexicalAssistant`
   is no longer reached in production.
3. **Vague words are redesigned from data, not replaced by a list.** WordNet stays: domain words
   like "person" in a story are genuinely vague, and a list would miss them. Step 1 builds a
   harness, and the rule is chosen from its output (checkpoint below).
4. **Webinar and livestream are glossary vocabulary, not dictionary words.** The ticket's AC is
   met by adding them as glossary terms in project 621 and having spelling honour the glossary.
5. **Spelling skips project vocabulary:** an exact glossary term or actor name, and each word of a
   multi-word term or actor name, case-insensitive. Being in the glossary does **not** exempt a
   word from the vague-word check ("event" in "Dry-run event" is vague; "Dry-run webinar" is the
   better term).
6. **Glossary candidates are one issue per phrase per project,** attached to every entity that
   uses the phrase, raised only when the phrase appears in three or more entities or is a proper
   noun (three from the harness; two was the first cut).
7. **Ignoring a glossary candidate applies to the whole project.**
8. **A minimal per-project on/off store for the lexical assistants,** shaped by assistant id so
   #258 child 2 extends it rather than replaces it.
9. **V24 flips existing lexical issues to advisory,** like #271's severity backfill.

### Approved (2026-09-27)

- **Analyze glossary terms** (spelling, vague-word and complexity only; not the glossary-candidate
  check, which would match the term itself). Makes `EditGlossaryTerm` an `AnalysisRequestSource`
  and adds a `GlossaryTerm` entry to `ProjectAnnotatableTextEditorConfiguration` so Fix Spelling
  can edit a term.
- **A project re-analysis command** (`AnalyzeProject`, Annotation[Edit]) and a "Re-run analysis"
  button. Without it neither the AC measurement nor "add a glossary term, watch the spelling
  findings clear" can happen.
- **Adopt the old path's issues.** When an SPI lexical assistant runs on an entity, an unresolved
  `LexicalIssue` on that entity with no `ASSISTANT:` source, whose word and property the run did
  not reproduce, is removed from the entity. It touches only unresolved, unowned issues, so an
  imported project's old noise clears on its first re-run.
- **Assistant settings do not travel with project XML.** They are preferences, not requirements
  content, and leaving them out keeps `project.xsd` unchanged. A re-imported project starts with
  every assistant on.

- **The controls live on the project overview page** (`projects/:name`,
  `ProjectWorkspaceComponent`): an "Assistants" panel beside "Open issues" and "Next actions",
  with one toggle per assistant applied immediately (read-only without Project[Edit]) and a
  "Re-run analysis" button (needs Annotation[Edit]). The Dictionary page keeps project words and
  the #319 ignores.

## Contracts

### Vague-word harness (step 1, dev profile only)

- `GET /api/dev/lexical-analysis?project=<name>`, `LexicalAnalysisHarness` +
  `LexicalAnalysisHarnessController` in `requel-app` (`com.rreganjr.requel.dev`; `service-impl`
  can't see the NLP modules). Registered by `requel.dev.lexical-harness.enabled=true`, which only
  `application-dev.properties` sets, like `DevProjectResetController`; `/api/dev/**` is
  unauthenticated and this returns project text, so it also refuses non-loopback callers.
  Read-only: no annotation, finding or run is written. `tmp/268-harness.sh` builds, starts the app,
  saves the report and stops it. Returns TSV, one row per analyzed token of every text entity's
  Name and Text: entity type, id, property, token, parse tag, named-entity flag, sentence-initial
  flag, WordNet sense, information content, person-hyponym flag (sense is a hyponym of
  `person#n#1`), in-glossary-word flag, in-actor-word flag, today's verdict, and the verdict of
  each candidate rule below.
- A second section lists glossary candidates with the entity count, the normalized phrase, and
  the verdict of each glossary rule (so the threshold and the verb-phrase filter are checked on
  the same data).
- Output is saved to `tmp/268-harness.tsv` (gitignored). Roundtable is Medlive work content, so
  none of its text goes into the repo, into tests or into a committed doc.

Candidate vague-word rules the harness scores:

- **A.** Score only nouns, adjectives and adverbs; skip verbs, auxiliaries and copulas.
- **B′.** No glossary exemption (decision 5); report the flag for information only.
- **C.** A person-hyponym noun in a Story, UseCase, Scenario or Step gets a different finding,
  "names no actor role" (`findingType` `person-not-role`), instead of "vague".
- **D.** Add the weak requirements words WordNet can't score (`should`, `may`, `might`, `could`,
  `as appropriate`, `as needed`, `etc.`, `and/or`, `user-friendly`, `easy`, `fast`, `robust`,
  `flexible`, `sufficient`, `adequate`) as a list alongside the WordNet check.
- **E.** Tune `INFO_CONTENT_THRESHOLD` (report the IC so a cut-off can be read off the data).

**Checkpoint:** after the harness runs, the chosen combination and the rewritten vague-word ACs
go into this plan's "Vague-word rule" section below, and get a thumbs-up before step 3.
Filled in and agreed 2026-09-28.

### Harness results (project 621, 2026-09-28)

The harness saw 2,671 words and 360 glossary candidates across the project's text entities, with no
NLP errors. `tmp/268-analyze.py` turns `tmp/268-harness.tsv` into the counts below; both stay in
`tmp/`.

**Vague words.** Today's rule flags 66 words: 34 verbs (`be`, `says`, `states`, `create`,
`creates`, `have`, `stated`, `being`, `covering`, `connects`, ...) and 32 nouns (`event` ×18,
`action` ×6, `tool` ×4, `statement` ×2, `concept`, `work`). No adjective or adverb scored under
0.50. Among nouns, only 43 score between 0.1 and 0.5 and 47 between 0.5 and 0.6, so the
threshold isn't sitting on a cliff. `person` scores 0.877 on today's parse and isn't flagged
at all (the baseline's `person` came from a pre-#314 fragment).

**Person nouns (rule C).** Word-sense disambiguation is too unreliable for it. `person` gets the
grammatical-category sense (`person%1:10:00::`), the hypernym walk to `person#n#1` matched 3
words (two of them the parts of `Chris Peterson`), and the noun.person lexicographer file
(`%1:18:`) gives about 13 hits in stories, use cases and steps, of which `dry`, `zombie` and
`guide` are wrong senses and `panelists`, `viewers` and `engineers` are plurals of actor names.

**Weak words (rule D).** Present in the project: `can` ×7, `should` ×2, `fast`, `several`.

### Vague-word rule

- **A.** Only nouns, adjectives and adverbs are scored. Verbs, auxiliaries and copulas never are.
- **B′.** The glossary does not exempt a word ("event" in "Dry-run event" stays flagged).
- **D.** A short list of weak requirements words WordNet can't score is flagged alongside:
  `should`, `may`, `might`, `could`, `easy`, `easily`, `fast`, `quickly`, `robust`, `flexible`,
  `sufficient`, `adequate`, `appropriate`, `appropriately`, `reasonable`, `user-friendly`,
  `efficient`, `several`, `various`, `etc`. `can` is left out: in requirements it usually states
  a capability, not a hedge.
- **E.** `INFO_CONTENT_THRESHOLD` stays 0.50.
- **C is dropped** from this ticket. "Names a person where a role is meant" needs context a sense
  lookup doesn't have; it is `PERSON_NAMED_NOT_ROLE` in #258's Goal vocabulary and belongs to the
  per-type Story/UseCase definitions there.

On project 621 this takes vague-word findings from 66 to about 36 (32 nouns plus about 4
weak words), with no verb among them.

**Rewritten vague-word AC:** no vague-word finding is raised for a verb, auxiliary or copula
("be", "creates", "covering", "says", "states"); `event`, `action` and `tool` are still raised;
`should` and `fast` are raised; `can` is not.

### Glossary rules, revised from the harness

Agreed 2026-09-28: the four passes below (already defined → not a term → worth defining → one
issue per project), a three-entity threshold for common multi-word phrases, and the
indefinite-opener rule. With two entities the revised rules raise about 51 distinct phrases; with
three, about 32, losing a few real terms (`CI pipeline`, `recovery ladder`, `session recording`)
along with most of the noise (`tool end`, `pill reads`, `manual pass`).

The first cut (threshold ≥ 2 entities, both verb-phrase filters) raised 106 distinct phrases,
more than today, because a phrase like "the stream" normalizes to a single common noun that
appears everywhere (`stream`, `session`, `one`, `run`, `page`, `path`, `step`, `top`). The
revised rules raise 52 distinct phrases, most of them real domain terms (`CI pipeline`,
`CloudWatch alarm`, `IVS channel`, `admin console`, `stream key`, `hot standby`, `gated watch
page`, `recovery ladder`, `session recording`, `Chris Peterson`):

- **After normalizing, a single common noun is never a candidate**, the same rule the assistant
  already applies to one-word phrases before normalization. A single word is raised only as a
  proper noun that isn't the first word of its sentence, or as an acronym (`IVS`, `CI`).
  Sentence-initial `Archive`, `Recover`, `Verification`, `Force`, `Start` go.
- **Coordinated phrases are rejected** (`stream key and RTMP endpoint` is two terms), and so are
  possessives (`CON-3685's architecture description`).
- **The sentence-verb filter is narrowed** to a first word that starts its sentence, ends in `-s`
  and whose singular is a verb (`Alarms route`, `Spans room creation`). The wider version also
  rejected `Bus factor`, `open finding`, `second audience` and `watch page`.
- **The determiner filter stays as it is.** All 16 of its rejections were verb phrases or
  clauses (`archives the room`, `not a person`, `fairly sure the problem`).
- **A token that is the letter part of a ticket key** (`CON` in `CON-3685`) is not a candidate.
- The existing-vocabulary match works as planned: 45 candidates matched a term or actor,
  including `room` ×13, `test room`, `panelists` (via the singular) and `dry-run event`.
- 25 of the 360 raw phrases don't occur in the source text as quoted (the joined-token problem);
  rebuilding from source fixes the quotes.

### Spelling rules, revised from the harness

102 unknown tokens (46 distinct). The planned skips cover 38: acronyms (`MP4`, `AWS`, `RTMP`,
`S3`, `IAM`), hyphenated compounds (`co-hosts`, `mid-session`, `re-encoding`, `non-production`)
and project vocabulary (`remux`, `passthrough`). The harness adds three more skips:

- **CamelCase** (a lower-case letter followed by an upper-case one inside the token):
  `CloudWatch`, `MediaConvert`, `CloudFront`, `DeleteChannel`.
- **A digit anywhere in the token:** `v1`, `h1`.
- **An underscore:** `ENTRA_SETUP`.

Still reported, correctly: `livestream` and `webinar` (until they're glossary terms), `Entra`,
`Microsoft`, and ordinary words missing from the dictionary (`app`, `offline`, `lifecycle`,
`repo`, `checkbox`, `onboarding`, `stakeholders`). Those last are install-dictionary additions
(the #313 admin page), not code.

**ReportGenerator is out.** Its Text is an HTML/XSL template; every markup token (`quot`, `div`,
`td`, `>`) in the harness came from one report generator. The assistants don't analyze report
generators today and `dispatchProject` won't enumerate them; the harness now skips them too.

### Spelling (`LexicalSpellingAssistant`)

A token is not reported when any of these hold (checked after the existing
punctuation/number/possessive skips and before the spell checker):

- it equals, case-insensitively, a glossary term name or actor name in the project, or one word
  of a multi-word term or actor name (vocabulary read once per run);
- it has an acronym shape: `^(?=.*[A-Z])[A-Z0-9]{2,}s?$` (`MP4`, `RTMP`, `CI`, `APIs`);
- it is hyphenated and every part is a known word (or its plural) or a known prefix (`co`, `re`,
  `pre`, `non`, `multi`, `sub`, `self`, `anti`, `semi`, `post`, `inter`, `cross`, `mid`);
  otherwise the whole token is reported, as today.
- it is CamelCase (a lower-case letter followed by an upper-case one), contains a digit, or
  contains an underscore (from the harness: `CloudWatch`, `v1`, `ENTRA_SETUP`).

Everything else is unchanged, including the #313 project dictionary layer.

### Glossary candidates (`LexicalGlossaryTermAssistant`)

- **Normalize** a candidate: drop a leading determiner, then rebuild the text from the entity's own
  text (below). The normalized phrase is the issue's `word` and the key's subject; comparison is
  case-insensitive.
- **Existing vocabulary:** skip when the normalized phrase matches a glossary term name (a synonym
  is itself a term, so `Test room` → `Dry-run event` is covered) or an actor name, either as
  written or with its last word singularized by the existing lemmatizer. A match still emits
  `ADD_GLOSSARY_TERM_REFERER`, as today.
- **Verb-phrase filter:** reject a candidate that has a determiner after its first word (`archives
  the room`, `downloads the resulting MP4`), or whose first word starts its sentence, ends in
  `-s` and has a verb singular (`Alarms route`, `Spans room creation`).
- **Coordination and possession:** reject a candidate containing `and`/`or` or a possessive `'s`.
- **Single words:** after normalizing, a single common noun is never a candidate. A single word is
  raised only as a proper noun (NNP/NNPS, not the first word of its sentence) or an acronym, and
  never as the letter part of a ticket key (`CON` in `CON-3685`). A multi-word run of NNPs
  (`Chris Peterson`) is a proper noun wherever it is.
- **Indefinite openers:** reject a candidate whose first word is `anything`, `something`,
  `everything`, `nothing`, `each`, `every`, `any` or `one` (`anything else`).
- **Threshold:** raise a multi-word candidate only if the normalized phrase appears (word-boundary,
  case-insensitive) in the Name or Text of at least three text entities in the project, or it is a
  proper noun. The project's text is read once per run. (Rules revised from the harness; see
  "Glossary rules, revised from the harness".)
- **One issue per project.** The action carries `scope=PROJECT` and `shareKey =
  glossary-term:<normalized, lower-cased>`. The applicator, when it has no existing issue for
  this entity's action key, looks at the assistant's `ACTIVE` findings in the project
  (`AssistantFindingRepository.findByAssistantIdAndProjectIdAndState`) for one whose key suffix
  (after `assistant:type:id:`) is the share key and whose issue is open, and attaches this entity
  to that issue instead of creating a new one. Findings stay per entity (one `assistant_findings`
  row per entity, all pointing at the same annotation), so the existing per-entity cleanup
  detaches one entity at a time, and the last detach deletes the issue.
- **Project-wide ignore, derived rather than stored** (revised while implementing; the first
  design stored an extra `target_type = 'Project'` row). Ignoring the shared issue already records
  one `ignored_findings` row per finding behind it, i.e. per entity. The applicator treats a
  `scope=PROJECT` action as ignored when any ignored key in the project has the same assistant
  and key suffix, so a new entity using the phrase is covered too. No schema, export, import or
  list change: the rows are ordinary per-entity ignores. `DeleteIgnoredFindingCommandImpl`
  removes a glossary-term ignore's siblings (same assistant and key suffix) with it, so removing
  it from the list un-ignores the phrase everywhere; the entity whose row was removed is
  re-analyzed at once, the others on their next analysis.
- The glossary check keeps its current skip of determiner-only and possessive phrases.

### Quoted evidence (#269 remainder, all four assistants)

- **Rebuild from source.** A quoted phrase is located in the property's original text by matching
  its tokens in order with `\s*` between them, and the matched substring is what the finding
  quotes. `force - stops the stream` becomes `force-stops the stream`.
- **Check before emitting.** A word or phrase that can't be found in the property text at word
  boundaries (whitespace-normalized, case-insensitive) is dropped, with a WARN naming the
  assistant, entity and property. No finding is emitted for a fragment, so the `ermissions`
  cascade can't recur.

### Partial analysis

- An assistant that fails on one property marks its result incomplete (`metadata.incomplete =
  true`, with `metadata.failedProperties`) instead of returning a result that looks complete. All
  four lexical assistants use one shared per-property wrapper, so spelling gains the same
  per-property isolation the others have.
- The applicator applies an incomplete result's actions but skips `reconcileStaleFindings` for
  it, so nothing is auto-resolved on the strength of an analysis that didn't finish.
- `AssistantRunWorker` records every thrown or incomplete assistant in the run's `error_summary`
  with `error_kind = 'PARTIAL'`; the status stays `SUCCEEDED` (the other assistants' results are
  real). A result that fails to apply is recorded the same way. As built: the wrapper is
  `PropertyChecks`, and the store method is `AssistantRunStore.markPartial`.

### Advisory findings

All four lexical assistants send `mustResolve: false`. `StepStructureAssistant` and AI findings
keep theirs. The applicator's default for a missing `mustResolve` stays `true` (callers other than
the lexical assistants rely on it).

### Old issues on an analyzed entity

After an SPI lexical assistant's run is applied, for the dispatch target: every unresolved
`LexicalIssue` on the entity whose `source` is null (written by the old path), whose
`annotatable_entity_property_name` is one the assistant analyzes (Name/Text, or none for the
glossary check), and whose word the run did not produce, is removed from the entity through
`RemoveAnnotationFromAnnotatableCommand`. Resolved issues are never touched.

As built (step 8): each assistant finds these while it analyzes (`LegacyLexicalIssues`) and puts a
`REMOVE_ANNOTATION_FROM_ANNOTATABLE` action with `metadata.legacyAnnotationId` ahead of its other
actions. An issue is the assistant's kind by the text the old path wrote ("is not recognized",
"is vague", "is complex", "potential glossary term"), and is considered only on a property the
assistant finished (the glossary kind only when both finished). One the run reports again is left
alone: `EditLexicalIssueCommand` reuses it by word and property, so it becomes the assistant's issue
with its positions and discussion. A complex-sentence issue has no word, so it is always replaced;
the removal going first means the run's own issue is created fresh rather than reusing it. The
applicator checks again at apply time (still unresolved, still no `ASSISTANT:` source) and makes
no removals for an incomplete result.

### Import and project re-analysis

- `AnalysisRequestDispatcher.dispatchProject(Project, User)` enumerates the ids of the project's
  goals, stories, actors, use cases, scenarios, steps and glossary terms in one
  read, builds one `AnalysisRequest` each, and calls a new
  `AssistantDispatcher.dispatchAll(List<AnalysisRequest>)`.
- The entities are listed in one read-only transaction and dispatched after it ends, so no run
  starts before the read finishes. Report generators and stakeholders are not analyzed.
- `dispatchAll` queues every run record, then submits **one** executor task that runs them in
  order, so a large project takes one executor slot. A run that throws is logged and the batch
  goes on. A rejected submit marks all its runs FAILED.
- New marker interface `ProjectAnalysisRequestSource` (`project-domain`): `Project
  getAnalysisProject()`, `User getAnalysisTriggeredBy()`. `AnalysisInvokingCommandHandler` checks
  it first and calls `dispatchProject`.
- `ImportProjectStreamingCommandImpl` implements it; its `invokeAnalysis()` body goes, so
  `AssistantFacade.analyzeProject` has no production caller.
- New `AnalyzeProject` command (`project-jpa`), project-scoped, `Annotation[Edit]`, implements
  `ProjectAnalysisRequestSource`. Input `AnalyzeProjectInput(projectName)`. Gateway-allowlisted
  with a `@CommandDescription` per #296.

### Per-project assistant settings

- **Schema (V24):** `project_assistant_settings (project_id bigint, assistant_id varchar(200),
  enabled bit(1) not null, updated_by_id bigint null, date_updated datetime(6) null, primary key
  (project_id, assistant_id))`. (As built: `updated_by_id`, not `created_by_id`, since a row is
  rewritten each time the setting changes.) No foreign keys, like `ignored_findings`; the project delete path
  removes the rows.
- **Domain:** `ProjectAssistantSettingsStore` in `project-domain` (`Set<String>
  disabledAssistants(Long projectId)`, `void setEnabled(Long projectId, String assistantId,
  boolean enabled, User by)`, `int deleteForProject(Long projectId)`); JPA implementation in
  `project-jpa`.
- **Which assistants:** `RequelAssistant` gains `default boolean projectSwitchable() { return
  false; }` and `default String displayName() { return assistantId(); }`. The four lexical
  assistants return true and a human name. Setting an unknown or non-switchable id is refused.
- **Registry:** `SimpleAssistantRegistry.findAssistantsFor` drops switchable assistants disabled
  for `context.projectRef()`. No row means enabled. As built, the registry is also the new
  `SwitchableAssistantCatalog` (`project-domain`), which the command uses to refuse an unknown id
  and the query uses for the list, so neither the project layer nor the service layer depends on
  the assistant SPI. Display names: Spelling, Vague words, Glossary candidates, Complex sentences.
- **Disabling leaves existing findings as they are;** a re-run with the assistant off doesn't
  reconcile its findings. The UI says so next to the toggle.
- **Command:** `EditProjectAssistantSetting(projectName, assistantId, enabled)`, project-scoped,
  `Project[Edit]`, gateway-allowlisted with a `@CommandDescription`.
- **Query:** `GET /api/projects/{name}/assistants` → `[{assistantId, displayName, enabled}]` for
  the switchable assistants.
- **Angular:** the "Assistants" panel on the overview page: one toggle per assistant,
  plus "Re-run analysis" (dispatches `AnalyzeProject`, then says analysis is running). As built:
  its own component, `ProjectAssistantsPanelComponent`, with `ProjectAssistantsService`; a flip
  applies at once and is put back if the server refuses; status and errors are announced.
- **Delete:** `DeleteProjectCommandImpl` calls `deleteForProject`.

### Migration `V24__lexical_advisory_and_assistant_settings.sql`

```sql
CREATE TABLE IF NOT EXISTS `project_assistant_settings` ( ... as above ... );

UPDATE `annotations` SET `must_be_resolved` = 0
 WHERE `annotation_type` = 'com.rreganjr.requel.annotation.impl.LexicalIssue';
```

## Step by step

1. **Harness.** The dev endpoint and its TSV. Run it on project 621 (commands handed over), read the
   output, fill in "Vague-word rule", rewrite the vague-word ACs. **Checkpoint: thumbs-up.**
2. **Evidence.** Rebuild-from-source and the quoted-evidence check, shared by the four assistants
   (a small helper in `assistant-legacy-nlp`). Dash/non-ASCII regression tests.
3. **Spelling.** Vocabulary, acronym and hyphen skips.
4. **Vague words.** The rule chosen at the checkpoint.
5. **Glossary candidates.** Normalization, existing-vocabulary match, verb-phrase filter,
   proper-noun rule, threshold. Then the project-scoped issue and the derived project-wide ignore
   in the applicator, and sibling removal in `DeleteIgnoredFindingCommandImpl`.
6. **Advisory.** `mustResolve: false` in the four assistants.
7. **Dispatch.** `dispatchAll`, `ProjectAnalysisRequestSource`, `dispatchProject`; move import
   onto it; `AnalyzeProject` with input DTO, registrar entry, allowlist and description.
8. **Old issues and partial runs.** Removal of unowned, unresolved lexical issues on an analyzed
   entity; incomplete results, the reconcile skip and `PARTIAL` on the run.
9. **Settings.** V24, store, registry filter, command, query, delete path.
10. **Glossary terms.** `EditGlossaryTerm` as an `AnalysisRequestSource`;
    `ProjectAnnotatableTextEditorConfiguration` entry. (Glossary terms went into
    `dispatchProject` in step 7.)
11. **Angular.** The Assistants panel and Re-run button on the overview page.
12. **Verify.** `tmp/268-verify.sh` (below). Then on the local stack: add Webinar and Livestream
    as glossary terms in project 621, re-run analysis, run `tmp/268-counts.sql` into
    `tmp/268-after.txt`, and record the before/after table on #268.

## Test plan

### `assistant-legacy-nlp` (unit)

- `LexicalSpellingAssistantTest`: a glossary term name, one word of a multi-word term, an actor
  name and one word of an actor name are not reported; `MP4`, `RTMP`, `CI`, `APIs` are not
  reported; `co-hosts` is not reported and `co-hsts` still is; `CloudWatch`, `v1` and
  `ENTRA_SETUP` are not reported; an unknown word still is.
- `LexicalGlossaryTermAssistantTest`: `the room` with term `Room` emits a referer, not an issue;
  `a test room` matches `Test room`; `rooms` matches `Room`; `archives the room` and
  `downloads the resulting MP4` are rejected; sentence-initial `Archive` is not a proper noun but
  mid-sentence `Conduit` is; `Chris Peterson` is raised from one entity; `the stream` (a single
  common noun after normalizing) is not; `stream key and RTMP endpoint` and a possessive phrase
  are rejected; `Bus factor` is not rejected as a verb phrase; `CON` in `CON-3685` is not raised;
  `anything else` is rejected; a common two-word phrase in two entities is not raised and in three
  is; a common phrase in one
  entity is not raised and in three entities is; the action carries `scope=PROJECT` and the
  project ignore key; a project-ignored phrase is not emitted.
- `LexicalVagueWordAssistantTest`: cases from the chosen rule, including "be", "creates" and
  "covering" not reported.
- Evidence helper: `force - stops` is rebuilt as `force-stops`; a token absent from the text
  (`ermissions` against `permissions`) is dropped; em dash, en dash, curly quotes and a non-ASCII
  letter around a phrase keep word boundaries.
- All four: `mustResolve` is `false` (pins the default).
- All four: a processor that throws on the Text still returns the Name's findings, with
  `incomplete = true` and `failedProperties = [Text]`.

### `assistant-core` (unit)

- `AssistantRunWorkerTest`: an assistant that throws leaves `error_kind = PARTIAL` and names the
  assistant in `error_summary`; the run is `SUCCEEDED` and the other assistants' results apply.
- `CommandBackedAssistantResultApplicatorTest`: an incomplete result applies its actions and
  auto-resolves nothing; a complete one still reconciles.
- `CommandBackedAssistantResultApplicatorTest`: a `scope=PROJECT` action with an open issue for
  the same phrase on another entity attaches to it (one issue, two annotatables, two findings); a
  re-run on one entity that no longer has the phrase detaches only that entity; detaching the
  last deletes the issue; an ignored key on another entity with the same assistant and suffix
  skips the action, and one from another assistant doesn't; a resolved shared issue isn't joined.
- `SimpleAssistantRegistry`: a disabled switchable assistant is not returned for that project and
  is for another; a non-switchable assistant can't be disabled.
- `AssistantDispatcherImpl.dispatchAll`: N runs queued, one executor submit, runs in order; a
  rejected submit marks all N FAILED.

### `requel-app` (integration, H2)

- `ProjectAnalysisIT` (new): import runs through the SPI, one `AssistantRun` per text entity and
  no call into the old `LexicalAssistant`; `AnalyzeProject` re-analyzes every entity; renaming a
  scenario and re-running auto-resolves the old Name's spelling issue; an old-path issue (native
  `JdbcTemplate` insert with `source` null) on an analyzed entity is removed when the run doesn't
  reproduce it, and a resolved one is kept; `AnalyzeProject` without Annotation[Edit] is refused.
- `LexicalFindingScopeTest`: the same phrase in two entities yields one glossary issue on both
  (spelling stays one issue per entity).
- `IgnoredFindingTest`: ignoring the shared issue on one entity leaves a later entity using the
  phrase unflagged; removing one of the ignore rows removes them all and the phrase is raised
  again.
- `ProjectAssistantSettingsIT` (new): disabling spelling stops spelling findings and leaves the
  other three running; the query lists the four; `EditProjectAssistantSetting` needs
  Project[Edit]; deleting the project deletes its rows.
- `OpenNLPUpgradeTest`: a multi-sentence text with em and en dashes sentencizes on boundaries.
- `CommandGatewayIT` / #296 catalog tests: the two new commands have record inputs and
  descriptions.
- Editing a glossary term raises a spelling finding on it, and
  Fix Spelling applies to it.

### `requel-app` (MySQL, Testcontainers)

- `LexicalAdvisoryMigrationMySqlIT` (the `IssueSeverityMigrationMySqlIT` pattern): V24 creates
  the table and sets `must_be_resolved = 0` on lexical issues only.

### Angular

- The Assistants panel (`project-workspace.spec.ts`): renders the four toggles from the query, a toggle sends
  `EditProjectAssistantSetting`, Re-run sends `AnalyzeProject` and announces it, a user without
  Project[Edit] sees the toggles disabled.
- e2e: the project workspace page object gains the Assistants panel; one flow toggles an assistant and
  re-runs analysis. Runs in CI.

### Verify script (`tmp/268-verify.sh`)

```bash
mvn clean verify
cd requel-angular
CI=1 npx ng test --watch=false
npx tsc -p tsconfig.app.json --noEmit && npx tsc -p tsconfig.spec.json --noEmit
npx ng build --configuration development
```

## Out of scope

- Deleting the old `project-jpa` assistant package and `AssistantFacade`. With import moved,
  nothing in production reaches `LexicalAssistant`, but `AssistantFacade` is a constructor
  argument of ~40 commands and their dead `invokeAnalysis()` bodies; removing it is a mechanical
  sweep best done on its own. Not filed yet; ask before filing.
- #270 (annotations never invalidated when text changes) beyond what the SPI already does and the
  old-issue removal above.
- Automatic re-analysis when a glossary term, actor or assistant setting changes. "Re-run
  analysis" is manual.
- Assistant settings in project XML.
- Porting the lexical assistants to #258 definitions.
- Glossary term renames. Renaming a term (say "Dry-run event" to "Dry-run webinar", after the
  vague-word finding on "event") changes only the term; text that still says "dry-run event"
  stops matching and comes back as a glossary candidate. Two fixes, both wanted: keep the old
  name as a synonym of the new one, and offer to rewrite the referers' text. Filed as #348
  (v2.1); body in `doc/work/2.0/268-followup-glossary-rename-issue.md`.
- Primary verbs for commands. `DependencyPrimaryVerbFinder` finds a verb only through a subject,
  so "Create the room" has none, and the step-structure check gives a step that names no actor
  the same could-not-analyze note as text it couldn't parse. Found in step 12 (109 "No primary
  verb found" lines in one re-run of the roundtable project). Filed as #349 (v2.1); body in
  `doc/work/2.0/268-followup-imperative-primary-verb-issue.md`.
- Spelling and vague findings stay per entity and property (#320); only glossary candidates become
  per project.

## Risks

- **Shared issues against #320's scoping.** #320 made lexical lookups per annotatable on purpose.
  The project-scope lookup is opt-in through `scope=PROJECT`, used by the glossary check only, and
  covered by `LexicalFindingScopeTest` in both directions.
- **A shared issue's provenance key.** `stampProvenance` writes the applying action's key to
  `annotations.assistant_idempotency_key`, so a shared glossary issue carries the key of the last
  entity that joined it. Nothing looks an issue up by that column for glossary findings (the
  findings table is the index), but the value is "one of the entities", not "the" entity.
- **Threshold cost.** Each glossary run scans the project's text once. Fine at roundtable size
  (≈70 entities); a much larger project would want a per-run cache or an index.
- **One task per re-analysis.** A big project re-analyzes serially in one executor thread; that is
  slower than parallel but can't be rejected half-way. The UI only says it has started.
- **Behaviour change in a release candidate.** Most existing lexical noise disappears on the next
  analysis, and every lexical issue becomes advisory. That is the point, but it changes what users
  see without their doing anything.
- **Verb-phrase filter false negatives.** A real term starting with a determiner-like word after
  its first word would be dropped. The harness's glossary section is the check.
- **Old-issue removal** relies on `source` being null only for old-path issues. On project 621 every
  open lexical issue has a source (`tmp/268-sources.sql`), so the removal is exercised only by the
  test fixture and by imported projects.

## AC mapping

| AC | Where |
|---|---|
| #268: no spelling finding for a glossary term; none for MP4, RTMP, webinar, livestream, co-hosts | Spelling; webinar/livestream via glossary terms (decision 4); `LexicalSpellingAssistantTest`; after count |
| #268: no vague-word finding for "be", "creates", "person", "action", "event", "covering" | Rewritten ("Vague-word rule"): no verb is raised ("be", "creates", "covering"); "person" isn't flagged on today's parse; "action" and "event" stay, deliberately |
| #268: a repeated noun phrase produces at most one glossary-candidate finding per project | One issue per project; `LexicalFindingScopeTest` |
| #268: lexical findings default to advisory; a test pins the default | Advisory; all four unit tests; V24 |
| #268: each lexical assistant can be disabled per project, the others keep running | Settings; `ProjectAssistantSettingsIT` |
| #268: before/after count on the roundtable project recorded on the ticket | Step 12 |
| #269: no finding on the roundtable project quotes a fragment | #314 plus the evidence check; after count |
| #269: regression test with em dashes, en dashes and non-ASCII punctuation | Evidence helper tests; `OpenNLPUpgradeTest` |
| #269: a fragment that fails the boundary check produces no finding | Evidence check |
| #269: the `ermissions` cascade can't recur | Evidence check; helper test |
