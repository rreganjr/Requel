-- #325: delete plain steps that no scenario uses any more.
--
-- Before #325, EditScenarioCommandImpl replaced a scenario's step list by clearing it and
-- re-adding the steps the caller sent, and never deleted the ones it dropped. Every use-case
-- save from the UI sent an empty list, so it dropped all of its primary scenario's steps. A
-- dropped plain step stayed in `scenarios`, attached to no scenario, still carrying its
-- project's projectordomain_id (which blocked DeleteProject on MySQL through
-- FKoplmulkyjqc4foqfjtcwjxm2x) and still holding its name in the shared step/scenario unique
-- key, so a new step or scenario with that name was refused.
--
-- The code now deletes a dropped step when no other scenario uses it, and DeleteProject finds
-- step rows by query instead of by walking scenarios. This migration removes the orphans the
-- old code already left behind. It is idempotent and a no-op on a clean database.
--
-- Only plain steps (the StepImpl discriminator). A scenario row with no scenario_steps row
-- pointing at it is a top-level scenario, not an orphan. The annotations that pointed at an
-- orphan step stay, as they do when DeleteScenarioStepCommand deletes a step; DeleteProject's
-- annotation sweep removes them with the project. Steps are not taggable, so tag_taggable has
-- no rows for them.
--
-- An ordinary table rather than a TEMPORARY one, for the reason V17 gives (MySQL cannot open a
-- temporary table twice in one statement). Dropped at the end.
DROP TABLE IF EXISTS v19_orphan_steps;

CREATE TABLE v19_orphan_steps (
    id BIGINT NOT NULL PRIMARY KEY
) ENGINE=InnoDB;

INSERT INTO v19_orphan_steps (id)
SELECT s.id
FROM scenarios s
WHERE s.type = 'com.rreganjr.requel.project.Step'
  AND NOT EXISTS (SELECT 1 FROM scenario_steps ss WHERE ss.step_id = s.id);

-- Soft references first (no FK, but they would dangle), then the FK'd join tables, then the
-- rows. V2__identity_cleanup.sql normalized annotatable_type to the short name; the FQCN is
-- matched too in case an older row escaped it.
DELETE FROM annotation_annotatable
WHERE annotatable_type IN ('Step', 'com.rreganjr.requel.project.Step')
  AND annotatable_id IN (SELECT id FROM v19_orphan_steps);

-- terms_referers is a @ManyToAny join (GlossaryTermImpl.referers): no FK, and a row naming a
-- deleted step makes loading the term's referers throw.
DELETE FROM terms_referers
WHERE referer_type = 'com.rreganjr.requel.project.Step'
  AND referer_id IN (SELECT id FROM v19_orphan_steps);

DELETE FROM scenarios_annotations WHERE step_impl_id IN (SELECT id FROM v19_orphan_steps);

DELETE FROM scenarios_glossary_terms WHERE step_impl_id IN (SELECT id FROM v19_orphan_steps);

DELETE FROM pods_scenarios WHERE scenarios_id IN (SELECT id FROM v19_orphan_steps);

DELETE FROM scenarios WHERE id IN (SELECT id FROM v19_orphan_steps);

DROP TABLE IF EXISTS v19_orphan_steps;
