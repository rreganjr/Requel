/*
 * $Id: $
 * Copyright 2008, 2009 Ron Regan Jr. All Rights Reserved.
 * 
 * This file is part of Requel - the Collaborative Requirments
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

package edu.harvard.fas.rregan.repository.jpa;

import net.sf.echopm.EchoPMApp;

import org.springframework.aop.aspectj.AspectJExpressionPointcut;
import org.springframework.aop.support.AopUtils;

import edu.harvard.fas.rregan.AbstractIntegrationTestCase;
import edu.harvard.fas.rregan.AbstractSystemInitializer;
import edu.harvard.fas.rregan.SystemInitializer;
import edu.harvard.fas.rregan.command.Command;
import edu.harvard.fas.rregan.nlp.dictionary.impl.command.EditDictionaryWordCommandImpl;
import edu.harvard.fas.rregan.nlp.dictionary.impl.command.ImportDictionaryCommandImpl;
import edu.harvard.fas.rregan.nlp.dictionary.impl.repository.init.DictionaryInitializer;
import edu.harvard.fas.rregan.nlp.dictionary.impl.repository.init.DictionaryPhoneticCodeInitializer;
import edu.harvard.fas.rregan.nlp.dictionary.impl.repository.init.DictionarySQLInitializer;
import edu.harvard.fas.rregan.requel.annotation.impl.command.ResolveIssueCommandImpl;
import edu.harvard.fas.rregan.requel.annotation.impl.command.ResolveIssueWithAddWordToDictionaryPositionCommandImpl;
import edu.harvard.fas.rregan.requel.annotation.impl.command.ResolveIssueWithChangeSpellingPositionCommandImpl;
import edu.harvard.fas.rregan.requel.project.impl.ProjectImpl;
import edu.harvard.fas.rregan.requel.project.impl.assistant.TextEntityAssistant;
import edu.harvard.fas.rregan.requel.project.impl.command.AddGoalToGoalContainerCommandImpl;
import edu.harvard.fas.rregan.requel.project.impl.command.EditGlossaryTermCommandImpl;
import edu.harvard.fas.rregan.requel.project.impl.command.EditGoalCommandImpl;
import edu.harvard.fas.rregan.requel.project.impl.command.EditProjectCommandImpl;
import edu.harvard.fas.rregan.requel.project.impl.command.EditUserStakeholderCommandImpl;
import edu.harvard.fas.rregan.requel.project.impl.command.ExportProjectCommandImpl;
import edu.harvard.fas.rregan.requel.project.impl.command.ImportProjectCommandImpl;
import edu.harvard.fas.rregan.requel.project.impl.command.RemoveGoalFromGoalContainerCommandImpl;
import edu.harvard.fas.rregan.requel.project.impl.command.ResolveIssueWithAddGlossaryTermPositionCommandImpl;
import edu.harvard.fas.rregan.requel.project.impl.repository.init.StakeholderPermissionsInitializer;
import edu.harvard.fas.rregan.requel.ui.annotation.ArgumentEditorPanel;
import edu.harvard.fas.rregan.requel.ui.annotation.IssueEditorPanel;
import edu.harvard.fas.rregan.requel.ui.annotation.PositionEditorPanel;
import edu.harvard.fas.rregan.requel.ui.login.LoginController;
import edu.harvard.fas.rregan.requel.ui.project.GoalEditorPanel;
import edu.harvard.fas.rregan.requel.ui.project.GoalRelationEditorPanel;
import edu.harvard.fas.rregan.requel.ui.project.GoalSelectorPanel;
import edu.harvard.fas.rregan.requel.ui.project.NonUserStakeholderEditorPanel;
import edu.harvard.fas.rregan.requel.ui.project.ProjectOverviewPanel;
import edu.harvard.fas.rregan.requel.ui.project.ProjectUserNavigatorTreeNodeFactory;
import edu.harvard.fas.rregan.requel.ui.project.RemoveGoalFromGoalContainerController;
import edu.harvard.fas.rregan.requel.ui.user.UserAdminNavigatorPanel;
import edu.harvard.fas.rregan.requel.ui.user.UserCollectionNavigatorTreeNodeFactory;
import edu.harvard.fas.rregan.requel.ui.user.UserNavigatorTreeNodeFactory;
import edu.harvard.fas.rregan.requel.user.impl.command.EditUserCommandImpl;
import edu.harvard.fas.rregan.requel.user.impl.command.LoginCommandImpl;
import edu.harvard.fas.rregan.requel.user.impl.repository.init.AdminUserInitializer;
import edu.harvard.fas.rregan.requel.user.impl.repository.init.AssistantUserInitializer;
import edu.harvard.fas.rregan.requel.user.impl.repository.init.DomainUserInitializer;
import edu.harvard.fas.rregan.requel.user.impl.repository.init.ProjectUserInitializer;
import edu.harvard.fas.rregan.requel.user.impl.repository.init.UserRolePermissionsInitializer;

/**
 * @author ron
 */
