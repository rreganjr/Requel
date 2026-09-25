/*
 * This file is part of Requel - the Collaborative Requirements
 * Elicitation System.
 *
 * Copyright 2008, 2009, 2025, 2026 Ron Regan Jr. All Rights Reserved.
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
package com.rreganjr.requel.annotation.impl;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.hibernate.Hibernate;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.metamodel.spi.MappingMetamodelImplementor;
import org.hibernate.persister.collection.CollectionPersister;
import org.hibernate.persister.entity.EntityPersister;
import org.hibernate.persister.entity.Joinable;

import com.rreganjr.requel.annotation.spi.AnnotatableTypeRegistry;
import com.rreganjr.validator.InvalidStateException;
import jakarta.persistence.NoResultException;
import jakarta.persistence.OptimisticLockException;
import jakarta.persistence.Query;

import org.hibernate.PropertyValueException;
import org.hibernate.StaleObjectStateException;
import org.hibernate.exception.LockAcquisitionException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.dao.CannotAcquireLockException;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import com.rreganjr.platform.exception.EntityException;
import com.rreganjr.platform.exception.EntityExceptionActionType;
import com.rreganjr.repository.jpa.AbstractJpaRepository;
import com.rreganjr.repository.jpa.ExceptionMapper;
import com.rreganjr.repository.jpa.GenericPropertyValueExceptionAdapter;
import com.rreganjr.repository.jpa.InvalidStateExceptionAdapter;
import com.rreganjr.repository.jpa.OptimisticLockExceptionAdapter;
import com.rreganjr.requel.annotation.Annotatable;
import com.rreganjr.requel.annotation.Annotation;
import com.rreganjr.requel.annotation.AnnotationExistsException;
import com.rreganjr.requel.annotation.AnnotationRepository;
import com.rreganjr.requel.annotation.Argument;
import com.rreganjr.requel.annotation.Issue;
import com.rreganjr.requel.annotation.NoSuchAnnotationException;
import com.rreganjr.requel.annotation.NoSuchPositionException;
import com.rreganjr.requel.annotation.Note;
import com.rreganjr.requel.annotation.Position;

/**
 * EJB3/JPA based repository
 * 
 * @author ron
 */
@Repository("annotationRepository")
@Scope("singleton")
@Transactional(propagation = Propagation.REQUIRED, noRollbackFor = { NoSuchPositionException.class,
		NoSuchAnnotationException.class, AnnotationExistsException.class, EntityException.class })
public class JpaAnnotationRepository extends AbstractJpaRepository implements AnnotationRepository {

	/**
	 * @param exceptionMapper
	 */
	@Autowired
	public JpaAnnotationRepository(ExceptionMapper exceptionMapper) {
		super(exceptionMapper);
		addExceptionAdapter(PropertyValueException.class,
				new GenericPropertyValueExceptionAdapter(), Position.class, Issue.class,
				Note.class, Argument.class);

		addExceptionAdapter(InvalidStateException.class, new InvalidStateExceptionAdapter(),
				Position.class, Issue.class, Note.class, Argument.class);

		addExceptionAdapter(OptimisticLockException.class, new OptimisticLockExceptionAdapter(),
				Position.class, Issue.class, Note.class, Argument.class);

		addExceptionAdapter(StaleObjectStateException.class, new OptimisticLockExceptionAdapter(),
				Position.class, Issue.class, Note.class, Argument.class);

		addExceptionAdapter(LockAcquisitionException.class, new OptimisticLockExceptionAdapter(),
				Position.class, Issue.class, Note.class, Argument.class);

		addExceptionAdapter(CannotAcquireLockException.class, new OptimisticLockExceptionAdapter(),
				Position.class, Issue.class, Note.class, Argument.class);

        addExceptionAdapter(ObjectOptimisticLockingFailureException.class,
                new OptimisticLockExceptionAdapter(), Position.class, Issue.class, Note.class,
                Argument.class);
	}

	@Override
	public <T> T findById(Class<T> entityType, Long id) {
		if (id == null) {
			return null;
		}
		// Annotation entity names follow the "<Interface>Impl" convention (IssueImpl,
		// NoteImpl, ...); the query is polymorphic so a subtype (e.g. a lexical issue)
		// is returned for Issue.class.
		String entityName = entityType.getSimpleName() + "Impl";
		try {
			Query query = getEntityManager()
					.createQuery("select e from " + entityName + " as e where e.id = :id");
			query.setParameter("id", id);
			return entityType.cast(query.getSingleResult());
		} catch (NoResultException e) {
			return null;
		}
	}

	@Override
	public Annotation findAnnotationById(Long id) {
		if (id == null) {
			return null;
		}
		try {
			Query query = getEntityManager()
					.createQuery("select e from AbstractAnnotation as e where e.id = :id");
			query.setParameter("id", id);
			return (Annotation) query.getSingleResult();
		} catch (NoResultException e) {
			return null;
		}
	}

