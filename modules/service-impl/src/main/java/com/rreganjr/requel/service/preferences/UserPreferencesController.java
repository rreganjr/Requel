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
package com.rreganjr.requel.service.preferences;

import com.rreganjr.requel.service.api.dto.UserPreferencesDto;
import com.rreganjr.requel.service.auth.CurrentUserResolver;
import com.rreganjr.requel.user.User;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.bind.annotation.*;

/**
 * REST endpoints for user UI preferences.
 * GET returns current preferences (creates defaults if none exist).
 * PUT updates preferences.
 *
 * Uses TransactionTemplate (programmatic transactions) rather than @Transactional
 * because the XML context's <aop:aspectj-autoproxy/> creates an AOP ordering conflict
 * that prevents Spring Boot's transaction advisor from intercepting controller methods.
 * TransactionTemplate bypasses AOP proxying entirely and guarantees commit.
 *
 * Uses EntityManager directly (same pattern as AbstractJpaRepository) because
 * Spring Data JPA's save() path does not reliably flush within this mixed
 * XML+Boot JPA context. em.merge() is the established working pattern here.
 */
@RestController
@RequestMapping("/api/user-preferences")
public class UserPreferencesController {

    private static final Logger log = LoggerFactory.getLogger(UserPreferencesController.class);

    private final UserPreferencesRepository repository;
    private final CurrentUserResolver currentUserResolver;
    private final TransactionTemplate txTemplate;

    public UserPreferencesController(UserPreferencesRepository repository,
                                     CurrentUserResolver currentUserResolver,
                                     PlatformTransactionManager transactionManager) {
        this.repository = repository;
        this.currentUserResolver = currentUserResolver;
        this.txTemplate = new TransactionTemplate(transactionManager);
        log.info("UserPreferencesController created with transactionManager: {}", transactionManager.getClass().getName());
    }

    @GetMapping
    public UserPreferencesDto getPreferences() {
        User user = currentUserResolver.resolve();
        log.info("getPreferences called for user={}", user.getUsername());
        UserPreferencesDto result = txTemplate.execute(status -> {
            UserPreferences prefs = findOrCreate(user.getUsername());
            return toDto(prefs);
        });
        log.info("getPreferences returning: {}", result);
        return result;
    }

    @PutMapping
    public ResponseEntity<UserPreferencesDto> updatePreferences(@RequestBody UserPreferencesDto input) {
        log.info("updatePreferences called: limit={} staleness={}", input.sidebarProjectLimit(), input.sidebarProjectStaleness());

        // Validate enum value before opening the transaction
        if (input.sidebarProjectStaleness() != null) {
            try {
                SidebarProjectStaleness.valueOf(input.sidebarProjectStaleness());
            } catch (IllegalArgumentException e) {
                return ResponseEntity.badRequest().build();
            }
        }

        User user = currentUserResolver.resolve();
        UserPreferencesDto result = txTemplate.execute(status -> {
            log.info("updatePreferences: inside txTemplate.execute, txStatus={}", status);
            UserPreferences prefs = findOrCreate(user.getUsername());
            log.info("updatePreferences: before update limit={} staleness={}", prefs.getSidebarProjectLimit(), prefs.getSidebarProjectStaleness());

            if (input.sidebarProjectLimit() > 0) {
                prefs.setSidebarProjectLimit(input.sidebarProjectLimit());
            }
            if (input.sidebarProjectStaleness() != null) {
                prefs.setSidebarProjectStaleness(
                        SidebarProjectStaleness.valueOf(input.sidebarProjectStaleness()));
            }

            repository.save(prefs);
            log.info("updatePreferences: after merge limit={} staleness={}", prefs.getSidebarProjectLimit(), prefs.getSidebarProjectStaleness());
            return toDto(prefs);
        });
        log.info("updatePreferences: txTemplate.execute completed, returning: {}", result);
        return ResponseEntity.ok(result);
    }

    /**
     * Load the preference row for this user, creating it with defaults if absent.
     * Must be called within an active transaction.
     */
    private UserPreferences findOrCreate(String username) {
        return repository.findByUsername(username).orElseGet(() -> {
            UserPreferences newPrefs = new UserPreferences(username);
            return repository.save(newPrefs);
        });
    }

    private UserPreferencesDto toDto(UserPreferences prefs) {
        return new UserPreferencesDto(
                prefs.getSidebarProjectLimit(),
                prefs.getSidebarProjectStaleness().name()
        );
    }
}
