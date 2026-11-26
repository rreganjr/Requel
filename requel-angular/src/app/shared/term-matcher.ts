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
import { GlossaryTermDto } from '../models/term';

/** One glossary term an entity's name or text uses (#506). */
export interface TermUsed {
  /** The term found in the text. */
  id: number;
  name: string;
  /** The term to open and to define it by: the canonical for an alternate, else the term. */
  targetId: number;
  /** Set when the found term is an alternate: the preferred term's name. */
  canonicalName: string | null;
  definition: string;
}

/** Letters and digits; anything else is a word boundary, as in the server's TermTextMatcher. */
const WORD_CHAR = '[\\p{L}\\p{N}]';

function phrasePattern(name: string): RegExp {
  const escaped = name.trim().replace(/[.*+?^${}()|[\]\\]/g, '\\$&');
  return new RegExp(`(?<!${WORD_CHAR})${escaped}(?!${WORD_CHAR})`, 'giu');
}

/**
 * The glossary terms the name and text use (#506), each once, in order of first appearance
 * (the name before the text). A term matches as a whole phrase in any case. Longer terms are
 * matched first and a match inside one already taken doesn't count, so "video room" hides the
 * "room" within it. An alternate term reports its canonical's name and definition.
 */
export function findTermsUsed(terms: readonly GlossaryTermDto[], name: string | null | undefined,
    text: string | null | undefined, excludeTermId?: number | null): TermUsed[] {
  const byId = new Map(terms.map(t => [t.id, t]));
  const candidates = terms
    .filter(t => t.id !== excludeTermId && t.name?.trim())
    .sort((a, b) => b.name.trim().length - a.name.trim().length);
  // The name and text joined by a character no term can span, so a match never crosses them.
  const haystack = `${name ?? ''}\n\n${text ?? ''}`;
  const taken: { start: number; end: number }[] = [];
  const firstAt = new Map<number, number>();
  for (const term of candidates) {
    for (const match of haystack.matchAll(phrasePattern(term.name))) {
      const start = match.index ?? 0;
      const end = start + match[0].length;
      if (taken.some(t => start < t.end && end > t.start)) {
        continue;
      }
      taken.push({ start, end });
      if (!firstAt.has(term.id)) {
        firstAt.set(term.id, start);
      }
    }
  }
  return [...firstAt.entries()]
    .sort((a, b) => a[1] - b[1])
    .map(([id]) => {
      const term = byId.get(id)!;
      const canonical = term.canonicalTermId != null ? byId.get(term.canonicalTermId) : undefined;
      return {
        id: term.id,
        name: term.name,
        targetId: canonical?.id ?? term.id,
        canonicalName: canonical ? canonical.name : (term.canonicalTermName ?? null),
        definition: (canonical ?? term).text?.trim() || 'No definition yet.',
      };
    });
}
