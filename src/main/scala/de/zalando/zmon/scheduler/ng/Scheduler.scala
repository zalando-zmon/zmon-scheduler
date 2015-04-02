package de.zalando.zmon.scheduler.ng

import com.codahale.metrics.MetricRegistry
import de.zalando.zmon.scheduler.ng.entities.EntityAdapter
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.stereotype.Component
import beans.BeanProperty

/**
 * Created by jmussler on 3/27/15.
 */

class SubConfig {
 @BeanProperty var refresh : Int = 240
}

@Component
@ConfigurationProperties(prefix = "testconfig")
class TestConfig {
 @BeanProperty var url: String = ""
 @BeanProperty var user: String = ""
 @BeanProperty var sub: SubConfig = null
}

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
