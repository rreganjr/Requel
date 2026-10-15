-- #247 DeleteProject cascade hardening: index annotation_annotatable by the annotated entity.
--
-- annotation_annotatable is the @ManyToAny join table from an annotation to the entities it
-- annotates. Its only index was the primary key (annotation_id, annotatable_type, annotatable_id),
-- so every "unlink this ENTITY from all its annotations" statement
-- (DELETE ... WHERE annotatable_id = ?) was a full table scan. Under InnoDB REPEATABLE READ a
-- scanning DELETE takes an exclusive next-key lock on every row it passes, i.e. on the whole
-- table: SHOW ENGINE INNODB STATUS during the e2e suite showed two such deletes (from two
-- concurrent Delete*Commands) each holding ~2,300 row locks and deadlocking each other, while
-- every assistant insert and every targeted RemoveAnnotationFromAnnotatable delete queued behind
-- them for 40+ seconds.
--
-- With (annotatable_id, annotatable_type) indexed, those deletes become a short index-range
-- scan that locks only the rows for that one entity. H2 test runs (Flyway disabled, JPA
-- create-drop) never exercised the MySQL locking, which is why mvn verify stayed green.
CREATE INDEX idx_annotation_annotatable_annotatable
    ON annotation_annotatable (annotatable_id, annotatable_type);
