package de.zalando.zmon.scheduler.ng;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Set;

/**
 * Created by jmussler on 5/11/15.
 */
@Component
@ConfigurationProperties(prefix = "oauth2")
public class StupsOAuthConfig {
    private String authURI;
    private String tokenURI;
    private String metaFolder;
    private Long lifeTime = 60 * 60 * 1000L;

    public Long getLifeTime() {
        return lifeTime;
    }

    public String getMetaFolder() {
        return metaFolder;
    }

    public void setMetaFolder(String metaFolder) {
        this.metaFolder = metaFolder;
    }

    public String getAuthURI() {
        return authURI;
    }

    public void setAuthURI(String authURI) {
        this.authURI = authURI;
    }

    public String getTokenURI() {
        return tokenURI;
    }

    public void setTokenURI(String tokenURI) {
        this.tokenURI = tokenURI;
    }

    public Map<String, Set<String>> getScopes() {
        return scopes;
    }

    public void setScopes(Map<String, Set<String>> scopes) {
        this.scopes = scopes;
    }

    public Map<String, Set<String>> scopes;
}
