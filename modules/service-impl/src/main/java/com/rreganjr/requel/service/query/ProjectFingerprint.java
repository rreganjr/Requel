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

package com.rreganjr.requel.service.query;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HexFormat;
import java.util.Iterator;
import java.util.List;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.rreganjr.requel.project.EntitySourceLink;
import com.rreganjr.requel.project.ExternalSource;
import com.rreganjr.requel.project.Project;
import com.rreganjr.requel.project.ProvenanceStore;
import com.rreganjr.requel.project.SourceAuthorityEdge;
import com.rreganjr.requel.service.api.dto.ProjectContentDto;
import com.rreganjr.requel.service.query.ProjectContentQueryService.AnnotationMode;

/**
 * Issue #275: the "project version" an emitted document names. It is a fingerprint of what a
 * generator renders — the #274 content read (open annotations, stale flags included) plus the
 * project's sources — so it changes when the content changes and only then: an edit that is
 * reverted gives the original version back.
 *
 * <p>Left out, because they move without the content moving: every JPA {@code version}, the
 * read's character count and cap, and the project summary's report-generator and
 * dictionary-word counts (adding the bundled generator or a dictionary word is not a content
 * change).
 */
@Service
@Transactional(readOnly = true)
public class ProjectFingerprint {

	/** Hex characters shown. */
	public static final int LENGTH = 12;

	private final ProjectContentQueryService contentQueryService;
	private final ObjectProvider<ProvenanceStore> provenanceStore;
	private final ObjectMapper objectMapper;

	public ProjectFingerprint(ProjectContentQueryService contentQueryService,
			ObjectProvider<ProvenanceStore> provenanceStore, ObjectMapper objectMapper) {
		this.contentQueryService = contentQueryService;
		this.provenanceStore = provenanceStore;
		this.objectMapper = objectMapper;
	}

	/**
	 * @return the first {@link #LENGTH} hex characters of the SHA-256 fingerprint. The caller has
	 *         already checked that the current user can read the project.
	 */
	public String of(Project project) {
		ProjectContentDto content = contentQueryService.readUncapped(project, AnnotationMode.OPEN);
		ObjectNode root = objectMapper.valueToTree(content);
		root.remove("characters");
		root.remove("maxCharacters");
		JsonNode summary = root.get("project");
		if (summary instanceof ObjectNode summaryNode) {
			summaryNode.remove("reportGeneratorCount");
			summaryNode.remove("dictionaryWordCount");
		}
		stripVersions(root);
		root.set("sources", sources(project));
		try {
			byte[] canonical = objectMapper.writeValueAsBytes(root);
			byte[] digest = MessageDigest.getInstance("SHA-256").digest(canonical);
			return HexFormat.of().formatHex(digest).substring(0, LENGTH);
		} catch (NoSuchAlgorithmException | com.fasterxml.jackson.core.JsonProcessingException e) {
			throw new IllegalStateException("project fingerprint failed for " + project.getName(), e);
		}
	}

	private static void stripVersions(JsonNode node) {
		if (node instanceof ObjectNode object) {
			object.remove("version");
			Iterator<JsonNode> children = object.elements();
			while (children.hasNext()) {
				stripVersions(children.next());
			}
		} else if (node instanceof ArrayNode array) {
			for (JsonNode child : array) {
				stripVersions(child);
			}
		}
	}

	/** Every field the Resources section renders, sorted so the order is stable. */
	private ArrayNode sources(Project project) {
		ArrayNode sources = objectMapper.createArrayNode();
		ProvenanceStore store = provenanceStore.getIfAvailable();
		if (store == null) {
			return sources;
		}
		List<ExternalSource> listed = new ArrayList<>(store.listSources(project.getId()));
		listed.sort(Comparator.comparing(ExternalSource::getSystem)
				.thenComparing(ExternalSource::getExternalId));
		List<SourceAuthorityEdge> edges = store.authorityEdges(project.getId());
		for (ExternalSource source : listed) {
			ObjectNode node = sources.addObject();
			node.put("system", source.getSystem());
			node.put("externalId", source.getExternalId());
			node.put("locatorType",
					source.getLocatorType() == null ? null : source.getLocatorType().name());
			node.put("locator", source.getLocator());
			node.put("title", source.getTitle());
			node.put("kind", source.getKind());
			node.put("note", source.getNote());
			node.put("contentHash", source.getContentHash());
			List<String> defersTo = new ArrayList<>();
			for (SourceAuthorityEdge edge : edges) {
				if (edge.getSubordinate().getId().equals(source.getId())) {
					defersTo.add(edge.getSuperior().getSystem() + " "
							+ edge.getSuperior().getExternalId() + " " + nullToEmpty(edge.getNote()));
				}
			}
			defersTo.sort(Comparator.naturalOrder());
			ArrayNode defers = node.putArray("defersTo");
			defersTo.forEach(defers::add);
			List<String> links = new ArrayList<>();
			for (EntitySourceLink link : store.linksForSource(source.getId())) {
				links.add(link.getRelation().name() + " " + link.getTargetType() + " "
						+ link.getTargetId() + " " + nullToEmpty(link.getFragment()));
			}
			links.sort(Comparator.naturalOrder());
			ArrayNode linkArray = node.putArray("links");
			links.forEach(linkArray::add);
		}
		return sources;
	}

	private static String nullToEmpty(String value) {
		return value == null ? "" : value;
	}
}
