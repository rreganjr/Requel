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

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.rreganjr.requel.gateway.QueryGateway;
import com.rreganjr.requel.gateway.rest.RestQueryGateway;
import java.util.concurrent.Callable;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;
import picocli.CommandLine.ParentCommand;

/**
 * Base for {@code requel} read subcommands over the {@link QueryGateway} (#100). Handles building the
 * REST-backed gateway from the parent's URL + token, mapping transport failures to exit codes, and
 * rendering the result as text or {@code --output json}. Subclasses supply the query call and the
 * text rendering. Reads go through {@code /api/gateway/query/**}, so read authorization is enforced
 * server-side exactly as in the UI.
 */
abstract class AbstractQueryCommand implements Callable<Integer> {

    @ParentCommand
    RequelCli parent;

    /** Test seam: when set, used instead of building a {@link RestQueryGateway} from the parent. */
    QueryGateway queryOverride;

    private final ObjectMapper mapper = new ObjectMapper();

    /** Run the query against the gateway. */
    protected abstract Object query(QueryGateway gateway);

    /** Render the (non-null) result for text output. */
    protected abstract String renderText(Object result);

    @Override
    public Integer call() {
        QueryGateway gateway = (queryOverride != null) ? queryOverride
                : new RestQueryGateway(parent.url, parent.tokenSource());
        Object result;
        try {
            result = query(gateway);
        } catch (RestClientResponseException e) {
            System.err.println("Error: " + e.getStatusCode().value() + " " + e.getStatusText());
            int status = e.getStatusCode().value();
            return (status == 401 || status == 403) ? ExitCode.AUTH : ExitCode.REQUEST_ERROR;
        } catch (RestClientException e) {
            System.err.println("Failed to reach Requel: " + e.getMessage());
            return ExitCode.REQUEST_ERROR;
        }
        if (parent.output == OutputFormat.JSON) {
            System.out.println(json(result));
        } else {
            System.out.println(renderText(result));
        }
        return ExitCode.SUCCESS;
    }

    /** Pretty JSON for the result, falling back to {@code toString()} if serialization fails. */
    protected String json(Object result) {
        try {
            return mapper.writerWithDefaultPrettyPrinter().writeValueAsString(result);
        } catch (JsonProcessingException e) {
            return String.valueOf(result);
        }
    }
}
