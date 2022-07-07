package uk.ac.ox.it.calendarimporter.service;

import com.nimbusds.jose.JOSEException;
import edu.ksu.canvas.oauth.OauthToken;
import uk.ac.ox.it.calendarimporter.persistence.model.Tenant;

public interface CanvasTokenCreator {
    /**
     * This retrieves a token to talk to the Canvas API for a user in a tenant.
     * @param tenant The tenant we are talking to.
     * @param subject The user subject.
     * @return A token to use against the Canvas API.
     */
    OauthToken getToken(Tenant tenant, String subject) throws JOSEException;
}
