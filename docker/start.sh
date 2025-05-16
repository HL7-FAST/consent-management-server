#!/bin/bash

# Start MySQL
su - mysql -c "mysqld" &

# Wait for MySQL to start
sleep 5

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

# Start WildFly
su - jboss -c "/opt/jboss/wildfly/bin/standalone.sh -b 0.0.0.0 -bmanagement 0.0.0.0"
