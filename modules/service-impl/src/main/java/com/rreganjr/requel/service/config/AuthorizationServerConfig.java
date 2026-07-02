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
package com.rreganjr.requel.service.config;

import com.rreganjr.requel.service.auth.OAuth2ConsentController;
import com.rreganjr.requel.service.auth.OAuth2LoginPageController;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jose.jwk.source.ImmutableJWKSet;
import com.nimbusds.jose.jwk.source.JWKSource;
import com.nimbusds.jose.proc.SecurityContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.env.Environment;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.crypto.factory.PasswordEncoderFactories;
import org.springframework.core.convert.converter.Converter;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.ClientAuthenticationMethod;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.server.authorization.oidc.OidcClientRegistration;
import org.springframework.security.oauth2.server.authorization.oidc.authentication.OidcClientRegistrationAuthenticationProvider;
import org.springframework.security.oauth2.server.authorization.JdbcOAuth2AuthorizationConsentService;
import org.springframework.security.oauth2.server.authorization.JdbcOAuth2AuthorizationService;
import org.springframework.security.oauth2.server.authorization.OAuth2AuthorizationConsentService;
import org.springframework.security.oauth2.server.authorization.OAuth2AuthorizationService;
import org.springframework.security.oauth2.server.authorization.client.JdbcRegisteredClientRepository;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClient;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClientRepository;
import org.springframework.security.oauth2.server.authorization.config.annotation.web.configuration.OAuth2AuthorizationServerConfiguration;
import org.springframework.security.oauth2.server.authorization.config.annotation.web.configurers.OAuth2AuthorizationServerConfigurer;
import org.springframework.security.oauth2.server.authorization.settings.AuthorizationServerSettings;
import org.springframework.security.oauth2.server.authorization.settings.ClientSettings;
import org.springframework.security.oauth2.server.authorization.settings.OAuth2TokenFormat;
import org.springframework.security.oauth2.server.authorization.settings.TokenSettings;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.LoginUrlAuthenticationEntryPoint;
import org.springframework.security.web.util.matcher.MediaTypeRequestMatcher;

import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.net.URI;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.function.Consumer;

/**
 * Embedded OAuth 2.1 Authorization Server for MCP client authentication (issue #83, Slice 1).
 *
 * <p>Requel runs its own authorization server (Spring Authorization Server) against its existing
 * user store; {@code /api/mcp/**} becomes an OAuth2 resource server (Slice 2) validating the tokens
 * this server issues. See {@code doc/oauth_mcp_plan.md} → "Implementation Plan".
 *
 * <p><b>Filter-chain layering</b> (unique {@code @Order}; more specific matchers get lower numbers):
 * <ol>
 *   <li>{@code @Order(1)} — this AS-endpoints chain ({@code /oauth2/**}, {@code /connect/**},
 *       {@code /.well-known/oauth-authorization-server}).</li>
 *   <li>{@code @Order(2)} — the interactive login/consent chain ({@code /login},
 *       {@code /oauth2/consent}) with form login backed by {@link RequelUserAuthenticationProvider}.</li>
 *   <li>{@code @Order(3)} — MCP resource server on {@code /api/mcp/**} (Slice 2, not yet added).</li>
 *   <li>{@code @Order(4)} — the existing stateless JWT chain on {@code /api/**}
 *       ({@code ApiSecurityConfig}).</li>
 * </ol>
 * The AS and login chains use sessions + CSRF (defaults) for the browser flow; the {@code /api/**}
 * chain stays stateless. Session policy and CSRF are per-chain, so they do not interfere.
 */
@Configuration
public class AuthorizationServerConfig {

    private static final Logger log = LoggerFactory.getLogger(AuthorizationServerConfig.class);

    /** Access-token TTL (short — clients silently refresh). */
    private static final Duration ACCESS_TOKEN_TTL = Duration.ofHours(1);
    /** Refresh-token TTL (long — rotating, with reuse detection; see TokenSettings below). */
    private static final Duration REFRESH_TOKEN_TTL = Duration.ofDays(30);

