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
package com.rreganjr.requel.tagging.impl;

import java.util.ArrayList;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import com.rreganjr.platform.exception.EntityException;
import com.rreganjr.repository.jpa.AbstractJpaRepository;
import com.rreganjr.repository.jpa.ExceptionMapper;
import com.rreganjr.requel.tagging.Tag;
import com.rreganjr.requel.tagging.TagRepository;

import jakarta.persistence.TypedQuery;

/**
 * JPA-based repository for {@link Tag}s.
 *
 * @author ron
 */
@Repository("tagRepository")
@Scope("singleton")
@Transactional(propagation = Propagation.REQUIRED, noRollbackFor = { EntityException.class })
public class JpaTagRepository extends AbstractJpaRepository implements TagRepository {

	@Autowired
	public JpaTagRepository(ExceptionMapper exceptionMapper) {
		super(exceptionMapper);
	}

	@Override
	public Tag createTag(Tag tag) throws EntityException {
		return create(tag);
	}

	@Override
	public Tag findTagById(Long id) {
		if (id == null) {
			return null;
		}
		return getEntityManager().find(TagImpl.class, id);
	}

	@Override
	public Tag findTag(Long projectId, String category, String value) {
		StringBuilder jpql = new StringBuilder("select t from TagImpl t where t.value = :value");
		jpql.append(category == null ? " and t.category is null" : " and t.category = :category");
		jpql.append(projectId == null ? " and t.projectId is null" : " and t.projectId = :projectId");
		TypedQuery<TagImpl> query = getEntityManager().createQuery(jpql.toString(), TagImpl.class);
		query.setParameter("value", value);
		if (category != null) {
			query.setParameter("category", category);
		}
		if (projectId != null) {
			query.setParameter("projectId", projectId);
		}
		List<TagImpl> results = query.getResultList();
		return results.isEmpty() ? null : results.get(0);
	}

	@Override
	public List<Tag> findTagsForProject(Long projectId) {
		TypedQuery<TagImpl> query;
		if (projectId == null) {
			query = getEntityManager().createQuery(
					"select t from TagImpl t where t.projectId is null order by t.category, t.value",
					TagImpl.class);
		} else {
			query = getEntityManager().createQuery(
					"select t from TagImpl t where t.projectId = :projectId or t.projectId is null "
							+ "order by t.category, t.value",
					TagImpl.class);
			query.setParameter("projectId", projectId);
		}
		return new ArrayList<>(query.getResultList());
	}

	@Override
	public List<Tag> findTagsOnEntity(String taggableType, Long taggableId) {
		@SuppressWarnings("unchecked")
		List<Number> tagIds = getEntityManager()
				.createNativeQuery("select tag_id from tag_taggable "
						+ "where taggable_type = :taggableType and taggable_id = :taggableId")
				.setParameter("taggableType", taggableType)
				.setParameter("taggableId", taggableId)
				.getResultList();
		List<Tag> tags = new ArrayList<>(tagIds.size());
		for (Number tagId : tagIds) {
			Tag tag = getEntityManager().find(TagImpl.class, tagId.longValue());
			if (tag != null) {
				tags.add(tag);
			}
		}
		return tags;
	}

	@Override
	public List<String> findDistinctCategories(Long projectId) {
		TypedQuery<String> query;
		if (projectId == null) {
			query = getEntityManager().createQuery(
					"select distinct t.category from TagImpl t "
							+ "where t.projectId is null and t.category is not null order by t.category",
					String.class);
		} else {
			query = getEntityManager().createQuery(
					"select distinct t.category from TagImpl t "
							+ "where (t.projectId = :projectId or t.projectId is null) "
							+ "and t.category is not null order by t.category",
					String.class);
			query.setParameter("projectId", projectId);
		}
		return new ArrayList<>(query.getResultList());
	}
}
