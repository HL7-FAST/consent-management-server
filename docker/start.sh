#!/bin/bash
set -x

echo "[start.sh] Launching MySQL via docker-entrypoint.sh..."
docker-entrypoint.sh mysqld &
MYSQL_PID=$!

echo "[start.sh] Waiting for MySQL to accept connections..."
for i in $(seq 1 60); do
    if mysqladmin ping --silent 2>/dev/null; then
        echo "[start.sh] MySQL is ready after ${i}s."
        break
    fi
    if [ "$i" -eq 60 ]; then
        echo "[start.sh] ERROR: MySQL did not become ready within 60s. Exiting."
        exit 1
    fi
    sleep 1
done

# Wait for MySQL to complete entry point start
sleep 15

# Remove Wildfly bin/env.conf
rm -f /opt/jboss/wildfly/bin/env.conf

# Echo environment variables to Wildfly bin/env.conf if defined
if [ "x$WILDFHIR_HOST" = "x" ]; then
  # Skip
  echo "WILDFHIR_HOST not defined"
else
  echo "export WILDFHIR_HOST=$WILDFHIR_HOST" >> /opt/jboss/wildfly/bin/env.conf
  echo "WILDFHIR_HOST:                       $WILDFHIR_HOST"
fi

if [ "x$WILDFHIR_DATABASE_HOST" = "x" ]; then
  # Skip
  echo "WILDFHIR_DATABASE_HOST not defined"
else
  echo "export WILDFHIR_DATABASE_HOST=$WILDFHIR_DATABASE_HOST" >> /opt/jboss/wildfly/bin/env.conf
  echo "WILDFHIR_DATABASE_HOST:              $WILDFHIR_DATABASE_HOST"
fi

if [ "x$WILDFHIR_BASEURL" = "x" ]; then
  # Skip
  echo "WILDFHIR_BASEURL not defined"
else
  echo "export WILDFHIR_BASEURL=$WILDFHIR_BASEURL" >> /opt/jboss/wildfly/bin/env.conf
  echo "WILDFHIR_BASEURL:                    $WILDFHIR_BASEURL"
fi

if [ "x$WILDFHIR_CONDITIONALDELETE" = "x" ]; then
  # Skip
  echo "WILDFHIR_CONDITIONALDELETE not defined"
else
  echo "export WILDFHIR_CONDITIONALDELETE=$WILDFHIR_CONDITIONALDELETE" >> /opt/jboss/wildfly/bin/env.conf
  echo "WILDFHIR_CONDITIONALDELETE:          $WILDFHIR_CONDITIONALDELETE"
fi

if [ "x$WILDFHIR_CONDITIONALREAD" = "x" ]; then
  # Skip
  echo "WILDFHIR_CONDITIONALREAD not defined"
else
  echo "export WILDFHIR_CONDITIONALREAD=$WILDFHIR_CONDITIONALREAD" >> /opt/jboss/wildfly/bin/env.conf
  echo "WILDFHIR_CONDITIONALREAD:            $WILDFHIR_CONDITIONALREAD"
fi

if [ "x$WILDFHIR_CONDITIONALCREATE" = "x" ]; then
  # Skip
  echo "WILDFHIR_CONDITIONALCREATE not defined"
else
  echo "export WILDFHIR_CONDITIONALCREATE=$WILDFHIR_CONDITIONALCREATE" >> /opt/jboss/wildfly/bin/env.conf
  echo "WILDFHIR_CONDITIONALCREATE:          $WILDFHIR_CONDITIONALCREATE"
fi

if [ "x$WILDFHIR_CONDITIONALUPDATE" = "x" ]; then
  # Skip
  echo "WILDFHIR_CONDITIONALUPDATE not defined"
else
  echo "export WILDFHIR_CONDITIONALUPDATE=$WILDFHIR_CONDITIONALUPDATE" >> /opt/jboss/wildfly/bin/env.conf
  echo "WILDFHIR_CONDITIONALUPDATE:          $WILDFHIR_CONDITIONALUPDATE"
fi

if [ "x$WILDFHIR_CREATERESPONSEPAYLOAD" = "x" ]; then
  # Skip
  echo "WILDFHIR_CREATERESPONSEPAYLOAD not defined"
