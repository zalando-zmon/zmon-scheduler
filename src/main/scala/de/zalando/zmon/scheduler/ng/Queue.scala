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

abstract class QueueWriter(metrics : MetricRegistry) {
  private val queueMetrics = new QueueMetrics(metrics)

  def exec(queue : String, command : Array[Byte] ): Unit = {
    write(queue, command)
    queueMetrics.mark(queue)
    queueMetrics.incThroughput(command.length)
  }

  protected def write(queue: String, command : Array[Byte]) : Unit = {}
}

class ArrayQueueWriter(metrics : MetricRegistry) extends QueueWriter(metrics) {
  val tasks = new mutable.HashMap[String, ArrayBuffer[Array[Byte]]]()

  override def write(queue : String, command : Array[Byte]): Unit = {
    this.synchronized {
      var l = tasks.getOrElse(queue,null)
      if(null==l) {
        l = new ArrayBuffer[Array[Byte]]()
        tasks.put(queue, l)
      }
      l.add(command)
    }
  }

  def getTasks(queue : String): ArrayBuffer[Array[Byte]] = {
    tasks.getOrElse(queue, null)
  }
}

class JedisQueueWriter(host : String, port : Int = 6379, metrics : MetricRegistry) extends QueueWriter(metrics) {

  var jc = new JedisPoolConfig()
  jc.setMinIdle(8)

  private val jedisPool = new JedisPool(jc, host, port)

  override def write(queue: String, command : Array[Byte]) : Unit = {
    val jedis = jedisPool.getResource
    try {
      jedis.rpush(queue.getBytes, command)
    }
    finally {
      jedis.close()
    }
  }
}

object LogQueueWriter {
  val LOG = LoggerFactory.getLogger(LogQueueWriter.getClass)
}

class LogQueueWriter(metrics : MetricRegistry) extends QueueWriter(metrics) {

  override def write(queue : String, command : Array[Byte] ): Unit = {
    LogQueueWriter.LOG.info("q: " + queue + " command: " + command)
  }
}

abstract class Selector() {
  def getQueue()(implicit entity : Entity, check: Check, alerts : ArrayBuffer[Alert]) : String = null
}

class RepoSelector(implicit val config : SchedulerConfig ) extends Selector {
  override def getQueue()(implicit entity : Entity, check: Check, alerts : ArrayBuffer[Alert]) : String = {
    if(null==check) return null

    for((k, v) <- config.queue_mapping_by_url) {

      if(null != check.getCheckDefinition().getSourceUrl() && check.getCheckDefinition().getSourceUrl().startsWith(k)) {
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
    if(null == check) return null
    mapById.getOrElse(check.getId(), null)
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

class QueueSelector(writer : QueueWriter)(implicit val config : SchedulerConfig, val metrics : MetricRegistry) {
  var propertySelector = new PropertyQueueSelector()
  val selectors : List[Selector] = List(new RepoSelector(), new HardCodedSelector(), propertySelector)
  val serializer = new CommandSerializer(config.task_serializer)

  def execute(command : Array[Byte], targetQueue : String = null)(implicit entity : Entity) : Unit = {
    var queue = targetQueue
    if(null==queue) {
      queue = propertySelector.getQueue()(entity, null, null)
    }

    if(null == queue) {
      queue = config.default_queue
    }

    writer.exec(queue, command)
  }

  def execute()(implicit entity : Entity, check: Check, alerts : ArrayBuffer[Alert], scheduledTime : Long) : Unit = {
    val command = serializer.write(entity, check, alerts, scheduledTime)

    var queue : String = null

    for(s <- selectors) {
      if(null == queue) {
        queue = s.getQueue()
      }
    }

    if(null == queue) {
      queue = config.default_queue
    }

    writer.exec(queue, command)
  }
}

class QueueMetrics(val metrics : MetricRegistry) {
  private val meters : TrieMap[String, Meter] = new TrieMap[String, Meter]()
  private val throughputMeter = metrics.meter("scheduler.byte-throughput")

  def incThroughput(l : Int) : Unit = {
    throughputMeter.mark(l)
  }

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

