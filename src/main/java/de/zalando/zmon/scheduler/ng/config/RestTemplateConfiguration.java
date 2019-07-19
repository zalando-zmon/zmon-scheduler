package de.zalando.zmon.scheduler.ng.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyNamingStrategy;
import de.zalando.zmon.scheduler.ng.TokenWrapper;
import org.apache.http.impl.client.HttpClientBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpRequest;
import org.springframework.http.client.*;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.web.client.RestTemplate;

import java.io.IOException;

/**
 * Created by jmussler on 11.02.16.
 */
@Configuration
public class RestTemplateConfiguration {

    private final static ObjectMapper mapper = (new ObjectMapper()).setPropertyNamingStrategy(PropertyNamingStrategy.CAMEL_CASE_TO_LOWER_CASE_WITH_UNDERSCORES);

    public static class TokenInterceptor implements ClientHttpRequestInterceptor {

        private TokenWrapper tokens;

        public TokenInterceptor(TokenWrapper tokens) {
            this.tokens = tokens;
        }

        @Override
        public ClientHttpResponse intercept(
                HttpRequest request, byte[] body, ClientHttpRequestExecution execution)
                throws IOException {

            HttpHeaders headers = request.getHeaders();
            headers.add("Authorization", "Bearer " + tokens.get());
            return execution.execute(request, body);
        }
    }

    @Bean
    public ClientHttpRequestFactory getFactory() {
        HttpComponentsClientHttpRequestFactory factory = new HttpComponentsClientHttpRequestFactory(HttpClientBuilder.create().build());
        factory.setReadTimeout(60000);
        factory.setConnectTimeout(3000);
        return factory;
    }

    @Bean
    public RestTemplate restTemplate(ClientHttpRequestFactory clientFactory, TokenWrapper tokens) {
        RestTemplate rt = new RestTemplate(clientFactory);

        TokenInterceptor ti = new TokenInterceptor(tokens);

        MappingJackson2HttpMessageConverter converter = new MappingJackson2HttpMessageConverter();
        converter.setObjectMapper(mapper);

        rt.getMessageConverters().clear();
        rt.getMessageConverters().add(converter);

        rt.getInterceptors().add(ti);

        return rt;
    }
}
