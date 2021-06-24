package uk.ac.ox.it.calendarimporter.persistence.model;

import lombok.Data;
import lombok.NoArgsConstructor;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Index;
import javax.persistence.Table;
import java.time.Instant;

/**
 * Allows the linking of a user to a job. We have this rather than encoding the data in the quartz
 * tables as quartz doesn't allow much control over the returned data, for example we can't perform
 * any paging on the returned data. In the long run we probably want to link a job to a context
 * instead so that everyone who uses the tool will see the jobs that have run.
 */
@Data()
@NoArgsConstructor
@Table(indexes = {@Index(name = "user_id_created_idx", columnList = "user_id,created")})
@Entity
public class UserJob {

    @Id
    // The trigger ID needs to be unique so we can just re-use it, this links to the JobProgress
    private String triggerId;

    @Column(name = "user_id")
    private Long userId;

    // We need to be able to sort the jobs in the DB.
    private Instant created;

    public UserJob(String triggerId) {
        setTriggerId(triggerId);
    }
}
