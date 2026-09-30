# PlatformQ Roundtable

Project version: f5269dd639ce

## Goals

1. **Conduit is the named owner** — Conduit is the named owner of the roundtable repo, its AWS stack, and its alarms, and Chris Peterson is no longer a required participant in running an event. [Ingested verbatim from CON-3685 acceptance criterion 1.]
2. **Two engineers can run an event unassisted** — At least two Conduit engineers have independently run the tool end to end for a dry-run event without assistance. [Ingested verbatim from CON-3685 acceptance criterion 2.]
3. **Admin console gated on an Entra app role** — The admin console is gated on a Microsoft Entra app role, so tenant membership alone does not grant access. [Ingested verbatim from CON-3685 acceptance criterion 3.]
4. **CI blocks merges that fail the gate** — A CI pipeline blocks merges that fail typecheck, tests, cdk synth, check-cycles, or check-iam. [Ingested verbatim from CON-3685 acceptance criterion 4.]
5. **Alarms route to Conduit with documented responses** — All CloudWatch alarms route to a Conduit-owned destination, each with a documented first response. [Ingested verbatim from CON-3685 acceptance criterion 5.]
6. **Every round-1 finding is dispositioned** — Every open finding from the round-1 review is either fixed or explicitly closed with a written reason. [Ingested verbatim from CON-3685 acceptance criterion 6. The round-1 review recorded 0 Critical / 5 High / 15 Medium / 12 Low = 32 findings; CON-3686 accounts for them as 24 fixed and 8 deliberately deferred.]
7. **Viewers remain invisible to panelists** — The relayed audience never joins the Zoom meeting and is invisible to the panelists. [NOT an acceptance criterion. Extracted from CON-3685's problem statement, where it is stated as a description of how the system currently behaves. Recorded here as a goal so that the property is protected rather than incidental.]
8. **Deployed state matches the reviewed intent** — What is running in production is what the round-1 review intended, confirmed independently of the author of the fixes, before anything else is built on top of it. [Extracted from CON-3686's "Why This Matters". Its acceptance criteria are verification procedures; this is the requirement behind them.]
9. **A viewer sees the stream only when the room is live** — A viewer is never shown the stream unless the room is in state live. This is what makes it safe to rehearse a real broadcast in test with an audience already sitting in the room. [Roundtable Production Guide v1.1, "The invariant worth trusting" — stated as enforced in one place and covered by a test over every combination of states, not by a judgement call in the code. Absent from CON-3685 and CON-3686 entirely.]
10. **Disclosure to the panel is guaranteed by the platform** — Panelists are told the session is being streamed and recorded by the platform itself, not by a human remembering to say so. Today Zoom's livestream indicator and recording notice do this work and cannot be switched off. [Roundtable Production Guide v1.1. NOTE THE TRAP: adopting hot standby puts an external encoder in the path, Zoom is then no longer the thing streaming, and the indicator disappears — moving a compliance guarantee from the platform to a step in the run of show. The guide states this explicitly; neither ticket mentions it.]
11. **A wedged stream is recoverable without viewer action** — When ingest fails mid-session the operator can recover without any watcher re-entering a room ID, reloading a page, or doing anything at all. Cold standby recovers in 20 to 30 seconds; hot standby in about 5. [Roundtable Production Guide v1.1, sections 2 and 8. "Watchers never have to do anything during any of these" is stated as a property of every rung of the recovery ladder.]

## Actors

- **Operator** — Runs a roundtable session from the admin console: creates the room, rotates the stream key, force-stops the stream, ends the room, downloads the resulting MP4, and archives the room. A role, not a person — CON-3685 names Chris Peterson where it means this role, and the substantive requirement behind that goal is that this role must be fillable by more than one person.
- **Viewer** — A member of the second, passive audience. Watches a gated CloudFront-fronted web page, never joins the Zoom meeting, and is invisible to the panelists. Distinct from a Zoom Attendee despite the adjacent naming.
- **Panelist** — A participant in the Zoom webinar panel whose output is relayed to viewers. Sees the Zoom livestream indicator and the recording notice; CON-3685 records that as intentional and explicitly not to be suppressed.
- **Zoom Host** — Hosts or co-hosts the Zoom webinar and starts the custom RTMP livestream to the IVS channel. Distinct from Panelist — CON-3685 references a Zoom webinar permissions matrix covering what hosts, co-hosts, panelists and attendees can each see and do.
- **Conduit Engineer** — Deploys the stack, maintains the CI pipeline, and is the first responder to a CloudWatch alarm. Ops-side actor rather than an event-time participant.

## Use Cases

### Run a roundtable session

Relay a Zoom webinar panel to the passive viewer audience for the duration of a session. Spans room creation, the Zoom custom RTMP livestream to the IVS channel, viewers watching the gated page, and ending the room. [Derived from CON-3685's architecture description; no use case is stated as such in either ticket.]

Primary actor: Operator

**Primary scenario: Run a roundtable session**

1. Create the room
2. Hand the stream key to Zoom
3. Start the livestream
4. Viewers watch the gated page
5. Stop the livestream
6. End the room

**Scenario: Manual verification pass on a test room**

1. Create a test room for verification
2. View the test room as a viewer
3. Rotate the stream key during verification
4. Force-stop the stream during verification
5. End the room
6. Download the resulting MP4

### Watch a session as a viewer

A member of the passive audience reaches the gated watch page and views the relayed stream, without joining the Zoom meeting. Depends on IVS playback authorization and the CloudFront-fronted static page. [Derived from CON-3685's architecture description. How a viewer is admitted is asserted as existing architecture and never specified as a requirement.]

Primary actor: Viewer

**Primary scenario: Watch a session as a viewer**


### Deliver the session recording

Turn the IVS auto-recording in S3 into a single downloadable MP4 and hand it to the operator. Triggered when the room is ended; a MediaConvert passthrough remux runs and the operator downloads the result from the admin console. CON-3686 identifies a job claim and reconciliation path, implying flows beyond the happy one.

Primary actor: Operator

**Primary scenario: Deliver the session recording**


### Sign in to the admin console

Authenticate an operator to the admin console through the Microsoft Entra app registration described in ENTRA_SETUP.md. CON-3685 requires that tenant membership alone must not grant access, without stating who should be admitted.

Primary actor: Operator

**Primary scenario: Sign in to the admin console**


### Archive a room

Tear down the AWS resources for a finished room: the IVS channel and the stream key are both deleted. Distinct from ending a room. CON-3686 notes that ivs:DeleteChannel requires permission on both the channel and the stream key resource types, and that mis-scoped IAM surfaces only here, not at deploy time.

Primary actor: Operator

**Primary scenario: Archive a room**


### Respond to a CloudWatch alarm

A CloudWatch alarm fires to a Conduit-owned destination and an engineer carries out the documented first response for that alarm. [Implied entirely by CON-3685 acceptance criterion 5; no flow is described anywhere and no first response is documented yet.]

Primary actor: Conduit Engineer

**Primary scenario: Respond to a CloudWatch alarm**


### Recover a failing stream mid-session

Restore the relay when ingest fails during a live session, without any viewer action. A ladder of three actions in strict order, each more disruptive than the one above it, plus a terminal case when both channels are unusable. [Roundtable Production Guide v1.1 section 8. Entirely absent from CON-3685 and CON-3686, despite the force-stop control appearing in CON-3686's manual pass with no flow behind it.]

Primary actor: Operator

**Primary scenario: Recover a failing stream mid-session**

1. Issue a new takeover key
2. Promote the standby channel
3. Force-stop the active stream
4. Escalate when both channels are unusable

## Glossary

- **Room** — The central domain object: one relayed session with its watch page, recordings, event log and TWO independent IVS channels (primary and standby). Has four states — draft, test, live, ended — where STATE is what viewers are allowed to see. Ended is terminal and irreversible. Corrected against the Roundtable Production Guide v1.1; the earlier definition was inferred from CON-3685/3686 and was wrong on both the channel count (the tickets say one channel) and the state names.
- **End a room** — The lifecycle transition that stops the relay and triggers the MediaConvert passthrough remux of the IVS recording into a single downloadable MP4. CONFLICT: CON-3685 describes ending as if it were the terminal state, while CON-3686 treats Archive as a separate later action that deletes the IVS channel and stream key. End and Archive are distinct and neither is defined in source.
- **Archive a room** — A Lifecycle action taken after a session has ended: destroys both IVS channels and KEEPS everything worth keeping — the room record, per-session stats, recordings and MP4s, which stay downloadable. Archived rooms are hidden from the list unless "Show archived" is ticked. Distinct from End a room (a state transition) and from Purge (deletes the room and its session history, and deliberately does not delete the video from storage). Corrected against the Roundtable Production Guide v1.1.
- **Attendee** — A Zoom webinar role, per the Zoom permissions matrix referenced by CON-3685. NOT the same population as a roundtable Viewer, who never joins the Zoom meeting at all. The adjacent naming is a live source of confusion and the two vocabularies are nowhere reconciled.
- **Passthrough remux** — The AWS Elemental MediaConvert job that rewraps the IVS S3 auto-recording into a single MP4 without re-encoding, triggered when a room is ended. Roughly $0.55 per 90-minute session, plus IVS channel hours.
- **Dry-run event** — A non-production session used to exercise the tool end to end without a real audience. Canonical term for a concept the source material names three different ways — CON-3685 says "dry-run event", CON-3686 says "test room" and "throwaway room". CON-3686 specifically directs that the manual verification pass be done against one of these and not against a room booked for a real event.
- **Test room** — Synonym for Dry-run event, as used in CON-3686. (see **Dry-run event**)
- **Throwaway room** — Synonym for Dry-run event, as used in CON-3686. (see **Dry-run event**)
- **New takeover key** — The first and usual recovery action: issues a new key for the SAME channel carrying a higher takeover priority, so IVS drops a stuck session the moment the new one connects rather than waiting out its thirty-second timeout. Works because the stream key carries a ?priority=1 suffix — a partial paste that loses it costs you the main recovery tool. NOT THE SAME AS "Rotate stream key", which destroys the channel's key, instantly invalidates the copy sitting in Zoom, and cuts off anyone mid-stream; rotation exists for a leaked key and is explicitly not a recovery action. CON-3686's manual verification pass says "rotate the stream key" — worth confirming with the author whether it means rotation or takeover, because they are different controls with different blast radius.
- **Ingest state** — Whether a stream is arriving right now: offline, connected, live, starved, or failed. A SECOND state machine, entirely independent of the room's STATE (draft/test/live/ended). The Production Guide calls confusing the two "the most common misread on this screen". STATE controls what viewers are allowed to see; INGEST reports what is actually arriving. Absent from CON-3685 and CON-3686.
- **Webinar** — The web in your swim trunks
- **Livestream** — When you pee on an electrical wire

## Open Issues

- **MEDIUM** The subject of the step text "Manual verification" does not match a known actor. — on Scenario "Manual verification pass on a test room"
- **MEDIUM** PROXY_FOR_OUTCOME + PERSON_NAMED_NOT_ROLE + CONJUNCTIVE_GOAL. Three problems in one sentence. (1) "Conduit is the named owner" is satisfied by editing CODEOWNERS and can be fully true while the stated intent — see the story "Bus factor of one is a risk to every booked event" — still fails. (2) It names an individual where the Operator role is meant; the same human is also a stakeholder. (3) Ownership and operator-independence are independently completable and are welded into one criterion, so sign-off covers both and progress cannot be tracked. — on Goal "Conduit is the named owner"
- **MEDIUM** CROSS-TICKET CONFLICT. The five checks listed here are exactly `npm run verify`. CON-3686 lists `npm run test:ui` as a separate criterion and warns that Playwright is not a committed dependency and that "the suite skips cleanly rather than failing when it is absent, so an unnoticed skip reads as a pass". The pipeline as specified therefore never runs the UI tests, and the exact failure mode CON-3686 flags is the one CI is guaranteed to have. Neither requirement is wrong in isolation — this finding belongs to the pair, not to either one. — on Goal "CI blocks merges that fail the gate"
- **MEDIUM** SOLUTION_NOT_OUTCOME + NEGATIVE_ONLY_SPECIFICATION. "Gated on a Microsoft Entra app role" names a mechanism, not an outcome — replace the identity provider and the goal becomes unachievable while the intent is untouched. And the clause says who must be excluded (tenant members without the role) and never who must be admitted, so the denial is testable and the grant is not: the authorized set is nowhere defined. — on Goal "Admin console gated on an Entra app role"
- **MEDIUM** CONJUNCTIVE_GOAL + CONSTRAINT_NOT_GOAL. Routing and runbooks are independently completable by different people but share one criterion. And "all alarms... each with a documented first response" has an open temporal quantifier: an alarm added next quarter silently regresses it. That makes it a standing rule with no success state rather than a goal — the kind of thing that belongs as a policy applied to every new alarm. — on Goal "Alarms route to Conduit with documented responses"
- **MEDIUM** CROSS-TICKET VOCABULARY MISMATCH. This requires every finding "fixed or explicitly closed with a written reason". CON-3686 accounts for the same 32 findings as "24 fixed and 8 deliberately deferred". Deferred is not closed — those 8 are still open — so this criterion is unsatisfiable by CON-3686's own framing, and CON-3686 produces no artifact that would satisfy it. (The arithmetic itself is consistent: 0+5+15+12 = 32 = 24+8. That part is correct and needs no change.) — on Goal "Every round-1 finding is dispositioned"
- **MEDIUM** FACTUALLY WRONG IN BOTH SOURCE TICKETS. round-1-review.md states plainly: "The ticket text is wrong about the split. CON-3685 and CON-3686 say 24 fixed and 8 deferred. Checking all 32 findings against the current tree gives 26 with the fix present and 6 without. The 24/8 figure was a recollection, never a count." Further, "deferred" is the wrong word for the 6: they are NOT FIXED, and the review tracks them individually (H4 and M12 to CON-3691, L1 to CON-3687, and M10, L7, L8 untracked and needing tickets). Supersedes the earlier vocabulary-mismatch finding on this goal — that was correct that something was off, but understated it. The arithmetic I verified as consistent (32 = 24+8) was consistent and wrong, which is a useful lesson: internal consistency is not correctness. — on Goal "Every round-1 finding is dispositioned"
- **MEDIUM** THIS GOAL MAY BE UNMET IN PRODUCTION RIGHT NOW. round-1-review.md finding L1: the committed cdk.json still carries "roundtable:entraAdminRole": "" and lambda/admin.ts gates the app-role check behind if (ADMIN_ROLE). An empty value skips the check entirely, so if the deployed stack was synthesised from HEAD, any authenticated member of the Medlive tenant can reach /admin and end a live room, rotate keys, or download a recording. The review is emphatic that this is not deferred but live, and says to confirm the deployed value before anything else. This is the exact condition the goal forbids, and it is tracked as CON-3687 — a ticket neither CON-3685 nor CON-3686 references. — on Goal "Admin console gated on an Entra app role"
- **MEDIUM** VACUOUS_QUANTIFIER — a new finding type. At the time of the round-1 review there were ZERO CloudWatch alarms (finding M6: "the stack creates zero alarms or metric filters", despite log markers written specifically for them). A goal of the form "all X have property P" is trivially and uselessly satisfied when the set of X is empty, so this acceptance criterion could have been signed off as met while the system had no alarming at all. The fix has since added an SNS topic, 7 alarms and 3 log metric filters — but note that alarm SUBSCRIPTION ownership, which is the actual substance of "route to a Conduit-owned destination", is tracked separately as CON-3690, a ticket this epic never references. A goal quantified over a set the project also controls needs the set's existence stated as a separate condition. — on Goal "Alarms route to Conduit with documented responses"
- **MEDIUM** The subject of the step text "Viewers" does not match a known actor. — on Step "Viewers watch the gated page"
- **MEDIUM** The subject of the step text "both channels" does not match a known actor. — on Step "Escalate when both channels are unusable"

## Traceability

Requel project "PlatformQ Roundtable": 11 goals, 5 actors, 7 use cases, 8 scenarios, 15 steps, 4 stories, 12 glossary terms, 11 open issues (0 stale), 100 advisory issues.
