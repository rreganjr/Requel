# 255 — document tag slug normalization (plan)

Issue: https://github.com/rreganjr/Requel/issues/255

## What is actually happening

`TagNormalizer.slug` is the single normalizer, and it does more than lower-case: trims,
lower-cases with `Locale.ROOT`, collapses every run of non-alphanumerics into one hyphen, strips
leading and trailing hyphens, and returns `null` when nothing survives. `"Source"` → `"source"`,
`"CON-3685"` → `"con-3685"`, `"v2.0"` → `"v2-0"`.

Six call sites: `EditTagCategoryCommandImpl` (`:104` name, `:150` each value),
`EditTagCommandImpl` (`:86` category, `:87` value), and `TagImportHandlerImpl` (`:62`, `:63`).

## Decision: the normalization stays

Requel's tags follow Conduit's, where slugging is what stops a project accumulating `Source`,
`source` and `Source ` as three vocabularies. The stored slug is also the uniqueness key —
`tag_category` is unique on `(project_id, name)`, `tag` carries a denormalized `category` beside
`value`. Preserving display case with case-insensitive uniqueness would keep the key controlled
while letting the displayed vocabulary drift, which is most of the near-duplicate problem back
again.

The issue's original framing — tags as a provenance carrier that must round-trip an external
identifier — is retired. `doc/entity-provenance-notes.md` lists this normalization as one of four
measured failures and concludes tags are "right for the experiment, wrong as the destination".
Provenance is #272.

So: no schema change, no display-case column. Document the behaviour and pin it with a test.

## Scope: which commands

Only two gateway DTOs carry caller-supplied strings that get normalized:

- `EditTagCategoryInput` — `name`, and each entry in `values`
- `EditTagInput` — `category`, `value`

`AssignTagInput`, `UnassignTagInput`, `DeleteTagInput` and `DeleteTagCategoryInput` are id-only;
`AssignTagCommandImpl` reads `tag.getCategory()` off the stored tag, so no raw caller string is
normalized behind its back. `TagImportHandlerImpl` normalizes on the XML import path, which has no
gateway DTO — it gets a javadoc note only.

## Decision: descriptions come from an annotation on the input DTO

`GatewayCommandCatalogImpl` builds every `CommandDescriptor` with `description` hardcoded `null`;
the only per-command text is `humanize(commandType)` as the title. `McpWriteService.description()`
already falls back to title plus field names when description is null or blank, so commands
without a description keep working unchanged.

Javadoc does not reach a caller — it is not retained at runtime — so documenting the DTO alone
does not satisfy the issue. A runtime-retained annotation on the input record does.

New `@CommandDescription` with `@Retention(RUNTIME)`, `@Target(TYPE)` and one `String value()`.
`GatewayCommandCatalogImpl` reads it off `inputType` and passes the text through instead of
`null`; an absent annotation stays `null`.

It lives in `service-api` (`com.rreganjr.requel.service.api`), **not** `gateway-api` beside
`CommandDescriptor` as first planned: `gateway-api` depends on `service-api`, so annotating a DTO
in `service-api` with a type from `gateway-api` would be a dependency cycle. `service-api` is where
every gateway input DTO and `CommandRegistry` already live, and `service-impl` depends on both, so
the catalog can still read it.

## Decision: the UI hint is wired by hand, not via `app-field`

`app-field` (#158) takes a `helper` input and wires label, helper and error into
`aria-describedby` structurally. It is the house pattern — but it renders a label-column form row,
and the tag-categories add form is a compact horizontal strip of small inputs with `aria-label`s
and no visible labels. Converting one input looks wrong beside the others; converting the form is
a visual redesign of the admin screen and out of scope here.

So the hint is added inline, and `tag-categories.ts`'s existing hand-wired binding

    [attr.aria-describedby]="nameErr.message() ? 'tag-category-name-error' : null"

is widened to always carry the hint id and append the error id when present. If the admin form
should become `app-field` rows, that is a separate UI ticket — file it, do not smuggle it in here.

## Changes

**`modules/tagging-domain/.../TagNormalizer.java`** — expand the class javadoc to state the full
contract, including the `null` return, and that the slug is the uniqueness key rather than a
display transform.

**`modules/service-api/.../service/api/CommandDescription.java`** (new) — the annotation, with
javadoc saying it is what reaches an MCP client and a CLI user, so it is written for a caller.

**`modules/service-impl/.../gateway/GatewayCommandCatalogImpl.java`** — read the annotation off
`inputType`; keep `null` when absent.

**`modules/service-api/.../dto/EditTagCategoryInput.java`** — `@CommandDescription` naming the
normalization for both `name` and `values`, with a worked example. Correct the existing javadoc,
which mentions the name but not `values`.

**`modules/service-api/.../dto/EditTagInput.java`** — the same for `category` and `value`.

**`modules/tagging-jpa/.../TagImportHandlerImpl.java`** — javadoc note that import normalizes on
the same terms.

**`requel-angular/src/app/features/admin/tag-categories.ts`** — hint element with an id, widened
`aria-describedby` binding.

## Tests

- `TagNormalizerTest` (new, `tagging-domain`) — case, punctuation collapse, trim, leading/trailing
  hyphen strip, `null` input, blank input, all-punctuation input. There is no test for this class
  anywhere in the tree today.
- `GatewayCommandCatalogImplTest` — an annotated input yields the description; an unannotated one
  still yields `null`.
- `tag-categories.a11y.spec.ts` — the hint renders with its worked example, both slugged inputs
  (`name` and `values`) point at it, and with the required message shown the name input carries
  both ids rather than dropping the hint.
- `tag-categories.spec.ts` — its existing `aria-describedby` assertion pinned the error id alone
  and has to be updated to expect the hint beside it. That test is the reason the change is safe:
  it caught the widened binding immediately.

## Gate

`mvn clean verify` for the Java side, and `npm test` in `requel-angular` for the two frontend
specs — CI runs those in a separate job, so the Maven gate alone does not cover this ticket.

## Out of scope

- Any schema change, display-case column, or change to `slug()` behaviour.
- Converting the tag-categories add form to `app-field`.
- Descriptions for commands outside the tagging surface. The mechanism lands here; populating the
  rest of the catalog is follow-up work, and worth its own ticket.
- Provenance (#272).
