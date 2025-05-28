/*
 * #%L
 * WildFHIR - wildfhir-model
 * %%
 * Copyright (C) 2024 AEGIS.net, Inc.
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

import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.UriInfo;
import javax.ws.rs.core.Response.Status;

import org.hl7.fhir.r4.formats.XmlParser;
import org.hl7.fhir.r4.formats.IParser.OutputStyle;
import org.hl7.fhir.r4.model.Consent;
import org.hl7.fhir.r4.model.DocumentReference;
import org.hl7.fhir.r4.model.Identifier;
import org.hl7.fhir.r4.model.OperationOutcome;
import org.hl7.fhir.r4.model.OperationOutcome.IssueSeverity;
import org.hl7.fhir.r4.model.OperationOutcome.IssueType;
import org.hl7.fhir.r4.model.OperationOutcome.OperationOutcomeIssueComponent;
import org.hl7.fhir.r4.model.Parameters;
import org.hl7.fhir.r4.model.QuestionnaireResponse;
import org.hl7.fhir.r4.model.Reference;
import org.hl7.fhir.r4.model.Resource;
import org.hl7.fhir.r4.model.Parameters.ParametersParameterComponent;

import net.aegis.fhir.model.ResourceContainer;
import net.aegis.fhir.service.BatchService;
import net.aegis.fhir.service.CodeService;
import net.aegis.fhir.service.ConformanceService;
import net.aegis.fhir.service.ResourceService;
import net.aegis.fhir.service.ResourcemetadataService;
import net.aegis.fhir.service.TransactionService;
import net.aegis.fhir.service.audit.AuditEventActionEnum;
import net.aegis.fhir.service.audit.AuditEventService;
import net.aegis.fhir.service.provenance.ProvenanceActivityTypeEnum;
import net.aegis.fhir.service.provenance.ProvenanceService;
import net.aegis.fhir.service.util.ServicesUtil;

/**
 * FAST Consent RI - $fileConsent operation
 * 
 * @author richard.ettema
 *
 */
public class FASTConsentFileConsent extends ResourceOperationProxy {

	private Logger log = Logger.getLogger("FASTConsentFileConsent");

	private AuditEventService auditEventService;
	private CodeService codeService;
	private ProvenanceService provenanceService;
	private ResourceService resourceService;
	private XmlParser xmlP;

	/* (non-Javadoc)
	 * @see net.aegis.fhir.operation.ResourceOperationProxy#executeOperation(javax.ws.rs.core.UriInfo, javax.ws.rs.core.HttpHeaders, net.aegis.fhir.service.ResourceService, net.aegis.fhir.service.ResourcemetadataService, net.aegis.fhir.service.BatchService, net.aegis.fhir.service.TransactionService, net.aegis.fhir.service.CodeService, net.aegis.fhir.service.audit.AuditEventService, net.aegis.fhir.service.provenance.ProvenanceService, net.aegis.fhir.service.ConformanceService, java.lang.String, java.lang.String, java.lang.String, org.hl7.fhir.r4.model.Parameters, org.hl7.fhir.r4.model.Resource, java.lang.String, java.lang.String, boolean, java.lang.StringBuffer)
	 */
	@Override
	public Parameters executeOperation(UriInfo context, HttpHeaders headers, ResourceService resourceService, ResourcemetadataService resourcemetadataService, BatchService batchService, TransactionService transactionService, CodeService codeService, AuditEventService auditEventService, ProvenanceService provenanceService, ConformanceService conformanceService, String softwareVersion, String resourceType, String resourceId, Parameters inputParameters, org.hl7.fhir.r4.model.Resource inputResource, String inputString, String contentType, boolean isPost, StringBuffer returnedDirective) throws Exception {

		log.info("[START] FASTConsentFileConsent.executeOperation()");

		this.auditEventService = auditEventService;
		this.codeService = codeService;
		this.provenanceService = provenanceService;
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
			 * - consent - FASTConsent resource (required)
			 * - document - FASTDocumentReference or FASTQuestionnaireResponse resource (optional)
			 */
			Consent paramConsent = null;
			DocumentReference paramDocumentReference = null;
			QuestionnaireResponse paramQuestionnaireResponse = null;

			if (inputParameters != null && inputParameters.hasParameter()) {

				for (ParametersParameterComponent parameter : inputParameters.getParameter()) {

					if (parameter.getName() != null && parameter.getName().equals("consent") && parameter.hasResource()) {

						Resource paramResource = parameter.getResource();
						if (paramResource != null && paramResource.fhirType().equals("Consent")) {
							paramConsent = (Consent) paramResource;
						}
					}

					if (parameter.getName() != null && parameter.getName().equals("document") && parameter.hasResource()) {

						Resource paramResource = parameter.getResource();
						if (paramResource != null && paramResource.fhirType().equals("DocumentReference")) {
							paramDocumentReference = (DocumentReference) paramResource;
						}
						else if (paramResource != null && paramResource.fhirType().equals("QuestionnaireResponse")) {
							paramQuestionnaireResponse = (QuestionnaireResponse) paramResource;
						}
					}
				}
			}

			/*
			 * If the 'consent' input parameter is null, throw exception
			 */
			if (paramConsent == null) {
				throw new Exception("The 'consent' input parameter was not defined or its resource contents were empty or null.");
			}

			OperationOutcome rOutcome = performFileConsent(context, headers, paramConsent, paramDocumentReference, paramQuestionnaireResponse);

			if (rOutcome == null) {
				/*
				 * Returned OperationOutcome is null, throw exception (should not happen)
				 */
				throw new Exception("The attempt to file the Consent and/or supporting document resources produced a null outcome. Please verify the contents of the input payload.");
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
			throw new Exception("$fileConsent failed! Exception thrown: " + e.getMessage());
		}

		log.info("[END] FASTConsentFileConsent.executeOperation()");

		return out;
	}

