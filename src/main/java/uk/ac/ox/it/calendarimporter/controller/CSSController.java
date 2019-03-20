package uk.ac.ox.it.calendarimporter.controller;

import edu.ksu.lti.launch.model.LtiSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.servlet.ModelAndView;
import uk.ac.ox.it.calendarimporter.service.CSSLoader;

import javax.servlet.http.HttpServletResponse;
import java.util.Collections;
import java.util.Map;

/**
 * This gets the brand URL from the LTI launch, downloads it and then builds a CSS document from it.
 * Currently unused as we just change JSON URL to a CSS one.
 */
@Controller
public class CSSController {

    @Autowired
    private CSSLoader cssLoader;

    @GetMapping(value = "/{tenant}/{context}/brand.css", produces = "text/css")
    public ModelAndView brandUrl(@PathVariable("tenant") String tenantName, @PathVariable("context") String context, LtiSession ltiSession, HttpServletResponse response) {
        // When returning a ModelAndView you have to override the content-type from HTML
        response.setHeader("Content-type", "text/css");
        if (ltiSession != null) {
            String url = ltiSession.getLtiLaunchData().getCustom().get("com_instructure_brand_config_json_url");
            Map map = cssLoader.loadJSON(url);
            return new ModelAndView("brand", Collections.singletonMap("values", map.entrySet()));
        }
        return null;
    }


}
