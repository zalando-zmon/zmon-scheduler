package de.zalando.zmon.scheduler.ng

import java.text.SimpleDateFormat
import java.util.Date
import java.util.concurrent.atomic.AtomicLong

import de.zalando.zmon.scheduler.ng.CeleryBody.{TrialRunCeleryAlertArg, TrialRunCeleryCommand, CeleryAlertArg, CeleryCommandArg}
import de.zalando.zmon.scheduler.ng.entities.Entity

import scala.collection.mutable.ArrayBuffer

/**
 * Created by jmussler on 4/8/15.
 */


class CommandSerializer(val serializerType : TaskSerializerType) {

  val writer = CeleryWriter.create(serializerType)

  val dateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSXXX")

  def expiresTime(interval : Long) : String = {
    val exp = new Date(System.currentTimeMillis()+(interval * 1000L))
    LocalDateFormatter.get().format(exp)
  }

  def writeTrialRun(entity : Entity, request: TrialRunRequest): Array[Byte] = {
    val body = new CeleryBody()

    body.expires = expiresTime(request.interval)// "2015-12-31T00:00:00.000+00:00"
    body.id="check-TR:"+request.id+"-"+entity.getId+"-"+System.currentTimeMillis()

    body.timelimit.add(request.interval)
    body.timelimit.add(request.interval * 2L)

    val command = new TrialRunCeleryCommand()
    command.check_id = "TR:"+request.id
    command.check_name = request.name
    command.interval = request.interval
    command.command = request.check_command
    command.entity = entity.getProperties
    body.args.add(command)

    val alertList : java.util.List[TrialRunCeleryAlertArg] = new java.util.ArrayList[TrialRunCeleryAlertArg]();
    body.args.add(alertList)

    val alertArg = new TrialRunCeleryAlertArg()
    alertList.add(alertArg)

    alertArg.id = "TR:" + request.id
    alertArg.check_id = "TR:" + request.id
    alertArg.condition = request.alert_condition
    alertArg.name = request.name
    alertArg.period = request.period
    if(alertArg.period == null) {
      alertArg.period = "";
    }
    alertArg.team = "TRIAL RUN"
    alertArg.responsible_team = "TRIAL RUN"
    alertArg.parameters = request.parameters
    alertArg.entities_map = request.entities

    body.task = "trial_run"

    writer.asCeleryTask(body)
  }

  def write(entity : Entity, check : Check, alerts : ArrayBuffer[Alert]): Array[Byte] = {
    val body = new CeleryBody()
    val checkDef = check.getCheckDef()

    body.expires = expiresTime(checkDef.getInterval)// "2015-12-31T00:00:00.000+00:00"
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

    val alertList : java.util.List[CeleryAlertArg] = new java.util.ArrayList[CeleryAlertArg]();
    body.args.add(alertList)

    for(alert <- alerts) {
      val alertArg = new CeleryAlertArg()
      val alertDef = alert.getAlertDef

      alertArg.id = alert.id
      alertArg.check_id = check.id
      alertArg.condition = alertDef.getCondition
      alertArg.name = alertDef.getName
      alertArg.notifications = alertDef.getNotifications
      alertArg.period = alertDef.getPeriod
      if(alertArg.period == null) {
        alertArg.period = "";
      }
      alertArg.team = alertDef.getTeam
      alertArg.responsible_team = alertDef.getResponsibleTeam
      alertArg.parameters = alertDef.getParameters
      alertArg.entities_map = alertDef.getEntities

      alertList.add(alertArg)
    }

    writer.asCeleryTask(body)
  }
}
