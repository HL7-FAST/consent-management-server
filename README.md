# FAST Consent Management Reference Implementation

[![Build Status](https://ci.hl7.org/api/badges/HL7-FAST/consent-management-server/status.svg)](https://ci.hl7.org/HL7-FAST/consent-management-server)

This is the [FAST Consent Management Reference Implementation](https://github.com/HL7/fhir-consent-management) of the [FAST Scalable Consent Management IG](https://build.fhir.org/ig/HL7/fhir-consent-management).  It is built on the [WildFHIR Community Edition](https://github.com/AEGISnetInc/WildFHIR) project and more detailed configuration information can be found in that repository.

## Foundry

The live deployment of this RI application is hosted in the [HL7 FHIR Foundry](https://foundry.hl7.org), where you will also be able to download curated configurations to run yourself.

| Name | Link |
| ---- | ---- |
| RI Server Base URL | https://consent-management-server.fast.hl7.org/fastconsent/r4 |
| RI Client URL | https://consent-management-server.fast.hl7.org/fastconsent/r4-client |

## Prerequisites
Building and running the server locally requires either Docker or
- Java 17+
- Red Hat Wildfly 39.0.1.Final
- MySQL Community Edition 9.x

## Using Red Hat Wildfly

Please see the [WildFHIR Community Edition Wiki - Installation](https://github.com/AEGISnetInc/WildFHIR/wiki/Installation-v0.7.1) instructions.

## Running via [Docker Hub](https://hub.docker.com/r/hlseven/fast-consent-management-server)

Each tagged/released version of `fast-consent-management-server` is built as a Docker image and published to Docker hub. To run the published Docker image from Docker Hub:

```
docker pull hlseven/fast-consent-management-server:latest
docker run -p 8080:8080 hlseven/fast-consent-management-server:latest
```

This will run the docker image with the default configuration, mapping port 8080 from the container to port 8080 in the host. Once running, you can access `http://localhost:8080/fastconsent/r4` as the base URL for your REST requests.

### Configuration via environment variables

You can customize the behavior and capabilities of the underlying WildFHIR CE server directly from the `run` command using environment variables. For example, FHIR validation support for the [FAST Scalable Consent Management IG](https://build.fhir.org/ig/HL7/fhir-consent-management) via the $validate operation requires the installation of the corresponding validation package via the FHIR_PACKAGES environment variable:

```
docker run -p 8080:8080 -e FHIR_PACKAGES=hl7.fhir.us.consent-management#1.0.0-preview hlseven/fast-consent-management-server:latest
```

or, to facilitate the definition of multiple settings you can create and use an environment variable list file; e.g. 'env.list' (see https://github.com/HL7-FAST/consent-management-server/blob/main/docker/env.list for a complete list):

```
docker run -p 8080:8080 --env-file env.list hlseven/fast-consent-management-server:latest
```

### Additional Port Mappings

You can also expose the following additional ports if desired:

- 3306:3306 - MySQL 8x database access; the MySQL root user does not define a password
- 8443:8443 - Secured access; the WildFHIR CE Red Hat Wildfly container generates a self-signed SSL certificate to support https
- 9990:9990 - Access to the WildFHIR CE Red Hat Wildfly admin console; default user 'admin' password 'admin'


## FAST Consent Custom Operations

The server implements the custom operations as described in the IG in the [Artifacts - Operation Definitions](https://build.fhir.org/ig/HL7/fhir-consent-management/artifacts.html#behavior-operation-definitions) section.


## FAST Consent Custom Search Parameters

The server implements the custom search parameters as described in the IG in the [Artifacts - Search Parameters](https://build.fhir.org/ig/HL7/fhir-consent-management/artifacts.html#behavior-search-parameters) section.


## Subscriptions R5 Backport Operations

The server implements the `Subscription/$status` custom operation as described in the Subscriptions R5 Backport IG in the [OperationDefinition: Subscription Status Operation](http://hl7.org/fhir/uv/subscriptions-backport/STU1.1/OperationDefinition-backport-subscription-status.html) section.


## Security

The server currently does not enable any security interface beyond HTTPS/TLS 1.2.


## Questions and Contributions
Questions about the project can be asked in the [FAST Consent Management stream on the FHIR Zulip Chat](https://chat.fhir.org/#narrow/channel/426241-FHIR-at-Scale-.28FAST.29.3A-Consent-Management).

This project welcomes Pull Requests. Any issues identified with the RI should be submitted via the [GitHub issue tracker](https://github.com/HL7-FAST/consent-management-server/issues).

AEGIS.net, Inc. is responsible for the management and maintenance of this Reference Implementation.
In addition to posting on FHIR Zulip Chat channel mentioned above you can contact [Richard Ettema](mailto:richard.ettema@aegis.net) for questions or requests.
