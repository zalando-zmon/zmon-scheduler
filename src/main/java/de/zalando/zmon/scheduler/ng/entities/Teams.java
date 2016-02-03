package de.zalando.zmon.scheduler.ng.entities;

/**
 * Created by jmussler on 4/16/15.
 */
public class Teams {
    public static String getNormalizedTeam(String team) {
        if (null == team) {
            return "";
        }

        if (team.startsWith("Zalando/Technology/")) {
            return team.substring(19);
        }

        return team;
    }
}
