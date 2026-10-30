# Old-release database fixtures (#380)

`requel-<version>.sql` is a Requel 1.0.1, 1.0.2 or 1.1.0 database as that release left it:
its schema, created by the release itself (Hibernate `ddl-auto`), plus the sample project
`doc/samples/Requel.xml` (without its glossary, which none of these releases can import)
imported through the release's own UI with analysis off. The dictionary tables are schema only,
except `categorydef`, whose id-0 row is what adding `AUTO_INCREMENT` trips over.

A 1.0.x database was made on MySQL 5.7 (1.0.x can't use 8.x) and moved to MySQL 8.4 with
mysqldump, as a 1.0.x user does before upgrading; 1.1.0's was made on MySQL 8.4.

They come from the published images, not from anyone's data, and are made by
`scripts/legacy-fixtures.sh <version>` (needs Docker and Playwright). Regenerate rather than
edit them. If a fixture changes, record its shape again for `db/legacy/requel-<version>.shape`
(`LegacySchemaShape.main`; see that class).

Used by `LegacySchemaUpgradeMySqlIT` and the `LegacyUpgradeFrom*MySqlIT` classes.
