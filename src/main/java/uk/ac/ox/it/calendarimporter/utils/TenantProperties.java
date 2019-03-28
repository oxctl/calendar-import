package uk.ac.ox.it.calendarimporter.utils;

import java.util.ArrayList;
import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import uk.ac.ox.it.calendarimporter.persistence.model.Tenant;

/** This is used to load tenants at startup. */
@Component
@ConfigurationProperties("calendar")
public class TenantProperties {

  private final List<Tenant> tenants = new ArrayList<>();

  public List<Tenant> getTenants() {
    return tenants;
  }
}
