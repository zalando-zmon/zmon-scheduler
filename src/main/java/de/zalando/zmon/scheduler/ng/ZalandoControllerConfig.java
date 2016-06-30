package de.zalando.zmon.scheduler.ng;

/**
 * Created by jmussler on 01.07.16.
 */
public class ZalandoControllerConfig {
    public String name = "";
    public String url = "";
    public int refresh = 0;
    public String user = null;
    public String password = null;
    public String token = null;

    public ZalandoControllerConfig() {
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public int getRefresh() {
        return refresh;
    }

    public void setRefresh(int refresh) {
        this.refresh = refresh;
    }

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
