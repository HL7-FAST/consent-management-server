#!/bin/sh

wildfhirce_client_war=wildfhir-client.war
path_to_client_war=./wildfhir-client/target/
wildfhirce_server_war=wildfhir-rest-server.war
path_to_server_war=./wildfhir-rest-server/target/

wildfhirceclient="$path_to_client_war$wildfhirce_client_war"
wildfhirceserver="$path_to_server_war$wildfhirce_server_war"

docker_build=true
if test -f "$wildfhirceclient" ; then
	echo "WildFHIR Client war FOUND in target"
else
	echo "WildFHIR Client war NOT FOUND in target"
	docker_build=false
fi

if test -f "$wildfhirceserver" ; then
	echo "WildFHIR Server war FOUND in target"
else
	echo "WildFHIR Server war NOT FOUND in target"
	docker_build=false
fi

if [[ $docker_build == true ]]; then
	docker build -t hlseven/fast-consent-management-server:1.0.0 .
else
	echo "Build the target war files before building the Docker image"
fi