else
  echo "export WILDFHIR_CREATERESPONSEPAYLOAD=$WILDFHIR_CREATERESPONSEPAYLOAD" >> /opt/jboss/wildfly/bin/env.conf
  echo "WILDFHIR_CREATERESPONSEPAYLOAD:      $WILDFHIR_CREATERESPONSEPAYLOAD"
fi

if [ "x$WILDFHIR_SEARCHRESPONSEPAYLOAD" = "x" ]; then
  # Skip
  echo "WILDFHIR_SEARCHRESPONSEPAYLOAD not defined"
else
  echo "export WILDFHIR_SEARCHRESPONSEPAYLOAD=$WILDFHIR_SEARCHRESPONSEPAYLOAD" >> /opt/jboss/wildfly/bin/env.conf
  echo "WILDFHIR_SEARCHRESPONSEPAYLOAD:      $WILDFHIR_SEARCHRESPONSEPAYLOAD"
fi

if [ "x$WILDFHIR_UPDATERESPONSEPAYLOAD" = "x" ]; then
  # Skip
  echo "WILDFHIR_UPDATERESPONSEPAYLOAD not defined"
else
  echo "export WILDFHIR_UPDATERESPONSEPAYLOAD=$WILDFHIR_UPDATERESPONSEPAYLOAD" >> /opt/jboss/wildfly/bin/env.conf
  echo "WILDFHIR_UPDATERESPONSEPAYLOAD:      $WILDFHIR_UPDATERESPONSEPAYLOAD"
fi

if [ "x$WILDFHIR_RESOURCEPURGEALLENABLED" = "x" ]; then
  # Skip
  echo "WILDFHIR_RESOURCEPURGEALLENABLED not defined"
else
  echo "export WILDFHIR_RESOURCEPURGEALLENABLED=$WILDFHIR_RESOURCEPURGEALLENABLED" >> /opt/jboss/wildfly/bin/env.conf
  echo "WILDFHIR_RESOURCEPURGEALLENABLED:    $WILDFHIR_RESOURCEPURGEALLENABLED"
fi

if [ "x$WILDFHIR_LASTNPROCESSEMPTYDATE" = "x" ]; then
  # Skip
  echo "WILDFHIR_LASTNPROCESSEMPTYDATE not defined"
else
  echo "export WILDFHIR_LASTNPROCESSEMPTYDATE=$WILDFHIR_LASTNPROCESSEMPTYDATE" >> /opt/jboss/wildfly/bin/env.conf
  echo "WILDFHIR_LASTNPROCESSEMPTYDATE:      $WILDFHIR_LASTNPROCESSEMPTYDATE"
fi

if [ "x$WILDFHIR_LASTNEMPTYDATEVALUE" = "x" ]; then
  # Skip
  echo "WILDFHIR_LASTNEMPTYDATEVALUE not defined"
else
  echo "export WILDFHIR_LASTNEMPTYDATEVALUE=$WILDFHIR_LASTNEMPTYDATEVALUE" >> /opt/jboss/wildfly/bin/env.conf
  echo "WILDFHIR_LASTNEMPTYDATEVALUE:        $WILDFHIR_LASTNEMPTYDATEVALUE"
fi

if [ "x$WILDFHIR_AUDITEVENTSERVICEENABLED" = "x" ]; then
  # Skip
  echo "WILDFHIR_AUDITEVENTSERVICEENABLED not defined"
else
  echo "export WILDFHIR_AUDITEVENTSERVICEENABLED=$WILDFHIR_AUDITEVENTSERVICEENABLED" >> /opt/jboss/wildfly/bin/env.conf
  echo "WILDFHIR_AUDITEVENTSERVICEENABLED:   $WILDFHIR_AUDITEVENTSERVICEENABLED"
fi

if [ "x$WILDFHIR_PROVENANCESERVICEENABLED" = "x" ]; then
  # Skip
  echo "WILDFHIR_PROVENANCESERVICEENABLED not defined"
else
  echo "export WILDFHIR_PROVENANCESERVICEENABLED=$WILDFHIR_PROVENANCESERVICEENABLED" >> /opt/jboss/wildfly/bin/env.conf
  echo "WILDFHIR_PROVENANCESERVICEENABLED:   $WILDFHIR_PROVENANCESERVICEENABLED"
