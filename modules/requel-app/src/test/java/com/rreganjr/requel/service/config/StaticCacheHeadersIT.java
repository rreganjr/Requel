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
 */package com.rreganjr.requel.service.config;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.rreganjr.AbstractIntegrationTestCase;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;

/**
 * Issue #396: after an upgrade the browser kept the old index.html (no Cache-Control, a 1980
 * Last-Modified) and the page stayed blank. The UI's entry points must be revalidated every
 * time, content-hashed files cached for a year, and nothing may carry Last-Modified.
 */
@AutoConfigureMockMvc
public class StaticCacheHeadersIT extends AbstractIntegrationTestCase {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void theUiEntryPointsAreNotCached() throws Exception {
        for (String path : new String[] { "/", "/index.html", "/login", "/projects/Requel/goals" }) {
            mockMvc.perform(get(path))
                    .andExpect(header().string("Cache-Control", StaticCacheHeadersFilter.UNHASHED));
        }
    }

    @Test
    void aContentHashedFileIsCachedForAYear() throws Exception {
        mockMvc.perform(get("/test396/main-ABCD1234.js"))
                .andExpect(status().isOk())
                .andExpect(header().string("Cache-Control", StaticCacheHeadersFilter.HASHED))
                .andExpect(header().doesNotExist("Last-Modified"));
    }

    @Test
    void anUnhashedFileIsRevalidatedAndHasNoLastModified() throws Exception {
        mockMvc.perform(get("/test396/plain.txt"))
                .andExpect(status().isOk())
                .andExpect(header().string("Cache-Control", StaticCacheHeadersFilter.UNHASHED))
                .andExpect(header().doesNotExist("Last-Modified"));
    }

    @Test
    void theApiKeepsItsOwnHeaders() throws Exception {
        mockMvc.perform(get("/api/projects"))
                .andExpect(status().isUnauthorized())
                .andExpect(result -> {
                    String cc = result.getResponse().getHeader("Cache-Control");
                    if (StaticCacheHeadersFilter.HASHED.equals(cc) || StaticCacheHeadersFilter.UNHASHED.equals(cc)) {
                        throw new AssertionError("the UI filter touched /api: " + cc);
                    }
                });
    }
}
