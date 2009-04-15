/*
 * $Id: UserSet.java,v 1.1 2008/03/27 09:26:03 rregan Exp $
 * Copyright (c) 2008 Ron Regan Jr. All Rights Reserved.
 */

package edu.harvard.fas.rregan.requel.user;

import java.util.Set;

/**
 * A set of users that recognizes a user even if the username changes.
 * 
 * @author ron
 */
public interface UserSet extends Set<User> {

}