package de.zalando.zmon.scheduler.ng.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

/**
 * Created by jmussler on 4/1/15.
 */

@Component
@Profile("zalando")
@ConfigurationProperties(prefix = "zalando.entities")
public class ZalandoConfig {

    public static class BaseAdapterConfig {
        public String url;

        public String getUrl() {
            return url;
        }

        public void setUrl(String url) {
            this.url = url;
        }

        public int refresh;

        public int getRefresh() {
            return refresh;
        }

        public void setRefresh(int refresh) {
            this.refresh = refresh;
        }
    }

    public static class AuthAdapterConfig extends BaseAdapterConfig {
        public String user;
        public String password;
        public String token;

        public String getUser() {
            return user;
        }

        public void setUser(String user) {
            this.user = user;
        }

        public String getPassword() {
            return password;
        }

        public void setPassword(String password) {
            this.password = password;
        }

        public String getToken() {
            return token;
        }

        public void setToken(String token) {
            this.token = token;
        }
    }

    public static class Entityservice extends AuthAdapterConfig {
    }

    public Entityservice entityservice;
    public Entityservice getEntityservice() {
        return entityservice;
    }

    public void setEntityservice(Entityservice entityservice) {
        this.entityservice = entityservice;
    }
}
