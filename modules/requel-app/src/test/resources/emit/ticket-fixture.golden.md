# Fixture Room

Project version: abc123def456
Sources: Production Guide v1.1, CON-3686

## Goals

1. **Operators are replaceable** — More than one person can fill the Operator role.
2. **Viewers stay invisible** — Viewers are never shown to the panel.

## Actors

- **Operator** — Runs a live room.
- **Viewer** — Watches the relay.

## Use Cases

### End a room

An operator ends a room.

Primary actor: Operator
Actors: Viewer
Goals: Operators are replaceable, Viewers stay invisible

**Primary scenario: End a room**

1. Open the admin console
2. Press End

**Scenario: End from the room list**

1. Press End
2. Confirm the end
   1. Confirm in the dialog

## Other Scenarios

**Scenario: Rehearse in test**

1. Open the admin console

**Scenario: Archive a room**

_No steps recorded._

## Glossary

- **webinar** — A Zoom webinar.
- **livestream** — The viewer relay. (see **webinar**)

## Open Issues

- **HIGH** Who holds the second seat? — on Goal "Operators are replaceable"
- **HIGH** Shared high, stale only on the goal. — on Actor "Operator", Goal "Viewers stay invisible" (stale: the entity changed since this was raised)
- **HIGH** Stale high on the replaceable goal. — on Goal "Operators are replaceable" (stale: the entity changed since this was raised)
- **LOW** Low, fresh. — on Goal "Viewers stay invisible"

## Resources

- **Production Guide v1.1** (guide) — doc guide, docs/guide.pdf
  - Note: Authoritative for behaviour.
  - Cited by: End a room
- **CON-3686** (ticket) — jira CON-3686, https://example.com/browse/CON-3686
  - Defers to: Production Guide v1.1 (the guide wins on behaviour)
  - Derived: Operators are replaceable (AC1), Viewers stay invisible (AC2)

## Traceability

Requel project "Fixture Room": 2 goals, 2 actors, 1 use cases, 5 scenarios, 3 steps, 1 stories, 2 glossary terms, 4 open issues (2 stale), 1 advisory issues.
Sources: Production Guide v1.1, CON-3686.
