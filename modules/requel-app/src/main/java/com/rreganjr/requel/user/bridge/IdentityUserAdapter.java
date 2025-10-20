package com.rreganjr.requel.user.bridge;

import com.rreganjr.platform.identity.User;

/**
 * Bridges the lean {@link com.rreganjr.platform.identity.User} contract exposed from
 * {@code platform-identity} to the richer application-facing
 * {@link com.rreganjr.requel.user.User} API. The adapter allows us to migrate repositories and
 * services to the platform module while legacy UI and domain code continues to depend on the
 * existing interface.
 *
 * <p>Callers should prefer {@link #toAppUser(User)} when accepting identity objects from
 * the platform layer and {@link #toIdentity(com.rreganjr.requel.user.User)} when handing
 * application users to shared code. Implementations may return the same instance when the object
 * already implements both contracts.
 */
public interface IdentityUserAdapter {

    /**
     * Convert an identity projection into the application-facing user.
     *
     * @param identityUser value from {@code platform-identity}
     * @return application {@link com.rreganjr.requel.user.User}, or {@code null} when the input is
     *         {@code null}
     * @throws IllegalArgumentException when the adapter cannot resolve a backing user
     */
    com.rreganjr.requel.user.User toAppUser(User identityUser);

    /**
     * Convert an application user into the lean identity view.
     *
     * @param user existing application user
     * @return the corresponding identity projection, or {@code null} when the input is {@code null}
     */
    User toIdentity(com.rreganjr.requel.user.User user);
}
