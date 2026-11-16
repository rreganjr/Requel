Follow-on to #349. Check that a scenario's steps alternate between an actor and the system.

### Why

#349 settled how one step reads: "&lt;actor&gt; does something" or "the system does something",
where the system is the implicit actor responding to a user action. A well-structured scenario
goes further: its steps alternate, an actor acts and the system responds. The step check sees
one step at a time, so it can't tell a scenario of five actor steps in a row, or a system step
with no actor step before it, from a well-formed flow.

### Work

- A scenario-level check (on epic #267) that reads the scenario's steps in order, using #349's
  subject classification for each step (actor, the system, a command, or unreadable).
- Raises an advisory (LOW, "Do nothing.") issue on the scenario when:
  - two system steps follow each other with no actor step between them;
  - the scenario opens with a system step.
- Decide at review:
  - whether several actor steps in a row (a person doing a sequence of things) are fine;
  - how a sub-scenario used as a step counts (as the actor that initiates it, per #349);
  - how command-form steps count (they already get #349's "doesn't say who does it" issue).

### AC

- A scenario "host acts, system responds, host acts, system responds" raises nothing.
- "System responds, system responds" raises the issue naming the two steps.
- A scenario opening with a system step raises the issue.
- The check runs on a scenario edit and on whole-project analysis, and re-runs update the
  issue in place.

### Not in scope

- How a single step reads (#349).
