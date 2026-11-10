-- #480: renaming a glossary term (#348) could delete another term's row in pods_glossary_terms,
-- the join table behind a project's glossary. The term stayed in the project
-- (terms.projectordomain_id), but every lookup through the glossary failed: editing a synonym of
-- it, for one, was refused with "GlossaryTerm not found". This puts back the row of every term
-- that has none. A term has exactly one row (glossary_terms_id is unique), so nothing is
-- duplicated.
INSERT INTO pods_glossary_terms (abstract_project_or_domain_id, glossary_terms_id)
SELECT t.projectordomain_id, t.id
  FROM terms t
  LEFT JOIN pods_glossary_terms j ON j.glossary_terms_id = t.id
 WHERE j.glossary_terms_id IS NULL;
