/*
 * #%L
 * WildFHIR - wildfhir-client
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
package net.aegis.fhir.client.controller;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Serializable;
import java.nio.file.DirectoryIteratorException;
import java.nio.file.DirectoryStream;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.TimeZone;
import java.util.logging.Logger;

import javax.faces.application.FacesMessage;
import javax.faces.bean.ManagedBean;
import javax.faces.bean.ManagedProperty;
import javax.faces.bean.SessionScoped;
import javax.faces.context.FacesContext;
import javax.faces.event.ActionEvent;
import javax.inject.Inject;
import javax.ws.rs.core.Response;

import org.hl7.fhir.r4.formats.IParser.OutputStyle;
import org.hl7.fhir.r4.formats.JsonParser;
import org.hl7.fhir.r4.formats.XmlParser;
import org.hl7.fhir.r4.model.Attachment;
import org.hl7.fhir.r4.model.CodeableConcept;
import org.hl7.fhir.r4.model.Coding;
import org.hl7.fhir.r4.model.Consent;
import org.hl7.fhir.r4.model.Extension;
import org.hl7.fhir.r4.model.Identifier;
import org.hl7.fhir.r4.model.Meta;
import org.hl7.fhir.r4.model.Organization;
import org.hl7.fhir.r4.model.Parameters;
import org.hl7.fhir.r4.model.Parameters.ParametersParameterComponent;
import org.hl7.fhir.r4.model.Patient;
import org.hl7.fhir.r4.model.Period;
import org.hl7.fhir.r4.model.Reference;
import org.hl7.fhir.r4.model.RelatedPerson;
import org.hl7.fhir.r4.model.Resource;
import org.hl7.fhir.r4.model.Consent.ConsentProvisionType;
import org.hl7.fhir.r4.model.Consent.ConsentState;
import org.hl7.fhir.r4.model.Consent.ProvisionComponent;
import org.hl7.fhir.r4.model.Consent.provisionActorComponent;
import org.hl7.fhir.r4.model.DocumentReference;
import org.hl7.fhir.r4.model.DocumentReference.DocumentReferenceContentComponent;
import org.hl7.fhir.r4.model.Enumerations.DocumentReferenceStatus;
import org.primefaces.event.TabChangeEvent;

import net.aegis.fhir.client.ApplicationContext;
import net.aegis.fhir.client.model.ResourceResponseWrapper;
import net.aegis.fhir.model.Clientresource;
import net.aegis.fhir.model.Constants;
import net.aegis.fhir.model.LabelKeyValueBean;
import net.aegis.fhir.model.Serverdirectory;
import net.aegis.fhir.service.util.UTCDateUtil;
import net.aegis.fhir.service.util.UUIDUtil;

/**
 * <p>
 * This class is the controller to handle interactions with the application view.</br> At a later point this controller
 * can be broken apart to separate varies functionality pieces managed by their own controllers e.g. Search,
 * Registration, Preferences, Security etc.
 * </p>
 *
 * @author richard.ettema
 *
 */
@ManagedBean(name = "controller", eager = true)
@SessionScoped
public class ApplicationController implements Serializable {

	private static final long serialVersionUID = 5848069089082841377L;

	@Inject
	private Logger log;

	@Inject
	UTCDateUtil utcDateUtil;

	@ManagedProperty(value = "#{context}")
	private ApplicationContext context;

	public ApplicationController() {
	}

	/**
	 * Method gets executed when user changes tab in UI
	 *
	 * @param event
	 */
	public void onTabChange(TabChangeEvent<?> event) {
		context.clear();
		Iterator<?> iter = FacesContext.getCurrentInstance().getMessages();
    	while (iter.hasNext()) {
    		iter.remove();
    	}
	}

	public ApplicationContext getContext() {
		return context;
	}

	public void setContext(ApplicationContext context) {
		this.context = context;
	}

	/*
	 * FAST Consent Operations methods
	 */

