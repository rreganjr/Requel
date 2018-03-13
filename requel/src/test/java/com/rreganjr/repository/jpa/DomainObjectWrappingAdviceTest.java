/*
 * $Id: $
 * Copyright 2008, 2009 Ron Regan Jr. All Rights Reserved.
 * 
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

package com.rreganjr.repository.jpa;

import net.sf.echopm.EchoPMApp;

import org.junit.Assert;
import org.springframework.aop.aspectj.AspectJExpressionPointcut;
import org.springframework.aop.support.AopUtils;

import com.rreganjr.AbstractIntegrationTestCase;
import com.rreganjr.AbstractSystemInitializer;
import com.rreganjr.SystemInitializer;
import com.rreganjr.command.Command;
import com.rreganjr.nlp.dictionary.impl.command.EditDictionaryWordCommandImpl;
import com.rreganjr.nlp.dictionary.impl.command.ImportDictionaryCommandImpl;
import com.rreganjr.nlp.dictionary.impl.repository.init.DictionaryInitializer;
import com.rreganjr.nlp.dictionary.impl.repository.init.DictionaryPhoneticCodeInitializer;
import com.rreganjr.nlp.dictionary.impl.repository.init.DictionarySQLInitializer;
import com.rreganjr.requel.annotation.impl.command.ResolveIssueCommandImpl;
import com.rreganjr.requel.annotation.impl.command.ResolveIssueWithAddWordToDictionaryPositionCommandImpl;
import com.rreganjr.requel.annotation.impl.command.ResolveIssueWithChangeSpellingPositionCommandImpl;
import com.rreganjr.requel.project.impl.ProjectImpl;
import com.rreganjr.requel.project.impl.assistant.TextEntityAssistant;
import com.rreganjr.requel.project.impl.command.AddGoalToGoalContainerCommandImpl;
import com.rreganjr.requel.project.impl.command.EditGlossaryTermCommandImpl;
import com.rreganjr.requel.project.impl.command.EditGoalCommandImpl;
import com.rreganjr.requel.project.impl.command.EditProjectCommandImpl;
import com.rreganjr.requel.project.impl.command.EditUserStakeholderCommandImpl;
import com.rreganjr.requel.project.impl.command.ExportProjectCommandImpl;
import com.rreganjr.requel.project.impl.command.ImportProjectCommandImpl;
import com.rreganjr.requel.project.impl.command.RemoveGoalFromGoalContainerCommandImpl;
import com.rreganjr.requel.project.impl.command.ResolveIssueWithAddGlossaryTermPositionCommandImpl;
import com.rreganjr.requel.project.impl.repository.init.StakeholderPermissionsInitializer;
import com.rreganjr.requel.ui.annotation.ArgumentEditorPanel;
import com.rreganjr.requel.ui.annotation.IssueEditorPanel;
import com.rreganjr.requel.ui.annotation.PositionEditorPanel;
import com.rreganjr.requel.ui.login.LoginController;
import com.rreganjr.requel.ui.project.GoalEditorPanel;
import com.rreganjr.requel.ui.project.GoalRelationEditorPanel;
import com.rreganjr.requel.ui.project.GoalSelectorPanel;
import com.rreganjr.requel.ui.project.NonUserStakeholderEditorPanel;
import com.rreganjr.requel.ui.project.ProjectOverviewPanel;
import com.rreganjr.requel.ui.project.ProjectUserNavigatorTreeNodeFactory;
import com.rreganjr.requel.ui.project.RemoveGoalFromGoalContainerController;
import com.rreganjr.requel.ui.user.UserAdminNavigatorPanel;
import com.rreganjr.requel.ui.user.UserCollectionNavigatorTreeNodeFactory;
import com.rreganjr.requel.ui.user.UserNavigatorTreeNodeFactory;
import com.rreganjr.requel.user.impl.command.EditUserCommandImpl;
import com.rreganjr.requel.user.impl.command.LoginCommandImpl;
import com.rreganjr.requel.user.impl.repository.init.AdminUserInitializer;
import com.rreganjr.requel.user.impl.repository.init.AssistantUserInitializer;
import com.rreganjr.requel.user.impl.repository.init.DomainUserInitializer;
import com.rreganjr.requel.user.impl.repository.init.ProjectUserInitializer;
import com.rreganjr.requel.user.impl.repository.init.UserRolePermissionsInitializer;

/**
 * @author ron
 */
