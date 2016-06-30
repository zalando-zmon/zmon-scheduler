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

/* Unnecessary and not used

object SchedulePersister {

  val mapper = new ObjectMapper with ScalaObjectMapper
  mapper.registerModule(DefaultScalaModule)

  def loadSchedule(): Map[Integer, Long] = {
    try {
      mapper.readValue(new File("schedule.json"), new TypeReference[Map[Integer, Long]] {})
    }
    catch {
      case e: Exception => {
        return Map[Integer, Long]()
      }
    }
  }

  def writeSchedule(schedule: collection.concurrent.Map[Integer, Long]) = {
    if (schedule.size > 0) {
      mapper.writeValue(new File("schedule.json"), schedule)
    }
  }
}

class SchedulePersister(val scheduledChecks: scala.collection.concurrent.TrieMap[Integer, ScheduledCheck]) extends Runnable {
  override def run(): Unit = {
    SchedulePersister.writeSchedule(scheduledChecks.filter(_._2.getLastRun > 0).map(x => (x._1, x._2.getLastRun)))
  }
}

 */