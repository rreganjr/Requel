# Flyway Schema Init Plan

## Goal
Have Flyway own schema creation and data bootstrapping so new environments don’t rely on Hibernate DDL or ad‑hoc initializers, eliminating migration ordering issues on fresh databases.

## Current state
- Hibernate auto-DDL (via `spring.jpa.hibernate.ddl-auto` / JPA annotations) creates tables on first run.
- Flyway runs before Hibernate, causing failures on empty DBs for upgrade-only migrations.
- Additional bootstrapping occurs via Java initializers (baseline users/roles/orgs) and some large `.sql.gz` files executed elsewhere.
- Migrations exist under `modules/requel-app/src/main/resources/db/migration`.
- Dictionary bootstrapping:
  - `DictionarySQLInitializer` (dictionary-jpa) loads a series of gzipped SQL files (`categorydef.sql.gz, word.sql.gz, morphdef.sql.gz, morphref.sql.gz, synset.sql.gz, sense.sql.gz, synset_definition_word.sql.gz, synset_subsumer_counts.sql.gz, linkdef.sql.gz, lexlinkref.sql.gz, semlinkref.sql.gz, vnclass.sql.gz, vnframedef.sql.gz, vnframeref.sql.gz, semcor_file.sql.gz, semcor_sentence.sql.gz, semcor_sentence_word.sql.gz`) when the dictionary is empty.
  - `DictionaryInitializer` can load `nlp/dictionary/dictionary.xml.gz`.
  - `WordNetDefinitionWordsInitializer` loads `adj.xml.gz, adv.xml.gz, noun.xml.gz, verb.xml.gz` (WordNet glosses).
  - `WordNetSenseKeyInitializer` also handles gz input.
  - Tests may load `nlp/dictionary/dictionary.xml.gz` (AbstractIntegrationTestCase).
  - These initializers read gz files and stream SQL/ XML into the DB outside Flyway.

## Target approach
- Disable Hibernate DDL in deployed environments (`ddl-auto=none`) and let Flyway create the schema.
- Provide an initial baseline migration (`V1__init.sql` and/or split by module) that creates all tables, indexes, constraints.
- Move Java/bootstrap data (admin/project users/roles/orgs) into repeatable or versioned Flyway scripts (`R__baseline_data.sql`).
- For large seed data `.sql.gz`, prefer using Flyway repeatable migrations with `UNDO` disabled, or keep them as external “seed” tasks invoked manually; Flyway can execute plain `.sql` (not compressed) out of the box.

## Key changes needed
1) **Hibernate DDL**  
   - Set `spring.jpa.hibernate.ddl-auto=none` (or remove) in prod/default profiles. Keep `create-drop` only for isolated tests if needed.
2) **Initial schema migration**  
   - Generate `V1__init.sql` containing all DDL (tables, PK/FK, indexes, sequences/auto-inc). Source from current database or JPA schema export.
   - Optionally modularize (`V1.1_project.sql`, `V1.2_annotation.sql`, etc.) but ensure a single baseline for Flyway.
3) **Baseline existing databases**  
   - Set `spring.flyway.baseline-on-migrate=true` for environments with existing schemas, or run `flyway baseline` once.
4) **Data bootstrap**  
   - Move admin/assistant/project users/roles/org initializers into a repeatable migration (e.g., `R__baseline_users_roles.sql`) with idempotent UPSERTs.
   - Move other Java initializers that insert reference data into SQL repeatable migrations where possible.
5) **Large seed files (.gz)**  
   - Flyway does not execute `.gz` directly. Options:
     - Unzip and include as `.sql` repeatable migration(s) if size is acceptable.
     - Keep gzipped files but load via a custom Flyway Java migration class that streams the gz; keep it idempotent.
     - Or treat them as out-of-band seed scripts run manually (document in DEPLOY).
6) **Clean up guard migrations**  
   - Once the baseline is in Flyway, upgrade-only migrations can assume tables exist; remove/relax the guards added earlier if desired.

## Open questions / decisions
- Should we modularize migrations per module or keep a single shared `db/migration`? (Simplest: keep current single location.)
- How large are the `.sql.gz` seed files, and can we tolerate uncompressed size in the repo/image?
- Do we need different baselines for H2 vs MySQL, or will a single MySQL-compatible DDL suffice?

## Implementation steps
1) Export current schema from a known-good DB to SQL (MySQL): `mysqldump --no-data --routines --triggers requel > V1__init.sql` (massage for portability/H2 if needed).
2) Place `V1__init.sql` in `modules/requel-app/src/main/resources/db/migration/`.
3) Add repeatable migration(s) for baseline users/roles/orgs: `R__baseline_identity.sql` with idempotent inserts.
4) Decide handling of large seed `.sql.gz`: either uncompress into `R__seed_data.sql` or implement a Flyway Java migration to stream the gz file.
5) Update `application.properties` / compose env:
   - `spring.flyway.baseline-on-migrate=true` (for existing DBs only; can be off for fresh)
   - `spring.jpa.hibernate.ddl-auto=none`
6) Remove or relax guard logic once baseline is solid (optional); keep guards for safety.
7) Test: fresh DB `docker compose up` should succeed; existing DB migrate with baseline should also succeed.
