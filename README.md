# Elytron client - Kerberos for remoting

## About

This demo uses WildFly `ModelControllerClient` to show, how to use Kerberos for Elytron-enabled WildFly client over remoting protocol.

The demo application ([SimpleClient.java](src/main/java/org/wildfly/security/elytron/SimpleClient.java)) connects to a WildFly server and calls `:whoami` operation twice:

1. with default AuthenticationContext (from `wildfly-config.xml`)
1. with programmatically created `AuthenticationContext`

## Prerequisities

### Run kerberos KDC

Build [sample Kerberos server](https://github.com/kwart/kerberos-using-apacheds), create keytab for later usage and run the KDC.

```bash
cd /tmp
git clone https://github.com/kwart/kerberos-using-apacheds.git
cd kerberos-using-apacheds
mvn clean package
java -classpath target/kerberos-using-apacheds.jar org.jboss.test.kerberos.CreateKeytab remote/localhost@JBOSS.ORG remotepwd remote-localhost-remotepwd.keytab
java -jar target/kerberos-using-apacheds.jar test.ldif
```

The last step - running the server - generates the `krb5.conf` config file in the current directory. 

### Configure WildFly to use GSSAPI for Management interface

There are hardcoded paths to `/tmp/kerberos-using-apacheds` directory. So if you have your generated keytab or `krb5.conf` in another location, customize the script before the execution. 

```bash
bin/jboss-cli.sh --file=demo.cli
```

### Start the WildFly server

```bash
bin/standalone.sh
```


## Authenticate to kerberos and run demo

Use "**secret**" as a password for hnelson user.

```bash
KRB5_CONFIG=/tmp/kerberos-using-apacheds/krb5.conf kinit hnelson@JBOSS.ORG
```

You can run the demo directly from the Maven build:

```bash
mvn clean package
java -Djava.security.krb5.conf=/tmp/kerberos-using-apacheds/krb5.conf -Djavax.security.auth.useSubjectCredsOnly=false -jar target/elytron-client-demo.jar
```
