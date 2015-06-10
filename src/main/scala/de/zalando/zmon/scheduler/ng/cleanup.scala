package de.zalando.zmon.scheduler.ng

import de.zalando.zmon.scheduler.ng.alerts.AlertRepository
import de.zalando.zmon.scheduler.ng.checks.CheckRepository
import de.zalando.zmon.scheduler.ng.entities.{Entity, EntityChangeListener, EntityRepository}
import org.apache.commons.pool2.impl.GenericObjectPoolConfig
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.annotation.{Bean, Configuration}
import redis.clients.jedis.JedisPool
import scala.collection.JavaConversions._

import scala.collection.mutable
import scala.collection.mutable.ArrayBuffer

/**
 * Created by jmussler on 6/5/15.
 */

object SingleEntityCleanup {
  val LOG = LoggerFactory.getLogger(SingleEntityCleanup.getClass)
}

@Configuration
class SingleEntityCleanupFactory {
  @Bean
  @Autowired
  def getSingleEntityCleanup(config: SchedulerConfig, alertRepo: AlertRepository, checkRepo: CheckRepository, entityRepository: EntityRepository): SingleEntityCleanup = {
    val cleanup = new SingleEntityCleanup(config, alertRepo, checkRepo, entityRepository)
    SingleEntityCleanup.LOG.info("Registering SingleEntityCleanUp job")
    entityRepository.registerListener(cleanup)
    cleanup
  }
}

class SingleEntityCleanup(val config: SchedulerConfig, val alertRepo: AlertRepository, val checkRepo: CheckRepository, val entityRepository: EntityRepository) extends EntityChangeListener {

  val poolConfig = new GenericObjectPoolConfig()
  poolConfig.setTestOnBorrow(true)
  val pool = new JedisPool(poolConfig, config.redis_host, config.redis_port)

  def getAlerts(id : Int): mutable.MutableList[Alert] = {
    val alerts = collection.mutable.MutableList[Alert]()

    for(ad <- alertRepo.getByCheckId(id)) {
      alerts += new Alert(ad.getId, alertRepo)
    }

    alerts
  }

  def notifyEntityRemove(repo: EntityRepository, entity: Entity) : Unit = {

    var checksCleaned = 0;
    var alertsCleaned = 0;

    for(checkDef <- checkRepo.get()) {
      val check = new Check(checkDef.getId, checkRepo)
      if (check.matchEntity(entity)) {
        val viableAlerts = ArrayBuffer[Alert]()
        for (alert <- getAlerts(checkDef.getId)) {
          if (alert.matchEntity(entity)) {
            viableAlerts += alert
          }
        }

        if (!viableAlerts.isEmpty) {
          val jedis = pool.getResource
          try {
            jedis.del("zmon:checks:"+check.id+":"+entity.getId)
            checksCleaned += 1

            for(alert <- viableAlerts) {
              jedis.srem("zmon:alerts:" + alert.id, entity.getId)
              jedis.del("zmon:alerts:" + alert.id + ":" + entity.getId)
              jedis.hdel("zmon:alerts:"+ alert.id + ":entities", entity.getId)
              alertsCleaned+=1
            }
          }
          catch {
            case ex : Exception => {
              SingleEntityCleanup.LOG.error("Error during cleanup of entity: " + entity.getId, ex);
            }
          }
          finally {
            pool.returnResource(jedis)
          }
        }
      }
    }

    SingleEntityCleanup.LOG.info("Cleanup entity: " + entity.getId + " checks: " + checksCleaned + " alerts: " + alertsCleaned)
  }

  def notifyEntityChange(repo: EntityRepository, e: Entity) : Unit = {}
  def notifyEntityAdd(repo: EntityRepository, e: Entity) : Unit = {}
}
