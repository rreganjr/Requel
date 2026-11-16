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
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nimbusds.jose.jwk.source.JWKSource;
import com.nimbusds.jose.proc.SecurityContext;
import com.rreganjr.AbstractIntegrationTestCase;
import com.rreganjr.requel.gateway.CommandGateway;
import com.rreganjr.requel.gateway.GatewayRequest;
import com.rreganjr.requel.gateway.GatewayResult;
import com.rreganjr.requel.gateway.rest.CommandInfo;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.security.oauth2.jose.jws.SignatureAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;
import org.springframework.test.web.servlet.MockMvc;
import picocli.CommandLine;

/**
 * #489: requel-cli's typed subcommands, built from this server's real command catalog, the JSON
 * the CLI reads from {@code /api/gateway/commands/descriptors}. In rc6 every one of them failed:
 * most Edit and Delete inputs have a {@code version} field, which clashed with picocli's standard
 * {@code --version} option, and the CLI registers them all at startup. The CLI's own tests use stub
 * descriptors, so a new input field can only break it here. The gateway is stubbed: this checks the
 * CLI builds and parses, not the commands themselves.
 */
@AutoConfigureMockMvc
public class TypedCommandsCatalogIT extends AbstractIntegrationTestCase {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JWKSource<SecurityContext> jwkSource;

    @Autowired
    private ObjectMapper objectMapper;

    private List<CommandInfo> descriptors;
    private CommandLine root;
    private final AtomicReference<GatewayRequest> sent = new AtomicReference<>();

    /** An access token as {@code requel login --oauth} stores it (as in OAuthGatewayAccessIT). */
    private String oauthToken() {
        Instant now = Instant.now();
        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer("http://localhost")
                .subject("admin")
                .audience(List.of("requel-cli"))
                .claim("scope", "mcp")
                .issuedAt(now)
                .expiresAt(now.plusSeconds(300))
                .build();
        JwsHeader header = JwsHeader.with(SignatureAlgorithm.RS256).build();
        return new NimbusJwtEncoder(jwkSource).encode(JwtEncoderParameters.from(header, claims))
                .getTokenValue();
    }

    @BeforeEach
    void readTheCatalog() throws Exception {
        String json = mockMvc.perform(get("/api/gateway/commands/descriptors")
                        .header("Authorization", "Bearer " + oauthToken()))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        descriptors = objectMapper.readValue(json, new TypeReference<List<CommandInfo>>() { });

        RequelCli parent = new RequelCli();
        parent.url = "http://localhost:8080";
        root = new CommandLine(parent);
        CommandGateway stub = request -> {
            sent.set(request);
            return new GatewayResult(request.commandType(), Map.of("id", 1));
        };
        assertThatCode(() -> TypedCommands.register(root, parent, descriptors, p -> stub))
                .doesNotThrowAnyException();
    }

    @Test
    void everyWriteCommandInTheCatalogBecomesASubcommand() {
        assertThat(descriptors).filteredOn(CommandInfo::write).isNotEmpty();
        for (CommandInfo d : descriptors) {
            if (d.write()) {
                assertThat(root.getSubcommands()).as(d.commandType())
                        .containsKey(TypedCommands.kebab(d.commandType()));
            }
        }
    }

    @Test
    void resolveIssueParsesAndSendsItsInput() {
        int code = root.execute("resolve-issue", "--project-name", "P", "--issue-id", "352",
                "--position-id", "589");

        assertThat(code).isEqualTo(ExitCode.SUCCESS);
        assertThat(sent.get().commandType()).isEqualTo("ResolveIssue");
        assertThat(input()).containsEntry("projectName", "P").containsEntry("issueId", 352L)
                .containsEntry("positionId", 589L);
    }

    @Test
    void editScenarioStepParsesAndSendsItsInput() {
        int code = root.execute("edit-scenario-step", "--project-name", "P", "--step-id", "27",
                "--text", "Edited from requel-cli");

        assertThat(code).isEqualTo(ExitCode.SUCCESS);
        assertThat(input()).containsEntry("stepId", 27L)
                .containsEntry("text", "Edited from requel-cli");
    }

    /** The field that clashed: an input's lock version is an ordinary flag. */
    @Test
    void editGoalTakesItsVersion() {
        int code = root.execute("edit-goal", "--project-name", "P", "--name", "G", "--version", "1");

        assertThat(code).isEqualTo(ExitCode.SUCCESS);
        assertThat(input()).containsEntry("version", 1L);
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> input() {
        return (Map<String, Object>) sent.get().input();
    }
}
