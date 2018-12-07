package uk.ac.ox.it.calendarimporter.persistence.model;

import lombok.Builder;
import lombok.Data;

import javax.persistence.Entity;
import javax.persistence.Id;
import javax.validation.constraints.NotNull;
import java.time.Instant;

@Data
@Builder
@Entity
public class JobProgress {

    public enum Status {QUEUED, RUNNING, COMPLETED, FAILED}

    @Id
    @NotNull
    /**
     * This is the trigger ID.
     */
    private final String id;

    private Instant started;
    private Instant completed;
    private String lastMessage;
    private Status status;
    private int percentage;

    public void update(int percentage) {
        this.percentage = percentage;
    }
}
