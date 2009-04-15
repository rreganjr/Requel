select * from synset where synsetid in (
select sdw1.sense_id from synset_definition_word sdw1
left join synset sy1 on ( sdw1.synset_id = sy1.synsetid )
left join synset_definition_word sdw2 on (
	sdw2.synset_id = sy1.synsetid ) where sdw1.sense_id = 110741590
and sdw2.word_id = 77963 )

union

select * from synset where synsetid in (
select sdw2.sense_id from synset_definition_word sdw1
left join synset sy1 on ( sdw1.synset_id = sy1.synsetid )
left join synset_definition_word sdw2 on (
	sdw2.synset_id = sy1.synsetid ) where sdw1.sense_id = 110741590
and sdw2.word_id = 77963 )


select * from synset where synsetid in (
select sdw1.sense_id from synset_definition_word sdw1
left join synset sy1 on ( sdw1.synset_id = sy1.synsetid )
left join synset_definition_word sdw2 on (
	sdw2.synset_id = sy1.synsetid ) where sdw1.word_id = :word1
and sdw2.word_id = :word2

 union

select sdw2.sense_id from synset_definition_word sdw1
left join synset sy1 on ( sdw1.synset_id = sy1.synsetid )
left join synset_definition_word sdw2 on (
	sdw2.synset_id = sy1.synsetid ) where sdw1.word_id = :word1
and sdw2.word_id = :word2 )
