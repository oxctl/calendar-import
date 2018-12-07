package uk.ac.ox.it.calendarimporter.jobs.ical;

import java.util.Set;

/**
 * Deals with the mapping of a iCal UUID to a Canvas calendar event ID. This uses some persistent
 * storage so that we keep the mappings.
 */
interface CalendarIDLookup {

    /**
     * @param uuid The iCal UUID
     * @return The set of canvas IDs that are found or <code>null</code> if there weren't any.
     */
    Set<Integer> getCanvasIDs(String uuid);

    /**
     * @param id The canvas calendar event ID.
     * @return The iCal UUID or <code>null</code> if there isn't one.
     */
    String getICalUUID(Integer id);

    /**
     * Adds or replaces ID mappings.
     * @param uuid the iCal UUID
     * @param ids The Canvas calendar event IDs.
     */
    void set(String uuid, Set<Integer> ids);

}
