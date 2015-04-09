package de.zalando.zmon.scheduler.ng

import java.util.concurrent.atomic.AtomicLong

import de.zalando.zmon.scheduler.ng.CeleryBody.{CeleryAlertArg, CeleryCommandArg}
import de.zalando.zmon.scheduler.ng.entities.Entity

import scala.collection.mutable.ArrayBuffer

/**
 * Created by jmussler on 4/8/15.
 */


object CommandWriter {

  val counter = new AtomicLong()

  def write(entity : Entity, check : Check, alerts : ArrayBuffer[Alert]): String = {
    val body = new CeleryBody()
    val checkDef = check.getCheckDef()

    body.expires = "2015-12-31T00:00:00.000+00:00"
    body.id="check-"+check.id+"-"+entity.getId+"-"+System.currentTimeMillis()

    body.timelimit.add(checkDef.getInterval)
    body.timelimit.add(checkDef.getInterval * 2)

    val command = new CeleryCommandArg()
    command.check_id = check.id
    command.check_name = checkDef.getName
    command.interval = checkDef.getInterval
    command.command = checkDef.getCommand
    command.entity = entity.getProperties
    body.args.add(command)

    for(alert <- alerts) {
      val alertArg = new CeleryAlertArg()
      val alertDef = alert.getAlertDef

      alertArg.id = alert.id
      alertArg.check_id = check.id
      alertArg.condition = alertDef.getCondition
      alertArg.name = alertDef.getName
      alertArg.notifications = alertDef.getNotifications
      alertArg.period = alertDef.getPeriod
      alertArg.team = alertDef.getTeam
      alertArg.responsible_team = alertDef.getResponsibleTeam
      alertArg.paramters = alertDef.getParameters

      body.args.add(alertArg)
    }

    val writer = new CeleryWriter()
    writer.asCeleryTask(body)
  }
}