	/**
	 * Create the Consent and optional supporting source document in the local repository.
	 * 
	 * @param context
	 * @param headers
	 * @param consent
	 * @param documentReference
	 * @param questionnaireResponse
	 * @return OperationOutcome
	 * @throws Exception
	 */
	private OperationOutcome performFileConsent(UriInfo context, HttpHeaders headers, Consent consent, DocumentReference documentReference, QuestionnaireResponse questionnaireResponse) throws Exception {

		log.info("[START] FASTConsentFileConsent.performFileConsent()");

		OperationOutcome rOutcome = null;
		List<OperationOutcomeIssueComponent> issues = new ArrayList<OperationOutcomeIssueComponent>();
		OperationOutcomeIssueComponent issue = null;

		boolean ok = true;
		xmlP = new XmlParser();
		xmlP.setOutputStyle(OutputStyle.PRETTY);
		net.aegis.fhir.model.Resource createResource = null;
		ResourceContainer resCon = null;
		ByteArrayOutputStream oResource = null;
		byte fileContent[] = null;
		String baseUrl = null;
		Reference sourceReference = null;
		Integer sourceId = null;
		Identifier consentIdentifier = null;

		try {
			// Get server base url from code table configuration
			baseUrl = codeService.getCodeValue("baseUrl");

			// Create DocumentReference if present
			if (documentReference != null) {
				createResource = new net.aegis.fhir.model.Resource();

				// Remove existing Resource.id
				documentReference.setIdElement(null);

				// Convert documentReference to XML for WildFHIR resource create
				oResource = new ByteArrayOutputStream();
				xmlP.compose(oResource, documentReference, true);
				fileContent = oResource.toByteArray();

				createResource.setResourceType("DocumentReference");
				createResource.setResourceContents(fileContent);

				resCon = resourceService.create(createResource, null, baseUrl);

				if (resCon.getResponseStatus().equals(Status.CREATED)) {
					sourceReference = new Reference();
					sourceReference.setReference("DocumentReference/" + resCon.getResource().getResourceId());
					if (documentReference.hasIdentifier()) {
						sourceReference.setIdentifier(documentReference.getIdentifierFirstRep());
					}
					// Save internal resource primary key if needed for hard delete
					sourceId = resCon.getResource().getId();
					// Capture successful DocumentReference create to OperationOutcome.issue
					issue = ServicesUtil.INSTANCE.getOperationOutcomeIssueComponent(IssueSeverity.INFORMATION, IssueType.PROCESSING,
							sourceReference.getReference() + " successfully created.", null, "Parameters.parameter.where(name = 'document')");
					issues.add(issue);
					ok = true;
				}
				else {
					// Capture failed DocumentReference create to OperationOutcome.issue
					issue = ServicesUtil.INSTANCE.getOperationOutcomeIssueComponent(IssueSeverity.ERROR, IssueType.PROCESSING,
							"$fileConsent failed! Error attempting to create consent source DocumentReference.",
							resCon.getResponseStatus().getStatusCode() + " " + resCon.getResponseStatus().toString() + "" + (resCon.getMessage() != null ? resCon.getMessage() : ""),
							"Parameters.parameter.where(name = 'document')");
					issues.add(issue);
					ok = false;
				}
			}
			// Create QuestionnaireResponse if present
			else if (questionnaireResponse != null) {
				createResource = new net.aegis.fhir.model.Resource();

				// Remove existing Resource.id
				questionnaireResponse.setIdElement(null);

				// Convert questionnaireResponse to XML for WildFHIR resource create
				oResource = new ByteArrayOutputStream();
				xmlP.compose(oResource, questionnaireResponse, true);
				fileContent = oResource.toByteArray();

				createResource.setResourceType("QuestionnaireResponse");
				createResource.setResourceContents(fileContent);

				resCon = resourceService.create(createResource, null, baseUrl);

				if (resCon.getResponseStatus().equals(Status.CREATED)) {
					sourceReference = new Reference();
					sourceReference.setReference("QuestionnaireResponse/" + resCon.getResource().getResourceId());
					if (questionnaireResponse.hasIdentifier()) {
						sourceReference.setIdentifier(questionnaireResponse.getIdentifier());
					}
					// Save internal resource primary key if needed for hard delete
					sourceId = resCon.getResource().getId();
					// Capture successful QuestionnaireResponse create to OperationOutcome.issue
					issue = ServicesUtil.INSTANCE.getOperationOutcomeIssueComponent(IssueSeverity.INFORMATION, IssueType.PROCESSING,
							sourceReference.getReference() + " successfully created.", null, "Parameters.parameter.where(name = 'document')");
					issues.add(issue);
					ok = true;
				}
				else {
					// Capture failed QuestionnaireResponse create to OperationOutcome.issue
					issue = ServicesUtil.INSTANCE.getOperationOutcomeIssueComponent(IssueSeverity.ERROR, IssueType.PROCESSING,
							"$fileConsent failed! Error attempting to create consent source QuestionnaireResponse.",
							resCon.getResponseStatus().getStatusCode() + " " + resCon.getResponseStatus().toString() + "" + (resCon.getMessage() != null ? resCon.getMessage() : ""),
							"Parameters.parameter.where(name = 'document')");
					issues.add(issue);
					ok = false;
				}
			}

			if (ok == true) {
				// Create Consent
				createResource = new net.aegis.fhir.model.Resource();

				// Remove existing Resource.id
				consent.setIdElement(null);

				// Set Consent.source if needed
				if (sourceReference != null) {
					consent.setSource(sourceReference);
				}

				// Save Consent.identifier if defined
				if (consent.hasIdentifier()) {
					consentIdentifier = consent.getIdentifierFirstRep();
				}

				// Convert consent to XML for WildFHIR resource create
				oResource = new ByteArrayOutputStream();
				xmlP.compose(oResource, consent, true);
				fileContent = oResource.toByteArray();

				createResource.setResourceType("Consent");
				createResource.setResourceContents(fileContent);

				resCon = resourceService.create(createResource, null, baseUrl);

				if (resCon.getResponseStatus().equals(Status.CREATED)) {
					// Create AuditEvent
					auditEventService.createAuditEvent(context, headers, null, "Consent", true, resCon.getResource().getResourceId(), consentIdentifier, AuditEventActionEnum.CREATE.getCode());
					// Create Provenance
					provenanceService.createProvenance(context, headers, null, "Consent", "Consent/" + resCon.getResource().getResourceId(), resCon.getResource().getResourceId(), consentIdentifier, ProvenanceActivityTypeEnum.CREATE.getCode());

					// Capture successful Consent create to OperationOutcome.issue
					issue = ServicesUtil.INSTANCE.getOperationOutcomeIssueComponent(IssueSeverity.INFORMATION, IssueType.PROCESSING,
							"Consent/" + resCon.getResource().getResourceId() + " successfully created.", null, "Parameters.parameter.where(name = 'consent')");
					issues.add(issue);
				}
				else {
					// Capture failed Consent create to OperationOutcome.issue
					issue = ServicesUtil.INSTANCE.getOperationOutcomeIssueComponent(IssueSeverity.ERROR, IssueType.PROCESSING,
							"$fileConsent failed! Error attempting to create Consent.",
							resCon.getResponseStatus().getStatusCode() + " " + resCon.getResponseStatus().toString() + "" + (resCon.getMessage() != null ? resCon.getMessage() : ""),
							"Parameters.parameter.where(name = 'consent')");
					issues.add(issue);

					// If sourceReference then hard delete DocumentReference or QuestionnaireResponse
					if (sourceReference != null && sourceId != null) {
						resourceService.purge(sourceId);
					}
				}
			}
		}
		catch (Exception e) {
			// Log exception
			e.printStackTrace();
			// Capture exception to OperationOutcome.issue
			issue = ServicesUtil.INSTANCE.getOperationOutcomeIssueComponent(IssueSeverity.FATAL, IssueType.EXCEPTION, e.getMessage(), null, "$fileConsent");
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
			sourceReference = null;
			sourceId = null;
			consentIdentifier = null;
		}

		rOutcome = new OperationOutcome();
		rOutcome.setIssue(issues);

		log.info("[END] FASTConsentFileConsent.performFileConsent()");

		return rOutcome;
	}

}
