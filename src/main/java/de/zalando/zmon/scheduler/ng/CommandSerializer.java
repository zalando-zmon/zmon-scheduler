package de.zalando.zmon.scheduler.ng;

import de.zalando.zmon.scheduler.ng.alerts.AlertDefinition;
import de.zalando.zmon.scheduler.ng.checks.CheckDefinition;
import de.zalando.zmon.scheduler.ng.entities.Entity;
import de.zalando.zmon.scheduler.ng.trailruns.TrialRunRequest;

import io.opentracing.Tracer;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.List;

/**
 * Created by jmussler on 30.06.16.
 */
public class CommandSerializer {

    private final CeleryWriter writer;
    private final Tracer tracer;

    public CommandSerializer(TaskSerializerType type, Tracer tracer) {
        this.writer = CeleryWriter.create(type);
        this.tracer = tracer;
    }

    public String expiresTime(long interval) {
        Date exp = new Date(System.currentTimeMillis()+(interval * 1000L));
        return LocalDateFormatter.get().format(exp);
    }

    public byte[] writeTrialRun(Entity entity, TrialRunRequest request) {
        CeleryBody body = new CeleryBody();

        body.expires = expiresTime(request.interval); // "2015-12-31T00:00:00.000+00:00"
        body.id="check-TR:"+request.id+"-"+entity.getId()+"-"+System.currentTimeMillis();

        body.timelimit.add(request.interval);
        body.timelimit.add(request.interval * 2L);

        CeleryBody.TrialRunCeleryCommand command = new CeleryBody.TrialRunCeleryCommand();
        command.check_id = "TR:"+request.id;
        command.check_name = request.name;
        command.interval = request.interval;
        command.command = request.checkCommand;
        command.entity = entity.getProperties();
        body.args.add(command);

        List<CeleryBody.TrialRunCeleryAlertArg> alertList = new java.util.ArrayList<>();
        body.args.add(alertList);

        CeleryBody.TrialRunCeleryAlertArg alertArg = new CeleryBody.TrialRunCeleryAlertArg();
        alertList.add(alertArg);

        alertArg.id = "TR:" + request.id;
        alertArg.check_id = "TR:" + request.id;
        alertArg.condition = request.alertCondition;
        alertArg.name = request.name;
        alertArg.period = request.period;
        if(alertArg.period == null) {
            alertArg.period = "";
        }
        alertArg.team = "TRIAL RUN";
        alertArg.responsible_team = "TRIAL RUN";
        alertArg.parameters = request.parameters;
        alertArg.entities_map = request.entities;

        body.task = "trial_run";

        return writer.asCeleryTask(body, tracer);
    }

    public byte[] write(Entity entity, Check check, Collection<Alert> alerts, long scheduledTime) {
        CeleryBody body = new CeleryBody();
        CheckDefinition checkDef = check.getCheckDefinition();

        body.expires = expiresTime(checkDef.getInterval()); // "2015-12-31T00:00:00.000+00:00"
        body.id = "check-" + check.getId() + "-" + entity.getId() + "-" + System.currentTimeMillis();

        body.timelimit.add(checkDef.getInterval());
        body.timelimit.add(checkDef.getInterval() * 2);

        CeleryBody.CeleryCommandArg command = new CeleryBody.CeleryCommandArg();
        command.check_id = check.getId();
        command.check_name = checkDef.getName();
        command.interval = checkDef.getInterval();
        command.command = checkDef.getCommand();
        command.entity = entity.getProperties();
        command.schedule_time = ((double)scheduledTime) / 1000.0;
        body.args.add(command);

        List<CeleryBody.CeleryAlertArg> alertList = new ArrayList<>();
        body.args.add(alertList);

        for(Alert alert : alerts) {
            CeleryBody.CeleryAlertArg alertArg = new CeleryBody.CeleryAlertArg();
            AlertDefinition alertDef = alert.getAlertDefinition();

            alertArg.id = alert.getId();
            alertArg.check_id = check.getId();
            alertArg.condition = alertDef.getCondition();
            alertArg.name = alertDef.getName();
            alertArg.notifications = alertDef.getNotifications();
            alertArg.period = alertDef.getPeriod();
            alertArg.priority = alertDef.getPriority();
            alertArg.tags = alertDef.getTags();

            if(alertArg.period == null) {
                alertArg.period = "";
            }

            alertArg.team = alertDef.getTeam();
            alertArg.responsible_team = alertDef.getResponsibleTeam();
            alertArg.parameters = alertDef.getParameters();
            alertArg.entities_map = alertDef.getEntities();

            alertList.add(alertArg);
        }

        return writer.asCeleryTask(body, tracer);
    }
}
