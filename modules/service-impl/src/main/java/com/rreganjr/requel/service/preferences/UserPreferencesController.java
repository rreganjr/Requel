package com.rreganjr.requel.service.preferences;

import com.rreganjr.requel.service.api.dto.UserPreferencesDto;
import com.rreganjr.requel.service.auth.CurrentUserResolver;
import com.rreganjr.requel.user.User;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * REST endpoints for user UI preferences.
 * GET returns current preferences (creates defaults if none exist).
 * PUT updates preferences.
 */
@RestController
@RequestMapping("/api/user-preferences")
public class UserPreferencesController {

    private final UserPreferencesRepository repository;
    private final CurrentUserResolver currentUserResolver;

    public UserPreferencesController(UserPreferencesRepository repository,
                                     CurrentUserResolver currentUserResolver) {
        this.repository = repository;
        this.currentUserResolver = currentUserResolver;
    }

    @GetMapping
    public UserPreferencesDto getPreferences() {
        User user = currentUserResolver.resolve();
        UserPreferences prefs = findOrCreate(user.getUsername());
        return toDto(prefs);
    }

    @PutMapping
    public ResponseEntity<UserPreferencesDto> updatePreferences(@RequestBody UserPreferencesDto input) {
        User user = currentUserResolver.resolve();
        UserPreferences prefs = findOrCreate(user.getUsername());

        if (input.sidebarProjectLimit() > 0) {
            prefs.setSidebarProjectLimit(input.sidebarProjectLimit());
        }
        if (input.sidebarProjectStaleness() != null) {
            try {
                prefs.setSidebarProjectStaleness(
                        SidebarProjectStaleness.valueOf(input.sidebarProjectStaleness()));
            } catch (IllegalArgumentException e) {
                return ResponseEntity.badRequest().build();
            }
        }

        repository.save(prefs);
        return ResponseEntity.ok(toDto(prefs));
    }

    private UserPreferences findOrCreate(String username) {
        return repository.findByUsername(username)
                .orElseGet(() -> repository.save(new UserPreferences(username)));
    }

    private UserPreferencesDto toDto(UserPreferences prefs) {
        return new UserPreferencesDto(
                prefs.getSidebarProjectLimit(),
                prefs.getSidebarProjectStaleness().name()
        );
    }
}
