# Emitted CON-3685 v2 — after three sources

Second emission from Requel project "PlatformQ Roundtable" (id 621), now carrying CON-3685,
CON-3686, the Roundtable Production Guide v1.1 and round-1-review.md. Compare against
`doc/pq-roundtable-emitted-CON-3685.md` (v1, tickets only). **Local document only — nothing was
written to Jira.**

========================= BEGIN GENERATED TICKET =========================

# Take ownership of roundtable.medlive.com

> **Read this first.** Review finding L1 may be live in production. Confirm the deployed
> `ENTRA_ADMIN_ROLE` value before any other work on this epic proceeds — see Blocking Check below.

## Problem Statement

roundtable.medlive.com relays a Zoom webinar panel out to a second, passive audience who never join
the Zoom meeting and are invisible to the panelists. Zoom streams to Amazon IVS over RTMPS, viewers
watch a password-gated web page, and when a room is ended the IVS recording is remuxed to a single
MP4 the operator downloads from the admin console.

It is deployed, working, and has been used for a live session. It is also owned by one person. A
single-owner production system with a bus factor of one is a risk to any event scheduled on it, and
several of its known failure modes can only surface in ways an operator would not notice until an
event is already running.

## Blocking Check

The committed `cdk.json` carries `"roundtable:entraAdminRole": ""`, and `lambda/admin.ts` gates the
app-role check behind `if (ADMIN_ROLE)`. An empty value skips the check. If the deployed stack was
synthesised from HEAD, **any authenticated member of the Medlive tenant can reach `/admin`** — end a
live room, rotate keys, download recordings. Read `ENTRA_ADMIN_ROLE` from the AdminFn Lambda's
environment before anything else; an empty result means the gate is off. Tracked as CON-3687.

## Goals

Conditions that must hold. Verification lives in Scenarios.

1. **The Operator role is fillable by more than one person** — role-based admin access, no personal
   credentials on the path, no step that exists only in someone's head.
2. **Conduit is the registered owner** of the repo, the AWS stack and the alarms. *(The
   administrative record of goal 1, not a substitute for it.)*
3. **Only authorized operators can reach the admin console.** Tenant membership is not sufficient.
   *(See Blocking Check — this may be false today.)*
4. **No merge lands that fails the quality gate** — typecheck, unit tests, cdk synth, check-cycles,
   check-iam, and the UI suite with Playwright actually installed.
