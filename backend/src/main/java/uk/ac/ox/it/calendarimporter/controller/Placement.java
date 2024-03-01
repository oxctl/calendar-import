package uk.ac.ox.it.calendarimporter.controller;

import java.util.Objects;

/**
 * This encapsulates the place a tool is launched from (it's placement). Currently, we support the user navigation,  
 * the course home sub-navigation, course navigation and account navigation.
 */
public class Placement {

	/**
	 * The different types of context. Currently, we don't support group contexts.
	 */
	private enum ContextType {COURSE, USER, ACCOUNT}

	/**
	 * The different types of placements and the context types they map to.
	 */
	public enum PlacementType {
		COURSE_HOME_SUB_NAVIGATION(ContextType.COURSE), COURSE_NAVIGATION(ContextType.COURSE), ACCOUNT_NAVIGATION(ContextType.ACCOUNT);

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

	/**
	 * Convert variables to a placement. This needs to support both strings and numbers as some Canvas
	 * instances pass in Numbers in the JSON and some pass in Strings.
	 * 
	 * @param type The type of placement.
	 * @param courseId The course ID (string or number)
	 * @param userId The user ID (string or number)
	 * @param accountId The account ID (string or number)
	 * @return A placement.
	 */
	public static Placement toPlacement(PlacementType type, Object courseId, Object userId, Object accountId) {
		Objects.requireNonNull(type, "type can't be null");
		if (type.context == ContextType.COURSE) {
			Objects.requireNonNull(courseId, "courseId can't be null");
			return new Placement(type, Long.valueOf(courseId.toString()));
		}
		if (type.context == ContextType.USER) {
			Objects.requireNonNull(userId, "userId can't be null");
			return new Placement(type, Long.valueOf(userId.toString()));
		}
		if (type.context == ContextType.ACCOUNT) {
			Objects.requireNonNull(accountId, "accountId can't be null");
			return new Placement(type, Long.valueOf(accountId.toString()));
		}
		throw new IllegalArgumentException("Unsupported context type: " + type.context);
	}
}
