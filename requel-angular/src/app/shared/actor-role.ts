/*
 * This file is part of Requel - the Collaborative Requirements
 * Elicitation System.
 *
 * Copyright 2026 Ron Regan Jr. All Rights Reserved.
 *
 * Requel is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Requel is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Requel. If not, see <http://www.gnu.org/licenses/>.
 *
 */
/**
 * #468: a step's (or a scenario's) actor as a role. With a role the step's name is only the action
 * ("ends the room"); who does it is filled in where the step is shown: the primary actor of the use
 * case, the system, or one named actor. With no role the name says who does it, as before.
 */
export type ActorRole = 'PRIMARY_ACTOR' | 'SYSTEM' | 'NAMED';

/** The fields a DTO carries for its role. */
export interface ActorRoleFields {
  actorRole?: ActorRole | null;
  actorName?: string | null;
}

/** What fills the actor's place for each role (StepNames on the server). */
export const PRIMARY_ACTOR_LABEL = 'Primary actor';
export const SYSTEM_LABEL = 'The system';
export const UNNAMED_ACTOR_LABEL = 'An actor';

/**
 * One choice in a "Who does it" select. The value packs the role and, for a named actor, the
 * actor: '' (the wording says), 'PRIMARY_ACTOR', 'SYSTEM' or 'NAMED:<actor name>'.
 */
export interface ActorRoleChoice {
  label: string;
  value: string;
}

const NAMED_PREFIX = 'NAMED:';

/**
 * The choices for a "Who does it" select. `primaryActorName`, when the step's use cases all have
 * the same one, is shown beside "Primary actor".
 */
export function actorRoleChoices(actorNames: readonly string[],
                                 primaryActorName?: string | null): ActorRoleChoice[] {
  return [
    { label: 'In the wording', value: '' },
    { label: primaryActorName ? `${PRIMARY_ACTOR_LABEL} (${primaryActorName})` : PRIMARY_ACTOR_LABEL,
      value: 'PRIMARY_ACTOR' },
    { label: SYSTEM_LABEL, value: 'SYSTEM' },
    ...[...actorNames]
      .sort((a, b) => a.localeCompare(b, undefined, { sensitivity: 'base' }))
      .map(name => ({ label: name, value: NAMED_PREFIX + name })),
  ];
}

/** The select value for a role. */
export function toActorRoleChoice(fields: ActorRoleFields | null | undefined): string {
  if (!fields?.actorRole) return '';
  if (fields.actorRole === 'NAMED') return NAMED_PREFIX + (fields.actorName ?? '');
  return fields.actorRole;
}

/**
 * The command fields for a select value: `actorRole` NONE clears the role (the wording names the
 * actor); NAMED carries the actor's name.
 */
export function fromActorRoleChoice(choice: string | null | undefined):
    { actorRole: 'NONE' | ActorRole; actorName?: string } {
  if (!choice) return { actorRole: 'NONE' };
  if (choice.startsWith(NAMED_PREFIX)) {
    return { actorRole: 'NAMED', actorName: choice.substring(NAMED_PREFIX.length) };
  }
  return { actorRole: choice as ActorRole };
}

/** The select's role, as the fields a DTO carries. */
export function actorRoleFieldsOf(choice: string | null | undefined): ActorRoleFields {
  const { actorRole, actorName } = fromActorRoleChoice(choice);
  return actorRole === 'NONE' ? { actorRole: null, actorName: null }
    : { actorRole, actorName: actorName ?? null };
}

/**
 * How a name reads with its role filled in, in a use case whose primary actor is
 * `primaryActorName` (null: the role itself, "Primary actor ..."). No role: the name as written.
 */
export function readsAs(name: string, fields: ActorRoleFields | null | undefined,
                        primaryActorName?: string | null): string {
  const role = fields?.actorRole;
  if (!role) return name;
  const actor = role === 'PRIMARY_ACTOR' ? (primaryActorName || PRIMARY_ACTOR_LABEL)
    : role === 'SYSTEM' ? SYSTEM_LABEL
    : (fields?.actorName || UNNAMED_ACTOR_LABEL);
  const action = (name ?? '').trim();
  return action ? `${actor} ${action}` : actor;
}
