/*
 * $Id: $
 *
 * Copyright 2025 Ron Regan Jr. All Rights Reserved.
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
package com.rreganjr.requel.utils.jaxb.imports;

import com.rreganjr.requel.imports.ImportException;
import com.rreganjr.requel.imports.identity.UserImportDraft;
import com.rreganjr.requel.imports.project.NonUserStakeholderImportDraft;
import com.rreganjr.requel.imports.project.StakeholderImportDraft;

public class StakeholderImportXmlMapper {

    private final UserImportXmlMapper userMapper = new UserImportXmlMapper();
    private final NonUserStakeholderImportXmlMapper nonUserMapper = new NonUserStakeholderImportXmlMapper();

    public StakeholderImportDraft toDraft(StakeholderImportXml xml) {
        if (xml == null || xml.getUser() == null) {
            throw new ImportException("stakeholder XML payload is required");
        }
        // ensure user draft is registered separately by caller
        return new StakeholderImportDraft(xml.getId(), xml.getCreatedBy(), xml.getUser().getId(),
                new java.util.HashSet<>(xml.getAnnotationRefs()));
    }

    public StakeholderImportDraft toDraft(NonUserStakeholderImportXml xml) {
        NonUserStakeholderImportDraft nonuser = nonUserMapper.toDraft(xml);
        return new StakeholderImportDraft(nonuser.getExternalId(), nonuser.getCreatedByExternalId(),
                nonuser.getName(), nonuser.getText(), nonuser.getAnnotationExternalIds());
    }

    public UserImportDraft toUserDraft(StakeholderImportXml xml) {
        if (xml == null || xml.getUser() == null) {
            throw new ImportException("stakeholder user payload is required");
        }
        return userMapper.toDraft(xml.getUser());
    }
}