	/**
	 * Process the $fileConsent operation request
	 * 
	 * @param event
	 */
	public void processFileConsent(ActionEvent event) {
		log.fine("[START] ApplicationController.processFileConsent()");
		log.info("$fileConsent info: ");

		String formatType = context.getSelectedFormatType();
		log.info("Selected Format Type: " + formatType);
		log.info("BasePath for $fileConsent: " + context.getSelectedServerURL());
		Response response = null;

		try {
			String clientPatientId = context.getSelectedPatientId();
			String clientRelatedPersonId = context.getSelectedRelatedPersonId();
			String clientOrganizationId = context.getSelectedOrganizationId();
			String provisionType = context.getSelectedProvisionType();
			StringBuilder sb = new StringBuilder();

			sb.append("Patient id: ").append(clientPatientId);
			sb.append("; RelatedPerson id: ").append(clientRelatedPersonId);
			sb.append("; Organization id: ").append(clientOrganizationId);
			sb.append("; Provision: ").append(provisionType);

			context.setResourceString(sb.toString());
			context.setResponseString("TBD");

			// Get selected client resources
			Patient grantor = (Patient) context.getClientresourceService().readFHIRResource(Integer.valueOf(clientPatientId));
			RelatedPerson grantee = (RelatedPerson) context.getClientresourceService().readFHIRResource(Integer.valueOf(clientRelatedPersonId));
			Organization manager = (Organization) context.getClientresourceService().readFHIRResource(Integer.valueOf(clientOrganizationId));

			// Build $fileConsent Parameters with Consent and DocumentReference
			Parameters p = new Parameters();
			p.setId(UUIDUtil.getUUID());
			Meta meta = new Meta();
			meta.addProfile("http://hl7.org/fhir/us/consent-management/StructureDefinition/fileConsentParameters");
			p.setMeta(meta);

			// consent parameter
			ParametersParameterComponent param = new ParametersParameterComponent();
			param.setName("consent");

			Consent consent = new Consent();
			String consentId = UUIDUtil.getUUID();
			consent.setId(consentId);
			meta = new Meta();
			meta.addProfile("http://hl7.org/fhir/us/consent-management/StructureDefinition/FASTConsent");
			consent.setMeta(meta);
			Extension extension = new Extension();
			extension.setUrl("http://hl7.org/fhir/5.0/StructureDefinition/extension-Consent.grantee");
			Reference referenceGrantee = new Reference();
			referenceGrantee.setReference("RelatedPerson/" + grantee.getId());
			if (grantee.hasIdentifier()) {
				referenceGrantee.setIdentifier(grantee.getIdentifierFirstRep());
			}
			extension.setValue(referenceGrantee);
			consent.addExtension(extension);

			extension = new Extension();
			extension.setUrl("http://hl7.org/fhir/5.0/StructureDefinition/extension-Consent.manager");
			Reference reference = new Reference();
			reference.setReference("Organization/" + manager.getId());
			if (manager.hasIdentifier()) {
				reference.setIdentifier(manager.getIdentifierFirstRep());
			}
			extension.setValue(reference);
			consent.addExtension(extension);

			Identifier identifer = new Identifier();
			identifer.setSystem("http://example.org/identifiers");
			identifer.setValue(consentId);
			consent.addIdentifier(identifer);

			consent.setStatus(ConsentState.ACTIVE);

			CodeableConcept codeableConcept = new CodeableConcept();
			Coding coding = new Coding();
			coding.setSystem("http://terminology.hl7.org/CodeSystem/consentscope");
			coding.setCode("patient-privacy");
			codeableConcept.addCoding(coding);
			consent.setScope(codeableConcept);

			codeableConcept = new CodeableConcept();
			coding = new Coding();
			coding.setSystem("http://terminology.hl7.org/CodeSystem/v3-ActCode");
			coding.setCode("INFA");
			codeableConcept.addCoding(coding);
			consent.addCategory(codeableConcept);

			Reference grantorReference = new Reference();
			grantorReference.setReference("Patient/" + grantor.getId());
			if (grantor.hasIdentifier()) {
				grantorReference.setIdentifier(grantor.getIdentifierFirstRep());
			}
			consent.setPatient(grantorReference);
			List<Reference> referenceList = new ArrayList<Reference>();
			referenceList.add(grantorReference);
			consent.setPerformer(referenceList);

			Date startDate = new Date();
			consent.setDateTime(startDate);

			// Set sourceReference to DocumentReference below

			codeableConcept = new CodeableConcept();
			coding = new Coding();
			coding.setSystem("http://terminology.hl7.org/CodeSystem/consentpolicycodes");
			coding.setCode("hipaa-auth");
			codeableConcept.addCoding(coding);
			consent.setPolicyRule(codeableConcept);

			ProvisionComponent provision = new ProvisionComponent();
			provision.setType(ConsentProvisionType.PERMIT);
			Period period = new Period();
			period.setStart(startDate);
			provision.setPeriod(period);
			provisionActorComponent actor = new provisionActorComponent();
			codeableConcept = new CodeableConcept();
			coding = new Coding();
			coding.setSystem("http://terminology.hl7.org/CodeSystem/v3-ParticipationType");
			coding.setCode("IRCP");
			codeableConcept.addCoding(coding);
			actor.setRole(codeableConcept);
			actor.setReference(referenceGrantee);
			provision.addActor(actor);
			consent.setProvision(provision);

			param.setResource(consent);

			p.addParameter(param);

			// document parameter
			param = new ParametersParameterComponent();
			param.setName("document");

			DocumentReference docRef = new DocumentReference();
			String docRefId = UUIDUtil.getUUID();
			docRef.setId(docRefId);
			meta = new Meta();
			meta.addProfile("http://hl7.org/fhir/us/consent-management/StructureDefinition/FASTDocumentReference");
			docRef.setMeta(meta);

			Identifier docRefIidentifer = new Identifier();
			docRefIidentifer.setSystem("http://example.org/identifiers");
			docRefIidentifer.setValue(docRefId);
			docRef.addIdentifier(docRefIidentifer);

			// Set Consent.sourceReference to DocumentReference
			reference = new Reference();
			reference.setReference("DocumentReference/" + docRefId);
			reference.setIdentifier(docRefIidentifer);
			consent.setSource(reference);

			docRef.setStatus(DocumentReferenceStatus.CURRENT);

			codeableConcept = new CodeableConcept();
			coding = new Coding();
			coding.setSystem("http://loinc.org");
			coding.setCode("64292-6");
			codeableConcept.addCoding(coding);
			docRef.setType(codeableConcept);

			codeableConcept = new CodeableConcept();
			coding = new Coding();
			coding.setSystem("http://loinc.org");
			coding.setCode("57016-8");
			codeableConcept.addCoding(coding);
			docRef.addCategory(codeableConcept);

			docRef.setSubject(grantorReference);
			docRef.setDate(startDate);
			docRef.addAuthor(grantorReference);

			DocumentReferenceContentComponent content = new DocumentReferenceContentComponent();
			Attachment attachment = new Attachment();
			attachment.setContentType("text/plain" + Constants.CHARSET_UTF8_EXT);
			sb = new StringBuilder("I, ");
			sb.append(grantor.getNameFirstRep().getNameAsSingleString());
			sb.append(", ");
			sb.append(ConsentProvisionType.PERMIT.toCode());
			sb.append(" ");
			sb.append(grantee.getNameFirstRep().getNameAsSingleString());
			sb.append(" access to my medical records.");
			attachment.setData(sb.toString().getBytes("UTF-8"));
			content.setAttachment(attachment);
			docRef.addContent(content);

			param.setResource(docRef);

			p.addParameter(param);

			// Send $fileConsent request
			ByteArrayOutputStream oOp = new ByteArrayOutputStream();
			if (formatType.equals("JSON")) {
				JsonParser jsonParser = new JsonParser();
				jsonParser.setOutputStyle(OutputStyle.PRETTY);
				jsonParser.compose(oOp, p);

				context.setResourceString(oOp.toString());

				response = context.getResourceOperationClient().resourceOperation(p, null, context.getSelectedServerURL(), "Consent", Constants.FHIR_JSON_CONTENT, Constants.FHIR_JSON_CONTENT, null, "fileConsent", null, null);
			}
			else {
				XmlParser xmlParser = new XmlParser();
				xmlParser.setOutputStyle(OutputStyle.PRETTY);
				xmlParser.compose(oOp, p, true);

				context.setResourceString(oOp.toString());

				response = context.getResourceOperationClient().resourceOperation(p, null, context.getSelectedServerURL(), "Consent", Constants.FHIR_XML_CONTENT, Constants.FHIR_XML_CONTENT, null, "fileConsent", null, null);
			}

			if (response != null) {
				ResourceResponseWrapper wrapper = new ResourceResponseWrapper(response);

				if (formatType.equals("JSON")) {
					context.setResponseString(wrapper.getResourceJSON());
				}
				else {
					context.setResponseString(wrapper.getResourceXML());
				}

				FacesContext.getCurrentInstance().addMessage("tabView:operationsTabView:fileConsentForm", new FacesMessage(FacesMessage.SEVERITY_INFO, "$fileConsent request successfully processed.", ""));
			}
			else {
				context.setResponseString("ERROR: Response is empty!");
				FacesContext.getCurrentInstance().addMessage("tabView:operationsTabView:fileConsentForm",
					new FacesMessage(FacesMessage.SEVERITY_ERROR, "Error processing $fileConsent! Response is empty.", "Error processing $fileConsent! Response is empty."));
			}

		}
		catch (Exception e) {
			FacesContext.getCurrentInstance().addMessage("tabView:operationsTabView:fileConsentForm",
					new FacesMessage(FacesMessage.SEVERITY_ERROR, "Error processing $fileConsent! Please check the client logs.", "Error processing $fileConsent! Please check the client logs."));
			e.printStackTrace();
		}

		log.fine("[END] ApplicationController.processFileConsent()");
	}

