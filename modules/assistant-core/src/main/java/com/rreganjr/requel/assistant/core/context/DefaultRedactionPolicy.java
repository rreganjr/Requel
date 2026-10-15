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
package com.rreganjr.requel.assistant.core.context;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.rreganjr.requel.project.DataHandlingSettings;
import com.rreganjr.requel.project.DataHandlingSettings.RedactionCategory;
import com.rreganjr.requel.project.ProjectAssistantSettingsStore;

/**
 * Issue #262: the default {@link RedactionPolicy}. Deterministic pattern detection that masks in
 * place with a typed placeholder ({@code [REDACTED:EMAIL]}) so the sentence still reads. The
 * categories, each a per-project switch ({@link DataHandlingSettings}):
 * <ul>
 * <li>{@code CREDENTIALS}: private-key blocks, passwords in URLs and in {@code password=} style
 * pairs, bearer tokens, JWTs, and well-known API-key shapes;</li>
 * <li>{@code EMAIL}; {@code SSN} (US, invalid ranges excluded); {@code CARD} (13–19 digits passing
 * Luhn); {@code PHONE} (international with a {@code +}, or US with separators).</li>
 * </ul>
 * Categories run in that order: credentials first so a key inside a URL is masked whole, and the
 * digit categories before phone so a card or SSN is not mistaken for a phone number. Placeholders
 * contain no digits or {@code @}, so a later pass never re-matches an earlier mask.
 *
 * <p>
 * Each field that changed gets one note, {@code <fieldPath>: EMAIL x1, CREDENTIALS x2}, which
 * {@link ContextPackMetadata#redactionCount()} and
 * {@link ContextPackMetadata#redactionCategories()} read back.
 */
@Component
public class DefaultRedactionPolicy implements RedactionPolicy {

	/** One detector: a pattern and how to rewrite a match (the whole match by default). */
	private record Rule(Pattern pattern, Function<Matcher, String> replacement) {
		static Rule whole(String regex, int flags, String placeholder) {
			return new Rule(Pattern.compile(regex, flags), m -> placeholder);
		}

		static Rule whole(Pattern pattern, String placeholder) {
			return new Rule(pattern, m -> placeholder);
		}
	}

	private static final Map<RedactionCategory, List<Rule>> RULES = rules();

	private final Set<RedactionCategory> categories;
	private ProjectAssistantSettingsStore settingsStore;

	/** Every category on; {@link #forProject} narrows it to a project's switches. */
	public DefaultRedactionPolicy() {
		this(EnumSet.allOf(RedactionCategory.class), null);
	}

	private DefaultRedactionPolicy(Set<RedactionCategory> categories,
			ProjectAssistantSettingsStore settingsStore) {
		this.categories = categories.isEmpty() ? EnumSet.noneOf(RedactionCategory.class)
				: EnumSet.copyOf(categories);
		this.settingsStore = settingsStore;
	}

	/** A policy that masks only {@code categories}. */
	public static DefaultRedactionPolicy of(Set<RedactionCategory> categories) {
		return new DefaultRedactionPolicy(Objects.requireNonNull(categories, "categories"), null);
	}

	@Autowired(required = false)
	public void setSettingsStore(ProjectAssistantSettingsStore settingsStore) {
		this.settingsStore = settingsStore;
	}

	@Override
	public RedactionPolicy forProject(Long projectId) {
		DataHandlingSettings settings = DataHandlingSettings.forProject(projectId, settingsStore);
		return new DefaultRedactionPolicy(settings.redactionCategories(), settingsStore);
	}