    /**
     * OAuth runtime flags are read from the {@link Environment} at point of use rather than via
     * {@code @Value} fields. This class contributes {@code SecurityFilterChain} beans and is
     * instantiated early enough that {@code @Value} placeholders resolved to their defaults (missing
     * the command-line / external values). The Environment always carries the full, correctly-ordered
     * property sources, so reading it at runtime binds correctly (issue #83).
     */
    private final Environment environment;

    public AuthorizationServerConfig(Environment environment) {
        this.environment = environment;
    }

    /** Optional explicit issuer; blank = derive per-request from the current context. */
    private String issuer() {
        return environment.getProperty("requel.oauth.issuer", "");
    }

    /** Seed a loopback PKCE dev client so the flow is testable before/without DCR (Slice 4). */
    private boolean seedDevClient() {
        return environment.getProperty("requel.oauth.seed-dev-client", Boolean.class, false);
    }

    /** Seed the DCR registrar client. The registration endpoint is unusable until a registrar exists. */
    private boolean dcrEnabled() {
        return environment.getProperty("requel.oauth.dcr.enabled", Boolean.class, false);
    }

    private String registrarClientId() {
        return environment.getProperty("requel.oauth.dcr.registrar-client-id", "requel-registrar");
    }

    /** Registrar client secret (raw); required when {@code requel.oauth.dcr.enabled=true}. */
    private String registrarClientSecret() {
        return environment.getProperty("requel.oauth.dcr.registrar-client-secret", "");
    }

    /** Log the resolved OAuth flags at startup so seeding behavior is diagnosable (issue #83). */
    @jakarta.annotation.PostConstruct
    void logResolvedOAuthConfig() {
        String secret = registrarClientSecret();
        log.info("OAuth AS config resolved: seed-dev-client={}, dcr.enabled={}, "
                + "dcr.registrar-client-id={}, registrar-secret-set={}, issuer='{}'",
                seedDevClient(), dcrEnabled(), registrarClientId(),
                (secret != null && !secret.isBlank()), issuer());
    }

    // ---- Chain 1: authorization-server endpoints -------------------------------------------------

    @Bean
    @Order(1)
    public SecurityFilterChain authorizationServerSecurityFilterChain(HttpSecurity http) throws Exception {
        OAuth2AuthorizationServerConfigurer authorizationServerConfigurer =
                OAuth2AuthorizationServerConfigurer.authorizationServer();

        http
            .securityMatcher(authorizationServerConfigurer.getEndpointsMatcher())
            .with(authorizationServerConfigurer, (authorizationServer) -> authorizationServer
                // OpenID Connect: userinfo + Dynamic Client Registration (Slice 4). The registration
                // endpoint requires an initial access token (client_credentials + client.create),
                // minted by the seeded registrar client; see the DCR notes in doc/oauth_mcp_plan.md.
                .oidc(oidc -> oidc
                    .clientRegistrationEndpoint(clientRegistration ->
                        clientRegistration.authenticationProviders(applyDcrClientDefaults())))
                // Custom consent page (issue #83: consent required for all MCP clients).
                .authorizationEndpoint(authorizationEndpoint ->
                        authorizationEndpoint.consentPage(OAuth2ConsentController.CONSENT_PAGE_URI))
            )
            .authorizeHttpRequests(authorize -> authorize.anyRequest().authenticated())
            // Unauthenticated browser requests to AS endpoints redirect to the AS login page.
            // NOTE: /oauth2/login, NOT /login — /login belongs to the Angular SPA's own route.
            .exceptionHandling(exceptions -> exceptions
                .defaultAuthenticationEntryPointFor(
                    new LoginUrlAuthenticationEntryPoint(OAuth2LoginPageController.LOGIN_PAGE_URI),
                    new MediaTypeRequestMatcher(MediaType.TEXT_HTML)))
            // Accept the AS's own JWTs at the OIDC userinfo endpoint.
            .oauth2ResourceServer(resourceServer -> resourceServer.jwt(Customizer.withDefaults()));

        return http.build();
    }

