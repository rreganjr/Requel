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

import java.util.concurrent.Callable;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.CommandLine.ParentCommand;

/**
 * Stores a personal access token (PAT, #73) for the current {@code --url}, so later commands
 * authenticate without re-passing {@code --token}. Interactive OAuth login is a later milestone;
 * this milestone covers the headless PAT path.
 */
@Command(name = "login", description = "Store a bearer token (PAT) for the server URL.")
public class LoginCommand implements Callable<Integer> {

    @ParentCommand
    RequelCli parent;

    @Option(names = "--token", paramLabel = "TOKEN", required = true,
            description = "A Requel personal access token (reqpat_...).")
    String token;

    @Override
    public Integer call() {
        parent.credentialStore.save(parent.url, token);
        System.out.println("Saved credentials for " + parent.url + " ("
                + parent.credentialStore.file() + ").");
        return ExitCode.SUCCESS;
    }
}
