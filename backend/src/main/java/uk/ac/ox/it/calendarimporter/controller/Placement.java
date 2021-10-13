package uk.ac.ox.it.calendarimporter.controller;

import java.util.Objects;

/**
 * This encapsulates the place a tool is launched from (it's placement). Currently, we support the user navigation and
 * the course home sub-navigation.
 */
public class Placement {

	/**
	 * The different types of context. Currently we don't support account or group contexts.
	 */
	private enum ContextType {COURSE, USER}

	/**
	 * The different types of placements and the context types they map to.
	 */
	public enum PlacementType {
		USER_NAVIGATION(ContextType.USER), COURSE_HOME_SUB_NAVIGATION(ContextType.COURSE), COURSE_NAVIGATION(ContextType.COURSE);

		public final ContextType context;

		PlacementType(ContextType context) {
			this.context = context;
		}
	}

	/**
	 * The PlacementType that is being used.
	 */
	public final PlacementType type;
	/**
	 * The ID associated with the placement (eg course ID or user ID).
	 */
	public final Number id;

	private Placement(PlacementType type, Number id) {
		this.type = type;
		this.id = id;
	}

	/**
	 * This is the placement as a string understood by Canvas in it's API calls.
	 */
	public String toContext() {
		return type.context.name().toLowerCase() + "_" + id;
	}

	public static Placement toPlacement(PlacementType type, Number courseId, Number userId) {
		Objects.requireNonNull(type, "type can't be null");
		if (type.context == ContextType.COURSE) {
			Objects.requireNonNull(courseId, "courseId can't be null");
			return new Placement(type, courseId);
		}
		if (type.context == ContextType.USER) {
			Objects.requireNonNull(userId, "userId can't be null");
			return new Placement(type, userId);
		}
		throw new IllegalArgumentException("Unsupported context type: " + type.context);
	}
}
