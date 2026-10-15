-- Widen the IBIS discussion-layer free-text columns to LONGTEXT (issue #171).
-- Plan: doc/171-bean-validation-plan.md (§4).
--
-- `arguments.text` and `positions.text` were VARCHAR(255) while `annotations.text` (notes and
-- issues, which share the annotations table) has always been LONGTEXT. The mismatch was not a
-- deliberate limit: ArgumentImpl.getText() and PositionImpl.getText() simply lacked the @Lob that
-- AbstractAnnotation.getText() carries, so Hibernate defaulted them to 255 when this schema was
-- first generated.
--
-- The user-visible effect was a defect rather than a constraint: a long argument or position body
-- failed at the driver and surfaced through CommandController's catch-all as a generic
-- INTERNAL_ERROR, with no indication of which field was at fault or why. These are free-text
-- discussion fields on the annotations surface, where 255 characters is not a defensible product
-- limit, so they are widened to match their sibling rather than being capped with a @Size.
--
-- Widening only. No existing value can fail to fit, so this is safe to run against populated
-- databases and needs no data migration. Tests build their schema from the entities
-- (ddl-auto=create-drop, Flyway disabled), so the @Lob annotations cover them without this file.

ALTER TABLE `arguments` MODIFY `text` LONGTEXT NULL;

ALTER TABLE `positions` MODIFY `text` LONGTEXT NULL;
