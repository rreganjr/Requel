# Emitted CON-3685 — generation experiment

Generated from Requel project "PlatformQ Roundtable" (id 621) as a first test of the emit half of
the ingest → analyse → annotate → emit loop. Compare against the original CON-3685. Findings from
the experiment are at the bottom; everything above the rule is the generated artifact.

========================= BEGIN GENERATED TICKET =========================

# Take ownership of roundtable.medlive.com

## Problem Statement

roundtable.medlive.com relays a Zoom webinar panel out to a second, passive audience who never join
the Zoom meeting and are invisible to the panelists. Zoom streams to Amazon IVS over RTMP, viewers
watch a gated web page, and when a room is ended the IVS recording is remuxed to a single MP4 the
operator downloads from the admin console.

It is deployed, working, and has been used for a live session. It is also owned by one person. A
single-owner production system with a bus factor of one is a risk to any event scheduled on it, and
several of its known failure modes can only surface in ways an operator would not notice until an
event is already running.

## Goals

Each goal is stated as a condition that must hold. Verification lives in Scenarios, below.

1. **The Operator role is fillable by more than one person.** Running an event requires no specific
   individual: admin access is role-based rather than person-based, no personal credentials sit on
   the path, and no step exists only in someone's head.
2. **Conduit is the registered owner** of the roundtable repo, its AWS stack, and its alarms.
   *(This is the administrative record of goal 1, not a substitute for it — it can be satisfied by a
   CODEOWNERS edit while goal 1 still fails.)*
3. **Only authorized operators can reach the admin console.** Membership of the Microsoft Entra
   tenant is not sufficient; the authorized set is defined explicitly and is auditable.
   *(OPEN: the authorized set is not yet defined anywhere. See Open Questions.)*
4. **No merge lands that fails the quality gate.** The gate covers typecheck, unit tests, cdk synth,
   check-cycles, check-iam, and the UI suite.
5. **Every CloudWatch alarm reaches a Conduit-owned destination.**
6. **Every CloudWatch alarm has a documented first response.**
7. **Every round-1 review finding has a recorded disposition** — fixed, closed with a written
   reason, or deferred with a rationale and a review date.
8. **Viewers remain invisible to the panelists.** The relayed audience never joins the Zoom meeting
   and is not visible to the panel. *(Existing behaviour, recorded as a goal so a future change
   cannot remove it silently.)*

## Actors

| Actor | Role |
| --- | --- |
| Operator | Creates rooms, rotates stream keys, force-stops streams, ends rooms, downloads recordings, archives rooms |
| Viewer | Watches the gated page; never joins the Zoom meeting; invisible to panelists |
| Panelist | On the Zoom webinar panel; sees the livestream indicator and recording notice |
| Zoom Host | Hosts the webinar and starts the custom RTMP livestream |
| Conduit Engineer | Deploys, maintains CI, first responder to an alarm |

## Use Cases and Scenarios

**Run a roundtable session** (Operator) — primary scenario, six steps: create the room → hand the
stream key to Zoom → start the livestream → viewers watch the gated page → stop the livestream →
end the room.

**Manual verification pass on a test room** (alternative scenario of the above, six steps): create a
test room → view it as a viewer → rotate the stream key → force-stop the stream → *end the room*
(shared step) → download the resulting MP4. Run against a dry-run event, never a booked one.

**Watch a session as a viewer** (Viewer). **Deliver the session recording** (Operator).
**Sign in to the admin console** (Operator). **Archive a room** (Operator) — deletes the IVS channel
and the stream key; distinct from ending a room. **Respond to a CloudWatch alarm** (Conduit
Engineer) — no flow described anywhere yet.

## Glossary

- **Room** — one relayed session with its IVS channel, stream key, gated watch page and recording.
  Has at least four states: created, live, ended, archived. *Not defined in the source material;
  this definition is inferred and needs confirmation.*
- **End a room** — stops the relay and triggers the MediaConvert passthrough remux.
- **Archive a room** — deletes the IVS channel and stream key. **Not the same as ending a room.**
- **Dry-run event** — a non-production session used to exercise the tool end to end. Also called
  "test room" and "throwaway room" in the source; use the canonical term.
- **Attendee** — a Zoom webinar role. **Not** a roundtable Viewer, who never joins the Zoom meeting.
- **Passthrough remux** — the MediaConvert job that rewraps the IVS recording into one MP4 without
  re-encoding. ~$0.55 per 90-minute session, plus IVS channel hours.

## Intentional behaviour — do not "fix"

Zoom panelists do see the livestream indicator and the Zoom recording notice. That is expected and
is not to be suppressed.

## Open Questions

- Goal 3 says who must be excluded and not who must be admitted. Who is the authorized operator set?
- *End* and *archive* are distinct lifecycle states and neither is defined. Confirm the state machine.
- Goals 5 and 6 apply to alarms added in future, so they are standing rules rather than one-time
  conditions. Should they be a policy with an owner rather than acceptance criteria?
- No observability requirement exists for failures that only surface mid-event.
- The cost of a session is recorded as a fact with no budget goal attached.

## Traceability

Generated from Requel project "PlatformQ Roundtable" (id 621): 8 goals, 5 actors, 6 use cases,
7 scenarios, 4 stories, 8 glossary terms. Source: CON-3685, CON-3686.

========================== END GENERATED TICKET ==========================

## What the experiment showed

**The generated ticket is better than the original in the ways the analysis predicted, and that is
not a compliment to the generator — it is the analysis being applied.** Goals 1 and 2 are the split
of the original's conjunctive AC1; goal 4 silently absorbs the `test:ui` fix; goals 5 and 6 are the
split of AC5; goal 7 adopts a disposition vocabulary that both tickets can satisfy; goal 8 did not
exist as a requirement at all. The verification steps that were AC checkboxes are now a scenario.

**Structure generated deterministically; prose did not.** The Actors table, the Use Cases and
Scenarios list, the Glossary, and Traceability are mechanical projections of the model — an XSLT
over the project XML would produce them, byte-identically, every run. The Problem Statement is a
synthesis of four Story entities into two paragraphs, and no template can do that. The Open
Questions section is a projection of unresolved Issues, but choosing which to surface was judgement.

**The emitted ticket is not the original with fixes — it is a different document.** Everything the
original carried that is *not* a requirement was dropped: the resources list, the AWS account and
stack name, the repo script names, the architecture summary, the ENTRA_SETUP.md pointer. None of it
is a goal, a use case, an actor or a glossary term, so none of it survived the model. Some of it
matters. Either Requel needs somewhere to hold reference material attached to a project, or emit has
to merge generated sections into an existing ticket rather than replacing it.

**Provenance was needed immediately and was not there.** Writing "generated from CON-3685" by hand
was possible only because this session remembers the ingest. Nothing in the project records which
goal came from which acceptance criterion, so the generator cannot say "AC1 became goals 1 and 2",
which is exactly what a reviewer comparing the two documents needs. The tag experiment predicted
AC-level granularity would be missed before ticket-level identity; it was missed on the first emit.

**Round-tripping is lossy in a specific, nameable way:** the model keeps what is normative and drops
what is contextual. That is correct behaviour for a requirements model and wrong behaviour for a
ticket, which is why emit cannot be "export the project" — it has to be "render the normative
sections and leave the rest alone."
