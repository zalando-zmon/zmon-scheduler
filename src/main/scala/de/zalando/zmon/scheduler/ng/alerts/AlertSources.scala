package de.zalando.zmon.scheduler.ng.alerts

/**
 * Created by jmussler on 4/2/15.
 */
class DefaultAlertSource {

}

class AlertSourceRegistry {
  val registry = scala.collection.mutable.HashMap[String,AlertSource]()

  def getSource(name : String) = {
    registry.getOrElse(name, null)
  }
}