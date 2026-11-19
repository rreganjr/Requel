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

import com.rreganjr.requel.gateway.QueryDescriptions;
import com.rreganjr.requel.gateway.QueryGateway;
import com.rreganjr.requel.service.api.dto.ServerInfoDto;
import picocli.CommandLine.Command;

/**
 * {@code requel server-info} — which build of Requel the server is running (issue #449).
 * {@code requel --version} reports the CLI's own version.
 */
@Command(name = "server-info", description = QueryDescriptions.GET_SERVER_INFO)
public class ServerInfoCommand extends AbstractQueryCommand {

    @Override
    protected Object query(QueryGateway gateway) {
        return gateway.getServerInfo();
    }

    @Override
    protected String renderText(Object result) {
        ServerInfoDto info = (ServerInfoDto) result;
        StringBuilder sb = new StringBuilder("version: ").append(info.version());
        if (info.commit() != null) {
            sb.append("\ncommit:  ").append(info.commit());
            if (info.commitTime() != null) {
                sb.append(" (").append(info.commitTime()).append(')');
            }
        }
        if (info.buildTime() != null) {
            sb.append("\nbuilt:   ").append(info.buildTime());
        }
        return sb.toString();
    }
}
