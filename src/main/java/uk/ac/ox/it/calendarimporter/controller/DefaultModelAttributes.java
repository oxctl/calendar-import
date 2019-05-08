package uk.ac.ox.it.calendarimporter.controller;

import edu.ksu.lti.launch.model.LtiSession;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

@ControllerAdvice
public class DefaultModelAttributes {

  @Value("${calendar.common.css}")
  private String defaultCommonCss;

  @Value("${calendar.brand.json}")
  private String defaultBrandJson;

  @Value("${spring.application.name}")
  private String applicationName;

  @ModelAttribute("canvasCommonCss")
  public String canvasCommonCss(LtiSession ltiSession) {
    String canvasCss = null;
    if (ltiSession != null && ltiSession.getLtiLaunchData() != null) {
      canvasCss = ltiSession.getLtiLaunchData().getCustom().get("canvas_css_common");
    }
    if (canvasCss == null) {
      canvasCss = defaultCommonCss;
    }
    return canvasCss;
  }

  @ModelAttribute("canvasBrandCss")
  public String canvasBrandCss(LtiSession ltiSession) {
    String canvasJson = null;
    String canvasCss = null;
    if (ltiSession != null && ltiSession.getLtiLaunchData() != null) {
      canvasJson =
          ltiSession.getLtiLaunchData().getCustom().get("com_instructure_brand_config_json_url");
    }
    if (canvasJson == null) {
      canvasJson = defaultBrandJson;
    }
    // This is a fudge as at the moment a CSS version exists alongside the JS version.
    if (canvasJson != null) {
      if (canvasJson.endsWith(".json")) {
        canvasCss = canvasJson.substring(0, canvasJson.length() - ".json".length()) + ".css";
      }
    }
    return canvasCss;
  }

  @ModelAttribute("applicationName")
  public String applicationName() {
    return applicationName;
  }
}
