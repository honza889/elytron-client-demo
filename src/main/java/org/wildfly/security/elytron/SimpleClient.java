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
import java.security.KeyStoreException;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.net.ssl.KeyManager;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;
import javax.net.ssl.X509ExtendedKeyManager;
import javax.net.ssl.X509TrustManager;

import org.jboss.as.controller.client.ModelControllerClient;
import org.jboss.as.controller.client.ModelControllerClientConfiguration;
import org.jboss.dmr.ModelNode;
import org.wildfly.security.SecurityFactory;
import org.wildfly.security.auth.client.AuthenticationConfiguration;
import org.wildfly.security.auth.client.AuthenticationContext;
import org.wildfly.security.auth.client.MatchRule;
import org.wildfly.security.ssl.SSLContextBuilder;

public class SimpleClient {

    private static final String LOOPBACK = "127.0.0.1";
    private static final String HOSTNAME = System.getProperty("hostname", LOOPBACK);

    public static void main(String[] args) throws Exception {
        Runnable runnable = new Runnable() {
            public void run() {
                try {
                    ModelControllerClient client = ModelControllerClient.Factory
                            .create(new ModelControllerClientConfiguration.Builder().setHostName(HOSTNAME).setPort(9993)
                                    .setProtocol("https-remoting").setConnectionTimeout(600 * 1000).build());

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
        System.setProperty("javax.net.debug", "all");

        AuthenticationConfiguration authCfg = AuthenticationConfiguration.EMPTY.useDefaultProviders()
                .allowSaslMechanisms("EXTERNAL");

        SecurityFactory<SSLContext> ssl = new SSLContextBuilder().setClientMode(true)
                .setKeyManager(getKeyManager("dung.keystore")).setTrustManager(getCATrustManager()).build();

        AuthenticationContext.empty().with(MatchRule.ALL, authCfg).withSsl(MatchRule.ALL, ssl).run(runnable);
    }

    /**
     * Get the key manager backed by the specified key store.
     *
     * @param keystorePath the path to the keystore with X509 private key
     * @return the initialised key manager.
     */
    private static X509ExtendedKeyManager getKeyManager(final String keystorePath) throws Exception {
        KeyManagerFactory keyManagerFactory = KeyManagerFactory.getInstance("SunX509");
        keyManagerFactory.init(loadKeyStore(keystorePath), "Elytron".toCharArray());

        for (KeyManager current : keyManagerFactory.getKeyManagers()) {
            if (current instanceof X509ExtendedKeyManager) {
                return (X509ExtendedKeyManager) current;
            }
        }

        throw new IllegalStateException("Unable to obtain X509ExtendedKeyManager.");
    }

    /**
     * Get the trust manager that trusts all certificates signed by the certificate authority.
     *
     * @return the trust manager that trusts all certificates signed by the certificate authority.
     * @throws KeyStoreException
     */
    private static X509TrustManager getCATrustManager() throws Exception {
        TrustManagerFactory trustManagerFactory = TrustManagerFactory.getInstance("SunX509");
        trustManagerFactory.init(loadKeyStore("ca.truststore"));

        for (TrustManager current : trustManagerFactory.getTrustManagers()) {
            if (current instanceof X509TrustManager) {
                return (X509TrustManager) current;
            }
        }

        throw new IllegalStateException("Unable to obtain X509TrustManager.");
    }

    private static KeyStore loadKeyStore(final String path) throws Exception {
        KeyStore ks = KeyStore.getInstance("JKS");
        try (FileInputStream fis = new FileInputStream(
                "/home/kwart/projects/wildfly-security/wildfly-elytron/src/test/resources/ca/jks/" + path)) {
            ks.load(fis, "Elytron".toCharArray());
        }
        return ks;
    }

    static {
        Logger.getLogger("org").setLevel(Level.WARNING);
    }
}
