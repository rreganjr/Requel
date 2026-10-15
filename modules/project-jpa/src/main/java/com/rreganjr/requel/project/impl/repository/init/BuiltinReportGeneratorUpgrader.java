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

package com.rreganjr.requel.project.impl.repository.init;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.HexFormat;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import com.rreganjr.platform.bootstrap.AbstractSystemInitializer;
import com.rreganjr.requel.project.Project;
import com.rreganjr.requel.project.ProjectRepository;
import com.rreganjr.requel.project.ReportGenerator;
import com.rreganjr.requel.project.impl.BuiltinReportGenerators;
import com.rreganjr.requel.project.impl.ReportGeneratorImpl;

/**
 * Issue #275: bring existing projects onto the bundled report generators. Idempotent, runs at
 * every start.
 *
 * <ul>
 * <li>A generator with no key whose name is a bundled generator's name and whose text is an
 * unedited bundled version (SHA-256 with line endings normalized, listed in
 * {@code xslt/builtin-history.txt}, or the current bundle) gets that key, so it renders the
 * current bundle from now on.</li>
 * <li>A project without a generator of a bundled key gets one. A name its own generator already
 * uses gets a " (bundled)" suffix.</li>
 * <li>An edited copy is left alone; one INFO line gives the count.</li>
 * </ul>
 */
@Component("builtinReportGeneratorUpgrader")
@Scope("prototype")
public class BuiltinReportGeneratorUpgrader extends AbstractSystemInitializer {

	static final String HISTORY_PATH = "xslt/builtin-history.txt";

	private final ProjectRepository projectRepository;

	@Autowired
	public BuiltinReportGeneratorUpgrader(ProjectRepository projectRepository) {
		// after the users (100/101/200) so a project's creator is in place
		super(300);
		this.projectRepository = projectRepository;
	}

	@Override
	@Transactional(propagation = Propagation.REQUIRED)
	public void initialize() {
		Map<String, Set<String>> known = knownVersions();
		int adopted = 0;
		int added = 0;
		int edited = 0;
		for (Project project : projectRepository.findAllProjects()) {
			Set<String> keys = new HashSet<>();
			Set<String> names = new HashSet<>();
			for (ReportGenerator generator : project.getReportGenerators()) {
				if (generator.getBuiltinKey() != null) {
					keys.add(generator.getBuiltinKey());
				}
			}
			for (ReportGenerator generator : project.getReportGenerators()) {
				names.add(generator.getName().toLowerCase(Locale.ROOT));
				if (generator.getBuiltinKey() != null) {
					continue;
				}
				for (BuiltinReportGenerators.Builtin builtin : BuiltinReportGenerators.ALL) {
					if (keys.contains(builtin.key())
							|| !builtin.name().equalsIgnoreCase(generator.getName())) {
						continue;
					}
					if (known.getOrDefault(builtin.key(), Set.of()).contains(hash(generator.getText()))) {
						ReportGeneratorImpl impl = (ReportGeneratorImpl) generator;
						impl.setBuiltinKey(builtin.key());
						projectRepository.merge(impl);
						keys.add(builtin.key());
						adopted++;
					} else {
						edited++;
					}
				}
			}
			for (BuiltinReportGenerators.Builtin builtin : BuiltinReportGenerators.ALL) {
				if (keys.contains(builtin.key())) {
					continue;
				}
				String name = names.contains(builtin.name().toLowerCase(Locale.ROOT))
						? builtin.name() + " (bundled)" : builtin.name();
				ReportGeneratorImpl generator = new ReportGeneratorImpl(project,
						project.getCreatedBy(), name, builtin.text());
				generator.setBuiltinKey(builtin.key());
				generator = projectRepository.persist(generator);
				project.getReportGenerators().add(generator);
				keys.add(builtin.key());
				names.add(name.toLowerCase(Locale.ROOT));
				added++;
			}
		}
		if (adopted + added + edited > 0) {
			log.info("bundled report generators: " + adopted + " copies linked to the bundle, "
					+ added + " added, " + edited + " edited copies left as they are");
		}
	}

	/** Key -> hashes of every bundled version of it, the current bundle included. */
	static Map<String, Set<String>> knownVersions() {
		Map<String, Set<String>> known = new HashMap<>();
		for (BuiltinReportGenerators.Builtin builtin : BuiltinReportGenerators.ALL) {
			known.computeIfAbsent(builtin.key(), k -> new HashSet<>()).add(hash(builtin.text()));
		}
		try (InputStream in = BuiltinReportGeneratorUpgrader.class.getClassLoader()
				.getResourceAsStream(HISTORY_PATH)) {
			if (in != null) {
				for (String line : new String(in.readAllBytes(), StandardCharsets.UTF_8).split("\n")) {
					String trimmed = line.trim();
					if (trimmed.isEmpty() || trimmed.startsWith("#")) {
						continue;
					}
					String[] fields = trimmed.split("\\s+");
					if (fields.length >= 2) {
						known.computeIfAbsent(fields[0], k -> new HashSet<>()).add(fields[1]);
					}
				}
			}
		} catch (IOException e) {
			throw new IllegalStateException("could not read " + HISTORY_PATH, e);
		}
		return known;
	}

	/** SHA-256 hex of the text with line endings normalized to LF. */
	static String hash(String text) {
		String normalized = text == null ? "" : text.replace("\r\n", "\n").replace('\r', '\n');
		try {
			return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
					.digest(normalized.getBytes(StandardCharsets.UTF_8)));
		} catch (NoSuchAlgorithmException e) {
			throw new IllegalStateException(e);
		}
	}
}
