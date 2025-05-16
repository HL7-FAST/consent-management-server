#!/bin/sh

wildfhirce_war=wildfhir-rest-server.war
path_to_war=./wildfhir-rest-server/target/

wildfhirce="$path_to_war$wildfhirce_war"

if test -f "$wildfhirce" ; then
	echo "WildFHIR CE war FOUND in target"
	docker_build=true
else
	echo "WildFHIR CE war NOT FOUND in target"
	echo "Build the target war file before building the Docker images"
	docker_build=false
fi

if [[ $docker_build == true ]]; then
	docker build -t hlseven/consent-management-server:0.1.0 .
fi
