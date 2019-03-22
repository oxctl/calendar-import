package uk.ac.ox.it.calendarimporter.persistence.repo;

import org.springframework.data.repository.CrudRepository;
import uk.ac.ox.it.calendarimporter.persistence.model.JobProgress;

public interface JobProgressRepository extends CrudRepository<JobProgress, String> {}
