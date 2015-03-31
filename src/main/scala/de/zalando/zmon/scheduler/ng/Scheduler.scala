package de.zalando.zmon.scheduler.ng

import com.codahale.metrics.MetricRegistry
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component

/**
 * Created by jmussler on 3/27/15.
 */

class Check {

}

class Alert {

}

class ScheduledCheck extends Runnable {

 override def run(): Unit = {

 }
}

@Component
class Scheduler {

 @Autowired
 var metrics : MetricRegistry = null

 def addAdapter(a: EntityAdapter): Unit = {

 }
}
