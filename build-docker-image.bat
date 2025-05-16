@ECHO OFF

SETLOCAL

SET wildfhirce_war=wildfhir-rest-server.war
SET path_to_war=.\wildfhir-rest-server\target\

IF NOT EXIST "%path_to_war%%wildfhirce_war%" (
	ECHO WildFHIR CE war NOT FOUND in target
	ECHO Build the target war file before building the Docker images
	GOTO end
) ELSE (
	ECHO WildFHIR CE war FOUND in target
	SET jarlocation="%input_cache_path%%publisher_jar%"
	SET jarlocationname=Input Cache
	GOTO docker_build
)

:docker_build
docker build -t hlseven/consent-management-server:0.1.0 .

:end
