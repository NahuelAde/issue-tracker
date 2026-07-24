package com.nahuel.issuetracker.service;

import com.nahuel.issuetracker.entity.Project;
import com.nahuel.issuetracker.entity.Sprint;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
@ActiveProfiles("test")
@Transactional
class SprintServiceTest {

    @Autowired
    private ProjectService projectService;
    @Autowired
    private SprintService sprintService;

    private Project project;

    @BeforeEach
    void setUp() {
        Project p = new Project();
        p.setName("Demo");
        p.setCode("DEMO");
        p.setActive(true);
        project = projectService.save(p);
    }

    private Sprint newSprint(String name, LocalDate start, LocalDate end) {
        Sprint sprint = new Sprint();
        sprint.setProject(project);
        sprint.setName(name);
        sprint.setStartDate(start);
        sprint.setEndDate(end);
        sprint.setActive(true);
        return sprint;
    }

    @Test
    void createsValidSprint() {
        Sprint saved = sprintService.save(
                newSprint("Sprint 1", LocalDate.now(), LocalDate.now().plusDays(14)));

        assertThat(saved.getId()).isNotNull();
        assertThat(saved.isActive()).isTrue();
    }

    @Test
    void rejectsDuplicateNameInProject() {
        sprintService.save(newSprint("Sprint 1", LocalDate.now(), LocalDate.now().plusDays(14)));

        assertThatThrownBy(() -> sprintService.save(
                newSprint("sprint 1", LocalDate.now(), LocalDate.now().plusDays(7))))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void rejectsEndBeforeStart() {
        assertThatThrownBy(() -> sprintService.save(
                newSprint("Sprint 1", LocalDate.now(), LocalDate.now().minusDays(1))))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void findCurrentReturnsSprintContainingToday() {
        Sprint previous = newSprint("Anterior", LocalDate.now().minusDays(30),
                LocalDate.now().minusDays(16));
        previous.setActive(false); // sprint pasado ya cerrado (solo uno puede estar activo)
        sprintService.save(previous);
        Sprint current = sprintService.save(newSprint("Actual", LocalDate.now().minusDays(1),
                LocalDate.now().plusDays(1)));

        assertThat(sprintService.findCurrent(project))
                .get().extracting(Sprint::getId).isEqualTo(current.getId());
    }

    @Test
    void rejectsOverlappingDates() {
        sprintService.save(newSprint("Sprint 1", LocalDate.now(), LocalDate.now().plusDays(14)));

        assertThatThrownBy(() -> sprintService.save(
                newSprint("Sprint 2", LocalDate.now().plusDays(7), LocalDate.now().plusDays(21))))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("solapan");
    }

    @Test
    void rejectsSecondActiveSprint() {
        sprintService.save(newSprint("Sprint 1", LocalDate.now(), LocalDate.now().plusDays(14)));

        // Fechas no solapadas, pero un segundo sprint activo no está permitido.
        assertThatThrownBy(() -> sprintService.save(
                newSprint("Sprint 2", LocalDate.now().plusDays(15), LocalDate.now().plusDays(28))))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("activo");
    }
}
