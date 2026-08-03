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
 * Shared severity-tint vocabulary for the `app-tag` / `app-chip` primitives
 * (issue #155, N2). One source of truth for the tone set, the tag variants, and
 * the generic tone -> icon defaults so every consumer maps the same way. Tint /
 * text colours are not here — they live as `--rq-tag-*` / `--rq-chip-*` CSS
 * tokens in `styles.scss`; this module is the TypeScript-side contract only.
 */

/**
 * The six severity tones. `neutral` is beyond the classic five because genuinely
 * non-severity labels (Position badges, Neutral-support arguments) are grey today
 * and must stay visually distinct from the blue `info` tone.
 */
export type RqTone = 'primary' | 'success' | 'info' | 'warning' | 'danger' | 'neutral';

/** Tag shape variants: default (rounded rect), pill (fully rounded), icon (leading icon). */
export type RqTagVariant = 'default' | 'pill' | 'icon';

/**
 * Generic default PrimeIcon per tone, used by the `icon` tag variant when the
 * caller does not supply a domain-specific icon. Callers with a domain meaning
 * (Note, Issue, ...) pass their own icon instead (see the N2 mapping table in
 * `doc/124-lookandfeel-plan.md`).
 */
export const RQ_TONE_ICON: Record<RqTone, string> = {
  primary: 'pi pi-tag',
  success: 'pi pi-check-circle',
  info: 'pi pi-info-circle',
  warning: 'pi pi-exclamation-triangle',
  danger: 'pi pi-times-circle',
  neutral: 'pi pi-minus-circle',
};

/**
 * Annotation argument support level -> tone. For/StronglyFor read as success,
 * Against/StronglyAgainst as danger, everything else (Neutral) as neutral.
 */
export function supportLevelTone(supportLevel: string): RqTone {
  if (supportLevel === 'StronglyFor' || supportLevel === 'For') return 'success';
  if (supportLevel === 'StronglyAgainst' || supportLevel === 'Against') return 'danger';
  return 'neutral';
}

/** Domain icon for an argument support level, paired with {@link supportLevelTone}. */
export function supportLevelIcon(supportLevel: string): string {
  const tone = supportLevelTone(supportLevel);
  if (tone === 'success') return 'pi pi-thumbs-up';
  if (tone === 'danger') return 'pi pi-thumbs-down';
  return 'pi pi-minus-circle';
}
