package uk.ac.ox.it.calendarimporter.persistence.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import javax.persistence.Embeddable;
import javax.validation.constraints.NotNull;
import java.io.Serializable;

@Embeddable
@NoArgsConstructor
@AllArgsConstructor
@Data
public class ImportedEventIdentity implements Serializable {

    /**
     * The tenant in which the event was created. We need this so that we can remove an event.
     */
    @NotNull
    private Long tenant;

    /**
     * The ID of the Calendar event created in Canvas. This is just unique to a tenant.
     */
    @NotNull
    private Integer id;
}
