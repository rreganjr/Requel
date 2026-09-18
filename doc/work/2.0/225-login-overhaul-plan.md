# #225 — Login screen overhaul — plan

Ticket: [#225](https://github.com/rreganjr/Requel/issues/225) (I1, Phase 4 of the
Post-#124 UI polish epic #219). Punch-list items L1, L2, L3, L5, L6, L7, L8. Branch:
`225-login-overhaul` off `release/2.0`. Frontend-only.

## Scope / locked decisions

Everything visual/structural on the login page, in `login.ts`, plus one shared global rule:

- **L8 — muted canvas + raised card.** `.login-container` background `--p-surface-ground`
  → `--rq-canvas-bg` (the same light blue-gray the editors sit on), so the white `app-card`
  reads as raised on a distinct canvas.
- **L1 — true vertical centering + mobile.** The flex centering is already there; the "low"
  look is body margin / card top content. Give the container real centering with breathing
  room and confirm it holds on a narrow (mobile) viewport.
- **L2 — no stray scrollbar.** Switch `min-height:100vh` → `100dvh` and trim the container so
  no vertical scrollbar appears unless the window is genuinely shorter than the card.
- **L3 — min-width floor.** `.login-card` gets a `min-width` so it stops collapsing; the
  container shows scrollbars past that floor instead of distorting the form.
- **L5 + L7 — logo + wordmark lockup.** Replace the plain `<h1>Requel</h1>` + subtitle with a
  brand lockup: the robot logo (`images/logo_robot.png`, the header asset) and a "Requel"
  wordmark on one line (wordmark scaled to ~ the logo height), with the
  "Requirements Elicitation System" tagline centered below.
- **L6 — equal-length fields + password toggle.** `p-password` renders its own inner
  `<input>`; `width:100%` on the host doesn't reach it, so the password field is visibly
  shorter than the username field. Fix as a **global** rule (benefits `edit-account.ts` and
  `user-editor.ts`, which have the same short-field bug) so the inner input fills the host;
  confirm the show/hide toggle sits at the right edge inside the field.

## Changes

### `login.ts` (template + component styles)

1. **Brand lockup (L5, L7):** replace `<h1>` + `.subtitle` with
   `<div class="login-brand"><img class="login-logo" src="images/logo_robot.png" alt="" />
   <span class="login-wordmark">Requel</span></div>` then `<p class="login-tagline">…</p>`.
   The image is decorative (the wordmark carries the name), so `alt=""`. Styles: `.login-brand`
   a centered flex row with a gap; `.login-logo { height: ~2.5rem }`; `.login-wordmark`
   sized to about the logo height (bold, brand letter-spacing to match the header wordmark);
   `.login-tagline` centered, muted, small top margin.
2. **Canvas (L8):** `.login-container` background → `var(--rq-canvas-bg)`.
3. **Centering + viewport (L1, L2):** `.login-container` → `min-height: 100dvh` (keep the flex
   center), add symmetric `padding` so the card never touches the edges and, on a short
   window, the container scrolls with the card fully reachable. Remove any reliance on default
   body margin.
4. **Min-width floor (L3):** `.login-card` add `min-width` (≈ 20rem) alongside the existing
   `max-width: 400px`; `.login-container { overflow: auto }` so past the floor the viewport
   scrolls rather than the card distorting.
5. Drop the now-unused `input, p-password { width: 100% }` local rule in favor of the shared
   global rule below (keep whatever login-local width is still needed for the native input).

### `styles.scss` (shared global rule — L6)

Add a global rule (near the other PrimeNG customizations, #126 pattern) so a full-width
`p-password` fills its host down to the inner input:

```
p-password { display: block; }
p-password .p-password { width: 100%; }
p-password .p-password input { width: 100%; }
```

(Exact inner selector confirmed against the live PrimeNG 21 DOM during implementation.) This
makes the password field match the username field on login and fixes the same short-field look
in `edit-account.ts` and `user-editor.ts`, which already set `p-password { width:100% }`
locally to no effect on the inner input.

### `login.spec.ts`

Update assertions that reference the old `<h1>`/subtitle markup to the new brand lockup
(logo present, wordmark text "Requel", tagline text). Keep the form/validation/submit tests
unchanged.

## Test plan (the verify gate)

- **Unit:** `login.spec` green after the markup update; a cheap assertion that the logo `<img>`
  and wordmark render. `edit-account.spec` / `user-editor.spec` stay green (global rule is
  additive).
- **Typecheck:** `tsc` app + spec.
- **Dev build (AOT):** compiles.
- **Manual/visual (the real proof — needs a logged-out session):** the login page centers with
  no stray scrollbar at a normal viewport; the logo + "Requel" sit on one line with the tagline
  below on the muted canvas; username and password fields are the same length with the toggle
  at the right edge; shrinking the window past the floor shows scrollbars instead of distortion;
  a narrow (mobile) width looks right. Re-check `edit-account` and a user editor: password
  fields now match text fields.
- **e2e:** the login flow is the gateway for most specs — read the report; update login locators
  only if the brand-markup change legitimately moved them.

## Risks

- The `p-password` inner-input selector is PrimeNG-version-specific; confirmed live and commented.
- `100dvh` is well-supported in current browsers; `100vh` fallback behavior is acceptable.
- Brand lockup sizing (logo height vs wordmark) is subjective — first pass then iterate in
  browser with Ron before finalizing.
- Estimate: filed at 5 pts; mostly CSS + a small template change + one shared rule — should land
  around estimate, with the visual iteration being the variable.

## AC mapping

- Centered, scrollbar-free login that holds on mobile and when shrunk → L1/L2/L3 container work.
- Equal-length fields, toggle at the right edge → L6 global rule + verify.
- Logo + wordmark lockup, tagline below → L5/L7 brand markup.
- Editor-style muted canvas + raised card → L8 token swap.
