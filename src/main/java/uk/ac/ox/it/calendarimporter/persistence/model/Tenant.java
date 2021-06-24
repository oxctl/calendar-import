package uk.ac.ox.it.calendarimporter.persistence.model;

import lombok.Data;
import lombok.ToString;
import org.hibernate.annotations.Cache;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.validation.constraints.NotNull;

import static org.hibernate.annotations.CacheConcurrencyStrategy.NONSTRICT_READ_WRITE;

/**
 * This is a Canvas installation, the idea is that all objects are tied to a tenant (not always
 * directly) so that we can support multi tenancy in a single instance.
 */
@Entity
@Data
@Cache(usage = NONSTRICT_READ_WRITE)
public class Tenant {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private long id;

    /**
     * This is the name for the tenant, this comes from the OAuth registration. We don't want to allow
     * people to update this value because it's embedded in other fields (eg the prefix for
     * principals).
     */
    @Column(nullable = false, unique = true, updatable = false)
    @NotNull
    private String name;

    /**
     * The name that this tenant is called.
     */
    private String displayName;

    /**
     * This is the endpoint for the canvas deployment, which we use for API calls back.
     */
    @Column(nullable = false)
    @NotNull
    private String url;

    @ToString.Exclude
    private String ltiSecret;

    private String oauth2Id;

    @ToString.Exclude
    private String oauth2Secret;

    // The LTI 1.3 Client ID. This will be in the audence of the JWT.
    @Column(unique = true)
    private String ltiClientId;

    @ToString.Exclude
    private String proxyHmacSecret;

    // The Host to use for proxying requests to Canvas.
    private String proxyHost;
}