    // ---- Chain 2: interactive login + consent ---------------------------------------------------

    @Bean
    @Order(2)
    public SecurityFilterChain authorizationServerLoginFilterChain(HttpSecurity http) throws Exception {
        http
            // Scoped to the AS interactive pages only (NOT /login — that is the SPA's route), so the
            // SPA routes and the /api/** chain are untouched. Everything here is browser form flow.
            .securityMatcher(OAuth2LoginPageController.LOGIN_PAGE_URI, OAuth2ConsentController.CONSENT_PAGE_URI)
            .authorizeHttpRequests(authorize -> authorize
                .requestMatchers(OAuth2LoginPageController.LOGIN_PAGE_URI).permitAll()
                .anyRequest().authenticated())
            // Custom login page at /oauth2/login (OAuth2LoginPageController); the same URL is the
            // form-login processing URL. Credentials are checked by RequelUserAuthenticationProvider.
            .formLogin(form -> form
                .loginPage(OAuth2LoginPageController.LOGIN_PAGE_URI)
                .loginProcessingUrl(OAuth2LoginPageController.LOGIN_PAGE_URI)
                .permitAll());

        return http.build();
    }

    // ---- Persistence (JDBC-backed; tables via Flyway V12) ---------------------------------------

    @Bean
    public RegisteredClientRepository registeredClientRepository(JdbcTemplate jdbcTemplate) {
        return new JdbcRegisteredClientRepository(jdbcTemplate);
    }

    @Bean
    public OAuth2AuthorizationService authorizationService(JdbcTemplate jdbcTemplate,
            RegisteredClientRepository registeredClientRepository) {
        return new JdbcOAuth2AuthorizationService(jdbcTemplate, registeredClientRepository);
    }

    @Bean
    public OAuth2AuthorizationConsentService authorizationConsentService(JdbcTemplate jdbcTemplate,
            RegisteredClientRepository registeredClientRepository) {
        return new JdbcOAuth2AuthorizationConsentService(jdbcTemplate, registeredClientRepository);
    }

    /**
     * Encoder for client secrets (confidential clients / DCR). Loopback PKCE public clients carry no
     * secret, but the AS requires a {@link PasswordEncoder} bean for client authentication.
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return PasswordEncoderFactories.createDelegatingPasswordEncoder();
    }

    // ---- Signing key + issuer -------------------------------------------------------------------

    /**
     * RSA signing key for issued JWTs.
     *
     * <p><b>Slice 1 caveat:</b> this key is generated at startup, so it changes on every restart and
     * previously-issued tokens stop validating after a restart. Fine for dev; the hardening
     * follow-up (see plan "Out of scope / follow-ups") loads a persistent key from a configured
     * keystore so tokens survive restarts.
     */
    @Bean
    public JWKSource<SecurityContext> jwkSource() {
        KeyPair keyPair = generateRsaKey();
        RSAPublicKey publicKey = (RSAPublicKey) keyPair.getPublic();
        RSAPrivateKey privateKey = (RSAPrivateKey) keyPair.getPrivate();
        RSAKey rsaKey = new RSAKey.Builder(publicKey)
                .privateKey(privateKey)
                .keyID(UUID.randomUUID().toString())
                .build();
        log.warn("OAuth AS signing key generated at startup (ephemeral). Tokens will not survive a "
                + "restart; configure a persistent keystore before production (issue #83 hardening).");
        return new ImmutableJWKSet<>(new JWKSet(rsaKey));
    }

    private static KeyPair generateRsaKey() {
        try {
            KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance("RSA");
            keyPairGenerator.initialize(2048);
            return keyPairGenerator.generateKeyPair();
        } catch (Exception e) {
            throw new IllegalStateException("Unable to generate RSA key for OAuth signing", e);
        }
    }

