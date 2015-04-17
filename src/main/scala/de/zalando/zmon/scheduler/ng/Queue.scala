package de.zalando.zmon.scheduler.ng

import com.codahale.metrics.{Meter, MetricRegistry}
import de.zalando.zmon.scheduler.ng.entities.Entity
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.annotation.{Configuration, Bean}
import redis.clients.jedis.{JedisPoolConfig, JedisPool, Jedis}

import scala.collection.concurrent.TrieMap
import scala.collection.mutable
import scala.collection.mutable.ArrayBuffer

import scala.collection.JavaConversions._

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
    new QueueSelector()(config, metrics)
  }
}

abstract class Selector() {
  def getQueue()(implicit entity : Entity, check: Check, alerts : ArrayBuffer[Alert]) : String = null
}

class RepoSelector(implicit val config : SchedulerConfig ) extends Selector {
  override def getQueue()(implicit entity : Entity, check: Check, alerts : ArrayBuffer[Alert]) : String = {
    for((k, v) <- config.queue_mapping_by_url) {
      if(check.getCheckDef().getSourceUrl().startsWith(k)) {
        return v
      }
    }
    null
  }
}

class HardCodedSelector(implicit val config: SchedulerConfig) extends Selector {

  val mapById = new collection.mutable.HashMap[Integer, String]()
  for((queue,ids) <- config.queue_mapping) {
    for(id <- ids) {
      mapById.put(id, queue)
    }
  }

  override def getQueue()(implicit entity : Entity, check: Check, alerts : ArrayBuffer[Alert]) : String = {
    mapById.getOrElse(check.id, null)
  }
}

class PropertyQueueSelector(implicit val config: SchedulerConfig) extends Selector {
  override def getQueue()(implicit entity : Entity, check: Check, alerts : ArrayBuffer[Alert]) : String = {
    for((q, fList) <- config.getQueue_property_mapping) {
      for(f <- fList) {
        if(filter.overlaps(f, entity.getFilterProperties)) {
          return q
        }
      }
    }
    null
  }
}

class  QueueSelector (implicit val config : SchedulerConfig, val metrics : MetricRegistry) {
  val queueWriter = WriterFactory.createWriter(config, metrics)

  val selectors : List[Selector]= List(new RepoSelector(), new HardCodedSelector(), new PropertyQueueSelector())

  def execute()(implicit entity : Entity, check: Check, alerts : ArrayBuffer[Alert]) : Unit = {
    val command = CommandWriter.write(entity, check, alerts)

    var queue : String = null
    for(s <- selectors) {
      if(null != queue) {
        queue = s.getQueue()
      }
    }

    if(null == queue) {
      queue = config.default_queue
    }

    queueWriter.exec(config.default_queue, command)
  }
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

abstract class QueueWriter(metrics : MetricRegistry) {
  private val queueMetrics = new QueueMetrics(metrics)

  def exec(queue : String, command : String ): Unit = {
    write(queue, command)
    queueMetrics.mark(queue)
  }

  protected def write(queue: String, command : String) : Unit = {}
}

class ArrayQueueWriter(metrics : MetricRegistry) extends QueueWriter(metrics) {
  val tasks = new mutable.HashMap[String, List[String]]()

  override def write(queue : String, command : String): Unit = {
    this.synchronized {

    }
  }
}

class JedisQueueWriter(host : String, port : Int = 6379, metrics : MetricRegistry) extends QueueWriter(metrics) {

  var jc = new JedisPoolConfig()
  jc.setMinIdle(8)

  private val jedisPool = new JedisPool(jc, host, port)

  override def write(queue: String, command : String) : Unit = {
    val jedis = jedisPool.getResource
    try {
      jedis.rpush(queue, command)
    }
    finally {
      jedisPool.returnResource(jedis)
    }
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
