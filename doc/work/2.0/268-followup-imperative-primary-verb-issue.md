Follow-on to #268. The NLP pipeline finds no primary verb in a command ("Create the room"), so the step-structure check can't tell a step that names no actor from text it couldn't parse.

### Why

Found while verifying #268 on the roundtable project. One whole-project re-run logged "No primary verb found in text" 109 times. Most were use case and scenario names written as commands: "Create the room", "Start the livestream", "Hand the stream key to Zoom", "Recover a failing stream mid-session". The rest were noun-phrase names ("Webinar", "Zoom Host") and bracketed source notes ("[Ingested verbatim from CON-3685 acceptance criterion 1.]"), which rightly have no verb.

`DependencyPrimaryVerbFinder` takes the primary verb to be the governor of the sentence's subject (a nominal subject, passive subject or agent). A command has no subject, so it gets no primary verb even though the verb is its first word. Two things read the primary verb:

- `StepStructureAssistant` finds the step's actor through it. With none, it posts "The assistant could not analyze the structure of the step text. It may be too complex or didn't have an identifiable syntactic subject." That is the same note for "Start the livestream", a clear step that just doesn't say who does it, and for text the parser really couldn't handle. `Scenario` extends `Step`, so every scenario named as a command gets the note too.
- `SemanticRoleLabeler` labels roles relative to it, so a command gets no roles.

### Work

- **Commands get a primary verb.** When a sentence has no subject relation and the root of its dependency parse is a base-form verb (`VB`), `DependencyPrimaryVerbFinder` uses that root as the primary verb and marks the sentence as imperative, so callers can tell "no actor stated" from "no verb found".
- **Step structure names the problem.** An imperative step raises its own advisory finding instead of the could-not-analyze note: `The step "Start the livestream" doesn't say who does it. Name the actor, for example "The host starts the livestream."` The could-not-analyze note stays for text with no verb at all.
- **Scenario names** (decide at review): a scenario's own name is usually a goal written as a command ("Create the room"). Either skip the actor check on a scenario's name and check only its steps, or raise the finding there too.

### AC

- `processText("Create the room")` has primary verb "Create" and is marked imperative; "The host creates the room" still has primary verb "creates" and is not imperative; "Webinar" still has none.
- A step named "Start the livestream" gets the no-actor finding and no could-not-analyze note. A step "The host starts the livestream", with an actor named Host, raises nothing new. "[Ingested verbatim from CON-3685 acceptance criterion 1.]" gets no no-actor finding.
- A scenario named "Create the room" gets whatever the review decides, pinned by a test.
- Re-running analysis on the roundtable project: the could-not-analyze count before and after is recorded on the issue.

### Not in scope

- Parser mistakes on full sentences, for example "Every open finding from the round-1 review is either fixed or explicitly closed with a written reason." getting no primary verb.
- The lexical checks (#268).