    @Bean
    public org.springframework.security.oauth2.jwt.JwtDecoder jwtDecoder(JWKSource<SecurityContext> jwkSource) {
        return OAuth2AuthorizationServerConfiguration.jwtDecoder(jwkSource);
    }

    @Bean
    public AuthorizationServerSettings authorizationServerSettings() {
        AuthorizationServerSettings.Builder builder = AuthorizationServerSettings.builder();
        String issuer = issuer();
        if (issuer != null && !issuer.isBlank()) {
            builder.issuer(issuer);
        }
        return builder.build();
    }

    // ---- Default token policy + optional dev-client seeding -------------------------------------

    /**
     * Token policy applied to registered clients: 1h access token, 30d rotating refresh token with
     * reuse detection (rotation is on because {@code reuseRefreshTokens(false)}), self-contained JWT
     * access tokens. Slice 4 (DCR) applies these same defaults to self-registered clients.
     */
    static TokenSettings defaultTokenSettings() {
        return TokenSettings.builder()
                .accessTokenFormat(OAuth2TokenFormat.SELF_CONTAINED)
                .accessTokenTimeToLive(ACCESS_TOKEN_TTL)
                .refreshTokenTimeToLive(REFRESH_TOKEN_TTL)
                .reuseRefreshTokens(false)
                .build();
    }

    /**
     * Registers a loopback, PKCE, consent-required dev client on startup when
     * {@code requel.oauth.seed-dev-client=true}, so the authorization-code + PKCE flow can be
     * exercised end to end before Dynamic Client Registration (Slice 4) exists. Idempotent.
     */
    @Bean
    public ApplicationRunner seedOAuthDevClient(RegisteredClientRepository registeredClientRepository) {
        return args -> {
            if (!seedDevClient()) {
                return;
            }
            String clientId = "requel-dev-client";
            if (registeredClientRepository.findByClientId(clientId) != null) {
                return;
            }
            RegisteredClient devClient = RegisteredClient.withId(UUID.randomUUID().toString())
                    .clientId(clientId)
                    // Public client (no secret): PKCE-only, matching desktop/CLI agent clients.
                    .clientAuthenticationMethod(ClientAuthenticationMethod.NONE)
                    .authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
                    .authorizationGrantType(AuthorizationGrantType.REFRESH_TOKEN)
                    // Loopback redirect URIs only (issue #83 DCR decision).
                    .redirectUri("http://127.0.0.1:8080/login/oauth2/code/requel-dev-client")
                    .redirectUri("http://localhost:8080/login/oauth2/code/requel-dev-client")
                    .scope("mcp")
                    .clientSettings(ClientSettings.builder()
                            .requireAuthorizationConsent(true)
                            .requireProofKey(true)
                            .build())
                    .tokenSettings(defaultTokenSettings())
                    .build();
            registeredClientRepository.save(devClient);
            log.info("Seeded OAuth dev client '{}' (loopback PKCE, scope=mcp, consent required).",
                    clientId);
        };
    }

    // ---- Dynamic Client Registration policy (Slice 4) -------------------------------------------

    /**
     * Stamps Requel's policy onto every dynamically-registered client (issue #83, Slice 4): rejects
     * non-loopback redirect URIs, forces PKCE + consent, restricts scope to {@code mcp}, and applies
     * the shared 1h/30d rotating token settings. Wraps Spring AS's default
     * {@code OidcClientRegistration -> RegisteredClient} conversion.
     */
    private Consumer<List<AuthenticationProvider>> applyDcrClientDefaults() {
        return authenticationProviders -> {
            for (AuthenticationProvider provider : authenticationProviders) {
                if (provider instanceof OidcClientRegistrationAuthenticationProvider registrationProvider) {
                    registrationProvider.setRegisteredClientConverter(new DcrRegisteredClientConverter());
                }
            }
        };
    }

