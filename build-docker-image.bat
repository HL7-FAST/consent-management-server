@ECHO OFF

SETLOCAL

SET wildfhirce_client_war=wildfhir-client.war
SET path_to_client_war=.\wildfhir-client\target\
SET wildfhirce_server_war=wildfhir-rest-server.war
SET path_to_server_war=.\wildfhir-rest-server\target\

IF NOT EXIST "%path_to_server_war%%wildfhirce_server_war%" (
	ECHO WildFHIR Server war NOT FOUND in target
	ECHO Build the target war file before building the Docker images
	GOTO end
) ELSE (
	IF NOT EXIST "%path_to_client_war%%wildfhirce_client_war%" (
		ECHO WildFHIR Client war NOT FOUND in target
		ECHO Build the target war file before building the Docker images
		GOTO end
	) ELSE (
		ECHO WildFHIR war files FOUND in target
		SET jarlocation="%input_cache_path%%publisher_jar%"
		SET jarlocationname=Input Cache
		GOTO docker_build
	)
)

:docker_build
docker build -t hlseven/fast-consent-management-server:0.1.0 .

:end
