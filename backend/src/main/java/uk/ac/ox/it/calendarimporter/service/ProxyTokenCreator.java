package uk.ac.ox.it.calendarimporter.service;

import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.JWSSigner;
import com.nimbusds.jose.crypto.MACSigner;
import com.nimbusds.jose.util.Base64URL;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import edu.ksu.canvas.oauth.NonRefreshableOauthToken;
import edu.ksu.canvas.oauth.OauthToken;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import uk.ac.ox.it.calendarimporter.persistence.model.Tenant;

import jakarta.annotation.PostConstruct;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.Map;

/**
 * We want to store refreshed tokens. This has the problem that if we allow tokens to be used from
 * live on beta/test. Then when the tokens get updated live will break.
 */
@Service
public class ProxyTokenCreator implements CanvasTokenCreator {

    public static final String PROTOCOL_SEP = "://";
    
    private final Logger log = LoggerFactory.getLogger(ProxyTokenCreator.class);
    
    @Value("https://${hostname:localhost:8443}")
    private String issuer;
    
    @PostConstruct
    public void init() {
        log.info("Using {} to sign tokens to the Canvas Proxy.", issuer);
    }

    /**
     * Just removes the local part from a URL. This is just needed so we don't need more
     * configuration. In the long run we should update the canvas-api library to take the full token
     * URL.
     *
     * @param url The full URL for the OAuth token refresh.
     * @return The URL without a local part (eg just return protocol, hostname and port).
     */
    String removeLocalPart(String url) {
        int hostnameStart = url.indexOf(PROTOCOL_SEP);
        if (hostnameStart == -1) {
            throw new IllegalArgumentException("Failed to find " + PROTOCOL_SEP + " in " + url);
        }
        int endHostname = url.indexOf("/", hostnameStart + PROTOCOL_SEP.length());
        if (endHostname == -1) {
            return url;
        }
        return url.substring(0, endHostname);
    }

    @Override
    public OauthToken getToken(Tenant tenant, String subject) throws JOSEException {
        String ltiClientId = tenant.getLtiClientId();
        JWTClaimsSet claims =
                new JWTClaimsSet.Builder()
                        .issuer(issuer)
                        .audience(ltiClientId)
                        .subject(subject)
                        .notBeforeTime(Date.from(Instant.now()))
                        .issueTime(Date.from(Instant.now()))
                        .expirationTime(Date.from(Instant.now().plus(1, ChronoUnit.HOURS)))
                        // This is so that the proxy knows where to contact
                        .claim(
                                "https://purl.imsglobal.org/spec/lti/claim/custom",
                                Map.of("canvas_api_base_url", tenant.getUrl()))
                        .build();
        byte[] secret = Base64URL.from(tenant.getProxyHmacSecret()).decode();
        JWSSigner signer = new MACSigner(secret);

        SignedJWT signedJWT = new SignedJWT(new JWSHeader(JWSAlgorithm.HS256), claims);
        signedJWT.sign(signer);
        String jwt = signedJWT.serialize();

        // This is a token for the proxy, not for Canvas.
        OauthToken token = new NonRefreshableOauthToken(jwt);
        return token;
    }
}