	/**
	 * The lowest-id position in the grouping object with this text.
	 * <p>
	 * #284: this used to be {@code getSingleResult()} over a {@code like} comparison, which failed
	 * two ways. {@code getSingleResult()} threw {@code NonUniqueResultException} when duplicate
	 * text existed — converted by the catch below into a failed command, so
	 * {@code EditPositionCommandImpl}'s reuse path (#281) never got a chance. And {@code like}
	 * treated {@code %} and {@code _} in a position's text as wildcards, so one position
	 * containing either could match others and produce that same failure with no duplicates
	 * present at all. Now it matches on equality and resolves deterministically to the lowest id;
	 * {@link #findPositions} exposes the full list for callers that can repair the duplication.
	 */
	@Override
	public Position findPosition(Object groupingObject, String text) throws NoSuchPositionException {
		List<Position> matches = findPositions(groupingObject, text);
		if (matches.isEmpty()) {
			throw NoSuchPositionException.forText(text);
		}
		return matches.get(0);
	}

	@Override
	@SuppressWarnings("unchecked")
	public List<Position> findPositions(Object groupingObject, String text) {
		try {
			// TODO: use named query so it can be configured externally
			Query query = getEntityManager().createQuery(
					"select object(position) from PositionImpl as position "
							+ "inner join position.issues issue where position.text = :text "
							+ "and issue.groupingObject = :groupingObject "
							+ "order by position.id");
			query.setParameter("groupingObject", groupingObject);
			query.setParameter("text", text);
			return query.getResultList();
		} catch (Exception e) {
			throw convertException(e, Position.class, null, EntityExceptionActionType.Reading);
		}
	}

	@Override
	@Transactional(propagation = Propagation.REQUIRED)
	public Position mergeDuplicatePositions(List<Position> duplicates) {
		if (duplicates.size() < 2) {
			return duplicates.isEmpty() ? null : duplicates.get(0);
		}
		try {
			return new PositionMerger(getEntityManager()).merge(duplicates);
		} catch (Exception e) {
			throw convertException(e, Position.class, duplicates.get(0),
					EntityExceptionActionType.Updating);
		}
	}

	public AddWordToDictionaryPosition findAddWordToDictionaryPosition(Object groupingObject,
			String word) throws NoSuchPositionException {
		try {
			// TODO: use named query so it can be configured externally
			Query query = getEntityManager().createQuery(
					"select object(position) from AddWordToDictionaryPosition as position "
							+ "inner join position.issues issue where issue.word like :word "
							+ "and issue.groupingObject = :groupingObject");
			query.setParameter("groupingObject", groupingObject);
			query.setParameter("word", word);
			return (AddWordToDictionaryPosition) query.getSingleResult();
		} catch (NoResultException e) {
			throw NoSuchPositionException.forAddingWordToDictionary(word);
		} catch (Exception e) {
			throw convertException(e, Position.class, null, EntityExceptionActionType.Reading);
		}
	}

	/**
	 * @see com.rreganjr.requel.annotation.AnnotationRepository#findChangeSpellingPosition(com.rreganjr.requel.annotation.Issue,
	 *      java.lang.String)
	 */
	@Override
	public ChangeSpellingPosition findChangeSpellingPosition(LexicalIssue issue, String proposedWord)
			throws NoSuchPositionException {
		try {
			// TODO: use named query so it can be configured externally
			Query query = getEntityManager().createQuery(
					"select object(position) from ChangeSpellingPosition as position "
							+ "inner join position.issues issue "
							+ "where position.proposedWord = :proposedWord "
							+ "and issue.groupingObject = :groupingObject");
			query.setParameter("groupingObject", issue.getGroupingObject());
			query.setParameter("proposedWord", proposedWord);
			return (ChangeSpellingPosition) query.getSingleResult();
		} catch (NoResultException e) {
			throw NoSuchPositionException.forChangeSpelling(issue, proposedWord);
		} catch (Exception e) {
			throw convertException(e, Position.class, null, EntityExceptionActionType.Reading);
		}
	}

	/**
	 * Issue #320: find the issue on {@code annotatable} with exactly this text. Only the
	 * annotatable's own annotations are searched. This used to be a project-wide query that
	 * ignored {@code annotatable}, so a caller creating an issue on one entity was handed another
	 * entity's issue with the same text and attached it to both.
	 */
	@Override
	public Issue findIssue(Object groupingObject, Annotatable annotatable, String message) {
		Issue issue = lowestIdMatch(groupingObject, annotatable, Issue.class,
				candidate -> message != null && message.equals(candidate.getText()));
		if (issue == null) {
			throw NoSuchAnnotationException.forMessage(message);
		}
		return issue;
	}

