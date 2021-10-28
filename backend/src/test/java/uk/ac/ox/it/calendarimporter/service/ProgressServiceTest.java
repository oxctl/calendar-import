package uk.ac.ox.it.calendarimporter.service;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.context.annotation.Import;
import uk.ac.ox.it.calendarimporter.persistence.model.JobProgress;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest
@Import(ProgressService.class)
public class ProgressServiceTest {

    @Autowired
    private ProgressService progressService;

    @Autowired
    private TestEntityManager entityManager;

    @Test
    public void testUpdateJobCreated() {
        JobProgress triggerId = progressService.updateJobCreated("triggerId");
        assertEquals(JobProgress.Status.QUEUED, triggerId.getStatus());
        assertNull(triggerId.getStarted());
        assertNotNull(triggerId.getLastMessage());
    }

    @Test
    public void testUpdateJobCreatedExists() {
        progressService.updateJobStarted("triggerId");
        JobProgress triggerId = progressService.updateJobCreated("triggerId");
        // Check we didn't reset the job back to being queued.
        assertNotEquals(JobProgress.Status.QUEUED, triggerId.getStatus());
        assertNotNull(triggerId.getStarted());
    }

    @Test
    public void testNormalProgression() {
        JobProgress progress;
        progress = progressService.updateJobCreated("triggerId");
        assertEquals("triggerId", progress.getId());
        progress = progressService.updateJobStarted("triggerId");
        assertEquals("triggerId", progress.getId());
        progress = progressService.updateJob("triggerId", "Testing 1", 0);
        assertEquals("triggerId", progress.getId());
        progress = progressService.updateJob("triggerId", "Testing 2", 1);
        assertEquals("triggerId", progress.getId());
        progress = progressService.updateJob("triggerId", "Testing 3", 2);
        assertEquals("triggerId", progress.getId());
        progress = progressService.updateJobStopped("triggerId", null, "file:///tmp/logfile");
        assertEquals("triggerId", progress.getId());
        assertEquals(JobProgress.Status.COMPLETED, progress.getStatus());
    }

    @Test
    public void testOutPercentage() {
        assertThrows(
                IllegalArgumentException.class, () -> progressService.updateJob("triggerId", "Status", -1));
    }

    @Test
    public void testUpdateCompleted() {
        assertThrows(
                IllegalStateException.class,
                () -> {
                    progressService.updateJobStopped("triggerId", null, null);
                    progressService.updateJob("triggerId", "Status", 1);
                });
    }

    @Test
    public void testUpdateErrored() {
        assertThrows(
                IllegalStateException.class,
                () -> {
                    progressService.updateJobStopped("triggerId", "Error", null);
                    progressService.updateJob("triggerId", "Status", 1);
                });
    }

    @Test
    public void testUpdateNoMessage() {
        assertThrows(
                IllegalArgumentException.class, () -> progressService.updateJob("triggerId", null, 10));
    }
}
