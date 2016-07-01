package de.zalando.zmon.scheduler.ng.scheduler;

import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by jmussler on 30.06.16.
 */
public class SchedulePersister implements Runnable {

    private final static ObjectMapper mapper = new ObjectMapper();

    public static Map<Integer, Long> loadSchedule() {
        return new HashMap<>();
    }

    public static void writeSchedule(Map<Integer, Long> schedule) {
        return;
    }

    @Override
    public void run() {

    }
}