	/**
	 * Issue #320: find the lexical issue on {@code annotatable} for {@code word} (compared
	 * case-insensitively) that is not tied to a property. Only the annotatable's own annotations
	 * are searched; see {@link #findIssue}.
	 */
	@Override
	public LexicalIssue findLexicalIssue(Object groupingObject, Annotatable annotatable, String word)
			throws NoSuchAnnotationException {
		LexicalIssue issue = lowestIdMatch(groupingObject, annotatable, LexicalIssue.class,
				candidate -> sameWord(word, candidate.getWord())
						&& candidate.getAnnotatableEntityPropertyName() == null);
		if (issue == null) {
			throw NoSuchAnnotationException.forWord(word, "<no property>");
		}
		return issue;
	}

	/**
	 * Issue #320: find the lexical issue on {@code annotatable} for {@code word} in the property
	 * {@code annotatableEntityPropertyName}, both compared case-insensitively. Only the
	 * annotatable's own annotations are searched; see {@link #findIssue}.
	 */
	@Override
	public LexicalIssue findLexicalIssue(Object groupingObject, Annotatable annotatable,
			String word, String annotatableEntityPropertyName) throws NoSuchAnnotationException {
		LexicalIssue issue = lowestIdMatch(groupingObject, annotatable, LexicalIssue.class,
				candidate -> sameWord(word, candidate.getWord())
						&& annotatableEntityPropertyName != null
						&& annotatableEntityPropertyName
								.equalsIgnoreCase(candidate.getAnnotatableEntityPropertyName()));
		if (issue == null) {
			throw NoSuchAnnotationException.forWord(word, annotatableEntityPropertyName);
		}
		return issue;
	}

	/**
	 * A null word never matches, the way {@code word like null} never did. Issues with no word
	 * (complexity findings) must not match one another.
	 */
	private static boolean sameWord(String word, String candidate) {
		return word != null && word.equalsIgnoreCase(candidate);
	}

	/**
	 * The annotation of {@code type} on {@code annotatable}, in {@code groupingObject}, that
	 * satisfies {@code test}. When several match, the one with the lowest id wins, so the choice
	 * doesn't depend on set iteration order. A null annotatable has nothing to search.
	 * Annotatables carry tens of annotations, so walking the collection is cheaper than a join
	 * through the {@code @ManyToAny} table and behaves the same on H2 and MySQL. A detached
	 * annotatable is reloaded first, because its collection is a snapshot from whenever it was
	 * loaded and misses annotations added since.
	 */
	private <T extends Annotation> T lowestIdMatch(Object groupingObject,
			Annotatable annotatable, Class<T> type, java.util.function.Predicate<T> test) {
		if (annotatable == null) {
			return null;
		}
		Annotatable managed = attach(getEntityManager(), annotatable);
		T best = null;
		for (Annotation annotation : managed.getAnnotations()) {
			Object unproxied = Hibernate.unproxy(annotation);
			if (!type.isInstance(unproxied)) {
				continue;
			}
			T candidate = type.cast(unproxied);
			if (!java.util.Objects.equals(groupingObject, candidate.getGroupingObject())
					|| !test.test(candidate)) {
				continue;
			}
			if (best == null || (candidate.getId() != null
					&& (best.getId() == null || candidate.getId() < best.getId()))) {
				best = candidate;
			}
		}
		return best;
	}

	@Override
	public void removeAnnotatableFromAnnotationJoinTable(Long annotationId, Long annotatableId) {
		getEntityManager()
				.createNativeQuery(
						"DELETE FROM annotation_annotatable WHERE annotation_id = :annId AND annotatable_id = :aId")
				.setParameter("annId", annotationId)
				.setParameter("aId", annotatableId)
				.executeUpdate();
	}

	private AnnotatableTypeRegistry annotatableTypeRegistry;

	/**
	 * The discriminator registry the {@code @ManyToAny} mapping is built from (see
	 * ProjectAnnotatableMetadataContributor); optional so the repository still
	 * constructs in slices without the annotatable types registered.
	 */
	@Autowired(required = false)
	public void setAnnotatableTypeRegistry(AnnotatableTypeRegistry annotatableTypeRegistry) {
		this.annotatableTypeRegistry = annotatableTypeRegistry;
	}

