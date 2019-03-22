package uk.ac.ox.it.calendarimporter.persistence.model;

import java.io.Serializable;
import javax.persistence.Embeddable;
import javax.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Embeddable
@NoArgsConstructor
@AllArgsConstructor
@Data
public class TenantAndPrincipal implements Serializable {

  @NotNull private String tenant;

  @NotNull private String principal;
}
