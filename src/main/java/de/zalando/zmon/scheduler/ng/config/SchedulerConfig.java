package de.zalando.zmon.scheduler.ng.config;

import de.zalando.zmon.scheduler.ng.SchedulePersistType;
import de.zalando.zmon.scheduler.ng.TaskSerializerType;
import de.zalando.zmon.scheduler.ng.TaskWriterType;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by jmussler on 01.07.16.
 */
@Component
@Configuration
@ConfigurationProperties(prefix = "scheduler")
public class SchedulerConfig {
    SchedulePersistType lastRunPersist = SchedulePersistType.DISABLED;
    boolean checkDetailMetrics = false;
    int threadCount = 8;

    List<Integer> checkFilter = new ArrayList<>();
    String entitySkipOnField = null;

    boolean baseFilterForward = true;
    List<Map<String, String>> entityBaseFilter = null;
    String entityBaseFilterStr = null;

    String defaultQueue = "zmon:queue:default";
    String trialRunQueue = "zmon:queue:default";
    boolean enableGlobalEntity = false;

    TaskWriterType taskWriterType = TaskWriterType.ARRAY_LIST;

    String redisHost = "";
    int redisPort = 6379;

    String oauth2AccessTokenUrl = null;
    List<String> oauth2Scopes = null;
    String oauth2StaticToken = "";

    // the entity service provides entities to run checks against ( it is part of the controller )
    String entityServiceUrl = null;
    String entityServiceUser = null;
    String entityServicPassword = null;

    // Using the zmon controller as a source for alerts and checks
    String controllerUrl = null;
    String controllerUser = null;
    String controllerPassword = null;

    // Remote/AWS support
    // used to enable polling for instant eval via http with DC id
    boolean instantEvalForward = false;
    String instantEvalHttpUrl = null;

    // used to enable polling for trial runs via http with DC id
    boolean trialRunForward = false;
    String trialRunHttpUrl = null;

    boolean downtimeEntityFilter = true;
    boolean downtimeForward = false;
    String downtimeHttpUrl = null;

    String dummyCities = null; // "dummy_data/cities.json"

    // DEPRECATED: Mapping based on check url prefix
    Map<String, String> queueMappingByUrl = new HashMap<>();

    // DEPRECATED: Map certain check IDs to queue
    Map<String, List<Integer>> queueMapping = new HashMap<>();

    // Map certain entity properties to queues e.g. "dc":"gth" => "dclocal:gth"
    Map<String, List<Map<String,String>>> queuePropertyMapping = new HashMap<>();

    // Maps chosen check/trial run attributes to queues
    private Map<String, List<Map<String, Object>>> genericQueueMapping = new HashMap<>();

    TaskSerializerType taskSerializer = TaskSerializerType.PLAIN;

    String entityPropertiesKey = null;

    long checkMinInterval = 15L;

    @Value("${server.port}")
    String serverPort  = null;

    public SchedulerConfig() {
    }

    public SchedulePersistType getLastRunPersist() {
        return lastRunPersist;
    }

    public void setLastRunPersist(SchedulePersistType lastRunPersist) {
        this.lastRunPersist = lastRunPersist;
    }

    public boolean isCheckDetailMetrics() {
        return checkDetailMetrics;
    }

    public void setCheckDetailMetrics(boolean checkDetailMetrics) {
        this.checkDetailMetrics = checkDetailMetrics;
    }

    public int getThreadCount() {
        return threadCount;
    }

    public void setThreadCount(int threadCount) {
        this.threadCount = threadCount;
    }

    public List<Integer> getCheckFilter() {
        return checkFilter;
    }

    public void setCheckFilter(List<Integer> checkFilter) {
        this.checkFilter = checkFilter;
    }

    public String getEntitySkipOnField() {
        return entitySkipOnField;
    }

    public void setEntitySkipOnField(String entitySkipOnField) {
        this.entitySkipOnField = entitySkipOnField;
    }

    public List<Map<String, String>> getEntityBaseFilter() {
        return entityBaseFilter;
    }

    public void setEntityBaseFilter(List<Map<String, String>> entityBaseFilter) {
        this.entityBaseFilter = entityBaseFilter;
    }

    public String getEntityBaseFilterStr() {
        return entityBaseFilterStr;
    }

    public void setEntityBaseFilterStr(String entityBaseFilterStr) {
        this.entityBaseFilterStr = entityBaseFilterStr;
    }

    public String getDefaultQueue() {
        return defaultQueue;
    }

    public void setDefaultQueue(String defaultQueue) {
        this.defaultQueue = defaultQueue;
    }

    public String getTrialRunQueue() {
        return trialRunQueue;
    }

    public void setTrialRunQueue(String trialRunQueue) {
        this.trialRunQueue = trialRunQueue;
    }

    public boolean isEnableGlobalEntity() {
        return enableGlobalEntity;
    }

    public void setEnableGlobalEntity(boolean enableGlobalEntity) {
        this.enableGlobalEntity = enableGlobalEntity;
    }

    public TaskWriterType getTaskWriterType() {
        return taskWriterType;
    }

    public void setTaskWriterType(TaskWriterType taskWriterType) {
        this.taskWriterType = taskWriterType;
    }

    public String getRedisHost() {
        return redisHost;
    }

    public void setRedisHost(String redisHost) {
        this.redisHost = redisHost;
    }

    public int getRedisPort() {
        return redisPort;
    }

    public void setRedisPort(int redisPort) {
        this.redisPort = redisPort;
    }

