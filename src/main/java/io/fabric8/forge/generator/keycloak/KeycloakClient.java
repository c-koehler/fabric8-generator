/**
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.fabric8.forge.generator.keycloak;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import java.util.Map;

/**
 */
public class KeycloakClient {
    private static final transient Logger LOG = LoggerFactory.getLogger(KeycloakClient.class);

    private static final String ACCESS_TOKEN = "access_token";
    private static final String SCOPE = "scope";

    public String getOpenShiftToken(String authHeader) {
        return getTokenFor(KeycloakEndpoint.GET_OPENSHIFT_TOKEN, authHeader);
    }

    public String getGitHubToken(String authHeader) {
        return getTokenFor(KeycloakEndpoint.GET_GITHUB_TOKEN, authHeader);
    }

    public String getTokenFor(KeycloakEndpoint endpoint, String authHeader) {
        // access_token=token&scope=scope
        String responseBody = getResponseBody(endpoint, authHeader);
        Map<String, String> parameter = UrlHelper.splitQuery(responseBody);
        String token = parameter.get(ACCESS_TOKEN);
        LOG.info("Token: {}", token);
        String scope = parameter.get(SCOPE);
        LOG.info("Scope: {}", scope);
        return token;
    }

    private String getResponseBody(KeycloakEndpoint endpoint, String authHeader) {
        Client client = ClientBuilder.newClient();
        return client.target(endpoint.toString())
                //.request(MediaType.APPLICATION_JSON)
                .request("application/jwt")
                .header("Authorization", authHeader)
                .get(String.class);

/*
        RestTemplate template = new RestTemplate();
        HttpHeaders headers = new HttpHeaders();
        headers.setAccept(Arrays.asList(MediaType.APPLICATION_JSON));
        headers.add("Authorization", authHeader);
        HttpEntity<String> entity = new HttpEntity<String>("parameters", headers);
        ResponseEntity<String> response = template.exchange(endpoint.toString(), HttpMethod.GET, entity, String.class);
        return response.getBody();
*/
    }

}