/*
 * This file is part of Requel - the Collaborative Requirements
 * Elicitation System.
 *
 * Copyright 2008, 2009, 2025 Ron Regan Jr. All Rights Reserved.
 *
 * Requel is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Requel is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Requel. If not, see <http://www.gnu.org/licenses/>.
 *
 */
package com.rreganjr.nlp.utils;


import com.rreganjr.nlp.dictionary.GrammaticalRelationType;
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
 * and defines the enum values to insert into {@link GrammaticalRelationType}, although the idea of
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
