package de.zalando.zmon.scheduler.ng;

/**
 * Created by jmussler on 3/26/15.
 */

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
import de.zalando.zmon.scheduler.ng.scheduler.Scheduler;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.web.bind.annotation.*;

import javax.net.ssl.SSLSession;
import java.io.IOException;
import java.util.Collection;
import java.util.List;
import java.util.Map;

@RestController
@EnableConfigurationProperties
@SpringBootApplication
public class Application {

    @Autowired
    EntityAdapterRegistry entityRegistry;

    @Autowired
    CheckSourceRegistry checkSourceRegistry;

    @Autowired
    AlertSourceRegistry alertSourceRegistry;

    @Autowired
    ObjectMapper mapper;

    @Autowired
    Scheduler scheduler;

    @RequestMapping(value = "/api/v1/entity-adapter")
    Collection<String> getAdapters() {
        return entityRegistry.getSourceNames();
    }

    @RequestMapping(value = "/api/v1/entity-adapter/{name}")
    Collection<Entity> getEntities(@PathVariable(value = "name") String name) {
        return entityRegistry.get(name).getCollection();
    }

    @RequestMapping(value = "/api/v1/check-source")
    Collection<String> getCheckSources() {
        return checkSourceRegistry.getSourceNames();
    }

    @RequestMapping(value = "/api/v1/check-source/{name}")
    Collection<CheckDefinition> getChecks(@PathVariable(value = "name") String name) {
        return checkSourceRegistry.get(name).getCollection();
    }

    @RequestMapping(value = "/api/v1/alert-source")
    Collection<String> getAlertSources() {
        return alertSourceRegistry.getSourceNames();
    }

    @RequestMapping(value = "/api/v1/alert-source/{name}")
    Collection<AlertDefinition> getAlerts(@PathVariable(value = "name") String name) {
        return alertSourceRegistry.get(name).getCollection();
    }

    @RequestMapping(value = "/api/v1/alert-coverage", method = RequestMethod.POST)
    Collection<AlertOverlapGenerator.EntityGroup> getAlertCoverage(@RequestBody List<Map<String, String>> entityFilters) {
        AlertOverlapGenerator g = new AlertOverlapGenerator(entityRepo, alertRepo.getByCheckId(), checkRepo.getCurrentMap(), alertRepo.getCurrentMap());
        return g.groupByAlertIds(entityFilters);
    }

    @RequestMapping(value = "/api/v1/entities")
    Collection<Entity> queryKnownEntities(@RequestParam(value = "filter") String sFilter,
                                          @RequestParam(value = "exclude_filter", defaultValue = "") String sExcludeFilter,
                                          @RequestParam(value = "local", defaultValue = "false") boolean baseFilter) throws IOException {

        List<Map<String, String>> filter = mapper.readValue(sFilter, new TypeReference<List<Map<String, String>>>() {
        });

        List<Map<String, String>> excludeFilter = mapper.readValue(sExcludeFilter, new TypeReference<List<Map<String, String>>>() {
        });

        return scheduler.queryKnownEntities(filter, excludeFilter, baseFilter);
    }

    @RequestMapping(value = "/api/v2/entities", method=RequestMethod.HEAD)
    Integer queryKnownEntitiesMultiFilterCount(@RequestParam(value = "include_filters") String sIncludeFilters,
                                                     @RequestParam(value = "exclude_filters", defaultValue = "") String sExcludeFilters,
                                                     @RequestParam(value = "local", defaultValue = "false") boolean baseFilter) throws IOException {

        List<List<Map<String, String>>> includeFilters = mapper.readValue(sIncludeFilters, new TypeReference<List<List<Map<String, String>>>>() {
        });

        List<List<Map<String, String>>> excludeFilters = mapper.readValue(sExcludeFilters, new TypeReference<List<List<Map<String, String>>>>() {
        });

        return scheduler.queryForKnownEntities(includeFilters, excludeFilters, baseFilter).size();
    }

    @RequestMapping(value = "/api/v2/entities")
    Collection<Entity> queryKnownEntitiesMultiFilter(@RequestParam(value = "include_filters") String sIncludeFilters,
                                          @RequestParam(value = "exclude_filters", defaultValue = "") String sExcludeFilters,
                                          @RequestParam(value = "local", defaultValue = "false") boolean baseFilter) throws IOException {

        List<List<Map<String, String>>> includeFilters = mapper.readValue(sIncludeFilters, new TypeReference<List<List<Map<String, String>>>>() {
        });

        List<List<Map<String, String>>> excludeFilters = mapper.readValue(sExcludeFilters, new TypeReference<List<List<Map<String, String>>>>() {
        });

        return scheduler.queryForKnownEntities(includeFilters, excludeFilters, baseFilter);
    }

    public static class EntitySearchRequest {
        public List<List<Map<String, String>>> includeFilters;
        public List<List<Map<String, String>>> excludeFilters;
        public boolean local = false;
    }

    @RequestMapping(value = "/api/v2/entities", method=RequestMethod.POST)
    Collection<Entity> queryKnownEntitiesMultiFilter(@RequestBody EntitySearchRequest searchRequest) throws IOException {
        return scheduler.queryForKnownEntities(searchRequest.includeFilters, searchRequest.excludeFilters, searchRequest.local);
    }


    @Autowired
    AlertRepository alertRepo;

    @Autowired
    CheckRepository checkRepo;

    @Autowired
    EntityRepository entityRepo;

    @RequestMapping(value = "/api/v1/repository-updates")
    JsonNode getUpdateStatus() {
        ObjectNode node = mapper.createObjectNode();
        node.put("alert-repo", alertRepo.getLastUpdated());
        node.put("check-repo", checkRepo.getLastUpdated());
        node.put("entity-repo", entityRepo.getLastUpdated());
        return node;
    }

    public static void main(String[] args) throws Exception {

        javax.net.ssl.HttpsURLConnection.setDefaultHostnameVerifier(
                (String hostname, SSLSession sslSession) -> hostname.equals("localhost"));

        SpringApplication.run(Application.class, args);
    }
}
