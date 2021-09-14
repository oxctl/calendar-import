package uk.ac.ox.it.calendarimporter.persistence.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.persistence.Column;
import javax.persistence.Embeddable;
import javax.validation.constraints.NotNull;
import java.io.Serializable;

@Embeddable
@NoArgsConstructor
@AllArgsConstructor
@Data
public class TenantAndPrincipal implements Serializable {

    @NotNull
    @Column(length = 50)
    private String tenant;

    @NotNull
    @Column(length = 128)
    private String principal;
}
