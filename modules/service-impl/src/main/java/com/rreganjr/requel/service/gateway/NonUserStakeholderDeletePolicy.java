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
package com.rreganjr.requel.service.gateway;

import com.rreganjr.requel.gateway.CommandPolicy;
import com.rreganjr.requel.gateway.PolicyDecision;
import com.rreganjr.requel.project.Project;
import com.rreganjr.requel.project.ProjectRepository;
import com.rreganjr.requel.project.Stakeholder;
import com.rreganjr.requel.service.api.dto.DeleteStakeholderInput;

/**
 * Input-aware {@link CommandPolicy} decorator: lets {@code DeleteStakeholder} through the gateway
 * only for <strong>non-user</strong> stakeholders. Deleting a user stakeholder would remove a
 * person's project membership, which is identity-adjacent and outside the gateway's remit (the
 * gateway never exposes user/identity management); that stays a UI/admin action.
 * <p>
 * The base (type-level) policy is consulted first, so a denylisted or unknown command is rejected
 * before any lookup. The guard only engages once the input has been bound to a
 * {@link DeleteStakeholderInput}; on the pre-binding pass (raw map / null input) it defers to the
 * base decision so the gateway can proceed to bind and re-evaluate.
 */
public final class NonUserStakeholderDeletePolicy implements CommandPolicy {

    static final String DELETE_STAKEHOLDER = "DeleteStakeholder";

    private final CommandPolicy delegate;
    private final ProjectRepository projectRepository;

    public NonUserStakeholderDeletePolicy(CommandPolicy delegate,
            ProjectRepository projectRepository) {
        this.delegate = delegate;
        this.projectRepository = projectRepository;
    }

    @Override
    public PolicyDecision evaluate(String commandType, Object input) {
        PolicyDecision base = delegate.evaluate(commandType, input);
        if (!base.allowed()) {
            return base;
        }
        if (DELETE_STAKEHOLDER.equals(commandType) && input instanceof DeleteStakeholderInput in) {
            Stakeholder stakeholder = resolve(in);
            if (stakeholder != null && stakeholder.isUserStakeholder()) {
                return PolicyDecision.deny("DeleteStakeholder is restricted to non-user stakeholders; "
                        + "stakeholder " + in.stakeholderId() + " is a user stakeholder");
            }
        }
        return base;
    }

    /**
     * Resolve the target stakeholder, or {@code null} if it cannot be found. A missing
     * stakeholder is not the policy's concern — the command applicator will surface the
     * not-found error during execution.
     */
    private Stakeholder resolve(DeleteStakeholderInput in) {
        if (in.projectName() == null || in.stakeholderId() == null) {
            return null;
        }
        try {
            Project project = projectRepository.findProjectByName(in.projectName());
            for (Stakeholder s : project.getStakeholders()) {
                if (in.stakeholderId().equals(s.getId())) {
                    return s;
                }
            }
        } catch (RuntimeException e) {
            // Unknown project / lookup failure: defer to the command, which will surface the
            // real not-found error. The guard only denies when it positively confirms a user
            // stakeholder.
            return null;
        }
        return null;
    }
}
