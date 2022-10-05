package uk.ac.ox.it.calendarimporter.service;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.context.annotation.Import;
import uk.ac.ox.it.calendarimporter.persistence.model.JobProgress;
import uk.ac.ox.it.calendarimporter.persistence.repo.JobProgressRepository;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest
@Import(ProgressService.class)
public class ProgressServiceTest {

    @Autowired
    private ProgressService progressService;

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private JobProgressRepository progressRepository;

    @Test
    public void testUpdateJobNoTriggerId(){
        assertThrows(IllegalArgumentException.class, () -> progressService.updateJob(null, "Status", 50));
    }

    @Test
    public void testUpdateJobOutPercentage() {
        assertThrows(IllegalArgumentException.class, () -> progressService.updateJob("triggerId", "Status", -1));
    }

    @Test
    public void testUpdateJobNoMessage() {
        assertThrows(IllegalArgumentException.class, () -> progressService.updateJob("triggerId", null, 10));
    }

    @Test
    public void testUpdateJobCompleted() {
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
    public void testResetJobNoTriggerId(){
        assertThrows(IllegalArgumentException.class, () -> progressService.resetJob(null));
    }

    @Test
    public void testResetJob(){
        String id = "triggerId";
        JobProgress progress = new JobProgress(id);
        progress.setCompleted(Instant.now());
        progress.setLastMessage("Last message");
        progress.setStatus(JobProgress.Status.COMPLETED);
        progress.setPercentage(100);
        progress.setLogfile("log.txt");
        progressRepository.save(progress);
        progressService.resetJob(id);

        progress = progressRepository.findById(id).orElseThrow();
        assertNull(progress.getCompleted());
        assertNull(progress.getLastMessage());
        assertNull(progress.getStatus());
        assertEquals(0, progress.getPercentage());
        assertNull(progress.getLogfile());
    }

    @Test
    public void testUpdateJobStoppedCompleted(){
        String id = "triggerId";
        JobProgress progress = new JobProgress(id);
        progressRepository.save(progress);
        progress = progressService.updateJobStopped(id, "log.txt", false, false);

        assertEquals(JobProgress.Status.COMPLETED, progress.getStatus());
    }

    @Test
    public void testUpdateJobStoppedProblems(){
        String id = "triggerId";
        JobProgress progress = new JobProgress(id);
        progressRepository.save(progress);
        progress = progressService.updateJobStopped(id, "log.txt", true, false);

        assertEquals(JobProgress.Status.PROBLEMS, progress.getStatus());
    }

    @Test
    public void testUpdateJobStoppedFailure(){
        String id = "triggerId";
        JobProgress progress = new JobProgress(id);
        progressRepository.save(progress);
        progress = progressService.updateJobStopped(id, "log.txt", false, true);

        assertEquals(JobProgress.Status.FAILED, progress.getStatus());
    }

    @Test
    public void testUpdateJobStoppedProblemsAndFailure(){
        String id = "triggerId";
        JobProgress progress = new JobProgress(id);
        progressRepository.save(progress);
        progress = progressService.updateJobStopped(id, "log.txt", true, true);

        assertEquals(JobProgress.Status.FAILED, progress.getStatus());
    }

    @Test
    public void testUpdateJobStoppedError(){
        String id = "triggerId";
        JobProgress progress = new JobProgress(id);
        progressRepository.save(progress);
        String error = "This is a very very long error message (too long if truth be told) that is over two-hundred-and-fifty-five characters long. This is to check that the error message is correctly truncated, abridged, curtailed, cut short, abbreviated, pruned of its excess and short enough to be saved in the database.";
        progress = progressService.updateJobStopped(id, error, "log.txt");

        assertEquals(JobProgress.Status.ERRORED, progress.getStatus());
        String truncatedError = "This is a very very long error message (too long if truth be told) that is over two-hundred-and-fifty-five characters long. This is to check that the error message is correctly truncated, abridged, curtailed, cut short, abbreviated, pruned of its excess a";
        assertEquals(truncatedError, progress.getLastMessage());
    }

    @Test
    public void testUpdateJobStoppedNoError(){
        String id = "triggerId";
        JobProgress progress = new JobProgress(id);
        progressRepository.save(progress);
        progress = progressService.updateJobStopped(id, null, "log.txt");

        assertEquals(JobProgress.Status.COMPLETED, progress.getStatus());
        assertNull(progress.getLastMessage());
    }
}