    private static final class DcrRegisteredClientConverter
            implements Converter<OidcClientRegistration, RegisteredClient> {

        @Override
        public RegisteredClient convert(OidcClientRegistration clientRegistration) {
            // Spring AS's built-in OidcClientRegistration -> RegisteredClient converter is not public
            // API, so build the client directly and impose Requel's policy: public (PKCE) loopback
            // native-app client, consent required, scope=mcp, 1h/30d rotating tokens.
            List<String> redirectUris = clientRegistration.getRedirectUris();
            if (redirectUris == null || redirectUris.isEmpty()) {
                throw new OAuth2AuthenticationException(new OAuth2Error(
                        "invalid_redirect_uri",
                        "At least one loopback redirect URI is required", null));
            }
            for (String redirectUri : redirectUris) {
                if (!isLoopbackRedirectUri(redirectUri)) {
                    throw new OAuth2AuthenticationException(new OAuth2Error(
                            "invalid_redirect_uri",
                            "Only loopback redirect URIs (127.0.0.1, [::1], localhost) are allowed",
                            null));
                }
            }
            String clientName = clientRegistration.getClientName();
            RegisteredClient.Builder builder = RegisteredClient.withId(UUID.randomUUID().toString())
                    .clientId(UUID.randomUUID().toString())
                    .clientIdIssuedAt(Instant.now())
                    .clientName(clientName != null ? clientName : "mcp-client")
                    // Public, PKCE-only (loopback native app) — no client secret issued.
                    .clientAuthenticationMethod(ClientAuthenticationMethod.NONE)
                    .authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
                    .authorizationGrantType(AuthorizationGrantType.REFRESH_TOKEN)
                    .scope("mcp")
                    .clientSettings(ClientSettings.builder()
                            .requireProofKey(true)
                            .requireAuthorizationConsent(true)
                            .build())
                    .tokenSettings(defaultTokenSettings());
            redirectUris.forEach(builder::redirectUri);
            return builder.build();
        }
    }

    /** Loopback per OAuth 2.1 native-app guidance: 127.0.0.1, [::1]/::1, or localhost. */
    private static boolean isLoopbackRedirectUri(String redirectUri) {
        try {
            String host = URI.create(redirectUri).getHost();
            return host != null && (host.equals("127.0.0.1") || host.equals("[::1]")
                    || host.equals("::1") || host.equalsIgnoreCase("localhost"));
        } catch (IllegalArgumentException e) {
            return false;
        }
    }

    /**
     * Seeds the DCR registrar client (client_credentials + client.create/client.read) when
     * {@code requel.oauth.dcr.enabled=true} and a secret is configured. An admin uses it to mint the
     * single-use initial access token required to register new clients. Idempotent.
     */
    @Bean
    public ApplicationRunner seedDcrRegistrarClient(RegisteredClientRepository registeredClientRepository,
            PasswordEncoder passwordEncoder) {
        return args -> {
            if (!dcrEnabled()) {
                return;
            }
            String registrarSecret = registrarClientSecret();
            if (registrarSecret == null || registrarSecret.isBlank()) {
                log.warn("requel.oauth.dcr.enabled=true but requel.oauth.dcr.registrar-client-secret "
                        + "is not set; DCR registrar client NOT seeded, so client registration is "
                        + "unusable until a registrar exists.");
                return;
            }
            String registrarId = registrarClientId();
            if (registeredClientRepository.findByClientId(registrarId) != null) {
                return;
            }
            RegisteredClient registrar = RegisteredClient.withId(UUID.randomUUID().toString())
                    .clientId(registrarId)
                    .clientSecret(passwordEncoder.encode(registrarSecret))
                    .clientAuthenticationMethod(ClientAuthenticationMethod.CLIENT_SECRET_BASIC)
                    .authorizationGrantType(AuthorizationGrantType.CLIENT_CREDENTIALS)
                    .scope("client.create")
                    .scope("client.read")
                    .build();
            registeredClientRepository.save(registrar);
            log.info("Seeded DCR registrar client '{}' (client_credentials; scopes client.create, "
                    + "client.read).", registrarId);
        };
    }
}