    public String getOauth2AccessTokenUrl() {
        return oauth2AccessTokenUrl;
    }

    public void setOauth2AccessTokenUrl(String oauth2AccessTokenUrl) {
        this.oauth2AccessTokenUrl = oauth2AccessTokenUrl;
    }

    public List<String> getOauth2Scopes() {
        return oauth2Scopes;
    }

    public void setOauth2Scopes(List<String> oauth2Scopes) {
        this.oauth2Scopes = oauth2Scopes;
    }

    public String getOauth2StaticToken() {
        return oauth2StaticToken;
    }

    public void setOauth2StaticToken(String oauth2StaticToken) {
        this.oauth2StaticToken = oauth2StaticToken;
    }

    public String getEntityServiceUrl() {
        return entityServiceUrl;
    }

    public void setEntityServiceUrl(String entityServiceUrl) {
        this.entityServiceUrl = entityServiceUrl;
    }

    public String getEntityServiceUser() {
        return entityServiceUser;
    }

    public void setEntityServiceUser(String entityServiceUser) {
        this.entityServiceUser = entityServiceUser;
    }

    public String getEntityServicPassword() {
        return entityServicPassword;
    }

    public void setEntityServicPassword(String entityServicPassword) {
        this.entityServicPassword = entityServicPassword;
    }

    public String getControllerUrl() {
        return controllerUrl;
    }

    public void setControllerUrl(String controllerUrl) {
        this.controllerUrl = controllerUrl;
    }

    public String getControllerUser() {
        return controllerUser;
    }

    public void setControllerUser(String controllerUser) {
        this.controllerUser = controllerUser;
    }

    public String getControllerPassword() {
        return controllerPassword;
    }

    public void setControllerPassword(String controllerPassword) {
        this.controllerPassword = controllerPassword;
    }

    public boolean isInstantEvalForward() {
        return instantEvalForward;
    }

    public void setInstantEvalForward(boolean instantEvalForward) {
        this.instantEvalForward = instantEvalForward;
    }

    public String getInstantEvalHttpUrl() {
        return instantEvalHttpUrl;
    }

    public void setInstantEvalHttpUrl(String instantEvalHttpUrl) {
        this.instantEvalHttpUrl = instantEvalHttpUrl;
    }

    public boolean isTrialRunForward() {
        return trialRunForward;
    }

    public void setTrialRunForward(boolean trialRunForward) {
        this.trialRunForward = trialRunForward;
    }

    public String getTrialRunHttpUrl() {
        return trialRunHttpUrl;
    }

    public void setTrialRunHttpUrl(String trialRunHttpUrl) {
        this.trialRunHttpUrl = trialRunHttpUrl;
    }

    public boolean isDowntimeEntityFilter() {
        return downtimeEntityFilter;
    }

    public void setDowntimeEntityFilter(boolean downtimeEntityFilter) {
        this.downtimeEntityFilter = downtimeEntityFilter;
    }

    public boolean isDowntimeForward() {
        return downtimeForward;
    }

    public void setDowntimeForward(boolean downtimeForward) {
        this.downtimeForward = downtimeForward;
    }

    public String getDowntimeHttpUrl() {
        return downtimeHttpUrl;
    }

    public void setDowntimeHttpUrl(String downtimeHttpUrl) {
        this.downtimeHttpUrl = downtimeHttpUrl;
    }

    public String getDummyCities() {
        return dummyCities;
    }

    public void setDummyCities(String dummyCities) {
        this.dummyCities = dummyCities;
    }

    public Map<String, String> getQueueMappingByUrl() {
        return queueMappingByUrl;
    }

    public void setQueueMappingByUrl(Map<String, String> queueMappingByUrl) {
        this.queueMappingByUrl = queueMappingByUrl;
    }

    public Map<String, List<Integer>> getQueueMapping() {
        return queueMapping;
    }

    public void setQueueMapping(Map<String, List<Integer>> queueMapping) {
        this.queueMapping = queueMapping;
    }

    public Map<String, List<Map<String, String>>> getQueuePropertyMapping() {
        return queuePropertyMapping;
    }

    public void setQueuePropertyMapping(Map<String, List<Map<String, String>>> queuePropertyMapping) {
        this.queuePropertyMapping = queuePropertyMapping;
    }

    public TaskSerializerType getTaskSerializer() {
        return taskSerializer;
    }

    public void setTaskSerializer(TaskSerializerType taskSerializer) {
        this.taskSerializer = taskSerializer;
    }

    public String getEntityPropertiesKey() {
        return entityPropertiesKey;
    }

    public void setEntityPropertiesKey(String entityPropertiesKey) {
        this.entityPropertiesKey = entityPropertiesKey;
    }

    public long getCheckMinInterval() {
        return checkMinInterval;
    }

    public void setCheckMinInterval(long checkMinInterval) {
        this.checkMinInterval = checkMinInterval;
    }

    public String getServerPort() {
        return serverPort;
    }

    public void setServerPort(String serverPort) {
        this.serverPort = serverPort;
    }

    public boolean isBaseFilterForward() {
        return baseFilterForward;
    }

    public void setBaseFilterForward(boolean baseFilterForward) {
        this.baseFilterForward = baseFilterForward;
    }

    public Map<String, List<Map<String, Object>>> getGenericQueueMapping() {
        return genericQueueMapping;
    }

    public void setGenericQueueMapping(Map<String, List<Map<String, Object>>> genericQueueMapping) {
        this.genericQueueMapping = genericQueueMapping;
    }
}
