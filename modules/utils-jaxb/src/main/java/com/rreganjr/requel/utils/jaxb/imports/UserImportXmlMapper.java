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
import com.rreganjr.requel.utils.jaxb.imports.UserRoleImportXmlMapper;

public class UserImportXmlMapper {

    private final UserRoleImportXmlMapper roleMapper = new UserRoleImportXmlMapper();

    public UserImportDraft toDraft(UserImportXml xml) {
        if (xml == null) {
            throw new ImportException("user XML payload is required");
        }
        String orgName = xml.getOrganization() != null ? xml.getOrganization().getName() : null;
        boolean editable = xml.getEditable() == null || Boolean.TRUE.equals(xml.getEditable());
        return new UserImportDraft(xml.getId(), xml.getUsername(), xml.getName(), xml.getEmail(), orgName, editable,
                xml.getEncryptedPassword(), xml.getPasswordSalt(), xml.getPasswordAlgorithm(), xml.getPasswordIterations(),
                roleMapper.toDrafts(xml.getUserRoles()));
    }
}