	/**
	 * Process the $updateConsent operation request
	 * 
	 * @param event
	 */
	public void processUpdateConsent(ActionEvent event) {
		log.fine("[START] ApplicationController.processUpdateConsent()");
		log.info("$updateConsent info: ");

		try {
			log.info("Selected Format Type: " + context.getSelectedFormatType());
			log.info("BasePath for $updateConsent: " + context.getSelectedServerURL());

			FacesContext.getCurrentInstance().addMessage("tabView:operationsTabView:updateConsentForm", new FacesMessage(FacesMessage.SEVERITY_INFO, "$updateConsent request successfully processed.", ""));
		}
		catch (Exception e) {
			FacesContext.getCurrentInstance().addMessage("tabView:operationsTabView:updateConsentForm",
					new FacesMessage(FacesMessage.SEVERITY_ERROR, "Error processing $updateConsent! Please check the client logs.", "Error processing $updateConsent! Please check the client logs."));
			e.printStackTrace();
		}

		log.fine("[END] ApplicationController.processUpdateConsent()");
	}

	/**
	 * Process the $revokeConsent operation request
	 * 
	 * @param event
	 */
	public void processRevokeConsent(ActionEvent event) {
		log.fine("[START] ApplicationController.processRevokeConsent()");
		log.info("$revokeConsent info: ");

		try {
			log.info("Selected Format Type: " + context.getSelectedFormatType());
			log.info("BasePath for $fileConsent: " + context.getSelectedServerURL());

			FacesContext.getCurrentInstance().addMessage("tabView:operationsTabView:revokeConsentForm", new FacesMessage(FacesMessage.SEVERITY_INFO, "$revokeConsent request successfully processed.", ""));
		}
		catch (Exception e) {
			FacesContext.getCurrentInstance().addMessage("tabView:operationsTabView:revokeConsentForm",
					new FacesMessage(FacesMessage.SEVERITY_ERROR, "Error processing $revokeConsent! Please check the client logs.", "Error processing $revokeConsent! Please check the client logs."));
			e.printStackTrace();
		}

		log.fine("[END] ApplicationController.processRevokeConsent()");
	}

