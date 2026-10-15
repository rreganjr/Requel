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
package com.rreganjr.requel.validation;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.apache.commons.lang3.StringUtils;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.rreganjr.requel.project.impl.ActorImpl;
import com.rreganjr.requel.project.impl.GlossaryTermImpl;
import com.rreganjr.requel.project.impl.GoalImpl;
import com.rreganjr.requel.project.impl.NonUserStakeholderImpl;
import com.rreganjr.requel.project.impl.ProjectTeamImpl;
import com.rreganjr.requel.project.impl.ReportGeneratorImpl;
import com.rreganjr.requel.project.impl.StepImpl;
import com.rreganjr.requel.project.impl.StoryImpl;
import com.rreganjr.requel.project.impl.UseCaseImpl;
import com.rreganjr.requel.tagging.impl.TagCategoryImpl;
import com.rreganjr.requel.user.impl.OrganizationImpl;
import com.rreganjr.requel.user.impl.UserImpl;
import com.rreganjr.validator.ValidationLimits;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;

/**
 * Verifies the entity-side {@code @Size} constraints added by issue #171 across all three JPA
 * modules that own an artifact {@code name}.
 *
 * <p>Uses {@link Validator#validateValue} rather than building entities, so this stays a fast unit
 * test with no persistence context: every one of these classes needs a project, an owner and a
 * creator to construct, none of which this is testing.
 *
 * <p>The entity constraint is not redundant with the DTO one, even though
 * {@code CommandInputValidator} now rejects an over-long name before the command runs. XML project
 * import ({@code utils-jaxb}) unmarshals straight into these entities with no DTO anywhere in the
 * path, so the entity annotation is the only thing between a malformed import file and a
 * driver-level truncation error.
 */
@DisplayName("Artifact name @Size constraints (#171)")
class ArtifactNameSizeConstraintTest {

    private static final String AT_LIMIT =
            StringUtils.repeat("a", ValidationLimits.ARTIFACT_NAME_MAX);
    private static final String OVER_LIMIT = AT_LIMIT + "a";

    /**
     * Every entity whose table has a {@code varchar(255)} name column. If a new artifact type is
     * added without its {@code @Size}, an over-long name reaches the database and fails as an opaque
     * 500 instead of a field message — so this list is the checklist, and should grow with them.
     */
    private static final Class<?>[] NAMED_ENTITIES = {
        ActorImpl.class, GlossaryTermImpl.class, GoalImpl.class, NonUserStakeholderImpl.class,
        ProjectTeamImpl.class, ReportGeneratorImpl.class, StepImpl.class, StoryImpl.class,
        UseCaseImpl.class, TagCategoryImpl.class, OrganizationImpl.class, UserImpl.class,
    };

    private static ValidatorFactory factory;
    private static Validator validator;

    @BeforeAll
    static void setUp() {
        factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();
    }

    @AfterAll
    static void tearDown() {
        if (factory != null) {
            factory.close();
        }
    }

    @Test
    @DisplayName("every named entity accepts a name of exactly the limit")
    void nameAtTheLimitIsAccepted() {
        List<String> failures = new ArrayList<>();
        for (Class<?> entityType : NAMED_ENTITIES) {
            Set<? extends ConstraintViolation<?>> violations =
                    validator.validateValue(entityType, "name", AT_LIMIT);
            if (!violations.isEmpty()) {
                failures.add(entityType.getSimpleName() + " -> " + violations);
            }
        }
        assertTrue(failures.isEmpty(),
                () -> "rejected a name of exactly " + ValidationLimits.ARTIFACT_NAME_MAX
                        + " characters: " + failures);
    }

    @Test
    @DisplayName("every named entity rejects a name one over the limit, with the shared message")
    void nameOverTheLimitIsRejected() {
        String expectedMessage =
                "must be " + ValidationLimits.ARTIFACT_NAME_MAX + " characters or fewer.";
        List<String> failures = new ArrayList<>();
        for (Class<?> entityType : NAMED_ENTITIES) {
            Set<? extends ConstraintViolation<?>> violations =
                    validator.validateValue(entityType, "name", OVER_LIMIT);
            if (violations.size() != 1) {
                failures.add(entityType.getSimpleName() + " -> " + violations.size()
                        + " violations, expected 1");
            } else if (!expectedMessage.equals(violations.iterator().next().getMessage())) {
                failures.add(entityType.getSimpleName() + " -> wrong message: "
                        + violations.iterator().next().getMessage());
            }
        }
        // The message assertion is doing real work: it proves the {max} placeholder interpolates and
        // that the wording matches the other hand-written entity messages ("a unique name is
        // required.") rather than the bean-validation default ("size must be between 0 and 255").
        // These strings render under the field in the UI.
        assertTrue(failures.isEmpty(), () -> "unexpected results: " + failures);
    }

    @Test
    @DisplayName("UserImpl name and username are both bounded")
    void userIdentityFieldsAreBounded() {
        for (String property : new String[] {"name", "username"}) {
            assertTrue(validator.validateValue(UserImpl.class, property, AT_LIMIT).isEmpty(),
                    property + " should accept a value at the limit");
            assertEquals(1, validator.validateValue(UserImpl.class, property, OVER_LIMIT).size(),
                    property + " should reject a value over the limit");
        }
    }

    @Test
    @DisplayName("the email constraint the client mirrors is real")
    void emailAddressIsValidated() {
        for (String malformed : new String[] {"not-an-email", "@nolocal.com", "two@@ats.com"}) {
            assertEquals(1, validator.validateValue(UserImpl.class, "emailAddress", malformed).size(),
                    "UserImpl.emailAddress should reject " + malformed);
        }
        for (String wellFormed : new String[] {"ron@example.com", "first.last+tag@sub.example.co.uk"}) {
            assertTrue(validator.validateValue(UserImpl.class, "emailAddress", wellFormed).isEmpty(),
                    "UserImpl.emailAddress should accept " + wellFormed);
        }
    }

    @Test
    @DisplayName("@Email checks format, not deliverability — a TLD-less domain is accepted")
    void singleLabelDomainIsAccepted() {
        // Pinned deliberately rather than left as a surprise. `ron@localhost` and `ron@intranet`
        // are well-formed under RFC 5322, and Hibernate Validator's @Email accepts them, so a user
        // who types "ron@exampl" (missing the .com) is NOT caught here. That is a product decision
        // about deliverability, not a validation bug, and adding a stricter regexp would need
        // Angular's Validators.email to be tightened in lockstep or the client and server would
        // disagree about what is acceptable.
        //
        // Angular's EMAIL_REGEXP also allows a single-label domain, so today they DO agree. Keep it
        // that way: a client-side rule stricter than the server blocks input the server accepts.
        for (String singleLabel : new String[] {"ron@localhost", "ron@intranet"}) {
            assertTrue(validator.validateValue(UserImpl.class, "emailAddress", singleLabel).isEmpty(),
                    () -> "expected @Email to accept the single-label domain " + singleLabel
                            + "; if this now fails, Hibernate Validator changed its regexp and "
                            + "shared/validation-limits.ts needs the matching client-side change");
        }
    }
}
