package com.rreganjr.requel.utils.jaxb;

import org.springframework.stereotype.Component;

import com.rreganjr.platform.identity.User;
import com.rreganjr.requel.project.ProjectRepository;
import com.rreganjr.requel.user.UserRepository;

/**
 * Factory to produce configured {@link UnmarshallerListener} instances.
 */
@Component
public class UnmarshallerListenerFactory {

    private final ProjectRepository projectRepository;
    private final UserRepository userRepository;

    public UnmarshallerListenerFactory(ProjectRepository projectRepository,
            UserRepository userRepository) {
        this.projectRepository = projectRepository;
        this.userRepository = userRepository;
    }

    public UnmarshallerListener create(User defaultUser, String projectNameOverride) {
        return new UnmarshallerListener(projectRepository, userRepository, defaultUser,
                projectNameOverride);
    }
}