	/**
	 * Process the $recordDisclosure operation request
	 * 
	 * @param event
	 */
	public void processRecordDisclosure(ActionEvent event) {
		log.fine("[START] ApplicationController.processRecordDisclosure()");
		log.info("$recordDisclosure info: ");

		try {
			log.info("Selected Format Type: " + context.getSelectedFormatType());
			log.info("BasePath for $fileConsent: " + context.getSelectedServerURL());

			FacesContext.getCurrentInstance().addMessage("tabView:operationsTabView:recordDisclosureForm", new FacesMessage(FacesMessage.SEVERITY_INFO, "$recordDisclosure request successfully processed.", ""));
		}
		catch (Exception e) {
			FacesContext.getCurrentInstance().addMessage("tabView:operationsTabView:recordDisclosureForm",
					new FacesMessage(FacesMessage.SEVERITY_ERROR, "Error processing $recordDisclosure! Please check the client logs.", "Error processing $recordDisclosure! Please check the client logs."));
			e.printStackTrace();
		}

		log.fine("[END] ApplicationController.processRecordDisclosure()");
	}

	/**
	 * Process the initialize client functionality
	 * 
	 * @param event
	 */
	public void initializeClient(ActionEvent event) {
		log.fine("[START] ApplicationController.initializeClient()");
		log.info("initializeClient info: ");

		int loadCount = 0;
		String loadPath = "??";

		try {
			log.info("Selected Format Type: " + context.getSelectedFormatType());
			log.info("BasePath for initializeClient: " + context.getSelectedServerURL());

			context.getClientresourceService().clientresourcePurgeAll();

			log.info("Purge clientresource complete.");

			loadPath = context.getCodeService().getCodeValue("initializeClientPath");

			log.info("Load path: " + loadPath);

			Path loadDir = FileSystems.getDefault().getPath(loadPath);

			List<Path> result = new ArrayList<>();
			try (DirectoryStream<Path> stream = Files.newDirectoryStream(loadDir, "*.json")) {

				for (Path entry : stream) {
					log.info("Path Found: " + entry.toString());
					File clientFile = new File(entry.toString());
					if (writeClientResource(clientFile)) {
						result.add(entry);
					}
				}

				log.info("=====================================================================");
				loadCount = result.size();
				log.info("Total file count: " + loadCount);

			}
			catch (DirectoryIteratorException ex) {
				// I/O error encountered during the iteration, the cause is an IOException
				throw ex.getCause();
			}
			finally {
				result = null;
			}

			FacesContext.getCurrentInstance().addMessage("tabView:adminTabView:initializeClientForm",
					new FacesMessage(FacesMessage.SEVERITY_INFO, "initializeClient request successfully processed " + loadCount + " files.", ""));

		}
		catch (NoSuchFileException e) {
			FacesContext.getCurrentInstance().addMessage("tabView:adminTabView:initializeClientForm",
					new FacesMessage(FacesMessage.SEVERITY_ERROR, "Error processing initializeClient! " + loadPath + " is not found.", "Error processing initializeClient! " + loadPath + " is not found."));
			e.printStackTrace();
		}
		catch (Exception e) {
			FacesContext.getCurrentInstance().addMessage("tabView:adminTabView:initializeClientForm",
					new FacesMessage(FacesMessage.SEVERITY_ERROR, "Error processing initializeClient! Please check the client logs.", "Error processing initializeClient! Please check the client logs."));
			e.printStackTrace();
		}

		log.fine("[END] ApplicationController.initializeClient()");
	}

