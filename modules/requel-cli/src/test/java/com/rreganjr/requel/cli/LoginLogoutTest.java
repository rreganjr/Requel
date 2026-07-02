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
package com.rreganjr.requel.cli;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class LoginLogoutTest {

    private static RequelCli cli(Path dir) {
        RequelCli parent = new RequelCli();
        parent.url = "http://localhost:8080";
        parent.credentialStore = new CredentialStore(dir);
        return parent;
    }

    @Test
    void loginStoresTokenForUrl(@TempDir Path dir) {
        RequelCli parent = cli(dir);
        LoginCommand login = new LoginCommand();
        login.parent = parent;
        login.token = "reqpat_ABC";

        assertThat(login.call()).isEqualTo(ExitCode.SUCCESS);
        assertThat(parent.credentialStore.find("http://localhost:8080")).isEqualTo("reqpat_ABC");
    }

    @Test
    void logoutClearsStoredToken(@TempDir Path dir) {
        RequelCli parent = cli(dir);
        parent.credentialStore.save("http://localhost:8080", "reqpat_ABC");

        LogoutCommand logout = new LogoutCommand();
        logout.parent = parent;

        assertThat(logout.call()).isEqualTo(ExitCode.SUCCESS);
        assertThat(parent.credentialStore.find("http://localhost:8080")).isNull();
    }

    @Test
    void tokenSourcePrefersFlagOverStored(@TempDir Path dir) {
        RequelCli parent = cli(dir);
        parent.credentialStore.save("http://localhost:8080", "reqpat_STORED");
        parent.token = "reqpat_FLAG";

        assertThat(parent.tokenSource().currentToken()).isEqualTo("reqpat_FLAG");
    }

    @Test
    void tokenSourceFallsBackToStoredWhenNoFlagOrEnv(@TempDir Path dir) {
        RequelCli parent = cli(dir);
        parent.credentialStore.save("http://localhost:8080", "reqpat_STORED");
        parent.token = null; // no --token and no REQUEL_TOKEN

        assertThat(parent.tokenSource().currentToken()).isEqualTo("reqpat_STORED");
    }

    @Test
    void tokenSourceReturnsNullWhenNothingConfigured(@TempDir Path dir) {
        RequelCli parent = cli(dir);
        parent.token = null;

        assertThat(parent.tokenSource().currentToken()).isNull();
    }
}
