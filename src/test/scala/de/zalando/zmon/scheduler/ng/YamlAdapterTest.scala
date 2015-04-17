package de.zalando.zmon.scheduler.ng

import com.codahale.metrics.MetricRegistry
import de.zalando.zmon.scheduler.ng.alerts.{AlertRepository, YamlAlertSource, AlertSourceRegistry}
import de.zalando.zmon.scheduler.ng.checks.{CheckRepository, CheckSourceRegistry, YamlCheckSource}
import de.zalando.zmon.scheduler.ng.entities.{EntityRepository, EntityAdapterRegistry, YamlEntityAdapter}
import org.scalatest._

/**
 * Created by jmussler on 4/17/15.
 */
class YamlAdapterTest extends FlatSpec with Matchers {
  val ea = new YamlEntityAdapter("yaml-entites","dummy_data/entities.yaml")
  val cs = new YamlCheckSource("yaml-checks","dummy_data/checks.yaml")
  val as = new YamlAlertSource("yaml-alerts","dummy_data/alerts.yaml")

  implicit val metrics = new MetricRegistry()

  val er = new EntityAdapterRegistry(metrics)
  er.register(ea)

  val cr = new CheckSourceRegistry(metrics)
  cr.register(cs)

  val ar = new AlertSourceRegistry(metrics)
  ar.register(as)

  val checkRepo = new CheckRepository(cr)
  val alertRepo = new AlertRepository(ar)
  val entityRepo = new EntityRepository(er)

  implicit val config = new SchedulerConfig()
  implicit val schedulerMetrics = new SchedulerMetrics()

  val writer = WriterFactory.createWriter(config, metrics)
  val selector = new QueueSelector(writer)

  val check1 = new ScheduledCheck(1, selector, checkRepo, alertRepo, entityRepo)

  "Entities" should "contain 2 entites" in {
    ea.getCollection.size() should be (2)
  }

  "Checks" should "contain 1 checks" in {
    cs.getCollection.size() should be (1)
  }

  "Alerts" should "contain 2 alerts" in {
    as.getCollection.size() should be (2)
  }

  "Check 1" should "match 2 entities" in {
    check1.runCheck(true).size should be (2)
  }

}
