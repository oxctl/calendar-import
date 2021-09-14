package uk.ac.ox.it.calendarimporter.beans;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.validation.constraints.NotBlank;

@AllArgsConstructor
@NoArgsConstructor
@Data
public class TenantAndContext {

    @NotBlank
    private String tenant;
    @NotBlank
    private String context;
}
