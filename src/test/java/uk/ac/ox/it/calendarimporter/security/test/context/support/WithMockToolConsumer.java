package uk.ac.ox.it.calendarimporter.security.test.context.support;

import java.lang.annotation.Documented;

/**
 * The tool consumer that a LtiUser is associated with.
 * @see edu.ksu.lti.launch.service.ToolConsumer
 */
@Documented
public @interface WithMockToolConsumer {

    /**
     * The instance. This is the unique name for the instance.
     * @return
     */
    String instance() default "instance";

    /**
     * The display name for the instance.
     * @return
     */
    String name() default "name";

    /**
     * The URL of the instance.
     * @return
     */
    String url() default "https://example.com";
}