public class DomainObjectWrappingAdviceTest extends AbstractIntegrationTestCase {

	public void testPointCutA() throws Exception {
		String pointCutExpression = "this(edu.harvard.fas.rregan.requel.utils.repository.EntityInitializer+)";
		AspectJExpressionPointcut ajexp = new AspectJExpressionPointcut(
				DomainObjectWrappingAdvice.class, new String[0], new Class[0]);
		ajexp.setExpression(pointCutExpression);

		assertFalse(AopUtils.canApply(ajexp, AddGoalToGoalContainerCommandImpl.class, false));
		assertFalse(AopUtils.canApply(ajexp, TextEntityAssistant.class, false));
		assertFalse(AopUtils.canApply(ajexp, ProjectImpl.class, false));

		assertTrue(AopUtils.canApply(ajexp, AdminUserInitializer.class, false));
		assertTrue(AopUtils.canApply(ajexp, AssistantUserInitializer.class, false));
		assertTrue(AopUtils.canApply(ajexp, DictionaryInitializer.class, false));
		assertTrue(AopUtils.canApply(ajexp, DictionaryPhoneticCodeInitializer.class, false));
		assertTrue(AopUtils.canApply(ajexp, DictionarySQLInitializer.class, false));
		assertTrue(AopUtils.canApply(ajexp, DomainUserInitializer.class, false));
		assertTrue(AopUtils.canApply(ajexp, ProjectUserInitializer.class, false));
		assertTrue(AopUtils.canApply(ajexp, StakeholderPermissionsInitializer.class, false));
		assertTrue(AopUtils.canApply(ajexp, UserRolePermissionsInitializer.class, false));
		assertTrue(AopUtils.canApply(ajexp, AbstractSystemInitializer.class, false));
	}

	public void testPointCutB() throws Exception {
		String pointCutExpression = "within(edu.harvard.fas.rregan.requel..*)";
		AspectJExpressionPointcut ajexp = new AspectJExpressionPointcut(
				DomainObjectWrappingAdvice.class, new String[0], new Class[0]);
		ajexp.setExpression(pointCutExpression);

		assertFalse(AopUtils.canApply(ajexp, java.util.Date.class, false));
		assertFalse(AopUtils.canApply(ajexp, EchoPMApp.class, false));

		assertTrue(AopUtils.canApply(ajexp, AddGoalToGoalContainerCommandImpl.class, false));
		assertTrue(AopUtils.canApply(ajexp, TextEntityAssistant.class, false));
		assertTrue(AopUtils.canApply(ajexp, ProjectImpl.class, false));

		assertTrue(AopUtils.canApply(ajexp, AdminUserInitializer.class, false));
		assertTrue(AopUtils.canApply(ajexp, AssistantUserInitializer.class, false));
		assertTrue(AopUtils.canApply(ajexp, DictionaryInitializer.class, false));
		assertTrue(AopUtils.canApply(ajexp, DictionaryPhoneticCodeInitializer.class, false));
		assertTrue(AopUtils.canApply(ajexp, DictionarySQLInitializer.class, false));
		assertTrue(AopUtils.canApply(ajexp, DomainUserInitializer.class, false));
		assertTrue(AopUtils.canApply(ajexp, ProjectUserInitializer.class, false));
		assertTrue(AopUtils.canApply(ajexp, StakeholderPermissionsInitializer.class, false));
		assertTrue(AopUtils.canApply(ajexp, UserRolePermissionsInitializer.class, false));
		assertTrue(AopUtils.canApply(ajexp, AbstractSystemInitializer.class, false));
	}

