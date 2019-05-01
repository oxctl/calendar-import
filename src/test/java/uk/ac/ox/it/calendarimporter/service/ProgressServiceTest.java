package uk.ac.ox.it.calendarimporter.service;

import static org.junit.Assert.*;

import java.util.Optional;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.junit4.SpringRunner;
import uk.ac.ox.it.calendarimporter.persistence.model.JobProgress;

@RunWith(SpringRunner.class)
@DataJpaTest
@Import(ProgressService.class)
public class ProgressServiceTest {

  @Autowired private ProgressService progressService;

  @Autowired private TestEntityManager entityManager;

  @Test
  public void testDoesNotExist() {
    Optional<JobProgress> byId = progressService.findById("does-not-exist");
    assertFalse(byId.isPresent());
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

  @Test(expected = IllegalArgumentException.class)
  public void testOutPercentage() {
    progressService.updateJob("triggerId", "Status", -1);
  }

  @Test(expected = IllegalStateException.class)
  public void testUpdateCompleted() {
    progressService.updateJobStopped("triggerId", null, null);
    progressService.updateJob("triggerId", "Status", 1);
  }

  @Test(expected = IllegalStateException.class)
  public void testUpdateErrored() {
    progressService.updateJobStopped("triggerId", "Error", null);
    progressService.updateJob("triggerId", "Status", 1);
  }

  @Test(expected = IllegalArgumentException.class)
  public void tesUpdateNoMessage() {
    progressService.updateJob("triggerId", null, 10);
  }
}
