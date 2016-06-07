package de.zalando.zmon.scheduler.ng;

import de.zalando.zmon.scheduler.ng.alerts.Parameter;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by jmussler on 3/31/15.
 */
public class CeleryBody {

    public String expires;
    public boolean utc = true;
    public final List<Object> args = new ArrayList<>(); // Order: CeleryCommandArg, CeleryAlertArg*
    public String chord = null;
    public String callbacks = null;
    public String errbacks = null;
    public String taskset = null;
    public String id;
    public int retries = 0;
    public String task = "check_and_notify";
    public final List<Long> timelimit = new ArrayList<>(2);
    public String eta;
    public final Map<String, Object> kwargs = new HashMap<>();

    public static class CeleryCommandArg {
        public int check_id;
        public Long interval;
        public Map<String, Object> entity;
        public String check_name;
        public String command;
        public double schedule_time;
    }

    public static class CeleryAlertArg {
        public String period;
        public List<String> notifications = new ArrayList<>();
        public int id;
        public String condition;
        public String name;
        public Map<String, Parameter> parameters;
        public int check_id;
        public List<Map<String, String>> entities_map;
        public String responsible_team;
        public int priority = 1;
        public String team;
    }

    public static class TrialRunCeleryCommand {
        public String check_id;
        public Long interval;
        public Map<String, Object> entity;
        public String check_name;
        public String command;
        public double schedule_time;
    }

    public static class TrialRunCeleryAlertArg {
        public String period;
        public List<String> notifications = new ArrayList<>();
        public String id;
        public String condition;
        public String name;
        public Map<String, Parameter> parameters;
        public String check_id;
        public List<Map<String, String>> entities_map;
        public String responsible_team;
        public int priority = 1;
        public String team;
    }
}

/*
{
  "expires": "2014-08-05T13:26:39.873547+00:00",
  "utc": true,
  "args": [
    {
      "check_id": 510,
      "interval": 30,
      "entity": {
        "data_center_code": "GTH",
        "host": "1.1.1.1",
        "type": "loadbalancers",
        "id": "gth-lv1-zal-de02"
      },
      "check_name": "LB Load",
      "command": "snmp(community='ZAL_readonly').load()",
      "schedule_time": 1407245139.8733
    },
    [
      {
        "period": "",
        "notifications": [

        ],
        "id": 1189,
        "condition": "capture(value['load1']) > 10 or capture(value['load5']) > 8 or capture(value['load15']) > 6",
        "name": "LB - load",
        "parameters": {

        },
        "check_id": 510,
        "entities_map": [
          {
            "type": "loadbalancers"
          }
        ],
        "responsible_team": "Platform\/System",
        "priority": 1,
        "team": "Platform\/System"
      },
      {
        "period": "",
        "notifications": [

        ],
        "id": 1190,
        "condition": "capture(value['load1']) > 5 or capture(value['load5']) > 4 or capture(value['load15']) > 4",
        "name": "LB - load warn",
        "parameters": {

        },
        "check_id": 510,
        "entities_map": [
          {
            "type": "loadbalancers"
          }
        ],
        "responsible_team": "Platform\/System",
        "priority": 3,
        "team": "Platform\/System"
      },
      {
        "period": "",
        "notifications": [

        ],
        "id": 1501,
        "condition": "capture(value['load1']) > 10 or capture(value['load5']) > 8 or capture(value['load15']) > 6",
        "name": "COP: LB - load - GTH",
        "parameters": {

        },
        "check_id": 510,
        "entities_map": [
          {
            "data_center_code": "GTH",
            "type": "loadbalancers"
          }
        ],
        "responsible_team": "Platform\/System",
        "priority": 1,
        "team": "Incident\/COP"
      }
    ]
  ],
  "chord": null,
  "callbacks": null,
  "errbacks": null,
  "taskset": null,
  "id": "check-510-gth-lv1-zal-de02-1407245139.87",
  "retries": 0,
  "task": "check_and_notify",
  "timelimit": [
    60,
    30
  ],
  "eta": null,
  "kwargs": {

  }
}
*/
