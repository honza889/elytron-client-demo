/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2016 Red Hat, Inc., and individual contributors
 * as indicated by the @author tags.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.wildfly.security.elytron;

import java.io.FileInputStream;
import java.security.KeyStore;
import java.security.Provider;
import java.util.Collections;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.jboss.as.controller.client.ModelControllerClient;
import org.jboss.as.controller.client.ModelControllerClientConfiguration;
import org.jboss.dmr.ModelNode;
import org.wildfly.security.WildFlyElytronProvider;
import org.wildfly.security.auth.client.AuthenticationConfiguration;
import org.wildfly.security.auth.client.AuthenticationContext;
import org.wildfly.security.auth.client.MatchRule;
import org.wildfly.security.sasl.SaslMechanismSelector;
import org.wildfly.security.sasl.WildFlySasl;
import org.wildfly.security.ssl.SSLContextBuilder;

import javax.net.ssl.TrustManagerFactory;
import javax.net.ssl.X509TrustManager;

/**
 * @author <a href="mailto:darran.lofthouse@jboss.com">Darran Lofthouse</a>
 */
public class SimpleClient {

    
    private static final String LOOPBACK = "127.0.0.1";
    private static final String HOSTNAME = System.getProperty("hostname", LOOPBACK);

    public static void main(String[] args) throws Exception {
        Runnable runnable = new Runnable() {
            public void run() {
                try {
                    ModelControllerClient client = ModelControllerClient.Factory
                            .create(new ModelControllerClientConfiguration.Builder()
                                    .setHostName(HOSTNAME)
                                    .setPort(9993)
                                    .setProtocol("remote+https")
                                    .setConnectionTimeout(36000).build());

                    ModelNode operation = new ModelNode();
                    operation.get("operation").set("whoami");
                    operation.get("verbose").set("true");

                    System.out.println("Executing Operation\n");
                    System.out.println(operation.toString());

                    ModelNode result = client.execute(operation);

                    System.out.println("\nResult\n");
                    System.out.println(result.toString());
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        };
        
        System.out.println(">>> Demo - AuthenticationContext created programatically");
        KeyStore ks = KeyStore.getInstance("JKS");
        ks.load(new FileInputStream("/home/jkalina/work/wildfly/build/target/wildfly-12.0.0.Alpha1-SNAPSHOT/standalone/configuration/keystore.jks"), "secret1".toCharArray());
        TrustManagerFactory tmf = TrustManagerFactory.getInstance("SunX509");
        tmf.init(ks);

        AuthenticationContext context = AuthenticationContext.empty()
                .with(MatchRule.ALL, AuthenticationConfiguration.empty()
                        .useProviders(() -> new Provider[] { new WildFlyElytronProvider() })
                        //.setSaslMechanismSelector(SaslMechanismSelector.NONE.addMechanism("SCRAM-SHA-1-PLUS"))
                        .setSaslMechanismSelector(SaslMechanismSelector.NONE.addMechanism("SCRAM-SHA-1"))
                        .useName("admin")
                        .usePassword("admin")
                        //.useMechanismProperties(Collections.singletonMap(WildFlySasl.CHANNEL_BINDING_UNSUPPORTED, "true"))
                ).withSsl(MatchRule.ALL, new SSLContextBuilder()
                .setTrustManager((X509TrustManager) tmf.getTrustManagers()[0])
                .setClientMode(true)
                .build()
        );
        context.run(runnable);
    }

    
    static {
        Logger.getLogger("org").setLevel(Level.WARNING);
    }
}
