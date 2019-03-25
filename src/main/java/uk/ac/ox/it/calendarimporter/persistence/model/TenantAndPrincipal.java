package uk.ac.ox.it.calendarimporter.persistence.model;

import java.io.Serializable;
import javax.persistence.Column;
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

  @NotNull
  @Column(length = 50)
  private String tenant;

  @NotNull
  @Column(length = 128)
  private String principal;
}
