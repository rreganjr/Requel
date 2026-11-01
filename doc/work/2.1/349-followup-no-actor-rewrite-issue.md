Follow-on to #349. When a step doesn't say who does it, give an example built from the step, and offer one-click rewrites that name the actor.

### Why

#349 raises a LOW `no-actor` issue on a step written as a command ("Download the resulting MP4"). Every one of these issues gives the same example, "The host starts the livestream". That example fits a livestream project only, and it doesn't show the reader their own step rewritten. Commands are 13 of the 15 steps on a real project, and each one is fixed by hand: open the step, retype the name with an actor in front.

### Work

1. **The example comes from the step.** The message reads: `The step "Download the resulting MP4" doesn't say who does it. Name the actor, for example "Host downloads the resulting MP4", or "The system downloads the resulting MP4" when the system does it.`
   - The actor comes from the step's use case. It is found the way the actor-in-use-case check finds it: up through the scenarios that use the step to their use cases.
     - Exactly one primary actor: that actor.
     - Several: the first by name.
     - None: `<actor>`.
   - The actor's name is kept as written, without "The", so a name like "IT Admin" keeps its case.
   - The verb is put into its "-s" form by rule:
     - "-es" after s, x, z, ch, sh or o.
     - A consonant plus "y" becomes "-ies".
     - A short list for the irregulars: be, have, do, go.
     - A hyphenated verb changes its last part: "Force-stop" becomes "force-stops".
     - The words after the verb stay as written.
2. **One-click rewrites.** The `no-actor` issue gets a position for each rewrite in the message ("Rewrite as "Host downloads the resulting MP4"" and "Rewrite as "The system downloads the resulting MP4""), next to "Do nothing.". Choosing one sets the step's name to that text and resolves the issue.
   - No existing position kind can do this. `CHANGE_SPELLING` swaps one word, and using it on the verb would make this a word-level issue, tied to that word. It would then merge with a spelling issue on the same word, the limit noted in #349.
   - A new position kind is needed, for example `REWRITE_NAME`. It carries the proposed text, and `CommandBackedAssistantResultApplicator` and the position's resolve command need to handle it.
3. **Decide at review:**
   - which rewrite positions to offer when the use case has several actors: the primary actor only, or one position per actor up to a cap;
   - whether the proposed text needs a Flyway migration (a new column), or fits how `ChangeSpellingPosition` stores its proposed word;
   - whether a scenario that is used as a step gets the rewrites too (#349 gives a command-named scenario no finding, so probably not).

### AC

- The step "Download the resulting MP4", in a use case whose primary actor is "Host", raises the `no-actor` issue with the two examples above and three positions: the two rewrites and "Do nothing.".
- "Force-stop the active stream" gives "Host force-stops the active stream". "Issue a new takeover key" gives "Host issues a new takeover key".
- A step with no use case gets `<actor>` in the example and only the system rewrite.
- Choosing "Rewrite as "Host downloads the resulting MP4"" changes the step's name to that text. A re-run raises nothing on the step.
- Re-runs update the issue in place: same key, positions not duplicated.

### Not in scope

- Which steps count as commands (#349).
- Alternating actor and system steps (the other #349 follow-on).
