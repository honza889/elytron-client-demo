# Elytron client demo

## About

This demo uses WildFly `ModelControllerClient` to show, how to work with an Elytron-enabled WildFly client.

The demo application ([SimpleClient.java](src/main/java/org/wildfly/security/elytron/SimpleClient.java)) connects to a WildFly server and calls `:whoami` operation twice:

1. with default AuthenticationContext (from `wildfly-config.xml`)
1. with programmatically created `AuthenticationContext`

### wildfly-config.xml

The default context is [loaded](https://github.com/wildfly/wildfly-client-config/blob/master/src/main/java/org/wildfly/client/config/ClientConfiguration.java#L131-L170) by a discovery mechanism ([wildfly-client-config](https://github.com/wildfly/wildfly-client-config) GitHub project) and can be customized by a `wildfly.config.url` system property.

The Elytron part of `wildfly-config.xml` client configuration is described in Elytron XSD (e.g. [version 1.1.0.Beta17](https://github.com/wildfly-security/wildfly-elytron/blob/1.1.0.Beta17/src/main/resources/schema/elytron-1_0.xsd)).

### Elytron API

Entrypoint for the programmatic Elytron client configuration is the class [AuthenticationContext](https://github.com/wildfly-security/wildfly-elytron/blob/1.1.0.Beta17/src/main/java/org/wildfly/security/auth/client/AuthenticationContext.java).

The `AuthenticationContext` instance created in this demo contains following rules:

1. client connecting to `localhost` hostname is handled as `administrator`
1. any client is handled as `monitor`

## Prerequisities

### Add management users

```bash
bin/add-user.sh -u monitor -p password1! -s
bin/add-user.sh -u administrator -p password1! -s
```

### Enable Elytron across the server

```bash
bin/jboss-cli.sh --file=enable-elytron.cli
```

### Run WildFly/EAP

```bash
bin/standalone.sh
```

## Run demo

### Default run
The default WildFly host to which this client connects is `127.0.0.1` 

```bash
mvn package exec:java
```

The first demo should print `$local` username:
```
"identity" => {"username" => "$local"},
```
Default configuration doesn't contain any user/password specification.

The second demo should print `monitor` username:
```
"identity" => {"username" => "monitor"},
```
As the default host is `127.0.0.1` and not the `localhost`, we see here the `monitor` identity.

### Run with hostname specified
By setting system property `hostname` you can set to which host the controller client will connect: 

```bash
mvn package exec:java -Dhostname=localhost
```
The first part of the demo should still report `$local`user, but the second part should print `administrator` user:
```
"identity" => {"username" => "administrator"},
```

### Run with custom wildfly-config XML
By setting system property `wildfly.config.url` you can control from which location is the default `AuthenticationContext` configuration loaded.

```bash
mvn package exec:java -Dwildfly.config.url=custom-config.xml
```
The first part of the demo should now report the same user as the second one:
```
"identity" => {"username" => "monitor"},
```

## Play with code

For instance, you can try to use `AuthenticationContext.captureCurrent()` instead of `AuthenticationContext.EMPTY`, which should take current context as a base instead of
building one from scratch.
