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
package com.rreganjr.requel.service.query;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.rreganjr.nlp.dictionary.DictionaryRepository;
import com.rreganjr.requel.service.api.dto.DictionaryWordDto;
import com.rreganjr.requel.service.auth.CurrentUserResolver;
import com.rreganjr.requel.user.User;
import com.rreganjr.requel.user.impl.SystemAdminUserRole;

/**
 * Read endpoint for the installation-wide dictionary (issue #319), for the admin page.
 * <p>
 * {@code ApiSecurityConfig} already restricts {@code /api/admin/**} to administrators; the role is
 * checked here as well so the endpoint stays closed if that rule is ever reordered behind the
 * {@code /api/**} catch-all.
 */
@RestController
@RequestMapping("/api/admin/dictionary")
public class DictionaryAdminQueryController {

    private final DictionaryRepository dictionaryRepository;
    private final CurrentUserResolver currentUserResolver;

    public DictionaryAdminQueryController(DictionaryRepository dictionaryRepository,
                                          CurrentUserResolver currentUserResolver) {
        this.dictionaryRepository = dictionaryRepository;
        this.currentUserResolver = currentUserResolver;
    }

    /**
     * GET /api/admin/dictionary — the installation-wide words, ordered by lemma. The WordNet
     * corpus and the jazzy word lists are not included; they are not editable.
     */
    @GetMapping
    public ResponseEntity<List<DictionaryWordDto>> listInstallWords() {
        User user = currentUserResolver.resolve();
        if (user == null || !user.hasRole(SystemAdminUserRole.class)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        List<DictionaryWordDto> words = dictionaryRepository.findInstallWords().stream()
                .map(w -> new DictionaryWordDto(w.getId(), w.getLemma()))
                .toList();
        return ResponseEntity.ok(words);
    }
}
