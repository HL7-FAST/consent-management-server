/*
 * #%L
 * WildFHIR - wildfhir-service
 * %%
 * Copyright (C) 2025 AEGIS.net, Inc.
 * All rights reserved.
 * %%
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 *  - Redistributions of source code must retain the above copyright notice,
 *    this list of conditions and the following disclaimer.
 *  - Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 *  - Neither the name of AEGIS nor the names of its contributors may be used
 *    to endorse or promote products derived from this software without specific
 *    prior written permission.
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * #L%
 */
package net.aegis.fhir.operation;

import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.MultivaluedHashMap;
import jakarta.ws.rs.core.MultivaluedMap;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.UriInfo;
import jakarta.ws.rs.core.Response.Status;

import org.hl7.fhir.r4.formats.XmlParser;
import org.hl7.fhir.r4.formats.IParser.OutputStyle;
import org.hl7.fhir.r4.model.AuditEvent;
import org.hl7.fhir.r4.model.Consent;
import org.hl7.fhir.r4.model.AuditEvent.AuditEventEntityComponent;
import org.hl7.fhir.r4.model.Identifier;
import org.hl7.fhir.r4.model.OperationOutcome;
import org.hl7.fhir.r4.model.Parameters;
import org.hl7.fhir.r4.model.Resource;
import org.hl7.fhir.r4.model.OperationOutcome.IssueSeverity;
import org.hl7.fhir.r4.model.OperationOutcome.IssueType;
import org.hl7.fhir.r4.model.OperationOutcome.OperationOutcomeIssueComponent;
import org.hl7.fhir.r4.model.Parameters.ParametersParameterComponent;

import net.aegis.fhir.model.ResourceContainer;
import net.aegis.fhir.service.BatchService;
import net.aegis.fhir.service.CodeService;
import net.aegis.fhir.service.ConformanceService;
import net.aegis.fhir.service.ResourceService;
import net.aegis.fhir.service.ResourcemetadataService;
import net.aegis.fhir.service.TransactionService;
import net.aegis.fhir.service.audit.AuditEventService;
import net.aegis.fhir.service.narrative.FHIRNarrativeGeneratorClient;
import net.aegis.fhir.service.provenance.ProvenanceService;
import net.aegis.fhir.service.util.ServicesUtil;

/**
 * FAST Consent RI - $recordDisclosure operation
 * 
 * @author richard.ettema
 *
 */
public class FASTConsentRecordDisclosure extends ResourceOperationProxy {

	private Logger log = Logger.getLogger("FASTConsentRecordDisclosure");

	private CodeService codeService;
	private ResourceService resourceService;
	private XmlParser xmlP;

	/* (non-Javadoc)
	 * @see net.aegis.fhir.operation.ResourceOperationProxy#executeOperation(javax.ws.rs.core.UriInfo, javax.ws.rs.core.HttpHeaders, net.aegis.fhir.service.ResourceService, net.aegis.fhir.service.ResourcemetadataService, net.aegis.fhir.service.BatchService, net.aegis.fhir.service.TransactionService, net.aegis.fhir.service.CodeService, net.aegis.fhir.service.audit.AuditEventService, net.aegis.fhir.service.provenance.ProvenanceService, net.aegis.fhir.service.ConformanceService, java.lang.String, java.lang.String, java.lang.String, org.hl7.fhir.r4.model.Parameters, org.hl7.fhir.r4.model.Resource, java.lang.String, java.lang.String, boolean, java.lang.StringBuffer)
	 */
	@Override
	public Parameters executeOperation(UriInfo context, HttpHeaders headers, ResourceService resourceService, ResourcemetadataService resourcemetadataService, BatchService batchService, TransactionService transactionService, CodeService codeService, AuditEventService auditEventService, ProvenanceService provenanceService, ConformanceService conformanceService, String softwareVersion, String resourceType, String resourceId, Parameters inputParameters, org.hl7.fhir.r4.model.Resource inputResource, String inputString, String contentType, boolean isPost, StringBuffer returnedDirective) throws Exception {

		log.fine("[START] FASTConsentRecordDisclosure.executeOperation()");

		this.codeService = codeService;
        this.resourceService = resourceService;

		Parameters out = new Parameters();

		try {
			/*
			 * If inputParameters is null, throw exception
			 */
			if (inputParameters == null) {
				throw new Exception("The input parameters contents were empty or null.");
			}

			/*
			 * Extract the individual expected parameters
			 * - disclosure - FASTConsentAuditEvent resource (required)
			 */
			AuditEvent paramAuditEvent = null;

			if (inputParameters != null && inputParameters.hasParameter()) {

				for (ParametersParameterComponent parameter : inputParameters.getParameter()) {

					if (parameter.getName() != null && parameter.getName().equals("disclosure") && parameter.hasResource()) {

						Resource paramResource = parameter.getResource();
						if (paramResource != null && paramResource.fhirType().equals("AuditEvent")) {
							paramAuditEvent = (AuditEvent) paramResource;
						}
					}
				}
			}

			/*
			 * If the 'disclosure' input parameter is null, throw exception
			 */
			if (paramAuditEvent == null) {
				throw new Exception("The 'disclosure' input parameter was not defined or its resource contents were empty or null.");
			}

			OperationOutcome rOutcome = performRecordDisclosure(context, headers, paramAuditEvent);

			if (rOutcome == null) {
				/*
				 * Returned OperationOutcome is null, throw exception (should not happen)
				 */
				throw new Exception("The attempt to record the AuditEvent disclosure resource produced a null outcome. Please verify the contents of the input payload.");
			}

			out = new Parameters();

			ParametersParameterComponent parameter = new ParametersParameterComponent();
			parameter.setName("return");
			parameter.setResource(rOutcome);

			out.addParameter(parameter);
		}
		catch (Exception e) {
			// Throw exceptions back
			e.printStackTrace();
			throw new Exception("$recordDisclosure failed! Exception thrown: " + e.getMessage());
		}

		return out;
	}

