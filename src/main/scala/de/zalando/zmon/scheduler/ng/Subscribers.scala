package de.zalando.zmon.scheduler.ng

import com.fasterxml.jackson.core.`type`.TypeReference
import com.fasterxml.jackson.databind.ObjectMapper
import de.zalando.zmon.scheduler.ng.alerts.AlertRepository
import org.apache.commons.pool2.impl.GenericObjectPoolConfig
import org.slf4j.LoggerFactory
import redis.clients.jedis.exceptions.JedisConnectionException
import redis.clients.jedis.{JedisPool, Jedis, JedisPubSub}

/**
 * Created by jmussler on 4/20/15.
 */

object RedisSubscriber {
  val LOG = LoggerFactory.getLogger(RedisSubscriber.getClass)
}

abstract class RedisSubscriber(val host : String, val port : Int,  val pubSubKey : String) extends JedisPubSub with Runnable {
  RedisSubscriber.LOG.info("Subscribing on host: " + host + " port: " + port)

  val poolConfig = new GenericObjectPoolConfig()
  poolConfig.setTestOnBorrow(true)

  val jedisPool = new JedisPool(poolConfig, host, port)
  var jedisSub = new Jedis(host, port)

  val thread = new Thread(this)
  thread.start()

  override def run() : Unit = {
    while ( true ) {
      try {
        if (pubSubKey != null && !pubSubKey.equals("")) {
          RedisSubscriber.LOG.info("start listening to " + pubSubKey)
          jedisSub.subscribe(this, pubSubKey)
        }
      }
      catch {
        case e: JedisConnectionException => jedisSub = new Jedis(host, port)
      }
    }
  }

  def handleMessage(jedis : Jedis, channel: String, message: String) : Unit

  override def onMessage(channel : String, message : String) : Unit = {
    RedisSubscriber.LOG.info("received channel: " + channel + " message: " + message )
    var jedis = jedisPool.getResource()
    try {
      handleMessage(jedis, channel, message)
    }
    catch {
      case e : JedisConnectionException => {
        jedisPool.returnBrokenResource(jedis)
        jedis = null
      }
      case e: Exception => RedisSubscriber.LOG.error("Error in subscription to " + pubSubKey, e)
    }
    finally {
      if(null!=jedis) {
        jedisPool.returnResource(jedis)
      }
    }
  }
}

object RedisInstantEvalSubscriber {
  val mapper = new ObjectMapper()
  val LOG = LoggerFactory.getLogger(RedisInstantEvalSubscriber.getClass)
}

class RedisInstantEvalSubscriber(val scheduler : Scheduler, val config : SchedulerConfig, val alertRepo: AlertRepository, val forwarder: InstantEvalForwarder) extends RedisSubscriber(config.redis_host, config.redis_port, config.redis_instant_eval_pubsub) {

  override def handleMessage(jedis: Jedis, channel : String, message : String) : Unit = {
    RedisInstantEvalSubscriber.LOG.info("received instant eval request: " + channel + " : " + message )

    val request = jedis.hget(config.redis_instant_eval_requests, message)
    val node = RedisInstantEvalSubscriber.mapper.readTree(request)

    if(node.has("alert_definition_id")) {
      val alertId = node.get("alert_definition_id").asInt()
      scheduler.executeImmediate(alertRepo.get(alertId).getCheckDefinitionId)
      forwarder.forwardRequest(alertRepo.get(alertId).getCheckDefinitionId)
    }
  }
}

object SubscriberMapper {
  val mapper = new ObjectMapper()
}

class RedisDownTimeSubscriber(val scheduler : Scheduler, val config : SchedulerConfig, val alertRepo: AlertRepository ) extends RedisSubscriber(config.redis_host, config.redis_port, config.redis_downtime_requests) {

  override def handleMessage(jedis : Jedis, channel: String, message : String ): Unit = {
    val request = jedis.hget(config.redis_downtime_requests, message)
    val node = SubscriberMapper.mapper.readTree(request)
    if (node.has("alert_definition_id")) {
      val alertId = node.get("alert_definition_id").asInt()
      scheduler.executeImmediate(alertRepo.get(alertId).getCheckDefinitionId)
    }
  }
}

class TrialRunSubscriber(val scheduler : Scheduler, val config: SchedulerConfig, val forwarder : TrialRunForwarder) extends RedisSubscriber(config.redis_host, config.redis_port, config.redis_trialrun_pubsub) {
  def handleMessage(jedis : Jedis, channel : String, message : String) : Unit = {
    val requestJson = jedis.hget(config.redis_trialrun_requests, message)
    val request : TrialRunRequest = SubscriberMapper.mapper.readValue(requestJson, new TypeReference[TrialRunRequest] {})
    scheduler.scheduleTrialRun(request)
    forwarder.forwardRequest(request)
  }
}