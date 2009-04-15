/*
 * $Id: JpaAnnotationRepository.java,v 1.17 2009/01/09 09:56:17 rregan Exp $
 * Copyright (c) 2008 Ron Regan Jr. All Rights Reserved.
 */
package edu.harvard.fas.rregan.requel.annotation.impl;

import javax.persistence.NoResultException;
import javax.persistence.OptimisticLockException;
import javax.persistence.Query;

import org.hibernate.PropertyValueException;
import org.hibernate.StaleObjectStateException;
import org.hibernate.exception.LockAcquisitionException;
import org.hibernate.validator.InvalidStateException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.dao.CannotAcquireLockException;
import org.springframework.orm.hibernate3.HibernateOptimisticLockingFailureException;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import edu.harvard.fas.rregan.repository.EntityException;
import edu.harvard.fas.rregan.repository.EntityExceptionActionType;
import edu.harvard.fas.rregan.repository.jpa.AbstractJpaRepository;
import edu.harvard.fas.rregan.repository.jpa.ExceptionMapper;
import edu.harvard.fas.rregan.repository.jpa.GenericPropertyValueExceptionAdapter;
import edu.harvard.fas.rregan.repository.jpa.InvalidStateExceptionAdapter;
import edu.harvard.fas.rregan.repository.jpa.OptimisticLockExceptionAdapter;
import edu.harvard.fas.rregan.requel.annotation.Annotatable;
import edu.harvard.fas.rregan.requel.annotation.AnnotationExistsException;
import edu.harvard.fas.rregan.requel.annotation.AnnotationRepository;
import edu.harvard.fas.rregan.requel.annotation.Argument;
import edu.harvard.fas.rregan.requel.annotation.Issue;
import edu.harvard.fas.rregan.requel.annotation.NoSuchAnnotationException;
import edu.harvard.fas.rregan.requel.annotation.NoSuchPositionException;
import edu.harvard.fas.rregan.requel.annotation.Note;
import edu.harvard.fas.rregan.requel.annotation.Position;

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

		addExceptionAdapter(HibernateOptimisticLockingFailureException.class,
				new OptimisticLockExceptionAdapter(), Position.class, Issue.class, Note.class,
				Argument.class);
	}

	@Override
	public Position findPosition(Object groupingObject, String text) throws NoSuchPositionException {
		try {
			// TODO: use named query so it can be configured externally
			Query query = getEntityManager().createQuery(
					"select object(position) from PositionImpl as position "
							+ "inner join position.issues issue where position.text like :text "
							+ "and issue.groupingObject = :groupingObject");
			query.setParameter("groupingObject", groupingObject);
			query.setParameter("text", text);
			return (Position) query.getSingleResult();
		} catch (NoResultException e) {
			throw NoSuchPositionException.forText(text);
		} catch (Exception e) {
			throw convertException(e, Position.class, null, EntityExceptionActionType.Reading);
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
	 * @see edu.harvard.fas.rregan.requel.annotation.AnnotationRepository#findChangeSpellingPosition(edu.harvard.fas.rregan.requel.annotation.Issue,
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

	@Override
	public Issue findIssue(Object groupingObject, Annotatable annotatable, String message) {
		try {
			// TODO: use named query so it can be configured externally
			Query query = getEntityManager().createQuery(
					"select object(issue) from IssueImpl as issue "
							+ "where issue.text like :message "
							+ "and issue.groupingObject = :groupingObject");
			query.setParameter("groupingObject", groupingObject);
			query.setParameter("message", message);
			return (Issue) query.getSingleResult();
		} catch (NoResultException e) {
			throw NoSuchAnnotationException.forMessage(message);
		} catch (Exception e) {
			throw convertException(e, Issue.class, null, EntityExceptionActionType.Reading);
		}
	}

	@Override
	public LexicalIssue findLexicalIssue(Object groupingObject, Annotatable annotatable, String word)
			throws NoSuchAnnotationException {
		try {
			// TODO: use named query so it can be configured externally
			Query query = getEntityManager().createQuery(
					"select object(issue) from LexicalIssue as issue "
							+ "where issue.word like :word "
							+ "and issue.groupingObject = :groupingObject "
							+ "and issue.annotatableEntityPropertyName is null");
			query.setParameter("word", word);
			query.setParameter("groupingObject", groupingObject);
			return (LexicalIssue) query.getSingleResult();
		} catch (NoResultException e) {
			throw NoSuchAnnotationException.forWord(word, null);
		} catch (Exception e) {
			throw convertException(e, Issue.class, null, EntityExceptionActionType.Reading);
		}
	}

	@Override
	public LexicalIssue findLexicalIssue(Object groupingObject, Annotatable annotatable,
			String word, String annotatableEntityPropertyName) throws NoSuchAnnotationException {
		try {
			// TODO: use named query so it can be configured externally
			Query query = getEntityManager()
					.createQuery(
							"select object(issue) from LexicalIssue as issue "
									+ "where issue.word like :word "
									+ "and issue.groupingObject = :groupingObject "
									+ "and issue.annotatableEntityPropertyName like :annotatableEntityPropertyName");
			query.setParameter("word", word);
			query.setParameter("groupingObject", groupingObject);
			query.setParameter("annotatableEntityPropertyName", annotatableEntityPropertyName);
			return (LexicalIssue) query.getSingleResult();
		} catch (NoResultException e) {
			throw NoSuchAnnotationException.forWord(word, annotatableEntityPropertyName);
		} catch (Exception e) {
			throw convertException(e, Issue.class, null, EntityExceptionActionType.Reading);
		}
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
