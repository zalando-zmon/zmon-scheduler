package de.zalando.zmon.scheduler.ng

import com.fasterxml.jackson.databind.ObjectMapper
import de.zalando.zmon.scheduler.ng.alerts.AlertRepository
import org.slf4j.LoggerFactory
import redis.clients.jedis.exceptions.JedisConnectionException
import redis.clients.jedis.{Jedis, JedisPubSub}

/**
 * Created by jmussler on 4/20/15.
 */

class Subscribers() {
}

object RedisInstantEvalSubscriber {
  val mapper = new ObjectMapper()
  val LOG = LoggerFactory.getLogger(RedisInstantEvalSubscriber.getClass)
}

class RedisInstantEvalSubscriber(val scheduler : Scheduler, val config : SchedulerConfig, val alertRepo: AlertRepository) extends JedisPubSub with Runnable {

  var jedisSub = new Jedis(config.redis_host, config.redis_port)
  var jedis = new Jedis(config.redis_host, config.redis_port)

  val threat = new Thread(this)
  threat.start()

  override def run() : Unit = {
    if(config.redis_instant_eval_pubsub!=null && !config.redis_instant_eval_pubsub.equals("")) {
      RedisInstantEvalSubscriber.LOG.info("start listening to instant evaluation")
      jedisSub.subscribe(this, config.redis_instant_eval_pubsub)
    }
  }

  override def onMessage(channel : String, message : String) : Unit = {
    RedisInstantEvalSubscriber.LOG.info("received instant eval request: " + channel + " : " + message )
    try {
      val request = jedis.hget(config.redis_instant_eval_requests, message)
      val node = RedisInstantEvalSubscriber.mapper.readTree(request)
      if(node.has("alert_definition_id")) {
        val alertId = node.get("alert_definition_id").asInt()
        scheduler.executeImmediate(alertRepo.get(alertId).getCheckDefinitionId)
      }
    }
    catch {
      case e : JedisConnectionException => jedis = new Jedis(config.redis_host, config.redis_port)
    }
  }
}

object RedisDownTimeSubscriber {
  val mapper = new ObjectMapper()
  val LOG = LoggerFactory.getLogger(RedisDownTimeSubscriber.getClass)
}

class RedisDownTimeSubscriber(val scheduler : Scheduler, val config : SchedulerConfig, val alertRepo: AlertRepository ) extends JedisPubSub with Runnable {

  var jedisSub = new Jedis(config.redis_host, config.redis_port)
  var jedis= new Jedis(config.redis_host, config.redis_port)
  val threat = new Thread(this)
  threat.start()

  override def run() : Unit = {
    if(config.redis_downtime_pubsub != null && !config.redis_downtime_pubsub.equals("")) {
      RedisDownTimeSubscriber.LOG.info("start listening to downtimes")
      jedisSub.subscribe(this, config.redis_downtime_pubsub)
    }
  }

  override def onMessage(channel : String, message : String) : Unit = {
    RedisDownTimeSubscriber.LOG.info("received downtime request: " + channel + " : " + message )
    try {
      val request = jedis.hget(config.redis_downtime_requests, message)
      val node = RedisDownTimeSubscriber.mapper.readTree(request)
      if (node.has("alert_definition_id")) {
        val alertId = node.get("alert_definition_id").asInt()
        scheduler.executeImmediate(alertRepo.get(alertId).getCheckDefinitionId)
      }
    }
    catch {
      case e : JedisConnectionException => jedis = new Jedis(config.redis_host, config.redis_port)
    }
  }
}