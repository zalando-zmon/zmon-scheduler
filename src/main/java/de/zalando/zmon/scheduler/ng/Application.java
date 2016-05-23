package de.zalando.zmon.scheduler.ng;

/**
 * Created by jmussler on 3/26/15.
 */

import com.codahale.metrics.MetricRegistry;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import de.zalando.zmon.scheduler.ng.alerts.AlertDefinition;
import de.zalando.zmon.scheduler.ng.alerts.AlertRepository;
import de.zalando.zmon.scheduler.ng.alerts.AlertSourceRegistry;
import de.zalando.zmon.scheduler.ng.checks.CheckDefinition;
import de.zalando.zmon.scheduler.ng.checks.CheckRepository;
import de.zalando.zmon.scheduler.ng.checks.CheckSourceRegistry;
import de.zalando.zmon.scheduler.ng.entities.Entity;
import de.zalando.zmon.scheduler.ng.entities.EntityAdapterRegistry;
import de.zalando.zmon.scheduler.ng.entities.EntityRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.*;
import org.springframework.boot.autoconfigure.*;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

@RestController
@EnableAutoConfiguration
@EnableConfigurationProperties
@Configuration
@ComponentScan
public class Application {

    private static final Logger LOG = LoggerFactory.getLogger(Application.class);

    @Autowired
    EntityAdapterRegistry entityRegistry;

    @Autowired
    CheckSourceRegistry checkSourceRegistry;

    @Autowired
    AlertSourceRegistry alertSourceRegistry;

    @Autowired
    MetricRegistry metrics;

    @Autowired
    ObjectMapper mapper;

    @Autowired
    Scheduler scheduler;

    @RequestMapping(value = "/api/v1/entity-adapter", method = RequestMethod.GET)
    Collection<String> getAdapters() {
        return entityRegistry.getSourceNames();
    }

    @RequestMapping(value = "/api/v1/entity-adapter/{name}", method = RequestMethod.GET)
    Collection<Entity> getEntities(@PathVariable(value = "name") String name) {
        return entityRegistry.get(name).getCollection();
    }

    @RequestMapping(value = "/api/v1/check-source", method = RequestMethod.GET)
    Collection<String> getCheckSources() {
        return checkSourceRegistry.getSourceNames();
    }

    @RequestMapping(value = "/api/v1/check-source/{name}", method = RequestMethod.GET)
    Collection<CheckDefinition> getChecks(@PathVariable(value = "name") String name) {
        return checkSourceRegistry.get(name).getCollection();
    }

    @RequestMapping(value = "/api/v1/alert-source", method = RequestMethod.GET)
    Collection<String> getAlertSources() {
        return alertSourceRegistry.getSourceNames();
    }

    @RequestMapping(value = "/api/v1/alert-source/{name}", method = RequestMethod.GET)
    Collection<AlertDefinition> getAlerts(@PathVariable(value = "name") String name) {
        return alertSourceRegistry.get(name).getCollection();
    }

    @RequestMapping(value = "/api/v1/alert-coverage", method = RequestMethod.POST)
    Collection<AlertOverlapGenerator.EntityGroup> getAlertCoverage(@RequestBody List<Map<String, String>> entityFilters) {
        AlertOverlapGenerator g = new AlertOverlapGenerator(entityRepo, alertRepo.getByCheckId(), checkRepo.getCurrentMap(), alertRepo.getCurrentMap());
        return g.groupByAlertIds(entityFilters);
    }

    @Autowired
    private InstantEvalForwarder instantEvalForwarder;

    @RequestMapping(value = "/api/v1/instant-evaluations/{dc}/", method = RequestMethod.GET)
    Collection<Integer> getPendingInstantEvaluations(@PathVariable(value = "dc") String dcId) {
        return instantEvalForwarder.getRequests(dcId);
    }

    @RequestMapping(value = "/api/v1/instant-evaluations/", method = RequestMethod.GET)
    Collection<String> getKnownInstantEvalForwardDCs() {
        return instantEvalForwarder.getKnwonDCs();
    }

    @Autowired
    private TrialRunForwarder trialRunForwarder;

    @RequestMapping(value = "/api/v1/trial-runs/{dc}/", method = RequestMethod.GET)
    Collection<TrialRunRequest> getPendingTrialRuns(@PathVariable(value = "dc") String dcId) {
        return trialRunForwarder.getRequests(dcId);
    }

    @RequestMapping(value = "/api/v1/checks/{id}/instant-eval", method = RequestMethod.POST)
    public void triggerInstantEvaluationByCheck(@PathVariable(value = "id") int checkId) {
        scheduler.executeImmediate(checkId);
        instantEvalForwarder.forwardRequest(checkId);
    }

    @RequestMapping(value = "/api/v1/alerts/{id}/instant-eval", method = RequestMethod.POST)
    public void triggerInstantEvaluation(@PathVariable(value = "id") int id) {
        int checkId = alertRepo.get(id).getCheckDefinitionId();
        scheduler.executeImmediate(checkId);
        instantEvalForwarder.forwardRequest(checkId);
    }

    @RequestMapping(value = "/api/v1/trial-runs", method = RequestMethod.POST)
    public void postTrialRun(@RequestBody TrialRunRequest trialRun) {
        scheduler.scheduleTrialRun(trialRun);
        trialRunForwarder.forwardRequest(trialRun);
    }

    @RequestMapping(value = "/api/v1/trial-runs/", method = RequestMethod.GET)
    Collection<String> getKnownTrialRunDCs() {
        return trialRunForwarder.getKnwonDCs();
    }

    @RequestMapping(value = "/api/v1/trigger-check/{id}", method = RequestMethod.GET)
    void triggerInstantEval(@PathVariable(value = "id") int checkId) {
        scheduler.executeImmediate(checkId);
    }

    @RequestMapping(value = "/api/v1/entities", method = RequestMethod.GET)
    Collection<Entity> queryKnownEntities(@RequestParam(value = "filter", required = true) String sFilter,
                                          @RequestParam(value = "exclude_filter", defaultValue = "") String sExcludeFilter,
                                          @RequestParam(value = "local", defaultValue = "false") boolean baseFilter) throws IOException {

        List<Map<String, String>> filter = mapper.readValue(sFilter, new TypeReference<List<Map<String, String>>>() {
        });

        List<Map<String, String>> excludeFilter = mapper.readValue(sExcludeFilter, new TypeReference<List<Map<String, String>>>() {
        });

        return scheduler.queryKnownEntities(filter, excludeFilter, baseFilter);
    }


    @Autowired
    AlertRepository alertRepo;

    @Autowired
    CheckRepository checkRepo;

    @Autowired
    EntityRepository entityRepo;

    @RequestMapping(value = "/api/v1/repository-updates", method = RequestMethod.GET)
    JsonNode getUpdateStatus() {
        ObjectNode node = mapper.createObjectNode();
        node.put("alert-repo", alertRepo.getLastUpdated());
        node.put("check-repo", checkRepo.getLastUpdated());
        node.put("entity-repo", entityRepo.getLastUpdated());
        return node;
    }

    public static void main(String[] args) throws Exception {

        javax.net.ssl.HttpsURLConnection.setDefaultHostnameVerifier(
                new javax.net.ssl.HostnameVerifier() {

                    public boolean verify(String hostname,
                                          javax.net.ssl.SSLSession sslSession) {
                        if (hostname.equals("localhost")) {
                            return true;
                        }
                        return false;
                    }
                });

        SpringApplication.run(Application.class, args);
    }
}