public class DomainObjectWrappingAdviceTest extends AbstractIntegrationTestCase {

	public void testPointCutA() throws Exception {
		String pointCutExpression = "this(com.rreganjr.requel.utils.repository.EntityInitializer+)";
		AspectJExpressionPointcut ajexp = new AspectJExpressionPointcut(
				DomainObjectWrappingAdvice.class, new String[0], new Class[0]);
		ajexp.setExpression(pointCutExpression);

		Assert.assertFalse(AopUtils.canApply(ajexp, AddGoalToGoalContainerCommandImpl.class, false));
		Assert.assertFalse(AopUtils.canApply(ajexp, TextEntityAssistant.class, false));
		Assert.assertFalse(AopUtils.canApply(ajexp, ProjectImpl.class, false));

		Assert.assertTrue(AopUtils.canApply(ajexp, AdminUserInitializer.class, false));
		Assert.assertTrue(AopUtils.canApply(ajexp, AssistantUserInitializer.class, false));
		Assert.assertTrue(AopUtils.canApply(ajexp, DictionaryInitializer.class, false));
		Assert.assertTrue(AopUtils.canApply(ajexp, DictionaryPhoneticCodeInitializer.class, false));
		Assert.assertTrue(AopUtils.canApply(ajexp, DictionarySQLInitializer.class, false));
		Assert.assertTrue(AopUtils.canApply(ajexp, DomainUserInitializer.class, false));
		Assert.assertTrue(AopUtils.canApply(ajexp, ProjectUserInitializer.class, false));
		Assert.assertTrue(AopUtils.canApply(ajexp, StakeholderPermissionsInitializer.class, false));
		Assert.assertTrue(AopUtils.canApply(ajexp, UserRolePermissionsInitializer.class, false));
		Assert.assertTrue(AopUtils.canApply(ajexp, AbstractSystemInitializer.class, false));
	}

	public void testPointCutB() throws Exception {
		String pointCutExpression = "within(com.rreganjr.requel..*)";
		AspectJExpressionPointcut ajexp = new AspectJExpressionPointcut(
				DomainObjectWrappingAdvice.class, new String[0], new Class[0]);
		ajexp.setExpression(pointCutExpression);

		Assert.assertFalse(AopUtils.canApply(ajexp, java.util.Date.class, false));
		Assert.assertFalse(AopUtils.canApply(ajexp, EchoPMApp.class, false));

		Assert.assertTrue(AopUtils.canApply(ajexp, AddGoalToGoalContainerCommandImpl.class, false));
		Assert.assertTrue(AopUtils.canApply(ajexp, TextEntityAssistant.class, false));
		Assert.assertTrue(AopUtils.canApply(ajexp, ProjectImpl.class, false));

		Assert.assertTrue(AopUtils.canApply(ajexp, AdminUserInitializer.class, false));
		Assert.assertTrue(AopUtils.canApply(ajexp, AssistantUserInitializer.class, false));
		Assert.assertTrue(AopUtils.canApply(ajexp, DictionaryInitializer.class, false));
		Assert.assertTrue(AopUtils.canApply(ajexp, DictionaryPhoneticCodeInitializer.class, false));
		Assert.assertTrue(AopUtils.canApply(ajexp, DictionarySQLInitializer.class, false));
		Assert.assertTrue(AopUtils.canApply(ajexp, DomainUserInitializer.class, false));
		Assert.assertTrue(AopUtils.canApply(ajexp, ProjectUserInitializer.class, false));
		Assert.assertTrue(AopUtils.canApply(ajexp, StakeholderPermissionsInitializer.class, false));
		Assert.assertTrue(AopUtils.canApply(ajexp, UserRolePermissionsInitializer.class, false));
		Assert.assertTrue(AopUtils.canApply(ajexp, AbstractSystemInitializer.class, false));
	}

