insert into word (wordid, lemma)
	select wordid, lemma from wordnet30.word order by lemma;

insert into categorydef (categoryid, name, pos)
	select categoryid, name, pos from wordnet30.categorydef order by categoryid;

insert into synset (synsetid, definition, pos, categoryid)
	select synsetid, definition, pos, categoryid from wordnet30.synset order by synsetid;

insert into sense (wordid, synsetid, rank)
	select wordid, synsetid, rank from wordnet30.sense order by wordid;
	
	
-- requel semcor data to wordnet30	
insert into semcor_sentence (id, file, section, snum)
	select id, file, section, snum from requel.semcor_sentence order by id;

insert into semcor_sentence_word (id, sentence_index, parse_tag, text, category_id, sense_id, word_id, sentence_id)
	select id, sentence_index, parse_tag, text, category_id, sense_id, word_id, sentence_id from requel.semcor_sentence_word order by id;

	