	/**
	 * Test that the pointcut expression for the DomainObjectWrappingAdvice
	 * class is applicable to UI components, but not in commands.
	 * 
	 * @throws Exception
	 */
	public void testPointCutC() throws Exception {
		String pointCutExpression = "!this(edu.harvard.fas.rregan.requel.utils.repository.EntityInitializer+) || "
				+ "this(edu.harvard.fas.rregan.requel.command.Command+)";

		AspectJExpressionPointcut ajexp = new AspectJExpressionPointcut(
				DomainObjectWrappingAdvice.class, new String[0], new Class[0]);
		ajexp.setExpression(pointCutExpression);

		assertTrue(AopUtils.canApply(ajexp, AddGoalToGoalContainerCommandImpl.class, false));
		assertFalse(AopUtils.canApply(ajexp, DictionarySQLInitializer.class, false));
	}

	/**
	 * Test that the pointcut expression for the DomainObjectWrappingAdvice
	 * class is applicable to UI components, but not in commands.
	 * 
	 * @throws Exception
	 */
	public void testPointCutD() throws Exception {
		String pointCutExpression = "this(edu.harvard.fas.rregan.requel.utils.repository.EntityInitializer+) || "
				+ "!this(edu.harvard.fas.rregan.requel.command.Command+)";

		AspectJExpressionPointcut ajexp = new AspectJExpressionPointcut(
				DomainObjectWrappingAdvice.class, new String[0], new Class[0]);
		ajexp.setExpression(pointCutExpression);

		assertFalse(AopUtils.canApply(ajexp, AddGoalToGoalContainerCommandImpl.class, false));
		assertFalse(AopUtils.canApply(ajexp, EditGoalCommandImpl.class, false));
		assertFalse(AopUtils.canApply(ajexp, EditProjectCommandImpl.class, false));
		assertFalse(AopUtils.canApply(ajexp, ImportProjectCommandImpl.class, false));
		assertFalse(AopUtils.canApply(ajexp, RemoveGoalFromGoalContainerCommandImpl.class, false));

		assertFalse(AopUtils.canApply(ajexp, EditGlossaryTermCommandImpl.class, false));
		assertFalse(AopUtils.canApply(ajexp, EditUserStakeholderCommandImpl.class, false));
		assertFalse(AopUtils.canApply(ajexp, EditDictionaryWordCommandImpl.class, false));
		assertFalse(AopUtils.canApply(ajexp, EditUserCommandImpl.class, false));
		assertFalse(AopUtils.canApply(ajexp, ExportProjectCommandImpl.class, false));
		assertFalse(AopUtils.canApply(ajexp, ImportDictionaryCommandImpl.class, false));
		assertFalse(AopUtils.canApply(ajexp, LoginCommandImpl.class, false));
		assertFalse(AopUtils.canApply(ajexp, ResolveIssueCommandImpl.class, false));
		assertFalse(AopUtils.canApply(ajexp,
				ResolveIssueWithAddGlossaryTermPositionCommandImpl.class, false));
		assertFalse(AopUtils.canApply(ajexp,
				ResolveIssueWithAddWordToDictionaryPositionCommandImpl.class, false));
		assertFalse(AopUtils.canApply(ajexp,
				ResolveIssueWithChangeSpellingPositionCommandImpl.class, false));

		assertTrue(AopUtils.canApply(ajexp, DictionarySQLInitializer.class, false));
	}