	/**
	 * Test that the pointcut expression for the DomainObjectWrappingAdvice
	 * class is applicable to UI components, but not in commands.
	 * 
	 * @throws Exception
	 */
	public void testPointCutC() throws Exception {
		String pointCutExpression = "!this(com.rreganjr.requel.utils.repository.EntityInitializer+) || "
				+ "this(com.rreganjr.requel.command.Command+)";

		AspectJExpressionPointcut ajexp = new AspectJExpressionPointcut(
				DomainObjectWrappingAdvice.class, new String[0], new Class[0]);
		ajexp.setExpression(pointCutExpression);

		Assert.assertTrue(AopUtils.canApply(ajexp, AddGoalToGoalContainerCommandImpl.class, false));
		Assert.assertFalse(AopUtils.canApply(ajexp, DictionarySQLInitializer.class, false));
	}

	/**
	 * Test that the pointcut expression for the DomainObjectWrappingAdvice
	 * class is applicable to UI components, but not in commands.
	 * 
	 * @throws Exception
	 */
	public void testPointCutD() throws Exception {
		String pointCutExpression = "this(com.rreganjr.requel.utils.repository.EntityInitializer+) || "
				+ "!this(com.rreganjr.requel.command.Command+)";

		AspectJExpressionPointcut ajexp = new AspectJExpressionPointcut(
				DomainObjectWrappingAdvice.class, new String[0], new Class[0]);
		ajexp.setExpression(pointCutExpression);

		Assert.assertFalse(AopUtils.canApply(ajexp, AddGoalToGoalContainerCommandImpl.class, false));
		Assert.assertFalse(AopUtils.canApply(ajexp, EditGoalCommandImpl.class, false));
		Assert.assertFalse(AopUtils.canApply(ajexp, EditProjectCommandImpl.class, false));
		Assert.assertFalse(AopUtils.canApply(ajexp, ImportProjectCommandImpl.class, false));
		Assert.assertFalse(AopUtils.canApply(ajexp, RemoveGoalFromGoalContainerCommandImpl.class, false));

		Assert.assertFalse(AopUtils.canApply(ajexp, EditGlossaryTermCommandImpl.class, false));
		Assert.assertFalse(AopUtils.canApply(ajexp, EditUserStakeholderCommandImpl.class, false));
		Assert.assertFalse(AopUtils.canApply(ajexp, EditDictionaryWordCommandImpl.class, false));
		Assert.assertFalse(AopUtils.canApply(ajexp, EditUserCommandImpl.class, false));
		Assert.assertFalse(AopUtils.canApply(ajexp, ExportProjectCommandImpl.class, false));
		Assert.assertFalse(AopUtils.canApply(ajexp, ImportDictionaryCommandImpl.class, false));
		Assert.assertFalse(AopUtils.canApply(ajexp, LoginCommandImpl.class, false));
		Assert.assertFalse(AopUtils.canApply(ajexp, ResolveIssueCommandImpl.class, false));
		Assert.assertFalse(AopUtils.canApply(ajexp,
				ResolveIssueWithAddGlossaryTermPositionCommandImpl.class, false));
		Assert.assertFalse(AopUtils.canApply(ajexp,
				ResolveIssueWithAddWordToDictionaryPositionCommandImpl.class, false));
		Assert.assertFalse(AopUtils.canApply(ajexp,
				ResolveIssueWithChangeSpellingPositionCommandImpl.class, false));

		Assert.assertTrue(AopUtils.canApply(ajexp, DictionarySQLInitializer.class, false));
	}

