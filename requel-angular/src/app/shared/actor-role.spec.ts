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
import { describe, expect, it } from 'vitest';
import {
  actorRoleChoices, actorRoleFieldsOf, fromActorRoleChoice, readsAs, toActorRoleChoice,
} from './actor-role';

describe('actor-role (#468)', () => {
  it('reads a name with its role filled in', () => {
    expect(readsAs('Operator ends the room', null, 'Engineer')).toBe('Operator ends the room');
    expect(readsAs('ends the room', { actorRole: 'PRIMARY_ACTOR' }, 'Operator')).toBe('Operator ends the room');
    expect(readsAs('ends the room', { actorRole: 'PRIMARY_ACTOR' }, 'Conduit Engineer'))
      .toBe('Conduit Engineer ends the room');
    expect(readsAs('ends the room', { actorRole: 'PRIMARY_ACTOR' }, null)).toBe('Primary actor ends the room');
    expect(readsAs('logs it', { actorRole: 'SYSTEM' })).toBe('The system logs it');
    expect(readsAs('approves it', { actorRole: 'NAMED', actorName: 'Clerk' })).toBe('Clerk approves it');
  });

  it('lists the choices with the primary actor beside the role and the actors sorted', () => {
    expect(actorRoleChoices(['Zoom', 'clerk'], 'Operator')).toEqual([
      { label: 'In the wording', value: '' },
      { label: 'Primary actor (Operator)', value: 'PRIMARY_ACTOR' },
      { label: 'The system', value: 'SYSTEM' },
      { label: 'clerk', value: 'NAMED:clerk' },
      { label: 'Zoom', value: 'NAMED:Zoom' },
    ]);
    expect(actorRoleChoices([], null)[1].label).toBe('Primary actor');
  });

  it('round-trips a role through the select value and the command fields', () => {
    expect(toActorRoleChoice({ actorRole: 'NAMED', actorName: 'Clerk' })).toBe('NAMED:Clerk');
    expect(toActorRoleChoice({ actorRole: 'SYSTEM' })).toBe('SYSTEM');
    expect(toActorRoleChoice(null)).toBe('');
    expect(fromActorRoleChoice('NAMED:Clerk')).toEqual({ actorRole: 'NAMED', actorName: 'Clerk' });
    expect(fromActorRoleChoice('PRIMARY_ACTOR')).toEqual({ actorRole: 'PRIMARY_ACTOR' });
    expect(fromActorRoleChoice('')).toEqual({ actorRole: 'NONE' });
    expect(actorRoleFieldsOf('')).toEqual({ actorRole: null, actorName: null });
    expect(actorRoleFieldsOf('NAMED:Clerk')).toEqual({ actorRole: 'NAMED', actorName: 'Clerk' });
  });
});