	/**
	 * Test that the pointcut expression for the DomainObjectWrappingAdvice
	 * class is applicable to UI components, but not in commands.
	 * 
	 * @throws Exception
	 */
	public void testPointCutE() throws Exception {
		String pointCutExpression = "within(edu.harvard.fas.rregan.requel..*) && !within(edu.harvard.fas.rregan.requel.command.Command+)";
		AspectJExpressionPointcut ajexp = new AspectJExpressionPointcut(
				DomainObjectWrappingAdvice.class, new String[0], new Class[0]);
		ajexp.setExpression(pointCutExpression);

		assertFalse(AopUtils.canApply(ajexp, AddGoalToGoalContainerCommandImpl.class, false));
		assertFalse(AopUtils.canApply(ajexp, EditGoalCommandImpl.class, false));
		assertFalse(AopUtils.canApply(ajexp, EditProjectCommandImpl.class, false));
		assertFalse(AopUtils.canApply(ajexp, ImportProjectCommandImpl.class, false));
		assertFalse(AopUtils.canApply(ajexp, RemoveGoalFromGoalContainerCommandImpl.class, false));

		assertFalse(AopUtils.canApply(ajexp, EditGlossaryTermCommandImpl.class, false));
		assertFalse(AopUtils.canApply(ajexp, EditUserStakeholderCommandImpl.class, false));
		assertFalse(AopUtils.canApply(ajexp, EditDictionaryWordCommandImpl.class, false));
		assertFalse(AopUtils.canApply(ajexp, EditUserCommandImpl.class, false));
		assertFalse(AopUtils.canApply(ajexp, ExportProjectCommandImpl.class, false));
		assertFalse(AopUtils.canApply(ajexp, ImportDictionaryCommandImpl.class, false));
		assertFalse(AopUtils.canApply(ajexp, LoginCommandImpl.class, false));
		assertFalse(AopUtils.canApply(ajexp, ResolveIssueCommandImpl.class, false));
		assertFalse(AopUtils.canApply(ajexp,
				ResolveIssueWithAddGlossaryTermPositionCommandImpl.class, false));
		assertFalse(AopUtils.canApply(ajexp,
				ResolveIssueWithAddWordToDictionaryPositionCommandImpl.class, false));
		assertFalse(AopUtils.canApply(ajexp,
				ResolveIssueWithChangeSpellingPositionCommandImpl.class, false));

		assertTrue(AopUtils.canApply(ajexp, AbstractJpaRepository.class, false));
		assertTrue(AopUtils.canApply(ajexp, AdminUserInitializer.class, false));
		assertTrue(AopUtils.canApply(ajexp, TextEntityAssistant.class, false));
		assertTrue(AopUtils.canApply(ajexp, ProjectImpl.class, false));
		assertTrue(AopUtils.canApply(ajexp, AssistantUserInitializer.class, false));
		assertTrue(AopUtils.canApply(ajexp, DictionaryInitializer.class, false));
		assertTrue(AopUtils.canApply(ajexp, DictionaryPhoneticCodeInitializer.class, false));
		assertTrue(AopUtils.canApply(ajexp, DictionarySQLInitializer.class, false));
		assertTrue(AopUtils.canApply(ajexp, DomainUserInitializer.class, false));
		assertTrue(AopUtils.canApply(ajexp, ProjectUserInitializer.class, false));
		assertTrue(AopUtils.canApply(ajexp, StakeholderPermissionsInitializer.class, false));
		assertTrue(AopUtils.canApply(ajexp, UserRolePermissionsInitializer.class, false));
		assertTrue(AopUtils.canApply(ajexp, AbstractSystemInitializer.class, false));
	}