	/**
	 * Test that the pointcut expression for the DomainObjectWrappingAdvice
	 * class is applicable to UI components, but not in commands.
	 * 
	 * @throws Exception
	 */
	public void testPointCutE() throws Exception {
		String pointCutExpression = "within(com.rreganjr.requel..*) && !within(com.rreganjr.requel.command.Command+)";
		AspectJExpressionPointcut ajexp = new AspectJExpressionPointcut(
				DomainObjectWrappingAdvice.class, new String[0], new Class[0]);
		ajexp.setExpression(pointCutExpression);

		Assert.assertFalse(AopUtils.canApply(ajexp, AddGoalToGoalContainerCommandImpl.class, false));
		Assert.assertFalse(AopUtils.canApply(ajexp, EditGoalCommandImpl.class, false));
		Assert.assertFalse(AopUtils.canApply(ajexp, EditProjectCommandImpl.class, false));
		Assert.assertFalse(AopUtils.canApply(ajexp, ImportProjectCommandImpl.class, false));
		Assert.assertFalse(AopUtils.canApply(ajexp, RemoveGoalFromGoalContainerCommandImpl.class, false));

		Assert.assertFalse(AopUtils.canApply(ajexp, EditGlossaryTermCommandImpl.class, false));
		Assert.assertFalse(AopUtils.canApply(ajexp, EditUserStakeholderCommandImpl.class, false));
		Assert.assertFalse(AopUtils.canApply(ajexp, EditDictionaryWordCommandImpl.class, false));
		Assert.assertFalse(AopUtils.canApply(ajexp, EditUserCommandImpl.class, false));
		Assert.assertFalse(AopUtils.canApply(ajexp, ExportProjectCommandImpl.class, false));
		Assert.assertFalse(AopUtils.canApply(ajexp, ImportDictionaryCommandImpl.class, false));
		Assert.assertFalse(AopUtils.canApply(ajexp, LoginCommandImpl.class, false));
		Assert.assertFalse(AopUtils.canApply(ajexp, ResolveIssueCommandImpl.class, false));
		Assert.assertFalse(AopUtils.canApply(ajexp,
				ResolveIssueWithAddGlossaryTermPositionCommandImpl.class, false));
		Assert.assertFalse(AopUtils.canApply(ajexp,
				ResolveIssueWithAddWordToDictionaryPositionCommandImpl.class, false));
		Assert.assertFalse(AopUtils.canApply(ajexp,
				ResolveIssueWithChangeSpellingPositionCommandImpl.class, false));

		Assert.assertTrue(AopUtils.canApply(ajexp, AbstractJpaRepository.class, false));
		Assert.assertTrue(AopUtils.canApply(ajexp, AdminUserInitializer.class, false));
		Assert.assertTrue(AopUtils.canApply(ajexp, TextEntityAssistant.class, false));
		Assert.assertTrue(AopUtils.canApply(ajexp, ProjectImpl.class, false));
		Assert.assertTrue(AopUtils.canApply(ajexp, AssistantUserInitializer.class, false));
		Assert.assertTrue(AopUtils.canApply(ajexp, DictionaryInitializer.class, false));
		Assert.assertTrue(AopUtils.canApply(ajexp, DictionaryPhoneticCodeInitializer.class, false));
		Assert.assertTrue(AopUtils.canApply(ajexp, DictionarySQLInitializer.class, false));
		Assert.assertTrue(AopUtils.canApply(ajexp, DomainUserInitializer.class, false));
		Assert.assertTrue(AopUtils.canApply(ajexp, ProjectUserInitializer.class, false));
		Assert.assertTrue(AopUtils.canApply(ajexp, StakeholderPermissionsInitializer.class, false));
		Assert.assertTrue(AopUtils.canApply(ajexp, UserRolePermissionsInitializer.class, false));
		Assert.assertTrue(AopUtils.canApply(ajexp, AbstractSystemInitializer.class, false));
	}

