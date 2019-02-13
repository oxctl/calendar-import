package uk.ac.ox.it.calendarimporter.controller;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import edu.ksu.lti.launch.model.LtiSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.SessionAttribute;
import org.springframework.web.servlet.ModelAndView;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

// TODO This is current uncached because of the headers.
// We need to support multiple LTI launches better at the moment a new launch overwrites the CSS for an old one.
// A better way would be to have Canvas supply the brand CSS URL in the launch
@Controller
@RequestMapping(value = "/{tenant}/{context}/brand.css")
public class CSSController {

    @Autowired
    private ObjectMapper jacksonObjectMapper;

    @GetMapping(produces = "text/css")
    public ModelAndView home(@PathVariable("tenant") String tenantName, @PathVariable("context") String context, @SessionAttribute(value = "edu.ksu.lti.launch.model.LtiSession", required = false) LtiSession ltiSession, HttpServletResponse response) {
        // We need to force this to be CSS.
        response.setHeader("Content-type", "text/css");
        if (ltiSession != null) {
            String json = ltiSession.getLtiLaunchData().getCustom_com_instructure_brand_config_json();
            TypeReference<HashMap<String, Object>> typeRef = new TypeReference<>() {};
            try {
                Map<String, Object> map = jacksonObjectMapper.readValue(json, typeRef);
                return new ModelAndView("brand", Collections.singletonMap("values", map.entrySet()));
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return null;
    }
}
