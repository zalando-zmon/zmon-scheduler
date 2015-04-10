package de.zalando.zmon.scheduler.ng

import com.codahale.metrics.{Meter, MetricRegistry}
import de.zalando.zmon.scheduler.ng.entities.Entity
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component
import org.springframework.context.annotation.{Configuration, Bean}
import redis.clients.jedis.Jedis

import scala.collection.concurrent.TrieMap
import scala.collection.mutable.ArrayBuffer

/**
 * Created by jmussler on 4/10/15.
 */


object WriterFactory {

  val LOG = LoggerFactory.getLogger(WriterFactory.getClass())

  def createWriter(schedulerConfig : SchedulerConfig, metrics: MetricRegistry): QueueWriter = {
    if(schedulerConfig.redis_host!=null && !schedulerConfig.redis_host.equals("")) {
      LOG.info(s"Creating Redis queue writer: ${schedulerConfig.redis_host} ${schedulerConfig.redis_port}")
      return new JedisQueueWriter(schedulerConfig.redis_host, schedulerConfig.redis_port, metrics)
    }
    LOG.info("Creating LOG queue writer")
    new LogQueueWriter(metrics)
  }
}

@Configuration
class QueueSelectorFactory {
  @Autowired
  @Bean
  def getSelector(config :SchedulerConfig, metrics : MetricRegistry): QueueSelector = {
    new QueueSelector(config, metrics)
  }
}

class  QueueSelector (val config : SchedulerConfig, val metrics : MetricRegistry) {
  val queueWriter = WriterFactory.createWriter(config, metrics)

  def execute(entity : Entity, check: Check, alerts : ArrayBuffer[Alert]) : Unit = {
    val command = CommandWriter.write(entity, check, alerts)
    queueWriter.exec(config.default_queue, command)
  }
}

abstract class QueueWriter(metrics : MetricRegistry) {
  private val queueMetrics = new QueueMetrics(metrics)

  def exec(queue : String, command : String ): Unit = {
    write(queue, command)
    queueMetrics.mark(queue)
  }

  protected def write(queue: String, command : String) : Unit = {}
}

class QueueMetrics(val metrics : MetricRegistry) {
  private val meters : TrieMap[String, Meter] = new TrieMap[String, Meter]()

  def mark(q : String): Unit = {
    if(meters.contains(q)) {
      meters(q).mark()
    }
    else {
      this.synchronized {
        if(meters.contains(q)) {
          meters(q).mark()
          return
        }

        val m = metrics.meter("scheduler.queue."+q)
        meters.put(q, m)
        m.mark()
      }
    }
  }
}

class JedisQueueWriter(host : String, port : Int = 6379, metrics : MetricRegistry) extends QueueWriter(metrics) {

  private val jedis = new Jedis(host, port)

  override def write(queue: String, command : String) : Unit = {
    jedis.rpush(queue, command)
  }
}

object LogQueueWriter {
  val LOG = LoggerFactory.getLogger(LogQueueWriter.getClass)
}

class LogQueueWriter(metrics : MetricRegistry) extends QueueWriter(metrics) {

  override def write(queue : String, command :String ): Unit = {
    LogQueueWriter.LOG.info("q: " + queue + " command: " + command)
  }
}
