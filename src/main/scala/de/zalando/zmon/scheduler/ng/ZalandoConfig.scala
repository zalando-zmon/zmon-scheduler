package de.zalando.zmon.scheduler.ng

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Component

import scala.beans.BeanProperty

/**
 * Created by jmussler on 4/2/15.
 */
class ZalandoControllerConfig {
  @BeanProperty var name : String = "";
  @BeanProperty var url : String = "";
  @BeanProperty var refresh : Int = 0;
  @BeanProperty var user : String = null;
  @BeanProperty var password : String = null;
}

@Component
@Profile(Array("zalando"))
@ConfigurationProperties(prefix = "zalando.checks")
class ZalandoCheckConfig {
  @BeanProperty var controller : ZalandoControllerConfig = null
}

@Component
@Profile(Array("zalando"))
@ConfigurationProperties(prefix = "zalando.alerts")
class ZalandoAlertConfig {
  @BeanProperty var controller : ZalandoControllerConfig = null
}

@Component
@Profile(Array("zalando"))
@ConfigurationProperties(prefix = "scheduler")
class SchedulerConfig {
  @BeanProperty var last_run_persist = SchedulePersistType.FILE
  @BeanProperty var check_detail_metrics = false
  @BeanProperty var thread_count = 8
  @BeanProperty var check_filter : List[Int] = List()
  @BeanProperty var entity_teams : List[String] = List()
  @BeanProperty var default_queue : String = "zmon:queue:default"

  // Mapping based on check url prefix
  @BeanProperty var queue_mapping_by_url : Map[String, String] = Map()

  // Map certrain check IDs to queue
  @BeanProperty var queue_mapping : Map[Int, String] = Map()

  // Map certrain properties to queues e.g. "dc":"gth" => "dclocal:gth"
  @BeanProperty var queue_property_mapping : List[Map[String,Map[String,String]]] = List()
}