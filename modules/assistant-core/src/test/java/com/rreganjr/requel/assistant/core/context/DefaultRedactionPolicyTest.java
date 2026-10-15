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

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.junit.jupiter.api.Test;

import com.rreganjr.requel.project.DataHandlingSettings.RedactionCategory;
import com.rreganjr.requel.project.ProjectAssistantSettingsStore;

/** Issue #262: what the default policy masks, what it leaves alone, and the per-project view. */
class DefaultRedactionPolicyTest {

	private final DefaultRedactionPolicy policy = new DefaultRedactionPolicy();

	private String redact(String value) {
		return policy.redact("goal.text", value, new ArrayList<>());
	}

	@ParameterizedTest
	@ValueSource(strings = {
			"use sk-proj-AbCdEfGhIjKlMnOpQrStUvWxYz012345 for the test",
			"key sk-ant-api03-AbCdEfGhIjKlMnOpQrStUvWx-yz0123",
			"token ghp_abcdefghijklmnopqrstuvwxyz0123456789",
			"aws AKIAIOSFODNN7EXAMPLE here",
			"slack xoxb-1234567890-abcdefghij",
			"Authorization: Bearer abcdefghijklmnop.qrstuvwxyz0123",
			"jwt eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiIxMjM0NTY3ODkwIn0.dozjgNryP4J3jVmNHl0w5N_XgL0n3I9PlFUP0THsR8U",
			"jdbc:mysql://root:hunter22@db.example.com:3306/requel",
			"set password=hunter22 in the config",
			"client_secret: 'abc123def456'"
	})
	void credentialsAreMasked(String text) {
		String out = redact(text);
		assertThat(out).contains("[REDACTED:CREDENTIALS]");
		assertThat(out).doesNotContain("hunter22").doesNotContain("AbCdEfGhIj")
				.doesNotContain("abcdefghijklmnop").doesNotContain("IOSFODNN7")
				.doesNotContain("abc123def456");
	}

	@Test
	void aPrivateKeyBlockIsMaskedWhole() {
		String out = redact("before -----BEGIN RSA PRIVATE KEY-----\nMIIEow\nIBAAK\n"
				+ "-----END RSA PRIVATE KEY----- after");
		assertThat(out).isEqualTo("before [REDACTED:CREDENTIALS] after");
	}

	@Test
	void aUrlKeepsItsUserAndHost() {
		assertThat(redact("postgres://app:s3cret@db:5432/x"))
				.isEqualTo("postgres://app:[REDACTED:CREDENTIALS]@db:5432/x");
	}

	@Test
	void emailsAreMaskedInPlace() {
		assertThat(redact("Contact ron.regan@example.com before release."))
				.isEqualTo("Contact [REDACTED:EMAIL] before release.");
	}

	@ParameterizedTest
	@ValueSource(strings = { "+1-555-123-4567", "+44 20 7946 0958", "(555) 123-4567",
			"555-123-4567", "555.123.4567" })
	void phonesAreMasked(String phone) {
		assertThat(redact("call " + phone + " now")).isEqualTo("call [REDACTED:PHONE] now");
	}

	@Test
	void ssnsAreMaskedButInvalidRangesAreNot() {
		assertThat(redact("ssn 123-45-6789")).isEqualTo("ssn [REDACTED:SSN]");
		assertThat(redact("ssn 000-12-3456")).isEqualTo("ssn 000-12-3456");
		assertThat(redact("ssn 666-12-3456")).isEqualTo("ssn 666-12-3456");
		assertThat(redact("ssn 912-12-3456")).isEqualTo("ssn 912-12-3456");
	}

	@Test
	void cardsNeedLuhn() {
		assertThat(redact("card 4111 1111 1111 1111 ok")).isEqualTo("card [REDACTED:CARD] ok");
		assertThat(redact("card 4111-1111-1111-1111")).isEqualTo("card [REDACTED:CARD]");
		assertThat(redact("order 4111111111111112")).isEqualTo("order 4111111111111112");
	}

	@ParameterizedTest
	@ValueSource(strings = {
			"Requel 2.0.0 ships in Q4",
			"see issue #262 and PR #357",
			"respond within 200 ms at the 95th percentile",
			"invoice 2026-09-30-001",
			"version 10.2.3.4",
			"the user picks a password",
			"the system should be fast"
	})
	void ordinaryRequirementTextIsLeftAlone(String text) {
		List<String> notes = new ArrayList<>();
		assertThat(policy.redact("goal.text", text, notes)).isEqualTo(text);
		assertThat(notes).isEmpty();
	}

	@Test
	void oneNotePerFieldWithCountsPerCategory() {
		List<String> notes = new ArrayList<>();
		String out = policy.redact("goal[42].text", "a@b.io and c@d.io, key sk-proj-"
				+ "AbCdEfGhIjKlMnOpQrStUvWx", notes);
		assertThat(out).isEqualTo("[REDACTED:EMAIL] and [REDACTED:EMAIL], key "
				+ "[REDACTED:CREDENTIALS]");
		assertThat(notes).containsExactly("goal[42].text: CREDENTIALS x1, EMAIL x2");
	}

	@Test
	void aKeyInsideAUrlIsMaskedOnce() {
		List<String> notes = new ArrayList<>();
		String out = policy.redact("goal.text",
				"https://api.example.com/v1?api_key=sk-proj-AbCdEfGhIjKlMnOpQrStUvWx", notes);
		assertThat(out).isEqualTo("https://api.example.com/v1?api_key=[REDACTED:CREDENTIALS]");
		assertThat(notes).containsExactly("goal.text: CREDENTIALS x1");
	}

	@Test
	void ofLimitsTheCategories() {
		DefaultRedactionPolicy emailOnly = DefaultRedactionPolicy.of(EnumSet.of(RedactionCategory.EMAIL));
		assertThat(emailOnly.redact("f", "a@b.io 555-123-4567", new ArrayList<>()))
				.isEqualTo("[REDACTED:EMAIL] 555-123-4567");
		assertThat(DefaultRedactionPolicy.of(Set.of()).redact("f", "a@b.io", new ArrayList<>()))
				.isEqualTo("a@b.io");
	}

	@Test
	void forProjectAppliesTheProjectsSwitches() {
		ProjectAssistantSettingsStore store = mock(ProjectAssistantSettingsStore.class);
		when(store.disabledAssistants(9L)).thenReturn(Set.of("redaction.phone"));
		policy.setSettingsStore(store);

		RedactionPolicy project = policy.forProject(9L);

		assertThat(project.redact("f", "a@b.io 555-123-4567", new ArrayList<>()))
				.isEqualTo("[REDACTED:EMAIL] 555-123-4567");
		assertThat(policy.forProject(null).redact("f", "555-123-4567", new ArrayList<>()))
				.isEqualTo("[REDACTED:PHONE]");
	}

	@Test
	void nullAndEmptyPassThrough() {
		assertThat(policy.redact("f", null, new ArrayList<>())).isNull();
		assertThat(policy.redact("f", "", new ArrayList<>())).isEmpty();
	}

	@Test
	void metadataReadsCountsAndCategoriesFromTheNotes() {
		ContextPackMetadata metadata = new ContextPackMetadata(java.time.Instant.EPOCH, 0, false,
				List.of("goal.text: CREDENTIALS x1, EMAIL x2", "goal.name: EMAIL x1"), List.of());
		assertThat(metadata.redactionCount()).isEqualTo(4);
		assertThat(metadata.redactionCategories()).containsExactly("CREDENTIALS", "EMAIL");
	}
}
