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
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Supplier;

import org.springframework.stereotype.Component;

import com.rreganjr.requel.assistant.core.context.ContextSection;
import com.rreganjr.requel.assistant.core.context.ProviderBudget;
import com.rreganjr.requel.assistant.core.context.ProviderContext;
import com.rreganjr.requel.assistant.core.context.RelatedEntity;
import com.rreganjr.requel.assistant.core.context.TextSimilarity;
import com.rreganjr.requel.project.GlossaryTerm;
import com.rreganjr.requel.project.ProjectOrDomain;

/**
 * Issue #263 (moved from #261): the terms a glossary term should be read against, for circularity
 * and conflict checks - its canonical term and alternate names, then the project's other terms
 * ranked by {@link TextSimilarity} on name and definition.
 */
@Component
public class GlossaryRelatedProvider extends AbstractContextProvider {

	public static final String ID = "glossary-related";

	@Override
	public String id() {
		return ID;
	}

	@Override
	public boolean appliesTo(Object target) {
		return target instanceof GlossaryTerm;
	}

	@Override
	public ContextSection contribute(Object target, ProviderBudget budget,
			ProviderContext context) {
		GlossaryTerm term = (GlossaryTerm) target;
		List<Supplier<RelatedEntity>> candidates = new ArrayList<>();
		Set<Long> listed = new HashSet<>();
		listed.add(term.getId());
		if (term.getCanonicalTerm() != null && listed.add(term.getCanonicalTerm().getId())) {
			GlossaryTerm canonical = term.getCanonicalTerm();
			candidates.add(() -> related(context, canonical, "canonical term", RELATED_CHARS));
		}
		List<GlossaryTerm> alternates = new ArrayList<>(term.getAlternateTerms());
		alternates.sort(BY_NAME);
		for (GlossaryTerm alternate : alternates) {
			if (listed.add(alternate.getId())) {
				candidates.add(() -> related(context, alternate, "alternate name", RELATED_CHARS));
			}
		}
		ProjectOrDomain project = term.getProjectOrDomain();
		if (project != null) {
			List<GlossaryTerm> others = new ArrayList<>();
			for (GlossaryTerm other : project.getGlossaryTerms()) {
				if (!listed.contains(other.getId())) {
					others.add(other);
				}
			}
			Map<Long, Double> score = similarity(term, others);
			others.sort(Comparator.comparing((GlossaryTerm g) -> -score.getOrDefault(g.getId(), 0.0))
					.thenComparing(g -> g, BY_NAME));
			for (GlossaryTerm other : others) {
				candidates.add(() -> related(context, other, "other term", SUMMARY_CHARS));
			}
		}
		return fill(budget, candidates);
	}

	private static Map<Long, Double> similarity(GlossaryTerm term, List<GlossaryTerm> others) {
		List<String> texts = new ArrayList<>(others.size());
		for (GlossaryTerm other : others) {
			texts.add(nameAndText(other));
		}
		double[] scores = new TextSimilarity().scores(nameAndText(term), texts);
		Map<Long, Double> byId = new HashMap<>();
		for (int i = 0; i < others.size(); i++) {
			byId.put(others.get(i).getId(), scores[i]);
		}
		return byId;
	}

	private static String nameAndText(GlossaryTerm term) {
		return (term.getName() == null ? "" : term.getName()) + " "
				+ (term.getText() == null ? "" : term.getText());
	}
}
