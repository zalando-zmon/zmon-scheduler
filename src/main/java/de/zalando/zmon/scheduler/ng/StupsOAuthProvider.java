package de.zalando.zmon.scheduler.ng;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.http.client.fluent.Executor;
import org.apache.http.client.fluent.Form;
import org.apache.http.client.fluent.Request;
import org.apache.http.entity.ContentType;
import org.apache.http.NameValuePair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;


import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.stream.Collectors;

/**
 * Created by jmussler on 5/11/15.
 */
@Component
public class StupsOAuthProvider {

    private static Logger LOG = LoggerFactory.getLogger(StupsOAuthProvider.class);

    private static final String ROBOT_CREDENTIALS_FILE = "user.json";
    private static final String CLIENT_CREDENTIALS_FILE = "client.json";

    private static final ObjectMapper mapper = new ObjectMapper();

    private final StupsOAuthConfig config;
    private final ScheduledThreadPoolExecutor executor = new ScheduledThreadPoolExecutor(1);

    private final Map<String, TokenContainer> tokens = new HashMap<>();

    public static class TokenContainer {
        private String token = "";
        private Set<String> scopes;
        private long expires = 0;

        public TokenContainer(Set<String> scopes) {
            this.scopes = scopes;
        }

        public Set<String> getScopes() {
            return scopes;
        }

        public void updateToken(String t, long e) {
            token = t;
            expires = e;
        }

        public String getToken() {
            return token;
        }
    }

    private static class TokenUpdater implements Runnable {
        private String key;
        private String metaFolder;
        private String uri;
        private TokenContainer container;
        private String scopes;

        public TokenUpdater(String uri, String metaFolder, TokenContainer tc) {
            this.metaFolder = metaFolder;
            this.uri = uri;
            this.container = tc;
            scopes = container.getScopes().stream().filter(x -> x != null).collect(Collectors.joining(" "));
        }

        @Override
        public void run() {
            try {
                Map<String, String> clientCredentials = mapper.readValue(new File(metaFolder + "/" + CLIENT_CREDENTIALS_FILE), new TypeReference<Map<String,String>>() {});
                Map<String, String> robotCredentials = mapper.readValue(new File(metaFolder + "/" + ROBOT_CREDENTIALS_FILE), new TypeReference<Map<String, String>>() {
                });

                final Executor executor = Executor.newInstance();
                executor.auth(clientCredentials.get("client_id"), clientCredentials.get("client_secret"));

                List<NameValuePair> form = Form.form().add("grant_type", "password")
                        .add("username", robotCredentials.get("application_username"))
                        .add("password", robotCredentials.get("application_password"))
                        .add("scopes", scopes).build();

                final String r = executor.execute(Request.Post(uri).bodyForm(form).useExpectContinue()).returnContent().asString();

                container.updateToken(r, System.currentTimeMillis()+60*60*1000);

            } catch (IOException ex) {
                LOG.error("", ex);
            }
        }
    }

    public String getToken(String scopeIdentifier) {
        return tokens.get(scopeIdentifier).getToken();
    }

    @Autowired
    public StupsOAuthProvider(StupsOAuthConfig config) {
        this.config = config;
        for(String k : config.getScopes().keySet()) {
            tokens.put(k, new TokenContainer(config.scopes.get(k)));
        }


    }
}
