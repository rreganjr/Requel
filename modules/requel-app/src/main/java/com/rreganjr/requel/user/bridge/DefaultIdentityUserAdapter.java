package com.rreganjr.requel.user.bridge;

import org.springframework.stereotype.Component;

import com.rreganjr.platform.identity.User;

/**
 * Passthrough adapter that keeps the build green while we migrate identity infrastructure.
 * <p>
 * Today the application {@link com.rreganjr.requel.user.User} already extends the platform
 * {@link User} interface, so we can freely cast between them. When the identity module starts
 * returning distinct implementations this component will be responsible for wrapping or looking up
 * the matching application user.
 */
@Component
public class DefaultIdentityUserAdapter implements IdentityUserAdapter {

    @Override
    public com.rreganjr.requel.user.User toAppUser(User identityUser) {
        if (identityUser == null) {
            return null;
        }
        if (identityUser instanceof com.rreganjr.requel.user.User appUser) {
            return appUser;
        }
        throw new IllegalArgumentException(
                "Platform identity type %s cannot be adapted to com.rreganjr.requel.user.User"
                        .formatted(identityUser.getClass().getName()));
    }

    @Override
    public User toIdentity(com.rreganjr.requel.user.User user) {
        return user;
    }
}