	@Override
	@SuppressWarnings("unchecked")
	public List<Long> unlinkAllAnnotations(Annotatable annotatable) {
		Object entity = attach(getEntityManager(), annotatable);
		Class<?> entityClass = Hibernate.getClass(entity);
		Long annotatableId = (Long) getEntityManager().getEntityManagerFactory()
				.getPersistenceUnitUtil().getIdentifier(entity);
		if (annotatableId == null) {
			return Collections.emptyList();
		}
		Set<String> discriminators = annotatableDiscriminators(entityClass);

		// 1) the @ManyToAny side (annotation_annotatable), restricted by type: the
		// annotatable_id column is shared across entity types.
		List<Number> linked = getEntityManager()
				.createNativeQuery("SELECT annotation_id FROM annotation_annotatable"
						+ " WHERE annotatable_id = :aId AND annotatable_type IN (:types)")
				.setParameter("aId", annotatableId)
				.setParameter("types", discriminators)
				.getResultList();
		getEntityManager()
				.createNativeQuery("DELETE FROM annotation_annotatable"
						+ " WHERE annotatable_id = :aId AND annotatable_type IN (:types)")
				.setParameter("aId", annotatableId)
				.setParameter("types", discriminators)
				.executeUpdate();

		// 2) the entity-owned @ManyToMany side (<table>_annotations), located through the
		// mapping so table and key column names are never hand-maintained here.
		SessionFactoryImplementor sessionFactory = getEntityManager().getEntityManagerFactory()
				.unwrap(SessionFactoryImplementor.class);
		MappingMetamodelImplementor metamodel = sessionFactory.getMappingMetamodel();
		EntityPersister entityDescriptor = metamodel.getEntityDescriptor(entityClass);
		CollectionPersister annotations = metamodel
				.findCollectionDescriptor(entityDescriptor.getEntityName() + ".annotations");
		if ((annotations instanceof Joinable joinable) && !annotations.isInverse()) {
			String table = joinable.getTableName();
			String[] keyColumns = joinable.getKeyColumnNames();
			if (keyColumns.length == 1) {
				getEntityManager()
						.createNativeQuery("DELETE FROM " + table + " WHERE " + keyColumns[0] + " = :aId")
						.setParameter("aId", annotatableId)
						.executeUpdate();
			} else {
				log.warn("unlinkAllAnnotations: composite key on " + table + " for "
						+ entityClass.getName() + "; entity-side join rows not cleared");
			}
		} else if (annotations == null) {
			log.debug("unlinkAllAnnotations: no 'annotations' collection mapped on "
					+ entityClass.getName());
		}

		List<Long> ids = new ArrayList<Long>(linked.size());
		for (Number id : linked) {
			ids.add(Long.valueOf(id.longValue()));
		}
		return ids;
	}

	@Override
	@SuppressWarnings("unchecked")
	public List<Long> findAnnotationIdsByGroupingObject(Object groupingObject) {
		Object managed = groupingObject;
		if (!getEntityManager().contains(managed)) {
			managed = attach(getEntityManager(), managed);
		}
		List<Number> ids = getEntityManager()
				.createQuery("select annotation.id from AbstractAnnotation as annotation "
						+ "where annotation.groupingObject = :groupingObject")
				.setParameter("groupingObject", managed)
				.getResultList();
		List<Long> result = new ArrayList<Long>(ids.size());
		for (Number id : ids) {
			result.add(Long.valueOf(id.longValue()));
		}
		return result;
	}

	@Override
	public int unlinkAnnotation(Long annotationId) {
		return getEntityManager()
				.createNativeQuery("DELETE FROM annotation_annotatable WHERE annotation_id = :annId")
				.setParameter("annId", annotationId)
				.executeUpdate();
	}

	/**
	 * Every discriminator value a row for this entity may carry in
	 * {@code annotatable_type}: the registered value for its concrete class, those of
	 * any registered supertype (a Scenario row may be typed "Scenario" or "Step"), and
	 * the class names themselves for rows written before the registry existed.
	 */
	private Set<String> annotatableDiscriminators(Class<?> entityClass) {
		Set<String> discriminators = new LinkedHashSet<String>();
		if (annotatableTypeRegistry != null) {
			for (Map.Entry<String, Class<? extends Annotatable>> registered : annotatableTypeRegistry
					.getRegisteredAnnotatableTypes().entrySet()) {
				if (registered.getValue().isAssignableFrom(entityClass)) {
					discriminators.add(registered.getKey());
				}
			}
		}
		for (Class<?> c = entityClass; (c != null) && !Object.class.equals(c); c = c.getSuperclass()) {
			discriminators.add(c.getName());
			discriminators.add(c.getSimpleName());
		}
		return discriminators;
	}

	@Override
	public Note findNote(Object groupingObject, Annotatable annotatable, String message) {
		try {
			// TODO: use named query so it can be configured externally
			Query query = getEntityManager()
					.createQuery(
							"select object(note) from NoteImpl as note "
									+ "where note.text like :message and note.groupingObject = :groupingObject");
			query.setParameter("groupingObject", groupingObject);
			query.setParameter("message", message);
			// query.setParameter("annotatable", annotatable);
			return (Note) query.getSingleResult();
		} catch (NoResultException e) {
			throw NoSuchAnnotationException.forMessage(message);
		} catch (Exception e) {
			throw convertException(e, Note.class, null, EntityExceptionActionType.Reading);
		}
	}

}
