package com.nahuel.issuetracker.service;

import com.nahuel.issuetracker.entity.Incident;
import com.nahuel.issuetracker.entity.Project;
import com.nahuel.issuetracker.enums.IncidentPriority;
import com.nahuel.issuetracker.enums.IncidentStatus;
import org.junit.jupiter.api.BeforeEach;
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
class IncidentServiceTest {

    @Autowired
    private ProjectService projectService;
    @Autowired
    private IncidentService incidentService;

    private Project project;

    @BeforeEach
    void setUp() {
        Project p = new Project();
        p.setName("Demo");
        p.setCode("DEMO");
        p.setActive(true);
        project = projectService.save(p);
    }

    private Incident newIncident(Project p, String code) {
        Incident incident = new Incident();
        incident.setProject(p);
        incident.setCode(code);
        incident.setTitle("Título de " + code);
        incident.setStatus(IncidentStatus.PENDING);
        incident.setPriority(IncidentPriority.MEDIUM);
        return incident;
    }

    @Test
    void createsIncidentWithDefaultAssignee() {
        Incident saved = incidentService.save(newIncident(project, "CP-001"));

        assertThat(saved.getId()).isNotNull();
        assertThat(saved.getAssignee()).isEqualTo("Sin asignar");
        assertThat(saved.getCreatedAt()).isNotNull();
    }

    @Test
    void rejectsDuplicateCodeWithinProject() {
        incidentService.save(newIncident(project, "CP-001"));

        assertThatThrownBy(() -> incidentService.save(newIncident(project, "cp-001")))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void allowsSameCodeInDifferentProjects() {
        incidentService.save(newIncident(project, "CP-001"));

        Project other = new Project();
        other.setName("Web");
        other.setCode("WEB");
        other.setActive(true);
        Project savedOther = projectService.save(other);

        Incident inOther = incidentService.save(newIncident(savedOther, "CP-001"));
        assertThat(inOther.getId()).isNotNull();
    }

    @Test
    void closeSetsClosedAt() {
        Incident saved = incidentService.save(newIncident(project, "CP-001"));

        Incident closed = incidentService.close(saved);

        assertThat(closed.getStatus()).isEqualTo(IncidentStatus.CLOSED);
        assertThat(closed.getClosedAt()).isNotNull();
    }

    @Test
    void reopenClearsClosedAtAndSetsInProgress() {
        Incident saved = incidentService.save(newIncident(project, "CP-001"));
        incidentService.close(saved);

        Incident reopened = incidentService.reopen(saved);

        assertThat(reopened.getStatus()).isEqualTo(IncidentStatus.IN_PROGRESS);
        assertThat(reopened.getClosedAt()).isNull();
    }

    @Test
    void finishedImpliesStarted() {
        Incident incident = newIncident(project, "CP-001");
        incident.setFinished(true);
        incident.setStarted(false);

        Incident saved = incidentService.save(incident);

        assertThat(saved.isStarted()).isTrue();
    }

    @Test
    void clearsBlockedReasonWhenNotBlocked() {
        Incident incident = newIncident(project, "CP-001");
        incident.setBlocked(false);
        incident.setBlockedReason("un motivo antiguo");

        Incident saved = incidentService.save(incident);

        assertThat(saved.getBlockedReason()).isNull();
    }
}
