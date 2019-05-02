package uk.ac.ox.it.calendarimporter.controller;

import edu.ksu.lti.launch.model.LtiSession;

public class Utils {

  public static String toCourse(LtiSession ltiSession) {
    return "course_" + ltiSession.getCanvasCourseId();
  }

  public static String toTenant(LtiSession ltiSession) {
    return ltiSession.getApplicationName();
  }
}
