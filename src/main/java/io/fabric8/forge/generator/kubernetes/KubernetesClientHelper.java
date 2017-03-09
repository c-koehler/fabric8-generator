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
package io.fabric8.forge.generator.kubernetes;

import io.fabric8.forge.generator.EnvironmentVariables;
import io.fabric8.kubernetes.api.Controller;
import io.fabric8.kubernetes.api.KubernetesHelper;
import io.fabric8.kubernetes.api.KubernetesNames;
import io.fabric8.kubernetes.api.extensions.Configs;
import io.fabric8.kubernetes.api.model.Namespace;
import io.fabric8.kubernetes.client.DefaultKubernetesClient;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.openshift.api.model.Project;
import io.fabric8.openshift.api.model.User;
import io.fabric8.openshift.client.OpenShiftClient;
import io.fabric8.utils.Strings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 */
public class KubernetesClientHelper {
    private static final transient Logger LOG = LoggerFactory.getLogger(KubernetesClientHelper.class);

    /**
     * Should create a kubernetes client using the current logged in users account
     *
     * @return the kubernetes client for the current user
     */
    public static KubernetesClient createKubernetesClientForUser() {
        // TODO use the current users token
        return new DefaultKubernetesClient();
    }

    /**
     * Returns the current users kubernetes/openshift user name
     */
    public static String getUserName(KubernetesClient kubernetesClient) {
        OpenShiftClient oc = getOpenShiftClientOrNull(kubernetesClient);
        if (oc != null) {
            User user = oc.users().withName("~").get();
            if (user == null) {
                LOG.warn("Failed to find current logged in user!");
            } else {
                String answer = KubernetesHelper.getName(user);
                if (Strings.isNullOrBlank(answer)) {
                    LOG.warn("No name for User " + user);
                } else {
                    return answer;
                }
            }
        }

        // TODO needs to use the current token to find the current user name
        return Configs.currentUserName();
    }

    public static OpenShiftClient getOpenShiftClientOrNull(KubernetesClient kubernetesClient) {
        return new Controller(kubernetesClient).getOpenShiftClientOrNull();
    }

    public static String getUserSecretNamespace(KubernetesClient kubernetesClient) {
        String userName = getUserName(kubernetesClient);
        if (Strings.isNullOrBlank(userName)) {
            throw new IllegalStateException("No kubernetes username could be found!");
        }
        return KubernetesNames.convertToKubernetesName("user-secrets-" + userName, false);
    }

    /**
     * Returns a unique key specific to the current user request
     */
    public static String getUserCacheKey() {
        // TODO
        return "TODO";
    }

    /**
     * Returns the namespace used to discover services like gogs and gitlab when on premise
     */
    public static String getDiscoveryNamespace(KubernetesClient kubernetesClient) {
        String namespace = System.getenv(EnvironmentVariables.NAMESPACE);
        if (Strings.isNotBlank(namespace)) {
            return namespace;
        }
        namespace = kubernetesClient.getNamespace();
        if (Strings.isNotBlank(namespace)) {
            return namespace;
        }
        return KubernetesHelper.defaultNamespace();
    }

    /**
     * Validates that the namespace exists and if not tries to create it
     */
    public static void lazyCreateNamespace(KubernetesClient kubernetesClient, String namespace) {
        OpenShiftClient openShiftClient = getOpenShiftClientOrNull(kubernetesClient);
        if (openShiftClient != null) {
            Project project = null;
            try {
                project = openShiftClient.projects().withName(namespace).get();
            } catch (Exception e) {
                LOG.info("Caught exception looking up project " + namespace + ". " + e, e);
            }
            if (project != null) {
                return;
            }
            try {
                LOG.info("Creating project " + namespace);
                openShiftClient.projectrequests().createNew().withNewMetadata().withName(namespace).endMetadata().done();
            } catch (Exception e) {
                throw new IllegalStateException("Failed to create project " + namespace + " due to: " + e, e);
            }
        } else {
            Namespace resource = null;
            try {
                resource = kubernetesClient.namespaces().withName(namespace).get();
            } catch (Exception e) {
                LOG.info("Caught exception looking up namespace " + namespace + ". " + e, e);
            }
            if (resource != null) {
                return;
            }
            try {
                LOG.info("Creating namespace " + namespace);
                kubernetesClient.namespaces().createNew().withNewMetadata().withName(namespace).endMetadata().done();
            } catch (Exception e) {
                throw new IllegalStateException("Failed to create namespace " + namespace + " due to: " + e, e);
            }
        }
    }
}
