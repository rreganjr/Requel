/*
 * This file is part of Requel - the Collaborative Requirements
 * Elicitation System.
 *
 * Copyright 2025 Ron Regan Jr. All Rights Reserved.
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
@jakarta.xml.bind.annotation.adapters.XmlJavaTypeAdapters({
    @jakarta.xml.bind.annotation.adapters.XmlJavaTypeAdapter(value = com.rreganjr.requel.utils.jaxb.DateAdapter.class, type = java.util.Date.class),
    @jakarta.xml.bind.annotation.adapters.XmlJavaTypeAdapter(value = com.rreganjr.requel.user.impl.User2UserImplAdapter.class, type = com.rreganjr.platform.identity.User.class)
})
package com.rreganjr.requel.annotation.impl;
