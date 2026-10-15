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
package com.rreganjr.requel.assistant.core.definition;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Issue #263, dev only: definitions read from {@code requel.ai.definitions.dir} replace the bundled
 * ones with the same key, at startup (after seeding) and on each {@link #reload()}, so review
 * definitions can be tuned without a rebuild or restart. A file is validated against the bundled
 * set with the overrides in place. Absent unless the property is set.
 */
@Component
@ConditionalOnProperty(name = "requel.ai.definitions.dir")
public class DevDefinitionOverrides {

	private static final Logger log = LoggerFactory.getLogger(DevDefinitionOverrides.class);

	private final AssistantDefinitionStore store;
	private final ObjectMapper objectMapper;
	private final File directory;

	@Autowired
	public DevDefinitionOverrides(AssistantDefinitionStore store, ObjectMapper objectMapper,
			@Value("${requel.ai.definitions.dir}") String directory) {
		this.store = store;
		this.objectMapper = objectMapper;
		this.directory = new File(directory);
	}

	/**
	 * Read every {@code *.json} in the directory and replace the bundled definitions with the same
	 * keys.
	 *
	 * @return the keys replaced, sorted
	 * @throws IllegalStateException if the directory is missing or a file is invalid; nothing is
	 *         replaced then
	 */
	public synchronized List<String> reload() {
		File[] files = directory.listFiles((dir, name) -> name.endsWith(".json"));
		if (files == null) {
			throw new IllegalStateException("requel.ai.definitions.dir is not a directory: "
					+ directory.getAbsolutePath());
		}
		Arrays.sort(files);
		Resource[] resources = new Resource[files.length];
		for (int i = 0; i < files.length; i++) {
			resources[i] = new FileSystemResource(files[i]);
		}
		// Parse and check the files among themselves, then against the bundled set they join.
		List<AssistantDefinition> overrides = BundledDefinitions.load(objectMapper,
				store.validator(), resources);
		Map<String, AssistantDefinition> merged = new LinkedHashMap<>();
		for (AssistantDefinition bundled : store.bundled()) {
			merged.put(bundled.key(), bundled);
		}
		for (AssistantDefinition override : overrides) {
			merged.put(override.key(), override);
		}
		List<AssistantDefinition> all = new ArrayList<>(merged.values());
		for (AssistantDefinition override : overrides) {
			store.validator().validate(override, all);
		}
		List<String> keys = new ArrayList<>();
		for (AssistantDefinition override : overrides) {
			store.overrideBundled(override);
			keys.add(override.key());
		}
		keys.sort(null);
		log.warn("Dev override: assistant definitions {} replaced from {}", keys,
				directory.getAbsolutePath());
		return keys;
	}
}