5. **Alarms exist, and every one of them reaches a Conduit-owned destination.** *(Stated as two
   conditions deliberately: at review time the stack created zero alarms, which satisfies "all
   alarms route correctly" perfectly and uselessly. Subscription ownership is CON-3690.)*
6. **Every alarm has a documented first response.**
7. **Every round-1 review finding has a recorded disposition** — fixed, or not fixed with a tracking
   reference. **26 are fixed and 6 are not.** *(Both source tickets say 24 and 8; the review states
   that figure "was a recollection, never a count".)*
8. **Viewers remain invisible to the panelists.**
9. **A viewer is never shown the stream unless the room is live** — which is what makes it safe to
   rehearse a real broadcast in `test` with an audience already in the room.
10. **Disclosure to the panel is guaranteed by the platform, not by a person remembering.**
11. **A wedged stream is recoverable without any viewer action** — 20-30s on cold standby, ~5s on
    hot.

## Actors

Operator · Viewer · Panelist · Zoom Host · Conduit Engineer.
*Platform DevOps is a stakeholder, not an actor: they configure Entra and AWS account settings, and
no use case contains a step they perform.*

## Use Cases

Run a roundtable session · Watch a session as a viewer · Deliver the session recording · Sign in to
the admin console · **Recover a failing stream mid-session** · Archive a room · Respond to a
CloudWatch alarm.

**Recovery ladder** (strict order, no viewer action at any rung): issue a new takeover key → promote
the standby channel → force-stop the active stream → escalate when both channels are unusable.

**Manual verification pass** (extracted from a CON-3686 acceptance criterion, which stated six steps
as one checkbox): create a test room → view as viewer → *rotate the stream key* → force-stop →
end the room → download the MP4. **Open: the guide states that Rotate stream key is not a recovery
action and cuts off anyone mid-stream, while New takeover key is the recovery control. Confirm which
this step means before running it.**

## Glossary

**Room** — one relayed session with its watch page, recordings, event log and **two** independent
IVS channels, primary and standby. Four states: draft, test, live, ended. Ended is terminal.
*(Both source tickets describe a single channel.)*
**Ingest state** — offline/connected/live/starved/failed. A **second, independent** state machine;
confusing it with STATE is the most common misread of the console.
**End a room** — stops the relay, triggers the remux. **Archive a room** — destroys both IVS
channels, keeps the record, stats, recordings and MP4s. **Purge** — deletes the room and its
history, and deliberately does not delete the video.
**New takeover key** vs **Rotate stream key** — see the Open item above; they are different controls
with different blast radius.
**Dry-run event** — canonical term; "test room" and "throwaway room" are the same thing.
**Attendee** — a Zoom role, *not* a roundtable Viewer.

## Intentional behaviour — do not "fix"

Zoom panelists see the livestream indicator and the recording notice. Expected, and not to be
suppressed. **Note the coupling:** that indicator is what makes goal 10 true. Adopting hot standby
puts an external encoder in the path, Zoom stops being the thing streaming, and the indicator
disappears — moving a compliance guarantee from the platform into the run of show.

## Open Questions

- Who is the authorized operator set? Goal 3 says who is excluded and never who is admitted.
- `End` and `archive` and `purge` are three distinct lifecycle actions. Confirm the state machine.
- No observability requirement exists for failures that only surface mid-event.
- Session cost (~$0.55 remux plus channel hours) is recorded as a fact with no budget goal.
- Storage expires at 90 days, raw recordings and MP4s alike. Nothing states what must outlive it.
- The MP4 download link carries no sign-in and lives 15 minutes. Is that the intended access model?
- M10, L7 and L8 are open review findings with no ticket.

## Traceability

Requel project "PlatformQ Roundtable" (id 621): 11 goals, 5 actors, 7 use cases, 8 scenarios,
4 stories, 10 glossary terms, 8 issues. Sources: CON-3685, CON-3686, Roundtable Production Guide
v1.1, round-1-review.md. Related: CON-3687, CON-3690, CON-3691.

========================== END GENERATED TICKET ==========================

## v1 → v2: what changed and what it means

**Goals went from 8 to 11, but the important changes are not the additions.** Three goals are new
(the live-state invariant, the disclosure guarantee, recoverability). Four existing goals changed in
ways that matter more:

| Goal | v1 | v2 | Source of the change |
| --- | --- | --- | --- |
| Entra gate | an Open Question about who is admitted | a **Blocking Check** at the top of the ticket | review L1 |
| Alarms | one goal, split from the conjunctive AC | **two** goals, because the set's existence is a separate condition | review M6 |
| Dispositions | "fixed, closed, or **deferred** with a rationale" | "26 fixed, 6 **not fixed**" | review tally |
| CI gate | absorbed the `test:ui` fix | unchanged | — |

**v1 enshrined an error while fixing a different one.** v1's goal 7 proposed the wording "fixed,
closed with a written reason, or deferred with a rationale and a review date". That resolved the
parent/child vocabulary mismatch correctly *and* baked in "deferred", which the review says was
never true — the six are not fixed, and one is live in production. A reconciliation between two
documents that agree with each other cannot detect that both are wrong.

**Nothing in v1 was fixable by more analysis.** Every material correction between v1 and v2 came
from a new source, not from thinking harder about the old ones. Two IVS channels instead of one,
four room states instead of the four I guessed, archive versus purge, takeover versus rotation, the
disposition counts, the live admin gate — none of these were inferable. *The quality of a living
document is bounded by its sources, and adding a source beats re-reading one.* For the assistants
work this argues that "ingest more" outranks "review harder" as a product priority.

**What the emit path still cannot do.**

- **There is no single read that returns project content.** `getProjectTree` returns names and ids
  only; a generator must walk it and fetch every entity, or go via the XML export. Emit needs a
  content read.
- **Provenance is still hand-carried.** v2 cites four sources and attributes specific claims to each
  — all of it from this session's memory, none from the model. The generator cannot state which goal
  came from which acceptance criterion or which page of the guide, and by v2 that is worse than it
  was at v1, because with four sources the reader's first question is "says who?".
- **Issues do not surface automatically.** The Blocking Check exists because a human knew issue 4666
  outranked everything. Nothing in the model ranks it.
- **The `ReportGenerator` hook exists and is unused.** The project carries one, "HTML Specification"
  (id 696), auto-created. That is where a deterministic renderer belongs, and both emissions were
  written by hand instead.

**What survived unchanged from v1**, and is therefore probably right: the split of the conjunctive
ownership AC into an outcome plus its administrative record, the extraction of the six-step AC into
a scenario, the recording of "viewers invisible to panelists" as a protected goal rather than an
incidental property, and the general shape — goals as conditions, verification as scenarios.
