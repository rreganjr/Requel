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
package com.rreganjr.requel.service.auth;

import org.springframework.security.core.AuthenticationException;

/**
 * The OAuth sign-in form refused an attempt because of the sign-in limits (issue #448). The
 * form's failure handler sends the browser back to the login page with {@code error=throttled}.
 */
public class LoginThrottledException extends AuthenticationException {

    private static final long serialVersionUID = 1L;

    private final transient LoginThrottle.Refusal refusal;

    public LoginThrottledException(LoginThrottle.Refusal refusal) {
        super(refusal.message());
        this.refusal = refusal;
    }

    public LoginThrottle.Refusal getRefusal() {
        return refusal;
    }
}
