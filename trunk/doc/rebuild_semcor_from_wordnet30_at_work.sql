insert into requel.semcor_file (file, section)
	select file, section from wordnet30.semcor_sentence where section = 'brown1' group by section, file;

insert into requel.semcor_sentence (snum, file_id)
	select s.snum, f.id from wordnet30.semcor_sentence s left join requel.semcor_file f on (s.file = f.file and s.section = f.section) where s.section = 'brown1';

insert into requel.semcor_sentence_word (word_index, parse_tag, text, category_id, sense_id, word_id, sentence_id)
	select w.sentence_index, w.parse_tag, w.text, w.category_id, w.sense_id, w.word_id, ss.id from wordnet30.semcor_sentence_word w 
	left join wordnet30.semcor_sentence s on (w.sentence_id = s.id) 
	left join requel.semcor_file f on (s.file = f.file and s.section = f.section)
	left join requel.semcor_sentence ss on (ss.file_id = f.id and s.snum = ss.snum)
	where s.section = 'brown1';

insert into requel.semcor_file (file, section)
	select file, section from wordnet30.semcor_sentence where section = 'brown2' group by section, file;

insert into requel.semcor_sentence (snum, file_id)
	select s.snum, f.id from wordnet30.semcor_sentence s left join requel.semcor_file f on (s.file = f.file and s.section = f.section) where s.section = 'brown2';

insert into requel.semcor_sentence_word (word_index, parse_tag, text, category_id, sense_id, word_id, sentence_id)
	select w.sentence_index, w.parse_tag, w.text, w.category_id, w.sense_id, w.word_id, ss.id from wordnet30.semcor_sentence_word w 
	left join wordnet30.semcor_sentence s on (w.sentence_id = s.id) 
	left join requel.semcor_file f on (s.file = f.file and s.section = f.section)
	left join requel.semcor_sentence ss on (ss.file_id = f.id and s.snum = ss.snum)
	where s.section = 'brown2';


select count(*) from wordnet30.semcor_sentence_word w left join wordnet30.semcor_sentence s on (w.sentence_id = s.id)