	private boolean writeClientResource(File clientFile) {

		boolean bResult = false;
		String clientFileName = "";

		try {
			clientFileName = clientFile.getName();

			log.fine("[START] ApplicationController.writeClientResource() for " + clientFileName);

			// Read contents of clientFileName and parse into a FHIR Resource
			String clientContents = stringBuilder(clientFile.getPath());

			JsonParser resourceParser = new JsonParser();
			Resource resource = resourceParser.parse(clientContents);

			// Instantiate new Clientresource and save to database
			Clientresource clientresource = new Clientresource();

			clientresource.setResourceId(resource.getId());
			clientresource.setResourceType(resource.getResourceType().name());
			clientresource.setStatus("valid");
			clientresource.setResourceContents(clientContents.getBytes("UTF-8"));

			context.getClientresourceService().create(clientresource, resource);

			bResult = true;
		}
		catch (Exception e) {
			// Swallow exception to allow processing to continue
			log.severe("Error processing clientFile '" + clientFile.getName() + "' - " + e.getMessage());
		}

		log.fine("[END] ResourceTemplateService.writeResourceTemplate()");

		return bResult;
	}

	/*
	 * Server directory manage methods
	 */

	/**
	 * Handles updating or creating new servers
	 *
	 * @param serverType
	 *            - new or existing
	 */
	public void manageServer(String serverType) {

		if (serverType.equalsIgnoreCase("existing")) {
			log.info("[Start] ApplicationController.manageServer() - update Existing Server");
			try {

				Serverdirectory server = context.getServerDirectoryService().update(context.getSelectedServer());
				if (server != null) {
					FacesContext.getCurrentInstance().addMessage("tabView:serversForm", new FacesMessage(FacesMessage.SEVERITY_INFO, "Server: " + server.getName() + " updated.", ""));
				}
			}
			catch (Exception e) {
				log.severe(e.getMessage());
				FacesContext.getCurrentInstance().addMessage("tabView:serversForm", new FacesMessage(FacesMessage.SEVERITY_ERROR, "Error updating Server.", ""));
			}
		}

		if (serverType.equalsIgnoreCase("new")) {
			log.info("[Start] ApplicationController.manageServer() - Create new Server");
			try {
				Serverdirectory server = context.getServerDirectoryService().create(context.getNewServer());
				if (server != null) {
					FacesContext.getCurrentInstance().addMessage("tabView:serversForm", new FacesMessage(FacesMessage.SEVERITY_INFO, "Server: " + server.getName() + " successfully created.", ""));
					context.setNewServer(new Serverdirectory());
					context.setAvailableServers(context.getServerDirectoryService().findAll());
				}
			}
			catch (Exception e) {
				log.severe(e.getMessage());
				FacesContext.getCurrentInstance().addMessage("tabView:serversForm", new FacesMessage(FacesMessage.SEVERITY_ERROR, "Error creating new Server.", ""));
			}
		}
	}

