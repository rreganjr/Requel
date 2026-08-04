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
import axe from 'axe-core';
import { expect } from 'vitest';

/**
 * Returns the currently-rendered modal dialog from the document.
 *
 * All app dialogs render with `appendTo="body"` (the correct pattern — it keeps the overlay
 * clear of ancestor overflow/z-index traps), so the dialog lives on document.body, NOT inside
 * a component fixture's nativeElement. Always query the document for dialog assertions.
 */
export function getOpenDialog(): HTMLElement | null {
  return document.querySelector<HTMLElement>('[role="dialog"]');
}

/**
 * Runs axe-core against `root` (default: the whole document body) and fails the test if there
 * are any violations, with a readable summary.
 *
 * The `color-contrast` rule is disabled: jsdom has no layout engine / real getComputedStyle box
 * model, so contrast results are unreliable in unit tests. Color contrast is governed separately
 * (UI/UX review Finding 4.7), not here.
 *
 * `exclude` takes CSS selectors for subtrees to skip, so a whole-page assertion is still
 * possible when some third-party host element carries a defect of its own. The known case is
 * `p-confirmdialog`: PrimeNG puts `role="alertdialog"` on the component's host element even
 * when no confirmation is showing (its container is an empty comment), so axe reports an
 * unnamed dialog on every page that mounts one. That is a PrimeNG-level issue affecting all
 * such pages rather than anything the page under test controls — it belongs with the modal
 * a11y work (#139), not to whichever ticket happens to run axe over the page.
 */
export async function expectNoAxeViolations(
  root: Element = document.body,
  exclude: string[] = []
): Promise<void> {
  const context = exclude.length
    ? { include: [root], exclude: exclude.map(selector => [selector]) }
    : root;

  const results = await axe.run(context as Parameters<typeof axe.run>[0], {
    rules: { 'color-contrast': { enabled: false } },
  });

  const summary = results.violations
    .map(v => `  • ${v.id} [${v.impact}] ${v.help} (${v.nodes.length} node(s))`)
    .join('\n');

  expect(results.violations, `Accessibility violations found:\n${summary}`).toEqual([]);
}
