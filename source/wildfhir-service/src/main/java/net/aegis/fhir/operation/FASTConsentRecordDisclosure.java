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

import java.util.logging.Logger;

import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.UriInfo;

import org.hl7.fhir.r4.formats.XmlParser;
import org.hl7.fhir.r4.model.AuditEvent;
import org.hl7.fhir.r4.model.OperationOutcome;
import org.hl7.fhir.r4.model.Parameters;
import org.hl7.fhir.r4.model.Resource;
import org.hl7.fhir.r4.model.Parameters.ParametersParameterComponent;

import net.aegis.fhir.service.BatchService;
import net.aegis.fhir.service.CodeService;
import net.aegis.fhir.service.ConformanceService;
import net.aegis.fhir.service.ResourceService;
import net.aegis.fhir.service.ResourcemetadataService;
import net.aegis.fhir.service.TransactionService;
import net.aegis.fhir.service.audit.AuditEventService;
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

	private ResourceService resourceService;

	/* (non-Javadoc)
	 * @see net.aegis.fhir.operation.ResourceOperationProxy#executeOperation(javax.ws.rs.core.UriInfo, javax.ws.rs.core.HttpHeaders, net.aegis.fhir.service.ResourceService, net.aegis.fhir.service.ResourcemetadataService, net.aegis.fhir.service.BatchService, net.aegis.fhir.service.TransactionService, net.aegis.fhir.service.CodeService, net.aegis.fhir.service.audit.AuditEventService, net.aegis.fhir.service.provenance.ProvenanceService, net.aegis.fhir.service.ConformanceService, java.lang.String, java.lang.String, java.lang.String, org.hl7.fhir.r4.model.Parameters, org.hl7.fhir.r4.model.Resource, java.lang.String, java.lang.String, boolean, java.lang.StringBuffer)
	 */
	@Override
	public Parameters executeOperation(UriInfo context, HttpHeaders headers, ResourceService resourceService, ResourcemetadataService resourcemetadataService, BatchService batchService, TransactionService transactionService, CodeService codeService, AuditEventService auditEventService, ProvenanceService provenanceService, ConformanceService conformanceService, String softwareVersion, String resourceType, String resourceId, Parameters inputParameters, org.hl7.fhir.r4.model.Resource inputResource, String inputString, String contentType, boolean isPost, StringBuffer returnedDirective) throws Exception {

		log.fine("[START] FASTConsentRecordDisclosure.executeOperation()");

        this.resourceService = resourceService;

		Parameters out = new Parameters();

		try {
			/*
			 * If inputParameters is null, throw exception
			 */
			if (inputParameters == null) {
				throw new Exception("$recordDisclosure failed. The input parameters contents were empty or null.");
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
				throw new Exception("$recordDisclosure failed. The 'disclosure' input parameter was not defined or its resource contents were empty or null.");
			}

			OperationOutcome rOutcome = performRecordDisclosure(paramAuditEvent);

			if (rOutcome == null) {
				/*
				 * Returned OperationOutcome is null, throw exception (should not happen)
				 */
				throw new Exception("$recordDisclosure failed. The attempt to record the AuditEvent disclosure resource produced a null outcome. Please verify the contents of the input payload.");
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

	private OperationOutcome performRecordDisclosure(AuditEvent auditEvent) throws Exception {
		OperationOutcome rOutcome = null;

		/*
		 * Creation of resources will generate corresponding Provenance and AuditEvent resource instances.
		 * The AuditEvent resource must be processed in this order.
		 * 
		 * - Return the OperationOutcome
		 */


		// OPERATION NOT IMPLEMENTED DEFAULT RESPONSE - REMOVE WHEN IMPLEMENTATION IS COMPLETE
		String outcome = ServicesUtil.INSTANCE.getOperationOutcome(OperationOutcome.IssueSeverity.ERROR, OperationOutcome.IssueType.NOTSUPPORTED, "$recordDisclosure not implemented.", null, null, "application/fhir+xml");

		XmlParser xmlParser = new XmlParser();

		rOutcome = (OperationOutcome) xmlParser.parse(outcome);

		return rOutcome;
	}

}
