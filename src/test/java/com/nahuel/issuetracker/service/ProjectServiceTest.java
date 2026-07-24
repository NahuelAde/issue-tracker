package com.nahuel.issuetracker.service;

import com.nahuel.issuetracker.entity.Project;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
@ActiveProfiles("test")
@Transactional
class ProjectServiceTest {

    @Autowired
    private ProjectService projectService;

    private Project newProject(String name, String code) {
        Project project = new Project();
        project.setName(name);
        project.setCode(code);
        project.setActive(true);
        return project;
    }

    @Test
    void createsValidProject() {
        Project saved = projectService.save(newProject("Demo", "DEMO"));

        assertThat(saved.getId()).isNotNull();
        assertThat(saved.isActive()).isTrue();
        assertThat(saved.getCreatedAt()).isNotNull();
        assertThat(saved.getUpdatedAt()).isNotNull();
    }

    @Test
    void rejectsDuplicateCodeIgnoringCase() {
        projectService.save(newProject("Demo", "DEMO"));

        assertThatThrownBy(() -> projectService.save(newProject("Otro", "demo")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("código");
    }

    @Test
    void rejectsDuplicateNameIgnoringCase() {
        projectService.save(newProject("Demo", "DEMO"));

        assertThatThrownBy(() -> projectService.save(newProject("demo", "ALT")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("nombre");
    }

    @Test
    void deactivateRemovesFromActiveList() {
        Project saved = projectService.save(newProject("Demo", "DEMO"));

        projectService.setActive(saved.getId(), false);

        assertThat(projectService.findActiveProjects())
                .extracting(Project::getCode)
                .doesNotContain("DEMO");
        assertThat(projectService.findAllProjects()).hasSize(1);
    }
}
