package de.zalando.zmon.scheduler.ng;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.springframework.beans.factory.annotation.Autowired;

import de.zalando.zmon.scheduler.ng.config.StupsOAuthConfig;

/**
 * Created by jmussler on 5/11/15.
 */

public class StupsOAuthProvider {

//    private static Logger LOG = LoggerFactory.getLogger(StupsOAuthProvider.class);
//
//    private static final String ROBOT_CREDENTIALS_FILE = "user.json";
//    private static final String CLIENT_CREDENTIALS_FILE = "client.json";

//    private static final ObjectMapper mapper = new ObjectMapper();

    private final Map<String, TokenContainer> tokens = new HashMap<>();

    public static class TokenContainer {
        private String token = "";
        private Set<String> scopes;

        public TokenContainer(Set<String> scopes) {
            this.scopes = scopes;
        }

        public Set<String> getScopes() {
            return scopes;
        }

        public void updateToken(String t, long e) {
            token = t;
        }

        public String getToken() {
            return token;
        }
    }
//
//    private static class TokenUpdater implements Runnable {
//        private String key;
//        private String metaFolder;
//        private String uri;
//        private TokenContainer container;
//        private String scopes;
//
//        public TokenUpdater(String uri, String metaFolder, TokenContainer tc) {
//            this.metaFolder = metaFolder;
//            this.uri = uri;
//            this.container = tc;
//            scopes = container.getScopes().stream().filter(x -> x != null).collect(Collectors.joining(" "));
//        }
//
//        @Override
//        public void run() {
//            try {
//                Map<String, String> clientCredentials = mapper.readValue(new File(metaFolder + "/" + CLIENT_CREDENTIALS_FILE), new TypeReference<Map<String, String>>() {
//                });
//                Map<String, String> robotCredentials = mapper.readValue(new File(metaFolder + "/" + ROBOT_CREDENTIALS_FILE), new TypeReference<Map<String, String>>() {
//                });
//
//                final Executor executor = Executor.newInstance();
//                executor.auth(clientCredentials.get("client_id"), clientCredentials.get("client_secret"));
//
//                List<NameValuePair> form = Form.form().add("grant_type", "password")
//                        .add("username", robotCredentials.get("application_username"))
//                        .add("password", robotCredentials.get("application_password"))
//                        .add("scopes", scopes).build();
//
//                final String r = executor.execute(Request.Post(uri).bodyForm(form).useExpectContinue()).returnContent().asString();
//
//                container.updateToken(r, System.currentTimeMillis() + 60 * 60 * 1000);
//
//            } catch (IOException ex) {
//                LOG.error("", ex);
//            }
//        }
//    }

    public String getToken(String scopeIdentifier) {
        return tokens.get(scopeIdentifier).getToken();
    }

    @Autowired
    public StupsOAuthProvider(StupsOAuthConfig config) {
        for (String k : config.getScopes().keySet()) {
            tokens.put(k, new TokenContainer(config.scopes.get(k)));
        }


    }
}
