@jakarta.xml.bind.annotation.adapters.XmlJavaTypeAdapters({
    @jakarta.xml.bind.annotation.adapters.XmlJavaTypeAdapter(value = com.rreganjr.requel.utils.jaxb.DateAdapter.class, type = java.util.Date.class),
    @jakarta.xml.bind.annotation.adapters.XmlJavaTypeAdapter(value = com.rreganjr.requel.user.impl.User2UserImplAdapter.class, type = com.rreganjr.platform.identity.User.class)
})
package com.rreganjr.requel.annotation.impl;
