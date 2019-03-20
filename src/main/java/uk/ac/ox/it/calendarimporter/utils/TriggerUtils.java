package uk.ac.ox.it.calendarimporter.utils;

import org.quartz.TriggerKey;

public class TriggerUtils {

    public static TriggerKey toTriggerKey(String id, String tenant, String username) {
        // This isn't strictly needed but makes it easier to debug as we don't need to do any de-referencing
        String triggerGroup = tenant+":"+username;
        return new TriggerKey(id, triggerGroup);
    }

}
