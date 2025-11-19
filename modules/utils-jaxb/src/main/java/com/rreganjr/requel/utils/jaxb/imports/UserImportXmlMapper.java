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
