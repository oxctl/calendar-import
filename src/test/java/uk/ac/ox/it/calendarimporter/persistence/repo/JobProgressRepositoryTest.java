package uk.ac.ox.it.calendarimporter.persistence.repo;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import uk.ac.ox.it.calendarimporter.persistence.model.JobProgress;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertTrue;

@DataJpaTest
public class JobProgressRepositoryTest {

  @Autowired private TestEntityManager entityManager;

  @Autowired private JobProgressRepository repository;

  @Test
  public void testSaveLoad() {
    {
      JobProgress jobProgress = new JobProgress("id");
      repository.save(jobProgress);
    }
    {
      Optional<JobProgress> jobProgress = repository.findById("id");
      assertTrue(jobProgress.isPresent());
    }
  }
}
