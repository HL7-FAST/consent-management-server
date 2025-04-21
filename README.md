# FAST Consent Management Reference Implementation

This is a FHIR server reference implementation of the [FAST Scalable Consent Management IG](https://build.fhir.org/ig/HL7/fhir-consent-management).  It is built on the [WildFHIR Community Edition](https://github.com/AEGISnetInc/WildFHIR) project and more detailed configuration information can be found in that repository.

## Foundry
> [!NOTE]
> **TBD** A live demo will be hosted by [HL7 FHIR Foundry](https://foundry.hl7.org), where you will also be able to download curated configurations to run yourself.

## Prerequisites
Building and running the server locally requires either Docker or
- Java 11+
- Red Hat Wildfly 20.0.1.Final
- MySQL Community Edition 8.0

## Using Red Hat Wildfly

Please see the [WildFHIR Community Edition Wiki - Installation](https://github.com/AEGISnetInc/WildFHIR/wiki/Installation) instructions.

## Using Docker

Please see the HL7 Foundry [WildFHIR Community Edition](https://foundry.hl7.org/products/3ffe9658-2849-417c-80d6-2ba3661f553f) product page.

## FAST Consent Custom Operations

The server implements the custom operations as described in the IG in the [Artifacts - Operation Definitions](https://build.fhir.org/ig/HL7/fhir-consent-management/artifacts.html#behavior-operation-definitions) section.


## Security

The server currently does not enable any security interface - OAuth, UDAP, etc.


## Questions and Contributions
Questions about the project can be asked in the [FAST Consent Management stream on the FHIR Zulip Chat](https://chat.fhir.org/#narrow/channel/426241-FHIR-at-Scale-.28FAST.29.3A-Consent-Management).

This project welcomes Pull Requests. Any issues identified with the RI should be submitted via the [GitHub issue tracker](https://github.com/HL7-FAST/consent-management-server/issues).

AEGIS.net, Inc. is responsible for the management and maintenance of this Reference Implementation.
In addition to posting on FHIR Zulip Chat channel mentioned above you can contact [Richard Ettema](mailto:richard.ettema@aegis.net) for questions or requests.
