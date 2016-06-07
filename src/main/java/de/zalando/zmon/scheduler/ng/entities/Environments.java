package de.zalando.zmon.scheduler.ng.entities;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by jmussler on 4/16/15.
 */
public class Environments {
    private final static Map<String, String> environmentMap = new HashMap<>();

    static {
        environmentMap.put("release", "release-staging");
        environmentMap.put("release-staging", "release-staging");
        environmentMap.put("be-staging", "release-staging");
        environmentMap.put("release.staging", "release-staging");
        environmentMap.put("patch", "patch-staging");
        environmentMap.put("fe-staging", "patch-staging");
        environmentMap.put("patch.staging", "patch-staging");
        environmentMap.put("patch-staging", "patch-staging");
        environmentMap.put("perf", "performance-staging");
        environmentMap.put("perf-staging", "performance-staging");
        environmentMap.put("perf.staging", "performance-staging");
        environmentMap.put("performance-staging", "performance-staging");
        environmentMap.put("live", "live");
        environmentMap.put("integration", "integration");
    }

    public static String getNormalized(String name) {
        String v = environmentMap.get(name);
        if (null == v) return name;
        return v;
    }
}
