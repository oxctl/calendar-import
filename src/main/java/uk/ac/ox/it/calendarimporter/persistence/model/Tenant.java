package uk.ac.ox.it.calendarimporter.persistence.model;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.validation.constraints.NotNull;
import lombok.Data;

/**
 * This is a Canvas installation, the idea is that all objects are tied to a tenant (not always
 * directly) so that we can support multi tenancy in a single instance.
 */
@Entity
@Data
public class Tenant {

  @Id
  @GeneratedValue(strategy = GenerationType.AUTO)
  private long id;

  /** This is the name for the tenant, this comes from the OAuth registration. */
  @Column(nullable = false, unique = true)
  @NotNull
  private String name;

  private String displayName;

  /** This is the endpoint for the canvas deployment, which we use for API calls back. */
  @Column(nullable = false)
  @NotNull
  private String url;

  /**
   * A URL to the CSS for this tenant, most of the time we just use the one in the LTI session. But
   * if the user doesn't have a LTI session then we can fallback to this.
   */
  private String cssUrl;
}
