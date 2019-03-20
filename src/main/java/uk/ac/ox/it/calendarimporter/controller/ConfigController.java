package uk.ac.ox.it.calendarimporter.controller;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;
import uk.ac.ox.it.calendarimporter.xml.CartridgeLtiLink;

import javax.servlet.http.HttpServletRequest;
import java.util.ArrayList;
import java.util.List;

import static uk.ac.ox.it.calendarimporter.xml.CartridgeLtiLink.*;

/**
 * Just provides a simple dynamic XML file for LTI tool registration. This allows XML configuration to be set
 * for any host without editing as it pulls the host from the current request.
 */
@RestController
public class ConfigController {

    @Value("${spring.application.name}")
    private String applicationName;

    @Value("${spring.application.description}")
    private String description;

    @Value("${spring.lti.launch.path}")
    private String ltiLaunchPath;

    @GetMapping(path = "/config.xml", produces = "application/xml")
    public CartridgeLtiLink getConfig(HttpServletRequest request) {
        CartridgeLtiLink link = new CartridgeLtiLink();
        link.setTitle(applicationName);
        String launchUrl = ServletUriComponentsBuilder.fromContextPath(request).path(ltiLaunchPath).toUriString();
        link.setLaunchUrl(launchUrl);
        link.setDescription(description);
        {
            List<Property> propertyList = new ArrayList<>();
            addProperty(propertyList, "canvas_css_common", "$Canvas.css.common");
            addProperty(propertyList, "com_instructure_brand_config_json_url", "$com.instructure.brandConfigJSON.url");
            link.setProperties(propertyList);
        }

        Extensions extensions = new Extensions();
        extensions.setPlatform("canvas.instructure.com");
        {
            List<Property> propertyList = new ArrayList<>();
            addProperty(propertyList, "privacy_level", "public");
            extensions.setProperties(propertyList);
        }
        Options options = new Options();
        options.setName("course_navigation");
        {
            List<Property> propertyList = new ArrayList<>();
            addProperty(propertyList, "enabled", "true");
            addProperty(propertyList, "default", "disabled");
            addProperty(propertyList, "visibility", "admins");
            options.setProperties(propertyList);
        }

        List<Options> extensionOptions = new ArrayList<>();
        extensionOptions.add(options);
        extensions.setOptions(extensionOptions);

        link.setExtensions(extensions);

        return link;
    }

    private static void addProperty(List<Property> propertyList, String name, String value) {
        Property property = new Property();
        property.setName(name);
        property.setValue(value);
        propertyList.add(property);
    }

}
