package com.rreganjr.requel.user;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

import com.rreganjr.requel.Application;
import com.rreganjr.requel.user.UserRepository;
import com.rreganjr.requel.user.exception.NoSuchUserException;
import com.rreganjr.requel.user.impl.repository.init.ProjectUserInitializer;

@SpringBootTest(classes = Application.class)
@TestPropertySource(locations = "classpath:db.properties", properties = {
        "db.name=requel_project_user_test",
        "db.driverUrl=jdbc:h2:mem:requel_project_user_test;MODE=MYSQL;DB_CLOSE_DELAY=-1;DATABASE_TO_UPPER=false"
})
@org.springframework.test.context.ActiveProfiles("test")
class ProjectUserCreationIT {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ProjectUserInitializer projectUserInitializer;

    @Test
    void projectUserInitializerCreatesMissingUser() throws Exception {
        assertThatThrownBy(() -> userRepository.findUserByUsername("project"))
                .isInstanceOf(NoSuchUserException.class);

        projectUserInitializer.initialize();

        assertThat(userRepository.findUserByUsername("project")).isNotNull();
    }
}
