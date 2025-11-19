package com.rreganjr.requel.utils.jaxb.imports;

import com.rreganjr.requel.imports.ImportException;
import com.rreganjr.requel.imports.identity.UserImportDraft;
import com.rreganjr.requel.imports.project.StakeholderImportDraft;

public class StakeholderImportXmlMapper {

    private final UserImportXmlMapper userMapper = new UserImportXmlMapper();

    public StakeholderImportDraft toDraft(StakeholderImportXml xml) {
        if (xml == null || xml.getUser() == null) {
            throw new ImportException("stakeholder XML payload is required");
        }
        // ensure user draft is registered separately by caller
        return new StakeholderImportDraft(xml.getId(), xml.getCreatedBy(), xml.getUser().getId());
    }

    public UserImportDraft toUserDraft(StakeholderImportXml xml) {
        if (xml == null || xml.getUser() == null) {
            throw new ImportException("stakeholder user payload is required");
        }
        return userMapper.toDraft(xml.getUser());
    }
}