	/**
	 * Test that the pointcut expression for the DomainObjectWrappingAdvice
	 * class is applicable to UI components, but not in commands.
	 * 
	 * @throws Exception
	 */
	public void testPointCut() throws Exception {
		String pointCutExpression = "within(edu.harvard.fas.rregan.requel..*) && !within("
				+ Command.class.getName() + "+)  && !within(" + SystemInitializer.class.getName()
				+ "+)";
		AspectJExpressionPointcut ajexp = new AspectJExpressionPointcut(
				DomainObjectWrappingAdvice.class, new String[0], new Class[0]);
		ajexp.setExpression(pointCutExpression);

		assertFalse(AopUtils.canApply(ajexp, AddGoalToGoalContainerCommandImpl.class, false));
		assertFalse(AopUtils.canApply(ajexp, EditGoalCommandImpl.class, false));
		assertFalse(AopUtils.canApply(ajexp, EditProjectCommandImpl.class, false));
		assertFalse(AopUtils.canApply(ajexp, ImportProjectCommandImpl.class, false));
		assertFalse(AopUtils.canApply(ajexp, RemoveGoalFromGoalContainerCommandImpl.class, false));

		assertFalse(AopUtils.canApply(ajexp, EditGlossaryTermCommandImpl.class, false));
		assertFalse(AopUtils.canApply(ajexp, EditUserStakeholderCommandImpl.class, false));
		assertFalse(AopUtils.canApply(ajexp, EditDictionaryWordCommandImpl.class, false));
		assertFalse(AopUtils.canApply(ajexp, EditUserCommandImpl.class, false));
		assertFalse(AopUtils.canApply(ajexp, ExportProjectCommandImpl.class, false));
		assertFalse(AopUtils.canApply(ajexp, ImportDictionaryCommandImpl.class, false));
		assertFalse(AopUtils.canApply(ajexp, LoginCommandImpl.class, false));
		assertFalse(AopUtils.canApply(ajexp, ResolveIssueCommandImpl.class, false));
		assertFalse(AopUtils.canApply(ajexp,
				ResolveIssueWithAddGlossaryTermPositionCommandImpl.class, false));
		assertFalse(AopUtils.canApply(ajexp,
				ResolveIssueWithAddWordToDictionaryPositionCommandImpl.class, false));
		assertFalse(AopUtils.canApply(ajexp,
				ResolveIssueWithChangeSpellingPositionCommandImpl.class, false));

		assertFalse(AopUtils.canApply(ajexp, AdminUserInitializer.class, false));
		assertFalse(AopUtils.canApply(ajexp, AssistantUserInitializer.class, false));
		assertFalse(AopUtils.canApply(ajexp, DictionaryInitializer.class, false));
		assertFalse(AopUtils.canApply(ajexp, DictionaryPhoneticCodeInitializer.class, false));
		assertFalse(AopUtils.canApply(ajexp, DictionarySQLInitializer.class, false));
		assertFalse(AopUtils.canApply(ajexp, DomainUserInitializer.class, false));
		assertFalse(AopUtils.canApply(ajexp, ProjectUserInitializer.class, false));
		assertFalse(AopUtils.canApply(ajexp, StakeholderPermissionsInitializer.class, false));
		assertFalse(AopUtils.canApply(ajexp, UserRolePermissionsInitializer.class, false));
		assertFalse(AopUtils.canApply(ajexp, AbstractSystemInitializer.class, false));

		assertTrue(AopUtils.canApply(ajexp, AbstractJpaRepository.class, false));
		assertTrue(AopUtils.canApply(ajexp, NonUserStakeholderEditorPanel.class, false));
		assertTrue(AopUtils.canApply(ajexp, LoginController.class, false));
		// TODO: why does AbstractRequelAnnotationEditorPanel not match?
		// assertTrue(AopUtils.canApply(ajexp,
		// AbstractRequelAnnotationEditorPanel.class, false));
		assertTrue(AopUtils.canApply(ajexp, ArgumentEditorPanel.class, false));
		assertTrue(AopUtils.canApply(ajexp, IssueEditorPanel.class, false));
		assertTrue(AopUtils.canApply(ajexp, PositionEditorPanel.class, false));
		assertTrue(AopUtils.canApply(ajexp, UserAdminNavigatorPanel.class, false));
		assertTrue(AopUtils.canApply(ajexp, UserCollectionNavigatorTreeNodeFactory.class, false));
		assertTrue(AopUtils.canApply(ajexp, UserNavigatorTreeNodeFactory.class, false));
		assertTrue(AopUtils.canApply(ajexp, ProjectOverviewPanel.class, false));
		assertTrue(AopUtils.canApply(ajexp, TextEntityAssistant.class, false));
		assertTrue(AopUtils.canApply(ajexp, GoalSelectorPanel.class, false));
		assertTrue(AopUtils.canApply(ajexp, GoalRelationEditorPanel.class, false));
		assertTrue(AopUtils.canApply(ajexp, RemoveGoalFromGoalContainerController.class, false));
		// TODO: why does GoalsTable not match?
		// assertTrue(AopUtils.canApply(ajexp, GoalsTable.class, false));
		assertTrue(AopUtils.canApply(ajexp, GoalEditorPanel.class, false));
		assertTrue(AopUtils.canApply(ajexp, ProjectUserNavigatorTreeNodeFactory.class, false));
	}
}