	/**
	 * set the server to update
	 *
	 * @param server
	 */
	public void serverToManage(Serverdirectory server) {
		log.fine("[START] ApplicationController.serverToManage");
		context.setSelectedServer(server);
		log.info("serverToManage name: " + context.getSelectedServer().getName());
	}

	/**
	 * Remove server that matches supplied id
	 *
	 * @param server
	 */
	public void deleteServer(Serverdirectory server) {
		log.fine("[START] ApplicationController.deleteServer() - Server ID: " + server.getId());

		Integer serverId = server.getId();
		int result = -1;
		try {
			result = context.getServerDirectoryService().delete(serverId);
			if (result == 1) {
				FacesContext.getCurrentInstance().addMessage("tabView:adminTabView:serversForm", new FacesMessage(FacesMessage.SEVERITY_INFO, "Server with ID " + serverId + " successfully deleted.", ""));
				context.setAvailableServers(context.getServerDirectoryService().findAll());
			}
			else {
				throw new Exception("Error deleting Server");
			}
		}
		catch (NumberFormatException e) {
			log.severe(e.getMessage());
			FacesContext.getCurrentInstance().addMessage("tabView:adminTabView:serversForm", new FacesMessage(FacesMessage.SEVERITY_ERROR, "Number format error deleting Server with ID " + serverId, ""));
		}
		catch (Exception e) {
			log.severe(e.getMessage());
			FacesContext.getCurrentInstance().addMessage("tabView:adminTabView:serversForm", new FacesMessage(FacesMessage.SEVERITY_ERROR, "Error deleting Server with ID " + serverId, ""));
		}
	}

