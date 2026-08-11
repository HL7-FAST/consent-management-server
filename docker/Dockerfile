# syntax=docker/dockerfile:1
 
#####################################################################
# STAGE 1: Official WildFly 39 + JDK 21 image, used only as a source
# for COPY --from below. This image already bundles eclipse-temurin
# JDK 21 and a full WildFly 39.0.1.Final install, so there's no need
# to jlink a custom JDK or download/extract the WildFly tarball
# ourselves - both come pre-built from this stage.
#
# NOTE: double-check the exact tag on
# https://quay.io/repository/wildfly/wildfly?tab=tags before building -
# documented tags follow {version}-jdk{N}; the "-2" revision suffix
# below reflects a rebuild of the image itself, not the WildFly version.
#####################################################################
FROM quay.io/wildfly/wildfly:39.0.1.Final-2-jdk21 AS wildfly-source
 
#####################################################################
# STAGE 2: Build a slim, runtime-only JDK 21 via jlink, based on the
# JDK bundled in the wildfly-source image above. This stage's full-JDK
# layers (compiler, jshell, jconsole, debug symbols, source archives)
# are never copied into the final image - only the jlink --output
# directory is, via COPY --from=jdk-slim below. Because this runs as
# its own stage rather than a COPY-then-rm inside the final stage, the
# full JDK never touches a layer that ends up in the final image.
#####################################################################
FROM wildfly-source AS jdk-slim
# This stage inherits USER jboss from wildfly-source, which lacks permission
# to create a new directory at filesystem root - switch to root for jlink.
USER root
RUN rm -rf /optimized-jdk-21 && \
    /opt/java/openjdk/bin/jlink \
        --module-path /opt/java/openjdk/jmods \
        --add-modules ALL-MODULE-PATH \
        --strip-debug \
        --no-man-pages \
        --no-header-files \
        --compress=zip-6 \
        --output /optimized-jdk-21
 
##############################
# HL7 FAST Consent RI Server #
##############################
# WildFHIR CE MySQL Database #
##############################
 
# Use the official MySQL 9.7 LTS image as the WildFHIR CE database and server
FROM mysql:9.7.2
 
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
 
# Note: MySQL 8.4+ removed the /entrypoint.sh backwards-compat symlink that
# existed through 8.0 - only docker-entrypoint.sh (on PATH) exists now.
#
# Deliberately NOT running docker-entrypoint.sh at build time here. Doing so
# would bake the initialized /var/lib/mysql data directory - InnoDB system
# tablespace, redo logs, and all seeded DDL/insert data - permanently into an
# image layer (this cost ~225MB in a prior build). The official entrypoint
# already runs everything in /docker-entrypoint-initdb.d/ automatically on
# first container start whenever the data directory is empty, so the DDL and
# seed scripts above still execute correctly - just on first boot instead of
# at build time. Tradeoff: first container start takes longer; the image
# itself stays smaller and isn't carrying baked-in state.
# RUN docker-entrypoint.sh mysqld & /tmp/wait_then_shutdown.sh
 
# Expose port 3306 to allow connections to the database
# EXPOSE 3306
 
################################################
# WildFHIR CE MySQL Wildfly Application Server #
################################################
 
USER root
 
# Create the jboss user on the mysql base image (matching uid/gid 1000 used
# by the official WildFly image, though chown below would fix this regardless).
# microdnf can install directly from configured repos - no need to pull in
# the full dnf package manager (and its Python3 dependency chain) just for
# the shadow-utils package.
RUN microdnf -y update && \
    microdnf -y install shadow-utils util-linux && \
    microdnf clean all && \
    rm -rf /var/cache/yum /var/cache/dnf && \
    useradd -u 1000 -ms /bin/bash jboss
 
ENV JAVA_HOME=/opt/jdk/jdk-21
ENV JBOSS_HOME=/opt/jboss/wildfly
 
# Copy the WildFly 39.0.1.Final install straight from the official quay.io
# image (see stage above) rather than downloading/extracting it here.
COPY --from=wildfly-source $JBOSS_HOME $JBOSS_HOME
 
# Copy the pre-built slim JDK from the jdk-slim stage (see above) - the full
# JDK used to build it never becomes part of this image's layer history.
COPY --from=jdk-slim /optimized-jdk-21 $JAVA_HOME
 
RUN chown -R jboss:0 ${JBOSS_HOME} && \
    chmod -R g+rw ${JBOSS_HOME}
 
# Ensure signals are forwarded to the JVM process correctly for graceful shutdown
ENV LAUNCH_JBOSS_IN_BACKGROUND=true
 
USER jboss
 
# Add MySQL connector library (ensure this is Connector/J 9.x - see notes)
ADD ./mysql $JBOSS_HOME/modules/system/layers/base/com/mysql
 
# Copy add-user.sh with JAVA_OPTS settings
COPY ./add-user.sh $JBOSS_HOME/bin
 
# Copy standalone conf for start up with JAVA_OPTS settings
COPY ./standalone.conf $JBOSS_HOME/bin
COPY ./standalone.sh $JBOSS_HOME/bin
 
# Copy standalone configuration with datasource connection to wildfhirce-mysql and https access using a self-signed certificate
COPY ./standalone.xml $JBOSS_HOME/standalone/configuration
 
# Add Wildfly default admin user
RUN /opt/jboss/wildfly/bin/add-user.sh admin admin --silent
 
# Copy WildFHIR CE Application for FHIR R4 FAST Consent RI to Wildfly standalone deployments
COPY ./fastconsentri-rest-server.war $JBOSS_HOME/standalone/deployments
COPY ./fastconsentri-client.war $JBOSS_HOME/standalone/deployments
 
# FAST Consent Package, Client resources, Server resources
RUN mkdir -p /home/jboss/.fhir/packages/hl7.fhir.us.consent-management#1.0.0-preview && \
    mkdir -p /home/jboss/.fhir/packages/hl7.fhir.uv.subscriptions-backport.r4#1.1.0 && \
    mkdir -p /home/jboss/initializeClient && \
    mkdir -p /home/jboss/initializeServer
 
ADD ./hl7.fhir.us.consent-management#1.0.0-preview /home/jboss/.fhir/packages/hl7.fhir.us.consent-management#1.0.0-preview
ADD ./hl7.fhir.uv.subscriptions-backport.r4#1.1.0 /home/jboss/.fhir/packages/hl7.fhir.uv.subscriptions-backport.r4#1.1.0
ADD ./initializeClient /home/jboss/initializeClient
ADD ./initializeServer /home/jboss/initializeServer
 
# Expose the ports we're interested in
EXPOSE 3306 8080 8443 9990
 
USER root
 
# Start script
COPY start.sh /opt/start.sh
RUN chmod +x /opt/start.sh
 
ENTRYPOINT ["/opt/start.sh"]
 
