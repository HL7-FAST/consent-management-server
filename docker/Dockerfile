# syntax=docker/dockerfile:1

##############################
# HL7 FAST Consent RI Server #
##############################
# WildFHIR CE MySQL Database #
##############################

# Use the official MySQL 8.0 image as the WildFHIR CE database and server
FROM mysql:8.0

# Used to gracefully shutdown mysql after each execution
COPY ./wait_then_shutdown.sh /tmp/wait_then_shutdown.sh

# Set the default user for the MySQL image
USER mysql

# Copy DDL and shell scripts
COPY ./00_wildfhirr4_DDL.sql /docker-entrypoint-initdb.d/00_wildfhirr4_DDL.sql
COPY ./01_calcDistanceKm_function.sql /docker-entrypoint-initdb.d/01_calcDistanceKm_function.sql
COPY ./02_calcDistanceMi_function.sql /docker-entrypoint-initdb.d/02_calcDistanceMi_function.sql
COPY ./03_code_insert.sql /docker-entrypoint-initdb.d/03_code_insert.sql
COPY ./04_conformance_insert.sql /docker-entrypoint-initdb.d/04_conformance_insert.sql
COPY ./05_serverdirectory_insert.sql /docker-entrypoint-initdb.d/05_serverdirectory_insert.sql
COPY ./06_resource_insert.sql /docker-entrypoint-initdb.d/06_resource_insert.sql
COPY ./07_resourcemetadata_insert.sql /docker-entrypoint-initdb.d/07_resourcemetadata_insert.sql
COPY ./99_last_processed_file.sh /docker-entrypoint-initdb.d/99_last_processed_file.sh

# Copy MySQL custom configuration
# - innodb_buffer_pool_size = 1024M
# - max_connections=256
COPY ./my.cnf /etc/my.cnf

# Set environment variables for MySQL
ENV MYSQL_ALLOW_EMPTY_PASSWORD=1

RUN /entrypoint.sh mysqld & /tmp/wait_then_shutdown.sh

# Expose port 3306 to allow connections to the database
# EXPOSE 3306

################################################
# WildFHIR CE MySQL Wildfly Application Server #
################################################

# Install Java 11 package
USER root

RUN microdnf -y update && \
    microdnf -y install java-11-openjdk-devel wget && \
    microdnf clean all

# Add jboss user account
RUN useradd -ms /bin/bash jboss

# Set the WILDFLY_VERSION env variable
ENV WILDFLY_VERSION=20.0.1.Final
ENV WILDFLY_SHA1=95366b4a0c8f2e6e74e3e4000a98371046c83eeb
ENV JBOSS_HOME=/opt/jboss/wildfly

# Add the WildFly distribution to /opt, and make jboss the owner of the extracted tar content
# Make sure the distribution is available from a well-known place
RUN cd /opt && mkdir jboss \
    && cd $HOME \
    && curl -O https://download.jboss.org/wildfly/$WILDFLY_VERSION/wildfly-$WILDFLY_VERSION.tar.gz \
    && sha1sum wildfly-$WILDFLY_VERSION.tar.gz | grep $WILDFLY_SHA1 \
    && tar xf wildfly-$WILDFLY_VERSION.tar.gz \
    && mv $HOME/wildfly-$WILDFLY_VERSION $JBOSS_HOME \
    && rm wildfly-$WILDFLY_VERSION.tar.gz \
    && chown -R jboss:0 ${JBOSS_HOME} \
    && chmod -R g+rw ${JBOSS_HOME}

# Ensure signals are forwarded to the JVM process correctly for graceful shutdown
ENV LAUNCH_JBOSS_IN_BACKGROUND=true

USER jboss

RUN /opt/jboss/wildfly/bin/add-user.sh admin admin --silent

# Add MySQL 8.0 connector library
ADD ./mysql $JBOSS_HOME/modules/system/layers/base/com/mysql

# Copy standalone conf for start up with JAVA_OPTS settings
COPY ./standalone.conf $JBOSS_HOME/bin
COPY ./standalone.sh $JBOSS_HOME/bin

# Copy standalone configuration with datasource connection to wildfhirce-mysql and https access using a self-signed certificate
COPY ./standalone.xml $JBOSS_HOME/standalone/configuration

# Copy WildFHIR CE Server Application for FHIR R4 to Wildfly standalone deployments
COPY ./wildfhir-rest-server.war $JBOSS_HOME/standalone/deployments
COPY ./wildfhir-client.war $JBOSS_HOME/standalone/deployments

# FHIR Package
RUN mkdir -p /home/jboss/.fhir/packages/hl7.fhir.us.consent-management#0.1.0
ADD ./hl7.fhir.us.consent-management#0.1.0 /home/jboss/.fhir/packages/hl7.fhir.us.consent-management#0.1.0

# Client resources
RUN mkdir -p /home/jboss/initializeClient
ADD ./initializeClient /home/jboss/initializeClient

# Server resources
RUN mkdir -p /home/jboss/initializeServer
ADD ./initializeServer /home/jboss/initializeServer

# Expose the ports we're interested in
EXPOSE 3306 8080 8443 9990

USER root

# Start script
COPY start.sh /opt/start.sh
RUN chmod +x /opt/start.sh

ENTRYPOINT ["/opt/start.sh"]
