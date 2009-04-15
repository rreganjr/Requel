/*
 * $Id: RemoveUnneedLexicalIssuesCommandImpl.java,v 1.2 2009/03/30 11:54:30 rregan Exp $
 * Copyright (c) 2009 Ron Regan Jr. All Rights Reserved.
 */

package edu.harvard.fas.rregan.requel.project.impl.command;

import java.util.HashSet;
import java.util.Set;

import org.springframework.beans.factory.annotation.Autowired;

import edu.harvard.fas.rregan.command.CommandHandler;
import edu.harvard.fas.rregan.nlp.NLPText;
import edu.harvard.fas.rregan.requel.annotation.Annotation;
import edu.harvard.fas.rregan.requel.annotation.command.AnnotationCommandFactory;
import edu.harvard.fas.rregan.requel.annotation.command.RemoveAnnotationFromAnnotatableCommand;
import edu.harvard.fas.rregan.requel.annotation.impl.LexicalIssue;
import edu.harvard.fas.rregan.requel.project.ProjectOrDomain;
import edu.harvard.fas.rregan.requel.project.ProjectOrDomainEntity;
import edu.harvard.fas.rregan.requel.project.ProjectRepository;
import edu.harvard.fas.rregan.requel.project.command.ProjectCommandFactory;
import edu.harvard.fas.rregan.requel.project.command.RemoveUnneedLexicalIssuesCommand;
import edu.harvard.fas.rregan.requel.project.impl.assistant.AssistantFacade;
import edu.harvard.fas.rregan.requel.user.UserRepository;

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
	 * @see edu.harvard.fas.rregan.command.Command#execute()
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
									log.info("comparing '" + nlpFragmentText + "' to '"
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
								log.info("found lexical issue that is no longer relevant: "
										+ lexicalIssue);
								RemoveAnnotationFromAnnotatableCommand command = getAnnotationCommandFactory()
										.newRemoveAnnotationFromAnnotatableCommand();
								command.setAnnotatable(thingBeingAnalyzed);
								command.setAnnotation(lexicalIssue);
								command = getCommandHandler().execute(command);
							} catch (Exception e) {
								log.error("Unable to remove " + lexicalIssue + " from "
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