	@Override
	public String redact(String fieldPath, String value, List<String> notes) {
		if (value == null || value.isEmpty() || categories.isEmpty()) {
			return value;
		}
		String result = value;
		Map<RedactionCategory, Integer> counts = new EnumMap<>(RedactionCategory.class);
		for (RedactionCategory category : RedactionCategory.values()) {
			if (!categories.contains(category)) {
				continue;
			}
			for (Rule rule : RULES.get(category)) {
				Matcher matcher = rule.pattern().matcher(result);
				StringBuilder out = new StringBuilder();
				int found = 0;
				while (matcher.find()) {
					String replacement = rule.replacement().apply(matcher);
					if (replacement == null) {
						continue; // the rule declined this match (e.g. a Luhn failure)
					}
					matcher.appendReplacement(out, Matcher.quoteReplacement(replacement));
					found++;
				}
				if (found > 0) {
					matcher.appendTail(out);
					result = out.toString();
					counts.merge(category, found, Integer::sum);
				}
			}
		}
		if (!counts.isEmpty() && notes != null) {
			List<String> parts = new ArrayList<>();
			counts.forEach((category, n) -> parts.add(category.name() + " x" + n));
			notes.add(fieldPath + ": " + String.join(", ", parts));
		}
		return result;
	}

	static String placeholder(RedactionCategory category) {
		return "[REDACTED:" + category.name() + "]";
	}

	private static Map<RedactionCategory, List<Rule>> rules() {
		Map<RedactionCategory, List<Rule>> rules = new EnumMap<>(RedactionCategory.class);
		String cred = placeholder(RedactionCategory.CREDENTIALS);
		// #359: the patterns are shared with the lexical checks (RedactablePatterns).
		rules.put(RedactionCategory.CREDENTIALS, List.of(
				// PEM private-key blocks, whole
				Rule.whole(RedactablePatterns.PRIVATE_KEY, cred),
				// scheme://user:password@host -> keep the user and host, mask the password
				new Rule(RedactablePatterns.URL_PASSWORD, m -> m.group(1) + cred + "@"),
				// password=..., secret: "...", api_key=... -> keep the name
				new Rule(RedactablePatterns.SECRET_PAIR, m -> m.group(1) + m.group(2) + cred),
				// Authorization: Bearer <token>
				new Rule(RedactablePatterns.BEARER, m -> m.group(1) + " " + cred),
				// JWTs
				Rule.whole(RedactablePatterns.JWT, cred),
				// well-known API key shapes
				Rule.whole(RedactablePatterns.API_KEY, cred)));
		rules.put(RedactionCategory.EMAIL, List.of(Rule.whole(RedactablePatterns.EMAIL,
				placeholder(RedactionCategory.EMAIL))));
		rules.put(RedactionCategory.SSN, List.of(Rule.whole(
				"\\b(?!000|666|9\\d\\d)\\d{3}-(?!00)\\d{2}-(?!0000)\\d{4}\\b", 0,
				placeholder(RedactionCategory.SSN))));
		String card = placeholder(RedactionCategory.CARD);
		rules.put(RedactionCategory.CARD, List.of(new Rule(
				Pattern.compile("(?<![\\d-])\\d(?:[ -]?\\d){12,18}(?![\\d-])"),
				m -> luhn(m.group().replaceAll("[ -]", "")) ? card : null)));
		String phone = placeholder(RedactionCategory.PHONE);
		rules.put(RedactionCategory.PHONE, List.of(
				// +44 20 7946 0958, +1-555-123-4567
				Rule.whole("(?<![\\w+])\\+\\d{1,3}(?:[ .-]?\\(?\\d{1,4}\\)?){2,5}(?!\\d)", 0, phone),
				// (555) 123-4567, 555-123-4567, 555.123.4567 (separators required)
				Rule.whole("(?<![\\d-])(?:\\(\\d{3}\\)\\s?|\\d{3}[.-])\\d{3}[.-]\\d{4}(?![\\d-])", 0,
						phone)));
		return rules;
	}

	/** The Luhn check, for 13–19 digit strings. */
	static boolean luhn(String digits) {
		int length = digits.length();
		if (length < 13 || length > 19) {
			return false;
		}
		int sum = 0;
		boolean doubleIt = false;
		for (int i = length - 1; i >= 0; i--) {
			int d = digits.charAt(i) - '0';
			if (d < 0 || d > 9) {
				return false;
			}
			if (doubleIt) {
				d *= 2;
				if (d > 9) {
					d -= 9;
				}
			}
			sum += d;
			doubleIt = !doubleIt;
		}
		return sum % 10 == 0;
	}
}
