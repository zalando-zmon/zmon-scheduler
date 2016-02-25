package de.zalando.zmon.scheduler.ng

import java.util

import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.context.annotation.Profile
import org.springframework.context.annotation.Configuration
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
  @BeanProperty var token : String = null;
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
@Configuration
@ConfigurationProperties(prefix = "scheduler")
class SchedulerConfig {
  @BeanProperty var controller : ZalandoControllerConfig = null
  @BeanProperty var last_run_persist = SchedulePersistType.DISABLED
  @BeanProperty var check_detail_metrics = false
  @BeanProperty var thread_count = 8

  @BeanProperty var check_filter : java.util.List[Integer] = new util.ArrayList[Integer]()

  @BeanProperty var entity_skip_on_field: String = null
  @BeanProperty var entity_base_filter: java.util.List[util.Map[String,String]] = null
  @BeanProperty var entity_base_filter_str: String = null

  @BeanProperty var default_queue : String = "zmon:queue:default"
  @BeanProperty var trial_run_queue : String = "zmon:queue:default"
  @BeanProperty var enable_global_entity : Boolean = false

  @BeanProperty var task_writer_type = TaskWriterType.ARRAY_LIST

  @BeanProperty var redis_host : String = ""
  @BeanProperty var redis_port : Int = 6379

  @BeanProperty var urls_without_rest : Boolean = false

  @BeanProperty var oauth2_access_token_url : String = null
  @BeanProperty var oauth2_scopes: java.util.List[String] = null
  @BeanProperty var oauth2_static_token: String = ""

  // the entity service provides entities to run checks against ( it is part of the controller )
  @BeanProperty var entity_service_url: String = null
  @BeanProperty var entity_service_user: String = null
  @BeanProperty var entity_service_password: String = null

  // Using the zmon controller as a source for alerts and checks
  @BeanProperty var controller_url: String = null
  @BeanProperty var controller_user: String = null
  @BeanProperty var controller_password: String = null

  @BeanProperty var enable_downtime_redis_sub : Boolean = true
  @BeanProperty var redis_downtime_pubsub : String = ""
  @BeanProperty var redis_downtime_requests : String = ""

  @BeanProperty var enable_instant_eval : Boolean = true
  @BeanProperty var redis_instant_eval_pubsub : String = ""
  @BeanProperty var redis_instant_eval_requests : String = ""

  // Remote/AWS support
  // used to enable polling for instant eval via http with DC id
  @BeanProperty var instant_eval_forward : Boolean = true
  @BeanProperty var instant_eval_http_url : String = null

  // used to enable polling for trial runs via http with DC id
  @BeanProperty var trial_run_forward : Boolean = true
  @BeanProperty var trial_run_http_url : String = null

  @BeanProperty var enable_trail_run : Boolean = true
  @BeanProperty var redis_trialrun_pubsub : String = "zmon:trial_run:pubsub"
  @BeanProperty var redis_trialrun_requests : String = "zmon:trial_run:requests"

  @BeanProperty var dummy_cities : String = null // "dummy_data/cities.json"

  // Mapping based on check url prefix
  @BeanProperty var queue_mapping_by_url : java.util.Map[String, String] = new java.util.HashMap[String,String]()

  // Map certrain check IDs to queue
  @BeanProperty var queue_mapping : java.util.Map[String, java.util.List[Integer]] = new java.util.HashMap[String, java.util.List[Integer]]()

  // Map certrain properties to queues e.g. "dc":"gth" => "dclocal:gth"
  @BeanProperty var queue_property_mapping : java.util.Map[String,java.util.List[java.util.Map[String,String]]] = new util.HashMap[String,java.util.List[java.util.Map[String,String]]]()

  @BeanProperty var task_serializer : TaskSerializerType = TaskSerializerType.COMPRESSED_NESTED

  @BeanProperty var entity_properties_key : String = null

  @BeanProperty var check_min_interval : Long = 15L

  @Value("${server.port}")
  @BeanProperty var server_port : String = null
}