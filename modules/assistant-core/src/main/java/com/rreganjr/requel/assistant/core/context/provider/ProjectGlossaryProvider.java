/*
 * This file is part of Requel - the Collaborative Requirements
 * Elicitation System.
 *
 * Copyright 2026 Ron Regan Jr. All Rights Reserved.
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
package com.rreganjr.requel.assistant.core.context.provider;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

import org.springframework.stereotype.Component;

import com.rreganjr.requel.assistant.core.context.ContextSection;
import com.rreganjr.requel.assistant.core.context.ProviderBudget;
import com.rreganjr.requel.assistant.core.context.ProviderContext;
import com.rreganjr.requel.assistant.core.context.RelatedEntity;
import com.rreganjr.requel.assistant.core.context.TextSimilarity;
import com.rreganjr.requel.project.GlossaryTerm;
import com.rreganjr.requel.project.ProjectOrDomain;
import com.rreganjr.requel.project.ProjectOrDomainEntity;

/**
 * Issue #265: the project's glossary, for terminology checks on any entity. Each canonical term
 * (a term with no canonical term of its own) is listed with its definition and its alternate
 * names as children, so a review can tell an alternate used where the canonical term belongs.
 * Terms are ranked by {@link TextSimilarity} to the target's name and text, so the ones it is
 * likely to use survive the budget cut.
 */
@Component
public class ProjectGlossaryProvider extends AbstractContextProvider {

	public static final String ID = "project-glossary";

	@Override
	public String id() {
		return ID;
	}

	@Override
	public boolean appliesTo(Object target) {
		return target instanceof ProjectOrDomainEntity;
	}

	@Override
	public ContextSection contribute(Object target, ProviderBudget budget,
			ProviderContext context) {
		ProjectOrDomainEntity entity = (ProjectOrDomainEntity) target;
		ProjectOrDomain project = entity.getProjectOrDomain();
		List<Supplier<RelatedEntity>> candidates = new ArrayList<>();
		if (project != null) {
			List<GlossaryTerm> canonical = new ArrayList<>();
			for (GlossaryTerm term : project.getGlossaryTerms()) {
				if (term.getCanonicalTerm() == null) {
					canonical.add(term);
				}
			}
			Map<Long, Double> score = similarity(entity, canonical);
			canonical.sort(Comparator.comparing((GlossaryTerm g) -> -score.getOrDefault(g.getId(), 0.0))
					.thenComparing(g -> g, BY_NAME));
			for (GlossaryTerm term : canonical) {
				candidates.add(() -> withAlternates(context, term));
			}
		}
		return fill(budget, candidates);
	}

	private static RelatedEntity withAlternates(ProviderContext context, GlossaryTerm term) {
		List<GlossaryTerm> alternates = new ArrayList<>(term.getAlternateTerms());
		alternates.sort(BY_NAME);
		List<RelatedEntity> children = new ArrayList<>();
		for (GlossaryTerm alternate : alternates) {
			String path = "GlossaryTerm[" + alternate.getId() + "]";
			children.add(new RelatedEntity(ref(alternate), "alternate name",
					context.name(path + ".name", alternate.getName()), null));
		}
		return related(context, term, "glossary term", SUMMARY_CHARS, children);
	}

	private static Map<Long, Double> similarity(ProjectOrDomainEntity target,
			List<GlossaryTerm> terms) {
		List<String> texts = new ArrayList<>(terms.size());
		for (GlossaryTerm term : terms) {
			StringBuilder text = new StringBuilder(nullToEmpty(term.getName()));
			for (GlossaryTerm alternate : term.getAlternateTerms()) {
				text.append(' ').append(nullToEmpty(alternate.getName()));
			}
			texts.add(text.append(' ').append(nullToEmpty(term.getText())).toString());
		}
		String query = nullToEmpty(target.getName()) + " " + nullToEmpty(textOf(target));
		double[] scores = new TextSimilarity().scores(query, texts);
		Map<Long, Double> byId = new HashMap<>();
		for (int i = 0; i < terms.size(); i++) {
			byId.put(terms.get(i).getId(), scores[i]);
		}
		return byId;
	}

	private static String nullToEmpty(String value) {
		return value == null ? "" : value;
	}
}