	/**
	 * Start the Subscription Service using the passed in since date
	 * 
	 * @param event
	 */
	public void processSubscriptions(ActionEvent event) {
		log.fine("[START] ApplicationController.processSubscriptions()");
		log.info("Subscription Criteria: ");

		List<LabelKeyValueBean> results = null;

		try {
			if (context.getCodeService().isSupported("subscriptionServiceEnabled")) {
				Date datePicker = context.getDatePicker();

				if (datePicker == null) {

					FacesContext.getCurrentInstance().addMessage("tabView:subscriptionClientTab:subscriptionClientForm",
							new FacesMessage(FacesMessage.SEVERITY_WARN, "Missing Criteria - please enter Since DateTime.", "Missing Criteria - please enter Since DateTime."));

				}
				else {
					log.info("datePicker = " + utcDateUtil.formatDate(datePicker, UTCDateUtil.DATE_PARAMETER_FORMAT, TimeZone.getDefault()));

					results = context.getSubscriptionServiceR5().processSubscriptions(datePicker);

					if (results == null) {
						results = new ArrayList<LabelKeyValueBean>();
					}
					if (results.isEmpty()) {
						results.add(new LabelKeyValueBean("No active subscriptions found.","",""));
					}

					context.setListLabelKeyValue(results);

					FacesContext.getCurrentInstance().addMessage("tabView:subscriptionClientTab:subscriptionClientForm",
							new FacesMessage(FacesMessage.SEVERITY_INFO, "Subscription processing successfully executed.", "Subscription processing successfully executed."));
				}
			}
			else {
				FacesContext.getCurrentInstance().addMessage("tabView:subscriptionClientTab:subscriptionClientForm",
						new FacesMessage(FacesMessage.SEVERITY_WARN, "Subscription processing is not enabled.", "Subscription processing is not enabled."));
			}
		}
		catch (Exception e1) {
			log.info(e1.getMessage());
			FacesContext.getCurrentInstance().addMessage("tabView:subscriptionClientTab:subscriptionClientForm",
					new FacesMessage(FacesMessage.SEVERITY_ERROR, "Error executing subscription processing! Please check the client logs.", "Error executing subscription processing! Please check the client logs."));

			e1.printStackTrace();
		}

		log.fine("[END] ApplicationController.fhirProcessSubscriptions()");
	}

	/**
	 * read a file and converting it to String using StringBuilder
	 */
	protected String stringBuilder(String fileName) throws IOException {

		StringBuilder sbuilder = null;
		FileInputStream fStream = null;
		BufferedReader input = null;

		try {

			fStream = new FileInputStream(fileName);
			input = new BufferedReader(new InputStreamReader(fStream, "UTF-8"));

			sbuilder = new StringBuilder();

			String str = input.readLine();

			while (str != null) {
				sbuilder.append(str);
				str = input.readLine();
				if (str != null) {

					sbuilder.append("\n");

				}
			}

		}
		finally {
			input.close();
			fStream.close();
		}

		return sbuilder.toString();
	}

}
