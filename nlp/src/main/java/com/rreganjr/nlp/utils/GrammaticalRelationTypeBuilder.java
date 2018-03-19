package com.rreganjr.nlp.utils;


import edu.stanford.nlp.trees.EnglishGrammaticalRelations;
import edu.stanford.nlp.trees.GrammaticalRelation;

import java.util.Arrays;
import java.util.List;

import static edu.stanford.nlp.trees.UniversalEnglishGrammaticalRelations.*;
import static edu.stanford.nlp.trees.GrammaticalRelation.DEPENDENT;
import static edu.stanford.nlp.trees.GrammaticalRelation.GOVERNOR;
import static edu.stanford.nlp.trees.GrammaticalRelation.ROOT;


/**
 * This is a small program that takes the {@link GrammaticalRelation}s defined in {@link EnglishGrammaticalRelations}
 * and defines the enum values to insert into {@link com.rreganjr.nlp.GrammaticalRelationType}, although the idea of
 * the enum type is to insulate the rest of the program from dependencies on the Stanford Parser, but I guess it doesn't
 * if it needs to be rebuilt and then all the references changed.
 *
 */

public class GrammaticalRelationTypeBuilder {
    EnglishGrammaticalRelations x;

    static List<GrammaticalRelation> relations = Arrays.asList(
            GOVERNOR,
            DEPENDENT,
            ROOT,

            PREDICATE,
            AUX_MODIFIER,
            AUX_PASSIVE_MODIFIER,
            COPULA,
            CONJUNCT,
            COORDINATION,
            PUNCTUATION,
            ARGUMENT,
            SUBJECT,
            NOMINAL_SUBJECT,
            NOMINAL_PASSIVE_SUBJECT,
            CLAUSAL_SUBJECT,
            CLAUSAL_PASSIVE_SUBJECT,
            COMPLEMENT,
            OBJECT,
            DIRECT_OBJECT,
            INDIRECT_OBJECT,
            NOMINAL_MODIFIER,
            CLAUSAL_COMPLEMENT,
            XCLAUSAL_COMPLEMENT,
            MARKER,
            RELATIVE,
            REFERENT,
            EXPLETIVE,
            MODIFIER,
            ADV_CLAUSE_MODIFIER,
            TEMPORAL_MODIFIER,
            RELATIVE_CLAUSE_MODIFIER,
            NUMERIC_MODIFIER,
            ADJECTIVAL_MODIFIER,
            COMPOUND_MODIFIER,
            NAME_MODIFIER,
            APPOSITIONAL_MODIFIER,
            CLAUSAL_MODIFIER,
            ADVERBIAL_MODIFIER,
            NEGATION_MODIFIER,
            MULTI_WORD_EXPRESSION,
            DETERMINER,
            PREDETERMINER,
            PRECONJUNCT,
            POSSESSION_MODIFIER,
            CASE_MARKER,
            PHRASAL_VERB_PARTICLE,
            SEMANTIC_DEPENDENT,
            AGENT,
            NP_ADVERBIAL_MODIFIER,
            PARATAXIS,
            DISCOURSE_ELEMENT,
            GOES_WITH,
            LIST,
            PREPOSITION,
            QMOD,
            CONTROLLING_NOMINAL_SUBJECT,
            CONTROLLING_NOMINAL_PASSIVE_SUBJECT,
            CONTROLLING_CLAUSAL_SUBJECT,
            CONTROLLING_CLAUSAL_PASSIVE_SUBJECT
    );

    public static void main(String[] args) {
        for (GrammaticalRelation relation : relations) {
            String enumName = relation.getLongName().toUpperCase().replace(' ', '_').replace('-','_');
            String parentEnumName = (relation.getParent()==null?"null":relation.getParent().getLongName().toUpperCase().replace(' ', '_').replace('-','_'));
            System.out.println( enumName + "(" +
                    parentEnumName + ", \"" +
                    relation.getShortName() + "\", \"" +
                    relation.getLongName() + "\", \"\"),"
                    );
        }

    }
}
