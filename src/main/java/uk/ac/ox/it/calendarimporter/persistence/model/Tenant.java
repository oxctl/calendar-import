package uk.ac.ox.it.calendarimporter.persistence.model;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;

/**
 * This is a Canvas installation, the idea is that all objects are tied to a tenant so that we can support multi
 * tenancy in a single instance.
 */
@Entity
public class Tenant {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private long id;

    /**
     * This is the name for the tenant
     */
    @Column(nullable = false)
    private String name;

    private String displayName;

    /**
     * This is the endpoint for the canvas deployment, which we use for API calls back.
     */
    @Column(nullable = false)
    private String url;

}