	/**
	 * Test that the pointcut expression for the DomainObjectWrappingAdvice
	 * class is applicable to UI components, but not in commands.
	 * 
	 * @throws Exception
	 */
	public void testPointCut() throws Exception {
		String pointCutExpression = "within(com.rreganjr.requel..*) && !within("
				+ Command.class.getName() + "+)  && !within(" + SystemInitializer.class.getName()
				+ "+)";
		AspectJExpressionPointcut ajexp = new AspectJExpressionPointcut(
				DomainObjectWrappingAdvice.class, new String[0], new Class[0]);
		ajexp.setExpression(pointCutExpression);

		Assert.assertFalse(AopUtils.canApply(ajexp, AddGoalToGoalContainerCommandImpl.class, false));
		Assert.assertFalse(AopUtils.canApply(ajexp, EditGoalCommandImpl.class, false));
		Assert.assertFalse(AopUtils.canApply(ajexp, EditProjectCommandImpl.class, false));
		Assert.assertFalse(AopUtils.canApply(ajexp, ImportProjectCommandImpl.class, false));
		Assert.assertFalse(AopUtils.canApply(ajexp, RemoveGoalFromGoalContainerCommandImpl.class, false));

		Assert.assertFalse(AopUtils.canApply(ajexp, EditGlossaryTermCommandImpl.class, false));
		Assert.assertFalse(AopUtils.canApply(ajexp, EditUserStakeholderCommandImpl.class, false));
		Assert.assertFalse(AopUtils.canApply(ajexp, EditDictionaryWordCommandImpl.class, false));
		Assert.assertFalse(AopUtils.canApply(ajexp, EditUserCommandImpl.class, false));
		Assert.assertFalse(AopUtils.canApply(ajexp, ExportProjectCommandImpl.class, false));
		Assert.assertFalse(AopUtils.canApply(ajexp, ImportDictionaryCommandImpl.class, false));
		Assert.assertFalse(AopUtils.canApply(ajexp, LoginCommandImpl.class, false));
		Assert.assertFalse(AopUtils.canApply(ajexp, ResolveIssueCommandImpl.class, false));
		Assert.assertFalse(AopUtils.canApply(ajexp,
				ResolveIssueWithAddGlossaryTermPositionCommandImpl.class, false));
		Assert.assertFalse(AopUtils.canApply(ajexp,
				ResolveIssueWithAddWordToDictionaryPositionCommandImpl.class, false));
		Assert.assertFalse(AopUtils.canApply(ajexp,
				ResolveIssueWithChangeSpellingPositionCommandImpl.class, false));

		Assert.assertFalse(AopUtils.canApply(ajexp, AdminUserInitializer.class, false));
		Assert.assertFalse(AopUtils.canApply(ajexp, AssistantUserInitializer.class, false));
		Assert.assertFalse(AopUtils.canApply(ajexp, DictionaryInitializer.class, false));
		Assert.assertFalse(AopUtils.canApply(ajexp, DictionaryPhoneticCodeInitializer.class, false));
		Assert.assertFalse(AopUtils.canApply(ajexp, DictionarySQLInitializer.class, false));
		Assert.assertFalse(AopUtils.canApply(ajexp, DomainUserInitializer.class, false));
		Assert.assertFalse(AopUtils.canApply(ajexp, ProjectUserInitializer.class, false));
		Assert.assertFalse(AopUtils.canApply(ajexp, StakeholderPermissionsInitializer.class, false));
		Assert.assertFalse(AopUtils.canApply(ajexp, UserRolePermissionsInitializer.class, false));
		Assert.assertFalse(AopUtils.canApply(ajexp, AbstractSystemInitializer.class, false));

		Assert.assertTrue(AopUtils.canApply(ajexp, AbstractJpaRepository.class, false));
		Assert.assertTrue(AopUtils.canApply(ajexp, NonUserStakeholderEditorPanel.class, false));
		Assert.assertTrue(AopUtils.canApply(ajexp, LoginController.class, false));
		// TODO: why does AbstractRequelAnnotationEditorPanel not match?
		// Assert.assertTrue(AopUtils.canApply(ajexp,
		// AbstractRequelAnnotationEditorPanel.class, false));
		Assert.assertTrue(AopUtils.canApply(ajexp, ArgumentEditorPanel.class, false));
		Assert.assertTrue(AopUtils.canApply(ajexp, IssueEditorPanel.class, false));
		Assert.assertTrue(AopUtils.canApply(ajexp, PositionEditorPanel.class, false));
		Assert.assertTrue(AopUtils.canApply(ajexp, UserAdminNavigatorPanel.class, false));
		Assert.assertTrue(AopUtils.canApply(ajexp, UserCollectionNavigatorTreeNodeFactory.class, false));
		Assert.assertTrue(AopUtils.canApply(ajexp, UserNavigatorTreeNodeFactory.class, false));
		Assert.assertTrue(AopUtils.canApply(ajexp, ProjectOverviewPanel.class, false));
		Assert.assertTrue(AopUtils.canApply(ajexp, TextEntityAssistant.class, false));
		Assert.assertTrue(AopUtils.canApply(ajexp, GoalSelectorPanel.class, false));
		Assert.assertTrue(AopUtils.canApply(ajexp, GoalRelationEditorPanel.class, false));
		Assert.assertTrue(AopUtils.canApply(ajexp, RemoveGoalFromGoalContainerController.class, false));
		// TODO: why does GoalsTable not match?
		// Assert.assertTrue(AopUtils.canApply(ajexp, GoalsTable.class, false));
		Assert.assertTrue(AopUtils.canApply(ajexp, GoalEditorPanel.class, false));
		Assert.assertTrue(AopUtils.canApply(ajexp, ProjectUserNavigatorTreeNodeFactory.class, false));
	}
}
