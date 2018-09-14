package uk.ac.ox.it.calendarimporter.persistence.model;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.ManyToOne;

@Entity
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private long id;

    private String canvasId;

    /**
     * This is the OAuth token for the user.
     */
    private String token;

    private String email;

    /**
     * We store the locale so that the background jobs can send errors in the correct locale.
     */
    private String locale;

    @ManyToOne
    private Tenant tenant;

}
