-- Fix ChangeSpellingPosition records where proposed_word was incorrectly set to
-- the full description text (e.g. 'Change the word "foo" to "bar".')
-- instead of just the replacement word (e.g. 'bar').
-- Extract the proposed word as the last quoted token: everything after ' to "' up to the next '"'.
UPDATE positions
SET proposed_word = SUBSTRING_INDEX(SUBSTRING_INDEX(text, ' to "', -1), '"', 1)
WHERE position_type = 'com.rreganjr.requel.annotation.impl.ChangeSpellingPosition'
  AND proposed_word = text;
