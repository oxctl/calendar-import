package uk.ac.ox.it.calendarimporter.beans;

import javax.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@AllArgsConstructor
@NoArgsConstructor
@Data
public class TenantAndContext {

  @NotBlank private String tenant;
  @NotBlank private String context;
}
