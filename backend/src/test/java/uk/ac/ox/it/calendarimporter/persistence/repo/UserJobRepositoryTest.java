package uk.ac.ox.it.calendarimporter.persistence.repo;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.orm.jpa.JpaSystemException;
import uk.ac.ox.it.calendarimporter.persistence.model.UserJob;

import java.time.Instant;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DataJpaTest
public class UserJobRepositoryTest {

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private UserJobRepository repository;

    @Test
    public void testSaveMissingFields() {
        assertThrows(
                JpaSystemException.class,
                () -> {
                    UserJob userJob = new UserJob();
                    repository.save(userJob);
                });
    }

    @Test
    public void testSaveLoad() {
        {
            UserJob userJob = new UserJob("id");
            userJob.setCreated(Instant.now());
            userJob.setUserId(123L);
            repository.save(userJob);
            entityManager.flush();
            entityManager.clear();
        }
        {
            Optional<UserJob> userJob = repository.findById("id");
            assertTrue(userJob.isPresent());
        }
    }
}
