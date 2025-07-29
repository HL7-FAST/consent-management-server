# syntax=docker/dockerfile:1

#####################################
# STAGE 1: Build WAR with Maven
#####################################
FROM maven:3.9.9-eclipse-temurin-24 AS build

WORKDIR /app

COPY . .

# Build with Maven (assigning version and build values, skipping tests)
RUN mvn clean process-resources install -DbuildNumber=docker-ci -DbuildVersion=0.1.0-SNAPSHOT -DskipTests

#####################################
# STAGE 2: Final Image with MySQL + Wildfly
#####################################
FROM mysql:8.0

# --- MySQL Setup ---
COPY ./docker/wait_then_shutdown.sh /tmp/wait_then_shutdown.sh
RUN chmod +x /tmp/wait_then_shutdown.sh
USER mysql
COPY ./docker/00_wildfhirr4_DDL.sql /docker-entrypoint-initdb.d/00_wildfhirr4_DDL.sql
COPY ./docker/01_calcDistanceKm_function.sql /docker-entrypoint-initdb.d/01_calcDistanceKm_function.sql
COPY ./docker/02_calcDistanceMi_function.sql /docker-entrypoint-initdb.d/02_calcDistanceMi_function.sql
COPY ./docker/03_code_insert.sql /docker-entrypoint-initdb.d/03_code_insert.sql
COPY ./docker/04_conformance_insert.sql /docker-entrypoint-initdb.d/04_conformance_insert.sql
COPY ./docker/05_serverdirectory_insert.sql /docker-entrypoint-initdb.d/05_serverdirectory_insert.sql
COPY ./docker/06_resource_insert.sql /docker-entrypoint-initdb.d/06_resource_insert.sql
COPY ./docker/07_resourcemetadata_insert.sql /docker-entrypoint-initdb.d/07_resourcemetadata_insert.sql
COPY ./docker/99_last_processed_file.sh /docker-entrypoint-initdb.d/99_last_processed_file.sh
COPY ./docker/my.cnf /etc/my.cnf

ENV MYSQL_ALLOW_EMPTY_PASSWORD=1

RUN /entrypoint.sh mysqld & /tmp/wait_then_shutdown.sh

# --- Wildfly + Java ---
USER root
RUN microdnf -y update && \
    microdnf -y install java-11-openjdk-devel wget && \
    microdnf clean all

RUN useradd -ms /bin/bash jboss

ENV WILDFLY_VERSION=20.0.1.Final
ENV WILDFLY_SHA1=95366b4a0c8f2e6e74e3e4000a98371046c83eeb
ENV JBOSS_HOME=/opt/jboss/wildfly
ENV LAUNCH_JBOSS_IN_BACKGROUND=true

RUN cd /opt && mkdir jboss && \
    cd $HOME && \
    curl -O https://download.jboss.org/wildfly/${WILDFLY_VERSION}/wildfly-${WILDFLY_VERSION}.tar.gz && \
    sha1sum wildfly-${WILDFLY_VERSION}.tar.gz | grep $WILDFLY_SHA1 && \
    tar xf wildfly-${WILDFLY_VERSION}.tar.gz && \
    mv wildfly-${WILDFLY_VERSION} ${JBOSS_HOME} && \
    rm wildfly-${WILDFLY_VERSION}.tar.gz && \
    chown -R jboss:0 ${JBOSS_HOME} && \
    chmod -R g+rw ${JBOSS_HOME}

USER jboss
RUN ${JBOSS_HOME}/bin/add-user.sh admin admin --silent

ADD ./docker/mysql ${JBOSS_HOME}/modules/system/layers/base/com/mysql
COPY ./docker/standalone.conf ${JBOSS_HOME}/bin
COPY ./docker/standalone.sh ${JBOSS_HOME}/bin
# RUN chmod +x ${JBOSS_HOME}/bin/standalone.sh
COPY ./docker/standalone.xml ${JBOSS_HOME}/standalone/configuration

# ✅ COPY WAR FROM MAVEN BUILD STAGE
COPY --from=build /app/wildfhir-rest-server/target/wildfhir-rest-server.war ${JBOSS_HOME}/standalone/deployments/
COPY --from=build /app/wildfhir-client/target/wildfhir-client.war ${JBOSS_HOME}/standalone/deployments/

# FHIR Package
RUN mkdir -p /home/jboss/.fhir/packages/hl7.fhir.us.consent-management#0.1.0
ADD ./docker/hl7.fhir.us.consent-management#0.1.0 /home/jboss/.fhir/packages/hl7.fhir.us.consent-management#0.1.0

EXPOSE 3306 8080 8443 9990

USER root
COPY ./docker/start.sh /opt/start.sh
RUN chmod +x /opt/start.sh

ENTRYPOINT ["/opt/start.sh"]
