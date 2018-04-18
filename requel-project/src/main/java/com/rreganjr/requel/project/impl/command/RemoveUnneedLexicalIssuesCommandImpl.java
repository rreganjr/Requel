/*
 * $Id$
 * Copyright 2008, 2009 Ron Regan Jr. All Rights Reserved.
 * This file is part of Requel - the Collaborative Requirements
 * Elicitation System.
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
package com.rreganjr.requel.project.impl.command;

import java.util.HashSet;
import java.util.Set;

import com.rreganjr.repository.AbstractRepositoryCommand;
import org.springframework.beans.factory.annotation.Autowired;

import com.rreganjr.command.CommandHandler;
import com.rreganjr.nlp.NLPText;
import com.rreganjr.requel.annotation.Annotation;
import com.rreganjr.requel.annotation.command.AnnotationCommandFactory;
import com.rreganjr.requel.annotation.command.RemoveAnnotationFromAnnotatableCommand;
import com.rreganjr.requel.annotation.impl.LexicalIssue;
import com.rreganjr.requel.project.ProjectOrDomain;
import com.rreganjr.requel.project.ProjectOrDomainEntity;
import com.rreganjr.requel.project.ProjectRepository;
import com.rreganjr.requel.project.command.ProjectCommandFactory;
import com.rreganjr.requel.project.command.RemoveUnneedLexicalIssuesCommand;
import com.rreganjr.requel.project.impl.assistant.AssistantFacade;
import com.rreganjr.requel.user.UserRepository;

/**
 * @author ron
 */
public class RemoveUnneedLexicalIssuesCommandImpl extends AbstractEditProjectCommand implements
		RemoveUnneedLexicalIssuesCommand {

	private ProjectOrDomain projectOrDomain;
	private ProjectOrDomainEntity thingBeingAnalyzed;
	private String annotatableEntityPropertyName;
	private NLPText nlpText;

	/**
	 * @param assistantManager
	 * @param userRepository
	 * @param projectRepository
	 * @param projectCommandFactory
	 * @param annotationCommandFactory
	 * @param commandHandler
	 */
	@Autowired
	public RemoveUnneedLexicalIssuesCommandImpl(AssistantFacade assistantManager,
			UserRepository userRepository, ProjectRepository projectRepository,
			ProjectCommandFactory projectCommandFactory,
			AnnotationCommandFactory annotationCommandFactory, CommandHandler commandHandler) {
		super(assistantManager, userRepository, projectRepository, projectCommandFactory,
				annotationCommandFactory, commandHandler);
	}

	protected ProjectOrDomain getProjectOrDomain() {
		return projectOrDomain;
	}

	public void setProjectOrDomain(ProjectOrDomain projectOrDomain) {
		this.projectOrDomain = projectOrDomain;
	}

	public ProjectOrDomainEntity getThingBeingAnalyzed() {
		return thingBeingAnalyzed;
	}

	public void setThingBeingAnalyzed(ProjectOrDomainEntity thingBeingAnalyzed) {
		this.thingBeingAnalyzed = thingBeingAnalyzed;
	}

	protected String getAnnotatableEntityPropertyName() {
		return annotatableEntityPropertyName;
	}

	public void setAnnotatableEntityPropertyName(String annotatableEntityPropertyName) {
		this.annotatableEntityPropertyName = annotatableEntityPropertyName;
	}

	protected NLPText getNlpText() {
		return nlpText;
	}

	public void setNlpText(NLPText nlpText) {
		this.nlpText = nlpText;
	}

	/**
	 * @see com.rreganjr.command.Command#execute()
	 */
	@Override
	public void execute() throws Exception {
		ProjectOrDomainEntity thingBeingAnalyzed = getRepository().get(getThingBeingAnalyzed());

		Set<Annotation> annotations = new HashSet<Annotation>(thingBeingAnalyzed.getAnnotations());
		for (Annotation existingAnnotation : annotations) {
			if (existingAnnotation instanceof LexicalIssue) {
				LexicalIssue lexicalIssue = (LexicalIssue) existingAnnotation;
				if (!lexicalIssue.isResolved()) {
					String textInQuestion = lexicalIssue.getWord();
					if ((textInQuestion != null) && (textInQuestion.length() > 0)
							&& isIssueForProperty(lexicalIssue, getAnnotatableEntityPropertyName())) {
						boolean matchFound = false;
						for (NLPText nlpFragment : getNlpText().getLeaves()) {
							while ((nlpFragment != null) && !nlpFragment.equals(getNlpText())) {
								String nlpFragmentText = nlpFragment.getText();
								if (nlpFragmentText != null) {
									AbstractRepositoryCommand.log.info("comparing '" + nlpFragmentText + "' to '"
											+ textInQuestion + "'");
									// TODO: split out whitespace and compare
									// word by word
									if (nlpFragment.getText().equalsIgnoreCase(textInQuestion)) {
										matchFound = true;
										break;
									}
								}
								nlpFragment = nlpFragment.getParent();
							}
							if (matchFound) {
								break;
							}
						}
						if (!matchFound) {
							try {
								AbstractRepositoryCommand.log.info("found lexical issue that is no longer relevant: "
										+ lexicalIssue);
								RemoveAnnotationFromAnnotatableCommand command = getAnnotationCommandFactory()
										.newRemoveAnnotationFromAnnotatableCommand();
								command.setAnnotatable(thingBeingAnalyzed);
								command.setAnnotation(lexicalIssue);
								command = getCommandHandler().execute(command);
							} catch (Exception e) {
								AbstractRepositoryCommand.log.error("Unable to remove " + lexicalIssue + " from "
										+ thingBeingAnalyzed, e);
							}
						}
					}
				}
			}
		}
	}

	private boolean isIssueForProperty(LexicalIssue issue, String propertyName) {
		String issuePropertyName = issue.getAnnotatableEntityPropertyName();
		if ((issuePropertyName == null) && (propertyName == null)) {
			return true;
		} else if ((issuePropertyName != null) && issuePropertyName.equals(propertyName)) {
			return true;
		}
		return false;
	}
}