fi

if [ "x$WILDFHIR_SUBSCRIPTIONSERVICEENABLED" = "x" ]; then
  # Skip
  echo "WILDFHIR_SUBSCRIPTIONSERVICEENABLED not defined"
else
  echo "export WILDFHIR_SUBSCRIPTIONSERVICEENABLED=$WILDFHIR_SUBSCRIPTIONSERVICEENABLED" >> /opt/jboss/wildfly/bin/env.conf
  echo "WILDFHIR_SUBSCRIPTIONSERVICEENABLED: $WILDFHIR_SUBSCRIPTIONSERVICEENABLED"
fi

if [ "x$WILDFHIR_SUBSCRIPTIONACTIVATEREQUESTED" = "x" ]; then
  # Skip
  echo "WILDFHIR_SUBSCRIPTIONACTIVATEREQUESTED not defined"
else
  echo "export WILDFHIR_SUBSCRIPTIONACTIVATEREQUESTED=$WILDFHIR_SUBSCRIPTIONACTIVATEREQUESTED" >> /opt/jboss/wildfly/bin/env.conf
  echo "WILDFHIR_SUBSCRIPTIONACTIVATEREQUESTED: $WILDFHIR_SUBSCRIPTIONACTIVATEREQUESTED"
fi

if [ "x$WILDFHIR_SUBSCRIPTIONHANDSHAKEENABLED" = "x" ]; then
  # Skip
  echo "WILDFHIR_SUBSCRIPTIONHANDSHAKEENABLED not defined"
else
  echo "export WILDFHIR_SUBSCRIPTIONHANDSHAKEENABLED=$WILDFHIR_SUBSCRIPTIONHANDSHAKEENABLED" >> /opt/jboss/wildfly/bin/env.conf
  echo "WILDFHIR_SUBSCRIPTIONHANDSHAKEENABLED: $WILDFHIR_SUBSCRIPTIONHANDSHAKEENABLED"
fi

if [ "x$WILDFHIR_SUBSCRIPTIONHANDSHAKEDELAY" = "x" ]; then
  # Skip
  echo "WILDFHIR_SUBSCRIPTIONHANDSHAKEDELAY not defined"
else
  echo "export WILDFHIR_SUBSCRIPTIONHANDSHAKEDELAY=$WILDFHIR_SUBSCRIPTIONHANDSHAKEDELAY" >> /opt/jboss/wildfly/bin/env.conf
  echo "WILDFHIR_SUBSCRIPTIONHANDSHAKEDELAY: $WILDFHIR_SUBSCRIPTIONHANDSHAKEDELAY"
fi

if [ "x$WILDFHIR_TXCONCURRENTLIMIT" = "x" ]; then
  # Skip
  echo "WILDFHIR_TXCONCURRENTLIMIT not defined"
else
  echo "export WILDFHIR_TXCONCURRENTLIMIT=$WILDFHIR_TXCONCURRENTLIMIT" >> /opt/jboss/wildfly/bin/env.conf
  echo "WILDFHIR_TXCONCURRENTLIMIT:          $WILDFHIR_TXCONCURRENTLIMIT"
fi

if [ "x$FHIR_PACKAGES" = "x" ]; then
  # Skip
  echo "FHIR_PACKAGES not defined"
else
  echo "export FHIR_PACKAGES=$FHIR_PACKAGES" >> /opt/jboss/wildfly/bin/env.conf
  echo "FHIR_PACKAGES:                       $FHIR_PACKAGES"
fi

if [ "x$FHIR_T_SERVER" = "x" ]; then
  # Skip
  echo "FHIR_T_SERVER not defined"
else
  echo "export FHIR_T_SERVER=$FHIR_T_SERVER" >> /opt/jboss/wildfly/bin/env.conf
  echo "FHIR_T_SERVER:                       $FHIR_T_SERVER"
fi

# Start WildFly
echo "[start.sh] Starting WildFly..."
su - jboss -c "/opt/jboss/wildfly/bin/standalone.sh -b 0.0.0.0 -bmanagement 0.0.0.0"
echo "[start.sh] WildFly process exited with code $?"

