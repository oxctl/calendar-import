package uk.ac.ox.it.calendarimporter.service;

import edu.ksu.lti.launch.service.ToolConsumer;
import edu.ksu.lti.launch.service.ToolConsumerService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import uk.ac.ox.it.calendarimporter.persistence.model.Tenant;
import uk.ac.ox.it.calendarimporter.persistence.repo.TenantRepository;

/** Looks up ToolConsumers from the tenant. */
@Service
public class TenantToolConsumerService implements ToolConsumerService {

  @Autowired private TenantRepository tenantRepository;

  @Override
  public ToolConsumer getConsumer(String instance) {
    return tenantRepository.findByName(instance).map(TenantToolConsumer::new).orElse(null);
  }

  @Override
  public String getSecret(String instance) {
    return tenantRepository.findByName(instance).map(Tenant::getLtiSecret).orElse(null);
  }

  /** Proxy to make a Tenant appear as a ToolConsumer. */
  public static class TenantToolConsumer implements ToolConsumer {

    private final Tenant tenant;

    public TenantToolConsumer(Tenant tenant) {
      this.tenant = tenant;
    }

    @Override
    public String getInstance() {
      return tenant.getName();
    }

    @Override
    public String getName() {
      return tenant.getDisplayName();
    }

    @Override
    public String getUrl() {
      return tenant.getUrl();
    }
  }
}
