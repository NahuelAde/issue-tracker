package com.nahuel.issuetracker.service;

import com.nahuel.issuetracker.entity.Incident;
import com.nahuel.issuetracker.entity.IncidentEntry;
import com.nahuel.issuetracker.entity.Project;
import com.nahuel.issuetracker.enums.IncidentEntryType;
import com.nahuel.issuetracker.enums.IncidentPriority;
import com.nahuel.issuetracker.enums.IncidentStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
@ActiveProfiles("test")
@Transactional
class IncidentEntryServiceTest {

    @Autowired
    private ProjectService projectService;
    @Autowired
    private IncidentService incidentService;
    @Autowired
    private IncidentEntryService entryService;

    private Incident incident;

    @BeforeEach
    void setUp() {
        Project p = new Project();
        p.setName("Demo");
        p.setCode("DEMO");
        p.setActive(true);
        Project project = projectService.save(p);

        Incident i = new Incident();
        i.setProject(project);
        i.setCode("CP-001");
        i.setTitle("Incidencia de prueba");
        i.setStatus(IncidentStatus.PENDING);
        i.setPriority(IncidentPriority.MEDIUM);
        incident = incidentService.save(i);
    }

    private IncidentEntry newEntry(IncidentEntryType type, String hours, LocalDate date) {
        IncidentEntry entry = new IncidentEntry();
        entry.setIncident(incident);
        entry.setEntryType(type);
        entry.setEntryDate(date);
        entry.setDescription("Entrada " + type);
        if (hours != null) {
            entry.setHours(new BigDecimal(hours));
        }
        return entry;
    }

    @Test
    void addsEntry() {
        IncidentEntry saved = entryService.save(
                newEntry(IncidentEntryType.DEVELOPMENT, "1.50", LocalDate.now()));

        assertThat(saved.getId()).isNotNull();
        assertThat(entryService.findByIncident(incident)).hasSize(1);
    }

    @Test
    void rejectsNegativeHours() {
        assertThatThrownBy(() -> entryService.save(
                newEntry(IncidentEntryType.DEVELOPMENT, "-1.00", LocalDate.now())))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void computesTotalHours() {
        entryService.save(newEntry(IncidentEntryType.DEVELOPMENT, "1.50", LocalDate.now()));
        entryService.save(newEntry(IncidentEntryType.DEVELOPMENT, "2.25", LocalDate.now()));

        assertThat(entryService.getTotalHours(incident)).isEqualByComparingTo("3.75");
    }

    @Test
    void getsLastPreAndProDeployments() {
        entryService.save(newEntry(IncidentEntryType.PRE_DEPLOYMENT, "0", LocalDate.now().minusDays(5)));
        IncidentEntry lastPre = entryService.save(
                newEntry(IncidentEntryType.PRE_DEPLOYMENT, "0", LocalDate.now()));
        IncidentEntry pro = entryService.save(
                newEntry(IncidentEntryType.PRO_DEPLOYMENT, "0", LocalDate.now().minusDays(1)));

        assertThat(entryService.getLastDeployment(incident, IncidentEntryType.PRE_DEPLOYMENT))
                .get().extracting(IncidentEntry::getId).isEqualTo(lastPre.getId());
        assertThat(entryService.getLastDeployment(incident, IncidentEntryType.PRO_DEPLOYMENT))
                .get().extracting(IncidentEntry::getId).isEqualTo(pro.getId());
    }

    @Test
    void deleteEntryKeepsIncident() {
        IncidentEntry saved = entryService.save(
                newEntry(IncidentEntryType.DEVELOPMENT, "1.00", LocalDate.now()));

        entryService.delete(saved.getId());

        assertThat(entryService.findByIncident(incident)).isEmpty();
        assertThat(incidentService.findById(incident.getId())).isPresent();
    }
}
