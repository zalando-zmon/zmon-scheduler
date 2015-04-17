package de.zalando.zmon.scheduler.ng

import de.zalando.zmon.scheduler.ng.checks.YamlCheckSource
import de.zalando.zmon.scheduler.ng.entities.YamlEntityAdapter
import org.scalatest._

/**
 * Created by jmussler on 4/17/15.
 */
class YamlAdapterTest extends FlatSpec with Matchers {
  val ea = new YamlEntityAdapter("yaml-entites","dummy_data/entities.yaml")
  val cs = new YamlCheckSource("yaml-checks","dummy_data/checks.yaml")
  val as = new YamlCheckSource("yaml-alerts","dummy_data/alerts.yaml")

  "Entities" should "contain 2 entites" in {
    ea.getCollection.size() should be (2)
  }

  "Checks" should "contain 1 checks" in {
    cs.getCollection.size() should be (1)
  }

  "Alerts" should "contain 2 alerts" in {
    as.getCollection.size() should be (2)
  }

}
