/*
 * $Id: User2UserImplAdapter.java,v 1.2 2008/06/25 09:58:49 rregan Exp $
 * Copyright (c) 2008 Ron Regan Jr. All Rights Reserved.
 */
package edu.harvard.fas.rregan.requel.user.impl;

import javax.xml.bind.annotation.XmlTransient;
import javax.xml.bind.annotation.adapters.XmlAdapter;

import edu.harvard.fas.rregan.requel.user.User;

/**
 * Adapter for JAXB to convert interface User to class UserImpl and back.
 * 
 * @author ron
 */
@XmlTransient
public class User2UserImplAdapter extends XmlAdapter<UserImpl, User> {

	@Override
	public UserImpl marshal(User user) throws Exception {
		return (UserImpl) user;
	}

	@Override
	public User unmarshal(UserImpl user) throws Exception {
		return user;
	}

}