	/**
	 * Create the AuditEvent disclosure in the local repository.
	 * 
	 * @param context
	 * @param headers
	 * @param auditEvent
	 * @return OperationOutcome
	 * @throws Exception
	 */
	private OperationOutcome performRecordDisclosure(UriInfo context, HttpHeaders headers, AuditEvent auditEvent) throws Exception {

		log.info("[START] FASTConsentRecordDisclosure.performRecordDisclosure()");

		OperationOutcome rOutcome = null;
		List<OperationOutcomeIssueComponent> issues = new ArrayList<OperationOutcomeIssueComponent>();
		OperationOutcomeIssueComponent issue = null;

		boolean ok = false;
		xmlP = new XmlParser();
		xmlP.setOutputStyle(OutputStyle.PRETTY);
		net.aegis.fhir.model.Resource createResource = null;
		ResourceContainer resCon = null;
		ByteArrayOutputStream oResource = null;
		byte fileContent[] = null;
		String baseUrl = null;

		try {
			// Get server base url from code table configuration
			baseUrl = codeService.getCodeValue("baseUrl");

			if (auditEvent != null) {
				// Create AuditEvent
				createResource = new net.aegis.fhir.model.Resource();

				// Remove existing Resource.id
				auditEvent.setIdElement(null);

				// Check AuditEvent.entity.what reference for Consent; if matches existing, update with local reference; if not, report error
				String whatResourceType = null;
				String whatResourceId = null;
				Consent consent = null;

				for (AuditEventEntityComponent entity : auditEvent.getEntity()) {
					if (entity.hasWhat()) {
						if (entity.getWhat().hasReference()) {
							whatResourceType = ServicesUtil.INSTANCE.getResourceTypeFromReference(entity.getWhat().getReference());
							if (whatResourceType.equals("Consent")) {
								whatResourceId = ServicesUtil.INSTANCE.extractResourceIdFromURL(entity.getWhat().getReference());

								ResourceContainer readExisting = resourceService.read("Consent", whatResourceId, null);
								if (readExisting.getResponseStatus().equals(Response.Status.OK)) {
									log.info("(read) " + entity.getWhat().getReference() + " found.");
									byte[] resourceContents = readExisting.getResource().getResourceContents();
									consent = (Consent) xmlP.parse(resourceContents);
									// Assign local Consent reference to disclosure AuditEvent
									entity.getWhat().setReference("Consent/" + consent.getId());
									if (consent.hasIdentifier()) {
										entity.getWhat().setIdentifier(consent.getIdentifierFirstRep());
									}
									ok = true;
									break;
								}
							}
						}
						if (ok == false && entity.getWhat().hasIdentifier()) {
							// Read didn't work; try searching based on what identifier

							// Define query parameters and populate with search parameter values if defined
							StringBuffer param = new StringBuffer();
							Identifier identifier = entity.getWhat().getIdentifier();
							MultivaluedMap<String, String> queryParams = new MultivaluedHashMap<String, String>();
							if (identifier.hasSystem()) {
								param.append(identifier.getSystem()).append("|");
							}
							if (identifier.hasValue()) {
								param.append(identifier.getValue());
							}
							if (param.length() > 1) {
								queryParams.add("identifier", param.toString());
							}

							if (!queryParams.isEmpty()) {
								List<String[]> validParams = new ArrayList<String[]>();
								List<String[]> invalidParams = new ArrayList<String[]>();

								List<net.aegis.fhir.model.Resource> resources = resourceService.searchQuery(queryParams, null, null, "Consent", false, null, null, null, validParams, invalidParams);

								// Log any invalidParams
								if (!invalidParams.isEmpty()) {
									for (String[] invalidParam : invalidParams) {
										if (invalidParam[0].equals("ERROR")) {
											log.severe("Invalid search parameter '" + invalidParam[0] + "' found in search criteria." + (invalidParam.length > 1 && invalidParam[1] != null ? " " + invalidParam[1] : ""));
										}
										else {
											log.warning("Invalid search parameter '" + invalidParam[0] + "' found in search criteria." + (invalidParam.length > 1 && invalidParam[1] != null ? " " + invalidParam[1] : ""));
										}
									}
								}

								if (resources != null && resources.size() == 1) {
									// Exact match found; get Consent resource
									byte[] resourceContents = resources.get(0).getResourceContents();
									consent = (Consent) xmlP.parse(resourceContents);
									// Assign local Consent reference to disclosure AuditEvent
									entity.getWhat().setReference("Consent/" + consent.getId());
									if (consent.hasIdentifier()) {
										entity.getWhat().setIdentifier(consent.getIdentifierFirstRep());
									}
									log.info("(search) Consent/" + consent.getId() + " found.");
									ok = true;
									break;
								}
							}
						}
					}
				}

				if (ok == true) {
					// Use RI NarrativeGenerator
					FHIRNarrativeGeneratorClient.instance().generate(auditEvent);

					// Convert AuditEvent to XML for WildFHIR resource create
					oResource = new ByteArrayOutputStream();
					xmlP.compose(oResource, auditEvent, true);
					fileContent = oResource.toByteArray();

					createResource.setResourceType("AuditEvent");
					createResource.setResourceContents(fileContent);

					resCon = resourceService.create(createResource, null, baseUrl);

					if (resCon.getResponseStatus().equals(Status.CREATED)) {
						// Capture successful AuditEvent create to OperationOutcome.issue
						issue = ServicesUtil.INSTANCE.getOperationOutcomeIssueComponent(IssueSeverity.INFORMATION, IssueType.PROCESSING,
								"AuditEvent/" + resCon.getResource().getResourceId() + " successfully created.", null, "Parameters.parameter.where(name = 'disclosure')");
						issues.add(issue);
					}
					else {
						// Capture failed AuditEvent create to OperationOutcome.issue
						issue = ServicesUtil.INSTANCE.getOperationOutcomeIssueComponent(IssueSeverity.ERROR, IssueType.PROCESSING,
								"$recordDisclosure failed! Error attempting to create AuditEvent.",
								resCon.getResponseStatus().getStatusCode() + " " + resCon.getResponseStatus().toString() + "" + (resCon.getMessage() != null ? resCon.getMessage() : ""),
								"Parameters.parameter.where(name = 'disclosure')");
						issues.add(issue);
					}
				}
				else {
					issue = ServicesUtil.INSTANCE.getOperationOutcomeIssueComponent(IssueSeverity.ERROR, IssueType.PROCESSING,
							"$recordDisclosure failed! Error attempting to create AuditEvent.",
							"AuditEvent.entity.what does not reference an existing Consent within this repository.",
							"Parameters.parameter.where(name = 'disclosure')");
					issues.add(issue);
				}
			}
			else {
				// Should never happen but, just in case
				issue = ServicesUtil.INSTANCE.getOperationOutcomeIssueComponent(IssueSeverity.ERROR, IssueType.PROCESSING,
						"$recordDisclosure failed! Error attempting to create null AuditEvent.",
						"AuditEvent contents are null or empty! Please verify the contents of the input payload.",
						"Parameters.parameter.where(name = 'disclosure')");
				issues.add(issue);
			}
		}
		catch (Exception e) {
			// Log exception
			e.printStackTrace();
			// Capture exception to OperationOutcome.issue
			issue = ServicesUtil.INSTANCE.getOperationOutcomeIssueComponent(IssueSeverity.FATAL, IssueType.EXCEPTION, e.getMessage(), null, "$recordDisclosure");
			issues.add(issue);
		}
		finally {
			// Release resources for garbage collection
			xmlP = null;
			createResource = null;
			resCon = null;
			oResource = null;
			fileContent = null;
			baseUrl = null;
		}

		rOutcome = new OperationOutcome();
		rOutcome.setIssue(issues);

		log.info("[END] FASTConsentRecordDisclosure.performRecordDisclosure()");

		return rOutcome;
	}

}
