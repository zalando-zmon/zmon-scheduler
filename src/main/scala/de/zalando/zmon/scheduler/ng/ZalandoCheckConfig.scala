package de.zalando.zmon.scheduler.ng

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Component

import scala.beans.BeanProperty

/**
 * Created by jmussler on 4/2/15.
 */
class ZalandoControler {
  @BeanProperty var name : String = "";
  @BeanProperty var url : String = "";
  @BeanProperty var refresh : Int = 0;
  @BeanProperty var user : String = null;
  @BeanProperty var password : String = null;
}

@Component
@Profile(Array("zalando"))
@ConfigurationProperties(prefix = "zalando.checks")
class ZalandoCheckConfig {
  @BeanProperty var controller : ZalandoControler = null
}
