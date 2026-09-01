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
import java.io.ByteArrayInputStream;
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
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import jakarta.faces.annotation.ManagedProperty;
import jakarta.enterprise.context.SessionScoped;
import jakarta.faces.application.FacesMessage;
import jakarta.faces.context.ExternalContext;
import jakarta.faces.context.FacesContext;
import jakarta.faces.event.ActionEvent;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.ws.rs.core.Response;

import org.hl7.fhir.r4.formats.IParser.OutputStyle;
import org.hl7.fhir.r4.formats.JsonParser;
import org.hl7.fhir.r4.formats.XmlParser;
import org.hl7.fhir.r4.model.Attachment;
import org.hl7.fhir.r4.model.AuditEvent;
import org.hl7.fhir.r4.model.AuditEvent.AuditEventAction;
import org.hl7.fhir.r4.model.AuditEvent.AuditEventAgentComponent;
import org.hl7.fhir.r4.model.AuditEvent.AuditEventAgentNetworkComponent;
import org.hl7.fhir.r4.model.AuditEvent.AuditEventEntityComponent;
import org.hl7.fhir.r4.model.AuditEvent.AuditEventOutcome;
import org.hl7.fhir.r4.model.AuditEvent.AuditEventSourceComponent;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.Bundle.BundleLinkComponent;
import org.hl7.fhir.r4.model.CodeType;
import org.hl7.fhir.r4.model.CodeableConcept;
import org.hl7.fhir.r4.model.Coding;
import org.hl7.fhir.r4.model.Consent;
import org.hl7.fhir.r4.model.Consent.ConsentPolicyComponent;
import org.hl7.fhir.r4.model.Consent.ConsentProvisionType;
import org.hl7.fhir.r4.model.Consent.ConsentState;
import org.hl7.fhir.r4.model.Consent.ProvisionComponent;
import org.hl7.fhir.r4.model.Consent.provisionActorComponent;
import org.hl7.fhir.r4.model.DocumentReference;
import org.hl7.fhir.r4.model.DocumentReference.DocumentReferenceContentComponent;
import org.hl7.fhir.r4.model.Enumerations.DocumentReferenceStatus;
import org.hl7.fhir.r4.model.Extension;
import org.hl7.fhir.r4.model.Identifier;
import org.hl7.fhir.r4.model.Meta;
import org.hl7.fhir.r4.model.OperationOutcome;
import org.hl7.fhir.r4.model.Parameters;
import org.hl7.fhir.r4.model.Parameters.ParametersParameterComponent;
import org.hl7.fhir.r4.model.Patient;
import org.hl7.fhir.r4.model.Period;
import org.hl7.fhir.r4.model.PositiveIntType;
import org.hl7.fhir.r4.model.Reference;
import org.hl7.fhir.r4.model.RelatedPerson;
import org.hl7.fhir.r4.model.Resource;
import org.hl7.fhir.r4.model.StringType;
import org.hl7.fhir.r4.model.Subscription;
import org.hl7.fhir.r4.model.Subscription.SubscriptionChannelComponent;
import org.hl7.fhir.r4.model.Subscription.SubscriptionChannelType;
import org.hl7.fhir.r4.model.Subscription.SubscriptionStatus;
import org.hl7.fhir.r4.model.UnsignedIntType;
import org.primefaces.event.TabChangeEvent;

import net.aegis.fhir.client.ApplicationContext;
import net.aegis.fhir.client.util.WildfhirClientException;
import net.aegis.fhir.model.Clientresource;
import net.aegis.fhir.model.Constants;
import net.aegis.fhir.model.LabelKeyValueBean;
import net.aegis.fhir.model.ResourceType;
import net.aegis.fhir.model.Serverdirectory;
import net.aegis.fhir.model.client.BundleWrapper;
import net.aegis.fhir.model.client.ResourceResponseWrapper;
import net.aegis.fhir.service.util.ServicesUtil;
import net.aegis.fhir.service.util.StringUtils;
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
@Named("controller")
@SessionScoped
public class ApplicationController implements Serializable {

	private static final long serialVersionUID = 5848069089082841377L;

	private Logger log = Logger.getLogger("ApplicationController");

	@Inject
	@ManagedProperty("#{context}")
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
		Iterator<FacesMessage> iter = FacesContext.getCurrentInstance().getMessages();
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
	 * FHIR Interaction methods
	 */

	/**
	 * Creates a new Resource
	 *
	 * @see Resource
	 */
	public void fhirCreate() {
		log.fine("[START] ApplicationController.fhirCeate()");

		try {
			log.fine("BasePath for FHIR create: " + context.getSelectedServerURL());

			context.setCurrentView("create");

			String formatType = context.getSelectedFormatType();
			String ifNoneExist = context.getIfNoneExist();
			String prefer = context.getPrefer();
			String _format = context.get_format();
			String resourceString = context.getResourceString();

			ByteArrayInputStream iResource = null;
			Resource resource = null;
			Response response = null;
			ResourceResponseWrapper wrapper = null;

			if (resourceString.isEmpty()) {
				throw new Exception("No resource content provided.");
			}

			if (formatType.equals("XML")) {
				// Convert XML contents to Resource
				XmlParser xmlP = new XmlParser();
				int firstValid = resourceString.indexOf("<");
				if (firstValid > 0) {
					resourceString = resourceString.substring(firstValid);
				}

				iResource = new ByteArrayInputStream(resourceString.getBytes());
				resource = xmlP.parse(iResource);

				response = context.getResourceRESTClient().create(resource, context.getSelectedServerURL(), context.getSelectedResourceType(), Constants.FHIR_XML_CONTENT, ifNoneExist, prefer, _format, null);
			}
			else {
				// Convert JSON contents to Resource
				JsonParser jsonP = new JsonParser();
				int firstValid = resourceString.indexOf("{");
				if (firstValid > 0) {
					resourceString = resourceString.substring(firstValid);
				}

				iResource = new ByteArrayInputStream(resourceString.getBytes());
				resource = jsonP.parse(iResource);

				response = context.getResourceRESTClient().create(resource, context.getSelectedServerURL(), context.getSelectedResourceType(), Constants.FHIR_JSON_CONTENT, ifNoneExist, prefer, _format, null);
			}

			if (response != null) {
				String contentType = response.getHeaderString("Content-Type");
				if (contentType != null) {
					if (contentType.toUpperCase().contains("XML")) {
						context.setReturnedFormatType("XML");
					}
					else if (contentType.toUpperCase().contains("JSON")) {
						context.setReturnedFormatType("JSON");
					}
					else {
						context.setReturnedFormatType(formatType);
					}
				}
				else {
					context.setReturnedFormatType(formatType);
				}

				if ((response.getStatus() == Response.Status.OK.getStatusCode()) || (response.getStatus() == Response.Status.CREATED.getStatusCode())) {
					try {
						wrapper = new ResourceResponseWrapper(response);

						if (context.getReturnedFormatType().equals("XML")) {
							context.setResponseString(wrapper.getResourceXML());
						}
						else {
							context.setResponseString(wrapper.getResourceJSON());
						}

						FacesContext.getCurrentInstance().addMessage(
								"tabView:interactionsTabView:createForm",
								new FacesMessage(FacesMessage.SEVERITY_INFO, "Resource with ID: " + wrapper.getResourceBean().getResourceId() + " successfully created.", "Resource with ID: " + wrapper.getResourceBean().getResourceId()
										+ " successfully created."));
					}
					catch (Exception e) {
						log.severe(e.getMessage());
						FacesContext.getCurrentInstance().addMessage("tabView:interactionsTabView:createForm",
								new FacesMessage(FacesMessage.SEVERITY_ERROR, "Resource parsing failed! Please check the client logs.", "Resource parsing failed! Please check the client logs."));
						e.printStackTrace();
					}
				}
				else {
					try {
						wrapper = new ResourceResponseWrapper(response);

						if (context.getReturnedFormatType().equals("XML")) {
							context.setResponseString(wrapper.getResourceXML());
						}
						else {
							context.setResponseString(wrapper.getResourceJSON());
						}

						FacesContext.getCurrentInstance().addMessage("tabView:interactionsTabView:createForm", new FacesMessage(FacesMessage.SEVERITY_INFO, "Response " + response.getStatus() + " - Failed to create new Resource entry.", "Response " + response.getStatus() + " - Failed to create new Resource entry."));
					}
					catch (Exception e) {
						log.severe(e.getMessage());
						FacesContext.getCurrentInstance().addMessage("tabView:interactionsTabView:createForm",
								new FacesMessage(FacesMessage.SEVERITY_ERROR, "Resource parsing failed! Please check the client logs.", "Resource parsing failed! Please check the client logs."));
						e.printStackTrace();
					}
				}
			}

		}
		catch (Exception e) {
			log.severe(e.getMessage());
			FacesContext.getCurrentInstance().addMessage("tabView:interactionsTabView:createForm", new FacesMessage(FacesMessage.SEVERITY_ERROR, "Error creating resource! Please check the client logs.", "Error reading resource! Please check the client logs."));
			e.printStackTrace();
		}

		context.setResourceString(null);

		log.fine("[END] ApplicationController.fhirCreate()");
	}

	/**
	 * Display resource specific search criteria based on the selected resource type
	 *
	 * @param event
	 */
	public void showCreateOptions(ActionEvent event) {
		log.info("[START] ApplicationController.showCreateOptions()");

		String resourceType = context.getSelectedResourceType();
		String formatType = context.getSelectedFormatType();

		StringBuilder sbResource = new StringBuilder("");

		if (formatType.equals("XML")) {
			sbResource.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
			sbResource.append("<").append(resourceType).append(" xmlns=\"http://hl7.org/fhir\">\n");
			sbResource.append("  <!-- INSERT or REPLACE CONTENTS HERE -->\n");
			sbResource.append("<").append(resourceType).append("/>");
		}
		else {
			sbResource.append("{\n");
			sbResource.append("  \"resourceType\":\"").append(resourceType).append("\"\n");
			sbResource.append("  /* INSERT or REPLACE CONTENTS HERE */\n");
			sbResource.append("}\n");
		}

		context.setResourceString(sbResource.toString());

		log.info("[END] ApplicationController.showCreateOptions()");
	}

	/**
	 * Perform a FHIR read for the supplied resource id
	 *
	 * @return
	 */
	public void fhirRead(ActionEvent event) {
		log.fine("[START] ApplicationController.fhirRead()");

		try {
			log.fine("BasePath for FHIR read: " + context.getSelectedServerURL());
			context.setResourceResults(null);
			String resourceId = context.getResourceId();
			String ifModifiedSince = context.getIfModifiedSince();
			String ifNoneMatch = context.getIfNoneMatch();
			String _format = context.get_format();
			String _summary = context.get_summary();
			Response resourceResponse = null;
			context.setResourceResults(new ArrayList<ResourceResponseWrapper>());
			String formatType = context.getSelectedFormatType();

			resourceResponse = context.getResourceRESTClient().read(resourceId, context.getSelectedServerURL(), context.getSelectedResourceType(), formatType, ifModifiedSince, ifNoneMatch, _format, _summary, null);

			if (resourceResponse != null) {
				String contentType = resourceResponse.getHeaderString("Content-Type");
				if (contentType != null) {
					if (contentType.toUpperCase().contains("XML")) {
						context.setReturnedFormatType("XML");
					}
					else if (contentType.toUpperCase().contains("JSON")) {
						context.setReturnedFormatType("JSON");
					}
					else {
						context.setReturnedFormatType(formatType);
					}
				}
				else {
					context.setReturnedFormatType(formatType);
				}

				if (resourceResponse.getStatus() == (Response.Status.OK.getStatusCode())) {
					try {
						context.getResourceResults().add((new ResourceResponseWrapper(resourceResponse)));
					}
					catch (Exception e) {
						log.severe(e.getMessage());
						FacesContext.getCurrentInstance().addMessage("tabView:interactionsTabView:fhirReadForm",
								new FacesMessage(FacesMessage.SEVERITY_ERROR, "Resource parsing failed! Please check the client logs.", "Resource parsing failed! Please check the client logs."));
						e.printStackTrace();
					}

				}
				else if (resourceResponse.getStatus() == (Response.Status.NOT_MODIFIED.getStatusCode())) {
					log.fine(Integer.toString(resourceResponse.getStatus()));
					FacesContext.getCurrentInstance().addMessage("tabView:interactionsTabView:fhirReadForm", new FacesMessage(FacesMessage.SEVERITY_INFO, "RESOURCE " + resourceId + " NOT MODIFIED", "RESOURCE " + resourceId + " NOT MODIFIED"));
				}
				else {
					log.fine(Integer.toString(resourceResponse.getStatus()));
					FacesContext.getCurrentInstance().addMessage("tabView:interactionsTabView:fhirReadForm", new FacesMessage(FacesMessage.SEVERITY_INFO, "Response " + resourceResponse.getStatus() + " - No Resource found matching ID " + resourceId, "Response " + resourceResponse.getStatus() + " - No Resource found matching ID " + resourceId));
				}
			}
		}
		catch (Exception e) {
			log.severe(e.getMessage());
			FacesContext.getCurrentInstance().addMessage("tabView:interactionsTabView:fhirReadForm", new FacesMessage(FacesMessage.SEVERITY_ERROR, "Error reading resource! Please check the client logs.", "Error reading resource! Please check the client logs."));
			e.printStackTrace();
		}

		log.fine("[END] ApplicationController.fhirRead()");
	}

	/**
	 * Perform a FHIR read for the supplied resource id
	 *
	 * @return
	 */
	public void fhirVRead(ActionEvent event) {
		log.fine("[START] ApplicationController.fhirVRead()");

		try {
			log.fine("BasePath for FHIR vread: " + context.getSelectedServerURL());
			context.setResourceResults(null);
			String resourceId = context.getResourceId();
			String versionId = context.getResourceVersion();
			String _format = context.get_format();
			String _summary = context.get_summary();
			Response resourceResponse = null;
			context.setResourceResults(new ArrayList<ResourceResponseWrapper>());
			String formatType = context.getSelectedFormatType();

			resourceResponse = context.getResourceRESTClient().vread(resourceId, versionId, context.getSelectedServerURL(), context.getSelectedResourceType(), formatType, _format, _summary, null);

			if (resourceResponse != null) {
				String contentType = resourceResponse.getHeaderString("Content-Type");
				if (contentType != null) {
					if (contentType.toUpperCase().contains("XML")) {
						context.setReturnedFormatType("XML");
					}
					else if (contentType.toUpperCase().contains("JSON")) {
						context.setReturnedFormatType("JSON");
					}
					else {
						context.setReturnedFormatType(formatType);
					}
				}
				else {
					context.setReturnedFormatType(formatType);
				}

				if (resourceResponse.getStatus() == (Response.Status.OK.getStatusCode())) {
					try {
						context.getResourceResults().add((new ResourceResponseWrapper(resourceResponse)));
					}
					catch (Exception e) {
						log.severe(e.getMessage());
						FacesContext.getCurrentInstance().addMessage("tabView:interactionsTabView:fhirVReadForm",
								new FacesMessage(FacesMessage.SEVERITY_ERROR, "Resource parsing failed! Please check the client logs.", "Resource parsing failed! Please check the client logs."));
						e.printStackTrace();
					}

				}
				else if (resourceResponse.getStatus() == (Response.Status.NOT_MODIFIED.getStatusCode())) {
					log.fine(Integer.toString(resourceResponse.getStatus()));
					FacesContext.getCurrentInstance().addMessage("tabView:interactionsTabView:fhirVReadForm", new FacesMessage(FacesMessage.SEVERITY_INFO, "RESOURCE " + resourceId + " NOT MODIFIED", "RESOURCE " + resourceId + " NOT MODIFIED"));
				}
				else {
					log.fine(Integer.toString(resourceResponse.getStatus()));
					FacesContext.getCurrentInstance().addMessage("tabView:interactionsTabView:fhirVReadForm", new FacesMessage(FacesMessage.SEVERITY_INFO, "Response " + resourceResponse.getStatus() + " - No Resource found matching ID " + resourceId, "Response " + resourceResponse.getStatus() + " - No Resource found matching ID " + resourceId));
				}
			}
		}
		catch (Exception e) {
			log.severe(e.getMessage());
			FacesContext.getCurrentInstance().addMessage("tabView:interactionsTabView:fhirVReadForm", new FacesMessage(FacesMessage.SEVERITY_ERROR, "Error reading resource! Please check the client logs.", "Error reading resource! Please check the client logs."));
			e.printStackTrace();
		}

		log.fine("[END] ApplicationController.fhirVRead()");
	}

	/**
	 * Performs a FHIR history read for specified patient record in List of patient results, Updates messages for the UI
	 * form whose id is supplied
	 *
	 * @param event
	 */
	public void fhirHistory(ActionEvent event) {
		log.fine("[START] ApplicationController.fhirHistory()");
		log.fine("BasePath for FHIR history: " + context.getSelectedServerURL());

		String formatType = context.getSelectedFormatType();

		String _format = context.get_format();
		String _count = context.get_count();
		String _since = context.get_since();

		UTCDateUtil utcDateUtil;

		if (!StringUtils.isNullOrEmpty(_since)) {
			try {
				utcDateUtil = new UTCDateUtil();
				utcDateUtil.parseXMLDate(_since);
				log.fine("fhirHistory _since = " + _since);
			}
			catch (Exception e) {
				log.severe("Exception parsing _since parameter to UTC Date! " + e.getMessage());
				FacesContext.getCurrentInstance().addMessage("tabView:interactionsTabView:historyForm",
						new FacesMessage(FacesMessage.SEVERITY_ERROR, "_since parameter is not a UTC Date! Example '2015-12-09T15:53:18Z'.", "_since parameter is not a UTC Date! Example '2015-12-09T15:53:18Z'."));
				return;
			}
			finally {
				utcDateUtil = null;
			}
		}

		Response response = null;
		ResourceResponseWrapper wrapper = null;
		context.setResourceResults(new ArrayList<ResourceResponseWrapper>());

		try {
			if (formatType.equals("XML")) {
				response = context.getResourceRESTClient().history(context.getResourceId(), context.getSelectedServerURL(), context.getSelectedResourceType(), Constants.FHIR_XML_CONTENT, _format, _count, _since, null);
			}
			else {
				response = context.getResourceRESTClient().history(context.getResourceId(), context.getSelectedServerURL(), context.getSelectedResourceType(), Constants.FHIR_JSON_CONTENT, _format, _count, _since, null);
			}
		}
		catch (NumberFormatException e) {
			FacesContext.getCurrentInstance().addMessage("tabView:interactionsTabView:historyForm",
					new FacesMessage(FacesMessage.SEVERITY_ERROR, "Number format error getting resource history! Please check the client logs.", "Error reading resource! Please check the client logs."));
			e.printStackTrace();
		}
		catch (Exception e) {
			FacesContext.getCurrentInstance().addMessage("tabView:interactionsTabView:historyForm",
					new FacesMessage(FacesMessage.SEVERITY_ERROR, "Error getting resource history! Please check the client logs.", "Error reading resource! Please check the client logs."));
			e.printStackTrace();
		}

		if (response != null) {

			String contentType = response.getHeaderString("Content-Type");
			if (contentType != null) {
				if (contentType.toUpperCase().contains("XML")) {
					context.setReturnedFormatType("XML");
				}
				else if (contentType.toUpperCase().contains("JSON")) {
					context.setReturnedFormatType("JSON");
				}
				else {
					context.setReturnedFormatType(formatType);
				}
			}
			else {
				context.setReturnedFormatType(formatType);
			}

			if (response.getStatus() == Response.Status.OK.getStatusCode()) {
				try {
					wrapper = new ResourceResponseWrapper(response);
					context.getResourceResults().add(wrapper);

					// Check for Bundle Links
					BundleWrapper historyBundleWrapper = wrapper.getBundle();
					Bundle historyBundle = historyBundleWrapper.getBundle();

					if (historyBundle.hasLink()) {
						context.getPageReference().clear();

						for (BundleLinkComponent bundleLink : historyBundle.getLink()) {

							if (bundleLink.hasRelation()) {

								if (bundleLink.getRelation().equals("first")) {
									LabelKeyValueBean firstPage = new LabelKeyValueBean("First", "first", bundleLink.getUrl());
									context.getPageReference().add(firstPage);
								}

								if (bundleLink.getRelation().equals("next")) {
									LabelKeyValueBean nextPage = new LabelKeyValueBean("Next", "next", bundleLink.getUrl());
									context.getPageReference().add(nextPage);
								}

								if (bundleLink.getRelation().equals("previous")) {
									LabelKeyValueBean prevPage = new LabelKeyValueBean("Prev", "previous", bundleLink.getUrl());
									context.getPageReference().add(prevPage);
								}

								if (bundleLink.getRelation().equals("last")) {
									LabelKeyValueBean lastPage = new LabelKeyValueBean("Last", "last", bundleLink.getUrl());
									context.getPageReference().add(lastPage);
								}
							}
						}
					}

					FacesContext.getCurrentInstance().addMessage(
							"tabView:interactionsTabView:historyForm",
							new FacesMessage(FacesMessage.SEVERITY_INFO, "History for Resource with ID: " + context.getResourceId() + " successfully returned.", ""));
				}
				catch (Exception e) {
					FacesContext.getCurrentInstance().addMessage("tabView:interactionsTabView:historyForm",
							new FacesMessage(FacesMessage.SEVERITY_ERROR, "Resource parsing failed! Please check the client logs.", ""));
					e.printStackTrace();
				}
			}
			else {
				FacesContext.getCurrentInstance().addMessage("tabView:interactionsTabView:historyForm",
						new FacesMessage(FacesMessage.SEVERITY_WARN, "Response code: " + Integer.toString(response.getStatus()) + " from server not ok.", ""));
			}
		}

		log.fine("[END] ApplicationController.fhirHistory()");
	}

	/**
	 * Performs a FHIR history read for a specific page from a previous history result Bundle.
	 *
	 * @param historyPageUrl
	 */
	public void fhirHistoryPage(String historyPageUrl) {
		log.fine("[START] ApplicationController.fhirHistoryPage()");
		log.fine("BasePath for FHIR delete: " + context.getSelectedServerURL());

		String formatType = context.getSelectedFormatType();

		Response response = null;
		ResourceResponseWrapper wrapper = null;
		context.setResourceResults(new ArrayList<ResourceResponseWrapper>());

		try {
			if (formatType.equals("XML")) {
				response = context.getResourceRESTClient().historyPage(historyPageUrl, Constants.FHIR_XML_CONTENT, null);
			}
			else {
				response = context.getResourceRESTClient().historyPage(historyPageUrl, Constants.FHIR_JSON_CONTENT, null);
			}
		}
		catch (NumberFormatException e) {
			FacesContext.getCurrentInstance().addMessage("tabView:interactionsTabView:historyForm",
					new FacesMessage(FacesMessage.SEVERITY_ERROR, "Number format error getting resource history! Please check the client logs.", "Error reading resource! Please check the client logs."));
			e.printStackTrace();
		}
		catch (Exception e) {
			FacesContext.getCurrentInstance().addMessage("tabView:interactionsTabView:historyForm",
					new FacesMessage(FacesMessage.SEVERITY_ERROR, "Error getting resource history! Please check the client logs.", "Error reading resource! Please check the client logs."));
			e.printStackTrace();
		}

		if (response != null) {

			String contentType = response.getHeaderString("Content-Type");
			if (contentType != null) {
				if (contentType.toUpperCase().contains("XML")) {
					context.setReturnedFormatType("XML");
				}
				else if (contentType.toUpperCase().contains("JSON")) {
					context.setReturnedFormatType("JSON");
				}
				else {
					context.setReturnedFormatType(formatType);
				}
			}
			else {
				context.setReturnedFormatType(formatType);
			}

			if (response.getStatus() == Response.Status.OK.getStatusCode()) {
				try {
					wrapper = new ResourceResponseWrapper(response);
					context.getResourceResults().add(wrapper);

					// Check for Bundle Links
					BundleWrapper historyBundleWrapper = wrapper.getBundle();
					Bundle historyBundle = historyBundleWrapper.getBundle();

					if (historyBundle.hasLink()) {
						context.getPageReference().clear();

						for (BundleLinkComponent bundleLink : historyBundle.getLink()) {

							if (bundleLink.hasRelation()) {

								if (bundleLink.getRelation().equals("first")) {
									LabelKeyValueBean firstPage = new LabelKeyValueBean("First", "first", bundleLink.getUrl());
									context.getPageReference().add(firstPage);
								}

								if (bundleLink.getRelation().equals("next")) {
									LabelKeyValueBean nextPage = new LabelKeyValueBean("Next", "next", bundleLink.getUrl());
									context.getPageReference().add(nextPage);
								}

								if (bundleLink.getRelation().equals("previous")) {
									LabelKeyValueBean prevPage = new LabelKeyValueBean("Prev", "previous", bundleLink.getUrl());
									context.getPageReference().add(prevPage);
								}

								if (bundleLink.getRelation().equals("last")) {
									LabelKeyValueBean lastPage = new LabelKeyValueBean("Last", "last", bundleLink.getUrl());
									context.getPageReference().add(lastPage);
								}
							}
						}
					}

					FacesContext.getCurrentInstance().addMessage(
							"tabView:interactionsTabView:historyForm",
							new FacesMessage(FacesMessage.SEVERITY_INFO, "History for Resource with ID: " + context.getResourceId() + " successfully returned.", ""));
				}
				catch (Exception e) {
					FacesContext.getCurrentInstance().addMessage("tabView:interactionsTabView:historyForm",
							new FacesMessage(FacesMessage.SEVERITY_ERROR, "Resource parsing failed! Please check the client logs.", ""));
					e.printStackTrace();
				}
			}
			else {
				FacesContext.getCurrentInstance().addMessage("tabView:interactionsTabView:historyForm",
						new FacesMessage(FacesMessage.SEVERITY_WARN, "Response code: " + Integer.toString(response.getStatus()) + " from server not ok.", ""));
			}
		}

		log.fine("[END] ApplicationController.fhirHistoryPage()");
	}

	/**
	 * Perform a FHIR search operation based on the entered resource type (optional) and search parameter values
	 *
	 * @param event
	 */
	public void fhirSearch(ActionEvent event) {
		log.fine("[START] ApplicationController.fhirSearch()");
		log.fine("BasePath for FHIR search: " + context.getSelectedServerURL());
		log.fine("Search Criteria: ");

		Map<String, String> criteriaToSend = new HashMap<String, String>();

		for (LabelKeyValueBean lkvb : context.getResourceCriteria()) {
			if (!lkvb.getValue().isEmpty()) {
				log.fine(lkvb.getKey() + " = " + lkvb.getValue());
				criteriaToSend.put(lkvb.getKey(), lkvb.getValue());
			}
		}

		String _format = context.get_format();
		String _summary = context.get_summary();
		String formatType = context.getSelectedFormatType();
		String httpOperation = context.getSelectedHttpOperation();

		Response response = null;
		ResourceResponseWrapper wrapper = null;
		context.setResourceResults(new ArrayList<ResourceResponseWrapper>());

		try {
			if (formatType.equals("XML")) {
				if (httpOperation.equals("GET")) {
					response = context.getResourceRESTClient().searchGet(criteriaToSend, context.getSelectedServerURL(), context.getSelectedResourceType(), Constants.FHIR_XML_CONTENT, _format, _summary, null);
				}
				else {
					response = context.getResourceRESTClient().searchPost(criteriaToSend, context.getSelectedServerURL(), context.getSelectedResourceType(), Constants.FHIR_XML_CONTENT, _format, _summary, null);
				}
			}
			else {
				if (httpOperation.equals("GET")) {
					response = context.getResourceRESTClient().searchGet(criteriaToSend, context.getSelectedServerURL(), context.getSelectedResourceType(), Constants.FHIR_JSON_CONTENT, _format, _summary, null);
				}
				else {
					response = context.getResourceRESTClient().searchPost(criteriaToSend, context.getSelectedServerURL(), context.getSelectedResourceType(), Constants.FHIR_JSON_CONTENT, _format, _summary, null);
				}
			}
		}
		catch (Exception e1) {
			log.fine(e1.getMessage());
			e1.printStackTrace();
		}
		if (response != null) {
			String contentType = response.getHeaderString("Content-Type");
			if (contentType != null) {
				if (contentType.toUpperCase().contains("XML")) {
					context.setReturnedFormatType("XML");
				}
				else if (contentType.toUpperCase().contains("JSON")) {
					context.setReturnedFormatType("JSON");
				}
				else {
					context.setReturnedFormatType(formatType);
				}
			}
			else {
				context.setReturnedFormatType(formatType);
			}

			if (response.getStatus() == Response.Status.OK.getStatusCode()) {
				try {
					wrapper = new ResourceResponseWrapper(response);
					context.getResourceResults().add(wrapper);

					// Check for Bundle Links
					BundleWrapper searchBundleWrapper = wrapper.getBundle();
					Bundle searchBundle = searchBundleWrapper.getBundle();

					if (searchBundle.hasLink()) {
						context.getPageReference().clear();

						for (BundleLinkComponent bundleLink : searchBundle.getLink()) {

							if (bundleLink.hasRelation()) {

								if (bundleLink.getRelation().equals("first")) {
									LabelKeyValueBean firstPage = new LabelKeyValueBean("First", "first", bundleLink.getUrl());
									context.getPageReference().add(firstPage);
								}

								if (bundleLink.getRelation().equals("next")) {
									LabelKeyValueBean nextPage = new LabelKeyValueBean("Next", "next", bundleLink.getUrl());
									context.getPageReference().add(nextPage);
								}

								if (bundleLink.getRelation().equals("previous")) {
									LabelKeyValueBean prevPage = new LabelKeyValueBean("Prev", "previous", bundleLink.getUrl());
									context.getPageReference().add(prevPage);
								}

								if (bundleLink.getRelation().equals("last")) {
									LabelKeyValueBean lastPage = new LabelKeyValueBean("Last", "last", bundleLink.getUrl());
									context.getPageReference().add(lastPage);
								}
							}
						}
					}

					FacesContext.getCurrentInstance().addMessage("tabView:interactionsTabView:fhirSearchForm", new FacesMessage(FacesMessage.SEVERITY_INFO, "Search successfully executed.", "Search successfully excuted."));
				}
				catch (Exception e1) {
					log.fine(e1.getMessage());
					FacesContext.getCurrentInstance().addMessage("tabView:interactionsTabView:fhirSearchForm",
							new FacesMessage(FacesMessage.SEVERITY_ERROR, "Error searching resource(s)! Please check the client logs.", "Error reading resource! Please check the client logs."));
					e1.printStackTrace();
				}

			}
			else {
				FacesContext.getCurrentInstance().addMessage("tabView:interactionsTabView:fhirSearchForm", new FacesMessage(FacesMessage.SEVERITY_INFO, "No Resource Results found based on supplied criteria", "No Resource Results found based on supplied criteria"));
			}
		}

		log.fine("[END] ApplicationController.fhirSearch()");
	}

	/**
	 * Performs a FHIR search read for a specific page from a previous history result Bundle.
	 *
	 * @param searchPageUrl
	 */
	public void fhirSearchPage(String searchPageUrl) {
		log.fine("[START] ApplicationController.fhirSearchPage()");
		log.fine("BasePath for FHIR delete: " + context.getSelectedServerURL());

		String formatType = context.getSelectedFormatType();

		Response response = null;
		ResourceResponseWrapper wrapper = null;
		context.setResourceResults(new ArrayList<ResourceResponseWrapper>());

		try {
			if (formatType.equals("XML")) {
				response = context.getResourceRESTClient().searchPage(searchPageUrl, Constants.FHIR_XML_CONTENT, null);
			}
			else {
				response = context.getResourceRESTClient().searchPage(searchPageUrl, Constants.FHIR_JSON_CONTENT, null);
			}
		}
		catch (NumberFormatException e) {
			FacesContext.getCurrentInstance().addMessage("tabView:interactionsTabView:fhirSearchForm",
					new FacesMessage(FacesMessage.SEVERITY_ERROR, "Number format error getting page resources! Please check the client logs.", "Number format error getting page resources! Please check the client logs."));
			e.printStackTrace();
		}
		catch (Exception e) {
			FacesContext.getCurrentInstance().addMessage("tabView:interactionsTabView:fhirSearchForm",
					new FacesMessage(FacesMessage.SEVERITY_ERROR, "Error getting page resources! Please check the client logs.", "Error getting page resources! Please check the client logs."));
			e.printStackTrace();
		}

		if (response != null) {

			String contentType = response.getHeaderString("Content-Type");
			if (contentType != null) {
				if (contentType.toUpperCase().contains("XML")) {
					context.setReturnedFormatType("XML");
				}
				else if (contentType.toUpperCase().contains("JSON")) {
					context.setReturnedFormatType("JSON");
				}
				else {
					context.setReturnedFormatType(formatType);
				}
			}
			else {
				context.setReturnedFormatType(formatType);
			}

			if (response.getStatus() == Response.Status.OK.getStatusCode()) {
				try {
					wrapper = new ResourceResponseWrapper(response);
					context.getResourceResults().add(wrapper);

					// Check for Bundle Links
					BundleWrapper searchBundleWrapper = wrapper.getBundle();
					Bundle searchBundle = searchBundleWrapper.getBundle();

					if (searchBundle.hasLink()) {
						context.getPageReference().clear();

						for (BundleLinkComponent bundleLink : searchBundle.getLink()) {

							if (bundleLink.hasRelation()) {

								if (bundleLink.getRelation().equals("first")) {
									LabelKeyValueBean firstPage = new LabelKeyValueBean("First", "first", bundleLink.getUrl());
									context.getPageReference().add(firstPage);
								}

								if (bundleLink.getRelation().equals("next")) {
									LabelKeyValueBean nextPage = new LabelKeyValueBean("Next", "next", bundleLink.getUrl());
									context.getPageReference().add(nextPage);
								}

								if (bundleLink.getRelation().equals("previous")) {
									LabelKeyValueBean prevPage = new LabelKeyValueBean("Prev", "previous", bundleLink.getUrl());
									context.getPageReference().add(prevPage);
								}

								if (bundleLink.getRelation().equals("last")) {
									LabelKeyValueBean lastPage = new LabelKeyValueBean("Last", "last", bundleLink.getUrl());
									context.getPageReference().add(lastPage);
								}
							}
						}
					}

					FacesContext.getCurrentInstance().addMessage("tabView:interactionsTabView:fhirSearchForm", new FacesMessage(FacesMessage.SEVERITY_INFO, "Search successfully executed.", "Search successfully excuted."));
				}
				catch (Exception e1) {
					log.fine(e1.getMessage());
					FacesContext.getCurrentInstance().addMessage("tabView:interactionsTabView:fhirSearchForm",
							new FacesMessage(FacesMessage.SEVERITY_ERROR, "Error searching resource(s)! Please check the client logs.", "Error searching resource(s)! Please check the client logs."));
					e1.printStackTrace();
				}

			}
			else {
				FacesContext.getCurrentInstance().addMessage("tabView:interactionsTabView:fhirSearchForm", new FacesMessage(FacesMessage.SEVERITY_INFO, "No Resource Results found based on supplied criteria", "No Resource Results found based on supplied criteria"));
			}
		}

		log.fine("[END] ApplicationController.fhirSearchPage()");
	}

	/**
	 * Read resource for subsequent operations of update or validate
	 *
	 * @param event
	 */
	public void searchResourceForOperation(ActionEvent event) {
		log.fine("[START] ApplicationController.searchResourceForOperation()");
		context.setCurrentView("update");

		String formatType = context.getSelectedFormatType();

		// fetch resource record
		fhirRead(event);
		String operation = (String) event.getComponent().getAttributes().get("operation");

		if (context.getResourceResults() != null && context.getResourceResults().size() > 0) {

			String resourceString = null;

			if (formatType.equals("XML")) {
				resourceString = context.getResourceResults().get(0).getResourceXML();
			}
			else {
				resourceString = context.getResourceResults().get(0).getResourceJSON();
			}

			if (operation.equals("validate")) {
				Parameters input = new Parameters();
	    		ParametersParameterComponent parameter = new ParametersParameterComponent();
	    		parameter.setName("profile");
	    		StringType profile = new StringType("http://hl7.org/fhir/StructureDefinition/" + context.getSelectedResourceType());
	    		parameter.setValue(profile);
	    		input.addParameter(parameter);
	    		parameter = new ParametersParameterComponent();
	    		parameter.setName("resource");
	    		parameter.setResource(context.getResourceResults().get(0).getResource());
	    		input.addParameter(parameter);

				ByteArrayOutputStream oOp = new ByteArrayOutputStream();

				if (formatType.equals("XML")) {
					XmlParser xmlParser = new XmlParser();
					try {
						xmlParser.setOutputStyle(OutputStyle.PRETTY);
						xmlParser.compose(oOp, input, true);

						resourceString = oOp.toString();
					}
					catch (Exception e) {
						e.printStackTrace();
					}
				}
				else {
					JsonParser jsonParser = new JsonParser();
					try {
						jsonParser.setOutputStyle(OutputStyle.PRETTY);
						jsonParser.compose(oOp, input);

						resourceString = oOp.toString();
					}
					catch (Exception e) {
						e.printStackTrace();
					}
				}
			}

			context.setResourceString(resourceString);

			context.setResourceResults(new ArrayList<ResourceResponseWrapper>());

		}
		else {
			log.fine("No Resource found matching ID: " + context.getResourceId());
			String form = "tabView:interactionsTabView:" + operation + "Form";
			FacesContext.getCurrentInstance().addMessage(form, new FacesMessage(FacesMessage.SEVERITY_WARN, "No Resource found matching ID: " + context.getResourceId(), ""));
			context.setResourceString(null);
		}

		log.fine("resource id: " + context.getResourceId());
		log.fine("[END] ApplicationController.searchResourceForUpdate()");
	}

	/**
	 * Display resource specific search criteria based on the selected resource type
	 *
	 * @param event
	 */
	public void showSearchCriteria(ActionEvent event) {
		log.fine("[START] ApplicationController.showSearchCriteria()");

		List<LabelKeyValueBean> criteriaList = new ArrayList<LabelKeyValueBean>();

		criteriaList.addAll(ResourceType.getGlobalCriteria());

		String resourceType = context.getSelectedResourceType();
		criteriaList.addAll(ResourceType.getResourceTypeCriteria().get(resourceType));

		context.setResourceCriteria(criteriaList);

		context.setResourceResults(null);

		log.fine("[END] ApplicationController.showSearchCriteria()");
	}

	/**
	 * Update a resource
	 *
	 * @param event
	 */
	public void fhirUpdate(ActionEvent event) {
		log.fine("[START] ApplicationController.fhirUpdate()");
		log.fine("BasePath for FHIR update: " + context.getSelectedServerURL());

		context.setCurrentView("update");

		String formatType = context.getSelectedFormatType();
		String updateQuery = context.getUpdateQuery();
		String ifMatch = context.getIfMatch();
		String prefer = context.getPrefer();
		String _format = context.get_format();
		String resourceString = context.getResourceString();

		ByteArrayInputStream iResource = null;
		Resource resource = null;
		Response response = null;
		ResourceResponseWrapper wrapper = null;

		try {
			if (resourceString.isEmpty()) {
				throw new Exception("No found resource content to update.");
			}

			if (formatType.equals("XML")) {
				// Convert XML contents to Resource
				XmlParser xmlP = new XmlParser();
				int firstValid = resourceString.indexOf("<");
				if (firstValid > 0) {
					resourceString = resourceString.substring(firstValid);
				}
				iResource = new ByteArrayInputStream(resourceString.getBytes());
				resource = xmlP.parse(iResource);

				response = context.getResourceRESTClient().update(context.getResourceId(), resource, context.getSelectedServerURL(), context.getSelectedResourceType(), Constants.FHIR_XML_CONTENT, updateQuery, ifMatch, prefer, _format, null);
			}
			else {
				// Convert JSON contents to Resource
				JsonParser jsonP = new JsonParser();
				int firstValid = resourceString.indexOf("{");
				if (firstValid > 0) {
					resourceString = resourceString.substring(firstValid);
				}

				iResource = new ByteArrayInputStream(resourceString.getBytes());
				resource = jsonP.parse(iResource);

				response = context.getResourceRESTClient().update(context.getResourceId(), resource, context.getSelectedServerURL(), context.getSelectedResourceType(), Constants.FHIR_JSON_CONTENT, updateQuery, ifMatch, prefer, _format, null);
			}

		}
		catch (Exception e) {
			FacesContext.getCurrentInstance().addMessage("tabView:interactionsTabView:updateForm", new FacesMessage(FacesMessage.SEVERITY_ERROR, "Error updating resource! Please check the client logs.", "Error reading resource! Please check the client logs."));
			e.printStackTrace();
		}

		if (response != null) {
			String contentType = response.getHeaderString("Content-Type");
			if (contentType != null) {
				if (contentType.toUpperCase().contains("XML")) {
					context.setReturnedFormatType("XML");
				}
				else if (contentType.toUpperCase().contains("JSON")) {
					context.setReturnedFormatType("JSON");
				}
				else {
					context.setReturnedFormatType(formatType);
				}
			}
			else {
				context.setReturnedFormatType(formatType);
			}

			try {
				wrapper = new ResourceResponseWrapper(response);

				if (context.getReturnedFormatType().equals("XML")) {
					context.setResponseString(wrapper.getResourceXML());
				}
				else {
					context.setResponseString(wrapper.getResourceJSON());
				}
			}
			catch (Exception e) {
				FacesContext.getCurrentInstance().addMessage("tabView:interactionsTabView:updateForm",
						new FacesMessage(FacesMessage.SEVERITY_ERROR, "Resource parsing failed! Please check the client logs.", "Resource parsing failed! Please check the client logs."));
				e.printStackTrace();
			}

			if (response.getStatus() == Response.Status.OK.getStatusCode()) {
				FacesContext.getCurrentInstance().addMessage("tabView:interactionsTabView:updateForm",
						new FacesMessage(FacesMessage.SEVERITY_INFO, "Resource with ID: " + context.getResourceId() + " updated.", "Resource with ID: " + context.getResourceId() + " updated."));

			}
			else if (response.getStatus() == Response.Status.CREATED.getStatusCode()) {
				FacesContext.getCurrentInstance().addMessage("tabView:interactionsTabView:updateForm",
						new FacesMessage(FacesMessage.SEVERITY_INFO, "Resource with ID: " + context.getResourceId() + " created.", "Resource with ID: " + context.getResourceId() + " created."));

			}
			else {
				FacesContext.getCurrentInstance().addMessage("tabView:interactionsTabView:updateForm",
						new FacesMessage(FacesMessage.SEVERITY_INFO, "Resource with ID: " + context.getResourceId() + " update failed.", "Resource with ID: " + context.getResourceId() + " update failed."));
			}
		}

		context.setResourceString(null);
		context.setResourceId("");

		log.fine("[END] ApplicationController.fhirUpdate()");
	}

	/**
	 * Patch (partial update) a resource
	 *
	 * @param event
	 */
	public void fhirPatch(ActionEvent event) {
		log.fine("[START] ApplicationController.fhirPatch()");
		log.fine("BasePath for FHIR patch: " + context.getSelectedServerURL());

		String patchFormatType = context.getSelectedPatchFormatType();
		String ifMatch = context.getIfMatch();
		String prefer = context.getPrefer();
		String _format = context.get_format();
		String resourceString = context.getResourceString();
		Response response = null;
		ResourceResponseWrapper wrapper = null;

		try {
			if (resourceString.isEmpty()) {
				throw new Exception("No found resource content to update.");
			}

			if (patchFormatType.equals("FHIR Path (JSON)")) {
				response = context.getResourceRESTClient().patch(context.getResourceId(), resourceString, context.getSelectedServerURL(), context.getSelectedResourceType(), Constants.FHIR_PATCH_JSON_CONTENT, ifMatch, prefer, _format, null);
			}
			else if (patchFormatType.equals("FHIR Path (XML)")) {
				response = context.getResourceRESTClient().patch(context.getResourceId(), resourceString, context.getSelectedServerURL(), context.getSelectedResourceType(), Constants.FHIR_PATCH_XML_CONTENT, ifMatch, prefer, _format, null);
			}
			else if (patchFormatType.equals("JSON Patch")) {
				response = context.getResourceRESTClient().patch(context.getResourceId(), resourceString, context.getSelectedServerURL(), context.getSelectedResourceType(), Constants.JSON_PATCH_CONTENT, ifMatch, prefer, _format, null);
			}
			else {
				response = context.getResourceRESTClient().patch(context.getResourceId(), resourceString, context.getSelectedServerURL(), context.getSelectedResourceType(), Constants.XML_PATCH_CONTENT, ifMatch, prefer, _format, null);
			}
		}
		catch (Exception e) {
			FacesContext.getCurrentInstance().addMessage("tabView:interactionsTabView:updateForm", new FacesMessage(FacesMessage.SEVERITY_ERROR, "Error patching resource! Please check the client logs.", "Error patching resource! Please check the client logs."));
			e.printStackTrace();
		}

		if (response != null) {
			String contentType = response.getHeaderString("Content-Type");
			if (contentType != null) {
				if (contentType.toUpperCase().contains("XML")) {
					context.setReturnedFormatType("XML");
				}
				else if (contentType.toUpperCase().contains("JSON")) {
					context.setReturnedFormatType("JSON");
				}
				else {
					if (patchFormatType.toUpperCase().contains("XML")) {
						context.setReturnedFormatType("XML");
					}
					else if (patchFormatType.toUpperCase().contains("JSON")) {
						context.setReturnedFormatType("JSON");
					}
				}
			}
			else {
				if (patchFormatType.toUpperCase().contains("XML")) {
					context.setReturnedFormatType("XML");
				}
				else if (patchFormatType.toUpperCase().contains("JSON")) {
					context.setReturnedFormatType("JSON");
				}
			}

			try {
				wrapper = new ResourceResponseWrapper(response);

				if (context.getReturnedFormatType().equals("XML")) {
					context.setResponseString(wrapper.getResourceXML());
				}
				else {
					context.setResponseString(wrapper.getResourceJSON());
				}
			}
			catch (Exception e) {
				FacesContext.getCurrentInstance().addMessage("tabView:interactionsTabView:patchForm",
						new FacesMessage(FacesMessage.SEVERITY_ERROR, "Resource parsing failed! Please check the client logs.", "Resource parsing failed! Please check the client logs."));
				e.printStackTrace();
			}

			if (response.getStatus() == Response.Status.OK.getStatusCode()) {
				FacesContext.getCurrentInstance().addMessage("tabView:interactionsTabView:patchForm",
						new FacesMessage(FacesMessage.SEVERITY_INFO, "Resource with ID: " + context.getResourceId() + " patched.", "Resource with ID: " + context.getResourceId() + " patched."));

			}
			else {
				FacesContext.getCurrentInstance().addMessage("tabView:interactionsTabView:patchForm",
						new FacesMessage(FacesMessage.SEVERITY_INFO, "Resource with ID: " + context.getResourceId() + " patch failed.", "Resource with ID: " + context.getResourceId() + " patch failed."));
			}
		}

		context.setResourceString(null);
		context.setResourceId("");

		log.fine("[END] ApplicationController.fhirPatch()");
	}

	/**
	 * Deletes a resource who's ID is supplied from the view
	 *
	 * @param id
	 * @param form
	 */
	public void fhirDelete(String id, String form) {
		log.fine("[START] ApplicationController.delete()");

		String formId = "tabView:interactionsTabView:" + form;
		if (form == null || form == "" || form.length() < 1) {
			return;
		}

		try {
			log.fine("BasePath for FHIR delete: " + context.getSelectedServerURL());
			context.setCurrentView("delete");
			String formatType = context.getSelectedFormatType();

			Response response = context.getResourceRESTClient().delete(id, context.getSelectedServerURL(), context.getSelectedResourceType(), formatType, null);

			if (response != null) {
				if ((response.getStatus() == Response.Status.OK.getStatusCode()) || (response.getStatus() == Response.Status.GONE.getStatusCode()) || (response.getStatus() == Response.Status.NO_CONTENT.getStatusCode())) {
					FacesContext.getCurrentInstance().addMessage(formId, new FacesMessage(FacesMessage.SEVERITY_INFO, "Resource with ID: " + id + " deleted.", "Resource with ID: " + id + " deleted."));
					context.setResourceResults(new ArrayList<ResourceResponseWrapper>());
				}
				else {
					FacesContext.getCurrentInstance().addMessage(formId, new FacesMessage(FacesMessage.SEVERITY_INFO, "Response " + response.getStatus() + " - Resource with ID: " + id + " deletion failed.", "Response " + response.getStatus() + " - Resource with ID: " + id + " deletion failed."));
				}
			}
		}
		catch (NumberFormatException e) {
			log.severe(e.getMessage());
			FacesContext.getCurrentInstance().addMessage(formId, new FacesMessage(FacesMessage.SEVERITY_ERROR, "Number format error deleting resource! Please check the client logs.", "Error reading resource! Please check the client logs."));
			e.printStackTrace();
		}
		catch (Exception e) {
			log.severe(e.getMessage());
			FacesContext.getCurrentInstance().addMessage(formId, new FacesMessage(FacesMessage.SEVERITY_ERROR, "Error deleting resource! Please check the client logs.", "Error reading resource! Please check the client logs."));
			e.printStackTrace();
		}

		log.fine("[END] ApplicationController.delete()");
	}

	/**
	 * Execute the $validate operation
	 *
	 * @param event
	 */
	public void fhirValidate(ActionEvent event) {
		log.fine("[START] ApplicationController.fhirValidate()");
		log.fine("BasePath for FHIR validate: " + context.getSelectedServerURL());

		String formatType = context.getSelectedFormatType();
		String resourceString = context.getResourceString();

		ByteArrayInputStream iResource = null;
		Resource resource = null;
		Response response = null;
		ResourceResponseWrapper wrapper = null;
		String validateExceptionOutcomeString = null;

		try {
			if (resourceString.isEmpty()) {
				throw new Exception("No content provided for $validate operation.");
			}

			if (formatType.equals("XML")) {
				// Convert XML contents to Resource
				XmlParser xmlP = new XmlParser();
				int firstValid = resourceString.indexOf("<");
				if (firstValid > 0) {
					resourceString = resourceString.substring(firstValid);
				}
				iResource = new ByteArrayInputStream(resourceString.getBytes());
				resource = xmlP.parse(iResource);
			}
			else {
				// Convert JSON contents to Resource
				JsonParser jsonP = new JsonParser();
				int firstValid = resourceString.indexOf("{");
				if (firstValid > 0) {
					resourceString = resourceString.substring(firstValid);
				}
				iResource = new ByteArrayInputStream(resourceString.getBytes());
				resource = jsonP.parse(iResource);
			}

			if (resource instanceof Parameters) {
				Parameters parameters = (Parameters)resource;

				if (formatType.equals("XML")) {
					response = context.getResourceOperationClient().resourceOperation(parameters, null, context.getSelectedServerURL(), context.getSelectedResourceType(), Constants.FHIR_XML_CONTENT, Constants.FHIR_XML_CONTENT, null, "validate", null, null);
				}
				else {
					response = context.getResourceOperationClient().resourceOperation(parameters, null, context.getSelectedServerURL(), context.getSelectedResourceType(), Constants.FHIR_JSON_CONTENT, Constants.FHIR_JSON_CONTENT, null, "validate", null, null);
				}
			}
			else {
				validateExceptionOutcomeString = ServicesUtil.INSTANCE.getOperationOutcome(OperationOutcome.IssueSeverity.FATAL, OperationOutcome.IssueType.EXCEPTION, "Validate operation requires input contained in a Parameters resource type.", null, null, formatType.toLowerCase());
				FacesContext.getCurrentInstance().addMessage("tabView:operationsTabView:validateForm", new FacesMessage(FacesMessage.SEVERITY_ERROR, "Validate operation requires input contained in a Parameters resource type.", "Validate operation requires input contained in a Parameters resource type."));
			}
		}
		catch (NumberFormatException e) {
			validateExceptionOutcomeString = ServicesUtil.INSTANCE.getOperationOutcome(OperationOutcome.IssueSeverity.FATAL, OperationOutcome.IssueType.EXCEPTION, e.getMessage(), null, null, formatType.toLowerCase());
			FacesContext.getCurrentInstance().addMessage("tabView:operationsTabView:validateForm",
					new FacesMessage(FacesMessage.SEVERITY_ERROR, "Number format error validating resource request! Please check the client logs.", "Error reading resource! Please check the client logs."));
			e.printStackTrace();
		}
		catch (Exception e) {
			validateExceptionOutcomeString = ServicesUtil.INSTANCE.getOperationOutcome(OperationOutcome.IssueSeverity.FATAL, OperationOutcome.IssueType.EXCEPTION, e.getMessage(), null, null, formatType.toLowerCase());
			FacesContext.getCurrentInstance().addMessage("tabView:operationsTabView:validateForm",
					new FacesMessage(FacesMessage.SEVERITY_ERROR, "Error validating resource request! Please check the client logs.", "Error reading resource! Please check the client logs."));
			e.printStackTrace();
		}

		if (response != null) {
			String contentType = response.getHeaderString("Content-Type");
			if (contentType != null) {
				if (contentType.toUpperCase().contains("XML")) {
					context.setReturnedFormatType("XML");
				}
				else if (contentType.toUpperCase().contains("JSON")) {
					context.setReturnedFormatType("JSON");
				}
				else {
					context.setReturnedFormatType(formatType);
				}
			}
			else {
				context.setReturnedFormatType(formatType);
			}

			try {
				wrapper = new ResourceResponseWrapper(response);

				Resource responseResource = wrapper.getResource();
				if (responseResource instanceof OperationOutcome) {
					context.setValidateOperationOutcome((OperationOutcome) responseResource);
				}

				if (formatType.equals("XML")) {
					context.setResponseString(wrapper.getResourceXML());
				}
				else {
					context.setResponseString(wrapper.getResourceJSON());
				}

				FacesContext.getCurrentInstance().addMessage("tabView:operationsTabView:validateForm", new FacesMessage(FacesMessage.SEVERITY_INFO, "Resource validate complete.", "Resource validate complete."));
			}
			catch (Exception e) {
				FacesContext.getCurrentInstance().addMessage("tabView:operationsTabView:validateForm",
						new FacesMessage(FacesMessage.SEVERITY_ERROR, "Resource parsing failed! Please check the client logs.", "Resource parsing failed! Please check the client logs."));
				e.printStackTrace();
			}
		}
		else if (validateExceptionOutcomeString != null) {
			context.setReturnedFormatType(formatType);
			context.setResponseString(validateExceptionOutcomeString);

			// Exception caught parsing input parameters; generate an OperationOutcome for the display
			try {
				if (formatType.equals("XML")) {
					// Convert XML contents to Resource
					XmlParser xmlP = new XmlParser();
					int firstValid = validateExceptionOutcomeString.indexOf("<");
					if (firstValid > 0) {
						validateExceptionOutcomeString = validateExceptionOutcomeString.substring(firstValid);
					}
					iResource = new ByteArrayInputStream(validateExceptionOutcomeString.getBytes());
					resource = xmlP.parse(iResource);
				}
				else {
					// Convert JSON contents to Resource
					JsonParser jsonP = new JsonParser();
					int firstValid = validateExceptionOutcomeString.indexOf("{");
					if (firstValid > 0) {
						validateExceptionOutcomeString = validateExceptionOutcomeString.substring(firstValid);
					}
					iResource = new ByteArrayInputStream(validateExceptionOutcomeString.getBytes());
					resource = jsonP.parse(iResource);
				}

				if (resource instanceof OperationOutcome) {
					context.setValidateOperationOutcome((OperationOutcome) resource);
				}

				FacesContext.getCurrentInstance().addMessage("tabView:operationsTabView:validateForm", new FacesMessage(FacesMessage.SEVERITY_INFO, "Resource validate operation errors found.", "Resource validate operation errors found."));
			}
			catch (Exception e) {
				FacesContext.getCurrentInstance().addMessage("tabView:operationsTabView:validateForm",
						new FacesMessage(FacesMessage.SEVERITY_ERROR, "Error reporting validate operations errors! Please check the client logs.", "Error reporting validate operations errors! Please check the client logs."));
				e.printStackTrace();
			}
		}
		else {
			FacesContext.getCurrentInstance().addMessage("tabView:operationsTabView:validateForm", new FacesMessage(FacesMessage.SEVERITY_ERROR, "Validate operation did not report any results.", "Validate operation did not report any results."));
		}

		log.fine("[END] ApplicationController.fhirValidate()");
	}

	/**
	 * Perform a FHIR metadata against the specified server
	 *
	 * @return
	 */
	public void fhirMetadata(ActionEvent event) {
		log.fine("[START] ApplicationController.fhirMetadata()");
		log.fine("BasePath for FHIR metadata " + context.getSelectedServerURL());

		String formatType = context.getSelectedFormatType();

		Response response = null;
		context.setResourceResults(new ArrayList<ResourceResponseWrapper>());

		try {
			if (formatType.equals("XML")) {
				response = context.getConformanceResourceRESTClient().metadata(context.getSelectedServerURL(), Constants.FHIR_XML_CONTENT);
			}
			else {
				response = context.getConformanceResourceRESTClient().metadata(context.getSelectedServerURL(), Constants.FHIR_JSON_CONTENT);
			}
		}
		catch (Exception e) {
			log.severe(e.getMessage());
			FacesContext.getCurrentInstance().addMessage("tabView:interactionsTabView:fhirMetadataForm",
					new FacesMessage(FacesMessage.SEVERITY_ERROR, "Error executing metadata request! " + e.getMessage(), ""));
			e.printStackTrace();
		}

		if (response != null) {
			String contentType = response.getHeaderString("Content-Type");
			if (contentType != null) {
				if (contentType.toUpperCase().contains("XML")) {
					context.setReturnedFormatType("XML");
				}
				else if (contentType.toUpperCase().contains("JSON")) {
					context.setReturnedFormatType("JSON");
				}
				else {
					context.setReturnedFormatType(formatType);
				}
			}
			else {
				context.setReturnedFormatType(formatType);
			}

			if (response.getStatus() == Response.Status.OK.getStatusCode()) {
				try {
					context.getResourceResults().add(new ResourceResponseWrapper(response));
				}
				catch (Exception e) {
					FacesContext.getCurrentInstance().addMessage("tabView:interactionsTabView:fhirMetadataForm",
							new FacesMessage(FacesMessage.SEVERITY_ERROR, "CapabilityStatement parsing failed! " + e.getMessage(), ""));
					e.printStackTrace();
				}

			}
			else {
				FacesContext.getCurrentInstance().addMessage("tabView:interactionsTabView:fhirMetadataForm",
						new FacesMessage(FacesMessage.SEVERITY_INFO, "CapabilityStatement retrieval failed; response [" + response.getStatus() + "].", ""));
			}
		}

		log.fine("[END] ApplicationController.fhirMetadata()");
	}

	/**
	 * Execute the FHIR metadata operation and display the response in a new HTML page
	 * @param event
	 */
	public void fhirMetadataNewPage(ActionEvent event) {
		log.fine("[START] ApplicationController.fhirMetadataNewPage");
		log.fine("BasePath for FHIR metadata (new page) " + context.getSelectedServerURL());

		String formatType = context.getSelectedFormatType();

		String conformanceUrl = context.getSelectedServerURL() + "/metadata";

		try {
			FacesContext facesContext = FacesContext.getCurrentInstance();
			ExternalContext externalContext = facesContext.getExternalContext();
			HttpServletResponse response = (HttpServletResponse) externalContext.getResponse();

			if (formatType.equals("XML")) {
				response.setHeader("Accept", Constants.FHIR_XML_CONTENT);
			}
			else {
				response.setHeader("Accept", Constants.FHIR_JSON_CONTENT);
			}

			externalContext.redirect(conformanceUrl);
			facesContext.responseComplete();
		}
		catch (IOException e) {
			log.severe(e.getMessage());
			FacesContext.getCurrentInstance().addMessage(null, new FacesMessage(FacesMessage.SEVERITY_ERROR, "Error accessing server's metadata endpoint! " + e.getMessage(), ""));
		}

		log.fine("[END] ApplicationController.fhirMetadataNewPage");
	}

	/**
	 * Execute the $convert-format operation
	 *
	 * @param event
	 */
	public void fhirConvertFormat(ActionEvent event) {
		log.fine("[START] ApplicationController.fhirConvertFormat()");
		log.fine("BasePath for FHIR convert: " + context.getSelectedServerURL());

		String convertFromFormatType = context.getConvertFromFormatType();
		String convertToFormatType = context.getConvertToFormatType();
		String resourceString = context.getResourceString();
		String acceptFormatType = null;
		String contentTypeFormatType = null;

		ByteArrayInputStream iResource = null;
		Resource resource = null;
		Response response = null;
		ResourceResponseWrapper wrapper = null;
		String convertFormatExceptionOutcomeString = null;

		try {
			if (resourceString.isEmpty() || resourceString == null) {
				throw new Exception("No content provided for $convert operation.");
			}

			if (convertFromFormatType.equals("XML")) {
				contentTypeFormatType = Constants.FHIR_XML_CONTENT;
				// Convert XML contents to Resource
				XmlParser xmlP = new XmlParser();
				int firstValid = resourceString.indexOf("<");
				if (firstValid > 0) {
					resourceString = resourceString.substring(firstValid);
				}
				iResource = new ByteArrayInputStream(resourceString.getBytes());
				resource = xmlP.parse(iResource);
			}
			else {
				contentTypeFormatType = Constants.FHIR_JSON_CONTENT;
				// Convert JSON contents to Resource
				JsonParser jsonP = new JsonParser();
				int firstValid = resourceString.indexOf("{");
				if (firstValid > 0) {
					resourceString = resourceString.substring(firstValid);
				}
				iResource = new ByteArrayInputStream(resourceString.getBytes());
				resource = jsonP.parse(iResource);
			}

			if (convertToFormatType.equals("XML")) {
				acceptFormatType = Constants.FHIR_XML_CONTENT;
			}
			else {
				acceptFormatType = Constants.FHIR_JSON_CONTENT;
			}

			response = context.getResourceOperationClient().resourceOperation(null, resourceString, context.getSelectedServerURL(), null, acceptFormatType, contentTypeFormatType, null, "convert", null, null);
		}
		catch (Exception e) {
			convertFormatExceptionOutcomeString = ServicesUtil.INSTANCE.getOperationOutcome(OperationOutcome.IssueSeverity.FATAL, OperationOutcome.IssueType.EXCEPTION, e.getMessage(), null, null, convertToFormatType.toLowerCase());
			FacesContext.getCurrentInstance().addMessage("tabView:operationsTabView:convertFormatForm",
					new FacesMessage(FacesMessage.SEVERITY_ERROR, "Error parsing convert-format resource request body! Please check the client logs.", "Error parsing convert-format resource request body! Please check the client logs."));
			e.printStackTrace();
		}

		if (response != null) {
			context.setConvertFormatOperationOutcome(null);

			String contentType = response.getHeaderString("Content-Type");
			if (contentType != null) {
				if (contentType.toUpperCase().contains("XML")) {
					context.setReturnedFormatType("XML");
				}
				else if (contentType.toUpperCase().contains("JSON")) {
					context.setReturnedFormatType("JSON");
				}
				else {
					context.setReturnedFormatType(convertToFormatType);
				}
			}
			else {
				context.setReturnedFormatType(convertToFormatType);
			}

			try {
				wrapper = new ResourceResponseWrapper(response);

				Resource responseResource = wrapper.getResource();
				if (responseResource instanceof OperationOutcome) {
					context.setConvertFormatOperationOutcome((OperationOutcome) responseResource);
				}

				if (convertToFormatType.equals("XML")) {
					context.setResponseString(wrapper.getResourceXML());
				}
				else {
					context.setResponseString(wrapper.getResourceJSON());
				}

				FacesContext.getCurrentInstance().addMessage("tabView:operationsTabView:convertFormatForm", new FacesMessage(FacesMessage.SEVERITY_INFO, "Convert format complete.", "Convert format complete."));
			}
			catch (Exception e) {
				FacesContext.getCurrentInstance().addMessage("tabView:operationsTabView:convertFormatForm",
						new FacesMessage(FacesMessage.SEVERITY_ERROR, "Resource parsing failed! Please check the client logs.", "Resource parsing failed! Please check the client logs."));
				e.printStackTrace();
			}
		}
		else if (convertFormatExceptionOutcomeString != null) {
			context.setResponseString(null);
			context.setReturnedFormatType(convertToFormatType);
			context.setResponseString(convertFormatExceptionOutcomeString);

			// Exception caught parsing input parameters; generate an OperationOutcome for the display
			try {
				if (convertToFormatType.equals("XML")) {
					// Convert XML contents to Resource
					XmlParser xmlP = new XmlParser();
					int firstValid = convertFormatExceptionOutcomeString.indexOf("<");
					if (firstValid > 0) {
						convertFormatExceptionOutcomeString = convertFormatExceptionOutcomeString.substring(firstValid);
					}
					iResource = new ByteArrayInputStream(convertFormatExceptionOutcomeString.getBytes());
					resource = xmlP.parse(iResource);
				}
				else {
					// Convert JSON contents to Resource
					JsonParser jsonP = new JsonParser();
					int firstValid = convertFormatExceptionOutcomeString.indexOf("{");
					if (firstValid > 0) {
						convertFormatExceptionOutcomeString = convertFormatExceptionOutcomeString.substring(firstValid);
					}
					iResource = new ByteArrayInputStream(convertFormatExceptionOutcomeString.getBytes());
					resource = jsonP.parse(iResource);
				}

				if (resource instanceof OperationOutcome) {
					context.setConvertFormatOperationOutcome((OperationOutcome) resource);
					context.setResponseString(convertFormatExceptionOutcomeString);
				}

				FacesContext.getCurrentInstance().addMessage("tabView:operationsTabView:convertFormatForm", new FacesMessage(FacesMessage.SEVERITY_INFO, "Convert format operation errors found.", "Convert format operation errors found."));
			}
			catch (Exception e) {
				FacesContext.getCurrentInstance().addMessage("tabView:operationsTabView:convertFormatForm",
						new FacesMessage(FacesMessage.SEVERITY_ERROR, "Error reporting convert format operations errors! Please check the client logs.", "Error reporting convert format operations errors! Please check the client logs."));
				e.printStackTrace();
			}
		}
		else {
			FacesContext.getCurrentInstance().addMessage("tabView:operationsTabView:convertFormatForm", new FacesMessage(FacesMessage.SEVERITY_ERROR, "Convert format operation did not report any results.", "Convert format operation did not report any results."));
		}

		log.fine("[END] ApplicationController.fhirConvertFormat()");
	}

	/**
	 * Generate the $everything operation Parameters payload template
	 *
	 * @param event
	 */
	public void everythingShowTemplate(ActionEvent event) {
		log.info("[START] ApplicationController.everythingShowTemplate()");

		String formatType = context.getSelectedFormatType();

		StringBuilder sbTemplate = new StringBuilder("");

		if (formatType.equals("XML")) {
			sbTemplate.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n\n");
			sbTemplate.append("<Parameters xmlns=\"http://hl7.org/fhir\">\n");
			sbTemplate.append("  <parameter>\n");
			sbTemplate.append("    <name value=\"start\"/>\n");
			sbTemplate.append("    <valueDate value=\"YYYY-MM-DD\"/>\n");
			sbTemplate.append("  </parameter>\n");
			sbTemplate.append("  <parameter>\n");
			sbTemplate.append("    <name value=\"end\"/>\n");
			sbTemplate.append("    <valueDate value=\"YYYY-MM-DD\"/>\n");
			sbTemplate.append("  </parameter>\n");
			sbTemplate.append("</Parameters>");
		}
		else {
			sbTemplate.append("{\n");
			sbTemplate.append("  \"resourceType\": \"Parameters\",\n");
			sbTemplate.append("  \"parameter\": [\n");
			sbTemplate.append("    {\n");
			sbTemplate.append("      \"name\": \"start\",\n");
			sbTemplate.append("      \"valueDate\": \"YYYY-MM-DD\"\n");
			sbTemplate.append("    },\n");
			sbTemplate.append("    {\n");
			sbTemplate.append("      \"name\": \"end\",\n");
			sbTemplate.append("      \"valueDate\": \"YYYY-MM-DD\"\n");
			sbTemplate.append("    }\n");
			sbTemplate.append("  ]\n");
			sbTemplate.append("}");
		}

		context.setResourceString(sbTemplate.toString());
	}

	/**
	 * Execute the $everything operation
	 *
	 * @param event
	 */
	public void fhirEverything(ActionEvent event) {
		log.fine("[START] ApplicationController.fhirEverything()");
		log.fine("BasePath for FHIR everything: " + context.getSelectedServerURL());

		String formatType = context.getSelectedFormatType();
		String resourceId = context.getResourceId();
		String resourceString = context.getResourceString();

		ByteArrayInputStream iResource = null;
		Resource resource = null;
		Response response = null;
		ResourceResponseWrapper wrapper = null;
		String everythingExceptionOutcomeString = null;

		try {
			if (!resourceString.isEmpty()) {

				if (formatType.equals("XML")) {
					// Convert XML contents to Resource
					XmlParser xmlP = new XmlParser();
					int firstValid = resourceString.indexOf("<");
					if (firstValid > 0) {
						resourceString = resourceString.substring(firstValid);
					}
					iResource = new ByteArrayInputStream(resourceString.getBytes());
					resource = xmlP.parse(iResource);
				}
				else {
					// Convert JSON contents to Resource
					JsonParser jsonP = new JsonParser();
					int firstValid = resourceString.indexOf("{");
					if (firstValid > 0) {
						resourceString = resourceString.substring(firstValid);
					}
					iResource = new ByteArrayInputStream(resourceString.getBytes());
					resource = jsonP.parse(iResource);
				}
			}

			if (resource != null) {

				if (resource instanceof Parameters) {

					Parameters parameters = (Parameters) resource;

					if (formatType.equals("XML")) {
						response = context.getResourceOperationClient().resourceOperation(parameters, null, context.getSelectedServerURL(), context.getSelectedResourceType(), Constants.FHIR_XML_CONTENT, Constants.FHIR_XML_CONTENT, resourceId, "everything", null, null);
					}
					else {
						response = context.getResourceOperationClient().resourceOperation(parameters, null, context.getSelectedServerURL(), context.getSelectedResourceType(), Constants.FHIR_JSON_CONTENT, Constants.FHIR_JSON_CONTENT, resourceId, "everything", null, null);
					}
				}
				else {
					everythingExceptionOutcomeString = ServicesUtil.INSTANCE.getOperationOutcome(OperationOutcome.IssueSeverity.FATAL, OperationOutcome.IssueType.EXCEPTION,
							"Everything operation requires input contained in a Parameters resource type if a request payload is provided.", null, null, formatType.toLowerCase());
					FacesContext.getCurrentInstance().addMessage(
							"tabView:operationsTabView:validateForm",
							new FacesMessage(FacesMessage.SEVERITY_ERROR, "Everything operation requires input contained in a Parameters resource type if a request payload is provided.",
									"Everything operation requires input contained in a Parameters resource type if a request payload is provided."));
				}
			}
			else {

				if (formatType.equals("XML")) {
					response = context.getResourceOperationClient().resourceOperation(null, null, context.getSelectedServerURL(), context.getSelectedResourceType(), Constants.FHIR_XML_CONTENT, Constants.FHIR_XML_CONTENT, resourceId, "everything", null, null);
				}
				else {
					response = context.getResourceOperationClient().resourceOperation(null, null, context.getSelectedServerURL(), context.getSelectedResourceType(), Constants.FHIR_JSON_CONTENT, Constants.FHIR_JSON_CONTENT, resourceId, "everything", null, null);
				}
			}
		}
		catch (NumberFormatException e) {
			everythingExceptionOutcomeString = ServicesUtil.INSTANCE.getOperationOutcome(OperationOutcome.IssueSeverity.FATAL, OperationOutcome.IssueType.EXCEPTION, e.getMessage(), null, null, formatType.toLowerCase());
			FacesContext.getCurrentInstance().addMessage("tabView:operationsTabView:everythingForm",
					new FacesMessage(FacesMessage.SEVERITY_ERROR, "Number format error validating resource request! Please check the client logs.", "Error reading resource! Please check the client logs."));
			e.printStackTrace();
		}
		catch (Exception e) {
			everythingExceptionOutcomeString = ServicesUtil.INSTANCE.getOperationOutcome(OperationOutcome.IssueSeverity.FATAL, OperationOutcome.IssueType.EXCEPTION, e.getMessage(), null, null, formatType.toLowerCase());
			FacesContext.getCurrentInstance().addMessage("tabView:operationsTabView:everythingForm",
					new FacesMessage(FacesMessage.SEVERITY_ERROR, "Error validating resource request! Please check the client logs.", "Error reading resource! Please check the client logs."));
			e.printStackTrace();
		}

		if (response != null) {
			String contentType = response.getHeaderString("Content-Type");
			if (contentType != null) {
				if (contentType.toUpperCase().contains("XML")) {
					context.setReturnedFormatType("XML");
				}
				else if (contentType.toUpperCase().contains("JSON")) {
					context.setReturnedFormatType("JSON");
				}
				else {
					context.setReturnedFormatType(formatType);
				}
			}
			else {
				context.setReturnedFormatType(formatType);
			}

			try {
				wrapper = new ResourceResponseWrapper(response);

				Resource responseResource = wrapper.getResource();
				if (responseResource instanceof OperationOutcome) {
					context.setValidateOperationOutcome((OperationOutcome) responseResource);
				}

				if (formatType.equals("XML")) {
					context.setResponseString(wrapper.getResourceXML());
				}
				else {
					context.setResponseString(wrapper.getResourceJSON());
				}

				FacesContext.getCurrentInstance().addMessage("tabView:operationsTabView:everythingForm", new FacesMessage(FacesMessage.SEVERITY_INFO, context.getSelectedResourceType() + " everything complete.", context.getSelectedResourceType() + " everything complete."));
			}
			catch (Exception e) {
				FacesContext.getCurrentInstance().addMessage("tabView:operationsTabView:everythingForm",
						new FacesMessage(FacesMessage.SEVERITY_ERROR, "Resource parsing failed! Please check the client logs.", "Resource parsing failed! Please check the client logs."));
				e.printStackTrace();
			}
		}
		else if (everythingExceptionOutcomeString != null) {
			context.setReturnedFormatType(formatType);
			context.setResponseString(everythingExceptionOutcomeString);

			// Exception caught parsing input parameters; generate an OperationOutcome for the display
			try {
				if (formatType.equals("XML")) {
					// Convert XML contents to Resource
					XmlParser xmlP = new XmlParser();
					int firstValid = everythingExceptionOutcomeString.indexOf("<");
					if (firstValid > 0) {
						everythingExceptionOutcomeString = everythingExceptionOutcomeString.substring(firstValid);
					}
					iResource = new ByteArrayInputStream(everythingExceptionOutcomeString.getBytes());
					resource = xmlP.parse(iResource);
				}
				else {
					// Convert JSON contents to Resource
					JsonParser jsonP = new JsonParser();
					int firstValid = everythingExceptionOutcomeString.indexOf("{");
					if (firstValid > 0) {
						everythingExceptionOutcomeString = everythingExceptionOutcomeString.substring(firstValid);
					}
					iResource = new ByteArrayInputStream(everythingExceptionOutcomeString.getBytes());
					resource = jsonP.parse(iResource);
				}

				if (resource instanceof OperationOutcome) {
					context.setValidateOperationOutcome((OperationOutcome) resource);
				}

				FacesContext.getCurrentInstance().addMessage("tabView:operationsTabView:everythingForm", new FacesMessage(FacesMessage.SEVERITY_INFO, "Resource everything operation errors found.", "Resource everything operation errors found."));
			}
			catch (Exception e) {
				FacesContext.getCurrentInstance().addMessage("tabView:operationsTabView:everythingForm",
						new FacesMessage(FacesMessage.SEVERITY_ERROR, "Error reporting everything operation errors! Please check the client logs.", "Error reporting everything operation errors! Please check the client logs."));
				e.printStackTrace();
			}
		}
		else {
			FacesContext.getCurrentInstance().addMessage("tabView:operationsTabView:everythingForm", new FacesMessage(FacesMessage.SEVERITY_ERROR, "Everything operation did not report any results.", "Validate operation did not report any results."));
		}

		log.fine("[END] ApplicationController.fhirEverything()");
	}

	/**
	 * Execute the FHIR Path Evaluate
	 *
	 * @param event
	 */
	public void fhirpathEvaluate(ActionEvent event) {
		log.fine("[START] ApplicationController.fhirpathEvaluate()");
		log.fine("BasePath for FHIR fhirpath evaluate: " + context.getSelectedServerURL());

		String formatType = context.getSelectedFormatType();
		String methodString = context.getMethodString();
		String expressionString = context.getExpressionString();
		String resourceString = context.getResourceString();

		ByteArrayInputStream iResource = null;
		ByteArrayOutputStream oResponse = new ByteArrayOutputStream();
		Resource resource = null;
		Response response = null;
		ResourceResponseWrapper wrapper = null;
		String fhirpathEvaluateExceptionOutcomeString = null;

		XmlParser xmlP = new XmlParser();
		JsonParser jsonP = new JsonParser();

		try {
			if (expressionString.isEmpty() || resourceString.isEmpty()) {
				StringBuffer exceptionMessage = new StringBuffer("");

				if (expressionString.isEmpty()) {
					exceptionMessage.append("fhirpath expression is undefined or empty. ");
				}
				if (resourceString.isEmpty()) {
					exceptionMessage.append("FHIR Resource contents are undefined or empty.");
				}

				throw new Exception(exceptionMessage.toString());
			}

			if (formatType.equals("XML")) {
				// Convert XML contents to Resource
				int firstValid = resourceString.indexOf("<");
				if (firstValid > 0) {
					resourceString = resourceString.substring(firstValid);
				}
				iResource = new ByteArrayInputStream(resourceString.getBytes());
				resource = xmlP.parse(iResource);
			}
			else {
				// Convert JSON contents to Resource
				int firstValid = resourceString.indexOf("{");
				if (firstValid > 0) {
					resourceString = resourceString.substring(firstValid);
				}
				iResource = new ByteArrayInputStream(resourceString.getBytes());
				resource = jsonP.parse(iResource);
			}

			// Build input parameters
			Parameters inputParameters = new Parameters();

			ParametersParameterComponent inputParameter = new ParametersParameterComponent();
			inputParameter.setName("method");
			inputParameter.setValue(new StringType(methodString));
			inputParameters.getParameter().add(inputParameter);

			inputParameter = new ParametersParameterComponent();
			inputParameter.setName("resource");
			inputParameter.setResource(resource);
			inputParameters.getParameter().add(inputParameter);

			inputParameter = new ParametersParameterComponent();
			inputParameter.setName("expression");
			inputParameter.setValue(new StringType(expressionString));
			inputParameters.getParameter().add(inputParameter);

			if (formatType.equals("XML")) {
				response = context.getFhirpathEvaluatorRESTClient().evaluate(inputParameters, context.getSelectedServerURL(), Constants.FHIR_XML_CONTENT, null);
			}
			else {
				response = context.getFhirpathEvaluatorRESTClient().evaluate(inputParameters, context.getSelectedServerURL(), Constants.FHIR_JSON_CONTENT, null);
			}
		}
		catch (NumberFormatException e) {
			fhirpathEvaluateExceptionOutcomeString = ServicesUtil.INSTANCE.getOperationOutcome(OperationOutcome.IssueSeverity.FATAL, OperationOutcome.IssueType.EXCEPTION, e.getMessage(), null, null, formatType.toLowerCase());
			FacesContext.getCurrentInstance().addMessage("tabView:toolsTabView:fhirpathEvaluateForm",
					new FacesMessage(FacesMessage.SEVERITY_ERROR, "Number format error with $fhirpath-evaluate request! Please check the client logs.", "Number format error with $fhirpath-evaluate request! Please check the client logs."));
			e.printStackTrace();
		}
		catch (Exception e) {
			fhirpathEvaluateExceptionOutcomeString = ServicesUtil.INSTANCE.getOperationOutcome(OperationOutcome.IssueSeverity.FATAL, OperationOutcome.IssueType.EXCEPTION, e.getMessage(), null, null, formatType.toLowerCase());
			FacesContext.getCurrentInstance().addMessage("tabView:toolsTabView:fhirpathEvaluateForm",
					new FacesMessage(FacesMessage.SEVERITY_ERROR, "Error with $fhirpath-evaluate request! Please check the client logs.", "Error with $fhirpath-evaluate request! Please check the client logs."));
			e.printStackTrace();
		}

		if (response != null) {
			String contentType = response.getHeaderString("Content-Type");
			if (contentType != null) {
				if (contentType.toUpperCase().contains("XML")) {
					context.setReturnedFormatType("XML");
				}
				else if (contentType.toUpperCase().contains("JSON")) {
					context.setReturnedFormatType("JSON");
				}
				else {
					context.setReturnedFormatType(formatType);
				}
			}
			else {
				context.setReturnedFormatType(formatType);
			}

			try {
				wrapper = new ResourceResponseWrapper(response);

				oResponse = new ByteArrayOutputStream();
				if (formatType.equals("XML")) {
					xmlP.setOutputStyle(OutputStyle.PRETTY);
					xmlP.compose(oResponse, wrapper.getResource(), true);

					context.setResponseString(oResponse.toString());
				}
				else {
					jsonP.setOutputStyle(OutputStyle.PRETTY);
					jsonP.compose(oResponse, wrapper.getResource());

					context.setResponseString(oResponse.toString());
				}

				FacesContext.getCurrentInstance().addMessage("tabView:toolsTabView:fhirpathEvaluateForm", new FacesMessage(FacesMessage.SEVERITY_INFO, "$fhirpath-evaluate complete.", "$fhirpath-evaluate complete."));
			}
			catch (Exception e) {
				FacesContext.getCurrentInstance().addMessage("tabView:toolsTabView:fhirpathEvaluateForm",
						new FacesMessage(FacesMessage.SEVERITY_ERROR, "$fhirpath-evaluate failed! Please check the client logs.", "$fhirpath-evaluate failed! Please check the client logs."));
				e.printStackTrace();
			}
		}
		else if (fhirpathEvaluateExceptionOutcomeString != null) {
			context.setReturnedFormatType(formatType);
			context.setResponseString(fhirpathEvaluateExceptionOutcomeString);

			FacesContext.getCurrentInstance().addMessage("tabView:toolsTabView:fhirpathEvaluateForm", new FacesMessage(FacesMessage.SEVERITY_INFO, "$fhirpath-evaluate errors found.", "$fhirpath-evaluate errors found."));
		}
		else {
			FacesContext.getCurrentInstance().addMessage("tabView:toolsTabView:fhirpathEvaluateForm", new FacesMessage(FacesMessage.SEVERITY_ERROR, "$fhirpath-evaluate did not report any results.", "$fhirpath-evaluate did not report any results."));
		}

		log.fine("[END] ApplicationController.fhirpathEvaluate()");
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
			String provisionType = context.getSelectedProvisionType();
			Date consentDate = new Date();
			Date startDate = context.getStartDate();
			Date endDate = context.getEndDate();
			StringBuilder sb = new StringBuilder();

			sb.append("Patient id: ").append(clientPatientId);
			sb.append("; RelatedPerson id: ").append(clientRelatedPersonId);
			sb.append("; Provision: ").append(provisionType);
			sb.append("; Server URL: ").append(context.getSelectedServerURL());

			context.setResourceString(sb.toString());
			context.setResponseString("TBD");

			// Get selected client resources
			Patient grantor = (Patient) context.getClientresourceService().readFHIRResource(Integer.valueOf(clientPatientId));
			RelatedPerson recipient = (RelatedPerson) context.getClientresourceService().readFHIRResource(Integer.valueOf(clientRelatedPersonId));
			Serverdirectory server = context.getServerDirectoryService().findServerdirectoryByBasePath(context.getSelectedServerURL());

			// Build $fileConsent Parameters with Consent and DocumentReference
			Parameters p = new Parameters();
			p.setId(UUIDUtil.getUUID());
			Meta meta = new Meta();
			meta.addProfile("http://hl7.org/fhir/us/consent-management/StructureDefinition/FileConsentParameters");
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
			extension.setUrl("http://hl7.org/fhir/5.0/StructureDefinition/extension-Consent.manager");
			Reference reference = new Reference();
			// Build manager Organization reference from selected Serverdirectory
			String serverId = server.getName().toLowerCase().replaceAll(" ", "-");
			reference.setReference("Organization/" + serverId);
			Identifier identifer = new Identifier();
			identifer.setSystem("http://example.org/identifiers");
			identifer.setValue(serverId);
			reference.setIdentifier(identifer);
			extension.setValue(reference);
			consent.addExtension(extension);

			identifer = new Identifier();
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

			consent.setDateTime(consentDate);

			// Set sourceReference to DocumentReference below

			ConsentPolicyComponent policy = new ConsentPolicyComponent();
			policy.setUri("hipaa-auth");
			consent.addPolicy(policy);

			ProvisionComponent provision = new ProvisionComponent();
			provision.setType(ConsentProvisionType.fromCode(provisionType));

			if (startDate != null || endDate != null) {
				Period period = new Period();
				if (startDate != null) {
					period.setStart(startDate);
				}
				if (endDate != null) {
					period.setEnd(endDate);
				}
				if (startDate != null && endDate != null) {
					if (endDate.compareTo(startDate) <= 0) {
						throw new WildfhirClientException("Provision period error! End date must be greater than start date.");
					}
				}
				provision.setPeriod(period);
			}

			provisionActorComponent actor = new provisionActorComponent();
			codeableConcept = new CodeableConcept();
			coding = new Coding();
			coding.setSystem("http://terminology.hl7.org/CodeSystem/v3-ParticipationType");
			coding.setCode("IRCP");
			codeableConcept.addCoding(coding);
			actor.setRole(codeableConcept);
			Reference referenceRecipient = new Reference();
			referenceRecipient.setReference("RelatedPerson/" + recipient.getId());
			if (recipient.hasIdentifier()) {
				referenceRecipient.setIdentifier(recipient.getIdentifierFirstRep());
			}
			actor.setReference(referenceRecipient);
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
			sb.append(consent.getProvision().getType().toCode());
			sb.append(" ");
			sb.append(recipient.getNameFirstRep().getNameAsSingleString());
			sb.append(" access to my medical records.");
			attachment.setData(sb.toString().getBytes("UTF-8"));
			content.setAttachment(attachment);
			docRef.addContent(content);

			param.setResource(docRef);

			p.addParameter(param);

			// Send $fileConsent request
			JsonParser jsonParser = new JsonParser();
			jsonParser.setOutputStyle(OutputStyle.PRETTY);
			XmlParser xmlParser = new XmlParser();
			xmlParser.setOutputStyle(OutputStyle.PRETTY);
			ByteArrayOutputStream oOp = new ByteArrayOutputStream();
			if (formatType.equals("JSON")) {
				jsonParser.compose(oOp, p);

				context.setResourceString(oOp.toString());

				response = context.getResourceOperationClient().resourceOperation(p, null, context.getSelectedServerURL(), "Consent", Constants.FHIR_JSON_CONTENT, Constants.FHIR_JSON_CONTENT, null, "fileConsent", null, null);
			}
			else {
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

				if (wrapper.getResponseStatus() < 400) {
					FacesContext.getCurrentInstance().addMessage("tabView:fastconsentTabView:fileConsentForm", new FacesMessage(FacesMessage.SEVERITY_INFO, "$fileConsent request successfully processed.", ""));
				}
				else {
					FacesContext.getCurrentInstance().addMessage("tabView:fastconsentTabView:fileConsentForm", new FacesMessage(FacesMessage.SEVERITY_ERROR, "$fileConsent response failure [" + wrapper.getResponseStatus() + "].", ""));

					consent.setStatus(ConsentState.INACTIVE);
				}
			}
			else {
				context.setResponseString("ERROR: Response is empty!");
				FacesContext.getCurrentInstance().addMessage("tabView:fastconsentTabView:fileConsentForm",
					new FacesMessage(FacesMessage.SEVERITY_ERROR, "Error processing $fileConsent! Response is empty.", ""));
			}

			// Save generated Consent and DocumentReference to client resources
			Clientresource clientresource = new Clientresource();
			clientresource.setResourceId(consentId);
			clientresource.setResourceType("Consent");
			oOp = new ByteArrayOutputStream();
			jsonParser.compose(oOp, consent);
			clientresource.setResourceContents(oOp.toByteArray());
			context.getClientresourceService().create(clientresource, consent);

			clientresource = new Clientresource();
			clientresource.setResourceId(docRefId);
			clientresource.setResourceType("DocumentReference");
			oOp = new ByteArrayOutputStream();
			jsonParser.compose(oOp, docRef);
			clientresource.setResourceContents(oOp.toByteArray());
			context.getClientresourceService().create(clientresource, docRef);
		}
		catch (WildfhirClientException e) {
			FacesContext.getCurrentInstance().addMessage("tabView:fastconsentTabView:fileConsentForm",
					new FacesMessage(FacesMessage.SEVERITY_ERROR, "Error processing $fileConsent! " + e.getMessage(), ""));
		}
		catch (Exception e) {
			FacesContext.getCurrentInstance().addMessage("tabView:fastconsentTabView:fileConsentForm",
					new FacesMessage(FacesMessage.SEVERITY_ERROR, "Error processing $fileConsent! Please check the client logs.", ""));
			e.printStackTrace();
		}

		log.fine("[END] ApplicationController.processFileConsent()");
	}

	public void handleFileConsentPatientChange() throws Exception {
		try {
			// Get selected patient resource
			String patientId = context.getSelectedPatientId();
			Patient patient = (Patient) context.getClientresourceService().readFHIRResource(Integer.valueOf(patientId));

			if (patient != null) {
				// Populate fileConsent RelatedPerson list for selected Patient
				context.setClientRelatedPersons(patient);
			}
		}
		catch (Exception e) {
			FacesContext.getCurrentInstance().addMessage("tabView:fastconsentTabView:fileConsentForm",
					new FacesMessage(FacesMessage.SEVERITY_ERROR, "Error processing $fileConsent handle Patient change! Please check the client logs.", ""));
			e.printStackTrace();
		}
	}

	public void handleRecordDisclosurePatientChange() throws Exception {
		try {
			// Get selected patient resource
			String patientId = context.getSelectedPatientId();
			Patient patient = (Patient) context.getClientresourceService().readFHIRResource(Integer.valueOf(patientId));

			if (patient != null) {
				// Populate recordDisclosure Consent list for selected Patient
				context.setClientPatientConsents(patient);
			}
		}
		catch (Exception e) {
			FacesContext.getCurrentInstance().addMessage("tabView:fastconsentTabView:recordDisclosureForm",
					new FacesMessage(FacesMessage.SEVERITY_ERROR, "Error processing $recordDisclosure handle Patient change! Please check the client logs. " + e.getMessage(), ""));
			e.printStackTrace();
		}
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
			String formatType = context.getSelectedFormatType();
			log.info("Selected Format Type: " + formatType);
			log.info("BasePath for $revokeConsent: " + context.getSelectedServerURL());
			Response response = null;
			ResourceResponseWrapper wrapper = null;

			String clientConsentId = context.getSelectedConsentId();

			StringBuilder sb = new StringBuilder();
			sb.append("Client Consent id: ").append(clientConsentId);

			context.setResourceString(sb.toString());
			context.setResponseString("TBD");

			Consent consent = (Consent) context.getClientresourceService().readFHIRResource(Integer.valueOf(clientConsentId));

			// Build consent Reference parameter value
			Identifier consentIdentifer = consent.getIdentifierFirstRep();

			// Build search criteria for destination server
			Map<String, String> criteriaToSend = new HashMap<String, String>();
			criteriaToSend.put("identifier", (consentIdentifer.hasSystem() ? consentIdentifer.getSystem() + "|" : "") + consentIdentifer.getValue());

			response = context.getResourceRESTClient().searchGet(criteriaToSend, context.getSelectedServerURL(), "Consent", Constants.FHIR_XML_CONTENT, null, null, null);

			if (response != null && response.getStatus() == Response.Status.OK.getStatusCode()) {

				wrapper = new ResourceResponseWrapper(response);
				BundleWrapper searchBundleWrapper = wrapper.getBundle();
				Bundle searchBundle = searchBundleWrapper.getBundle();

				if (searchBundle.hasEntry()) {
					// Expecting only one match, use the first entry
					consent = (Consent) searchBundle.getEntryFirstRep().getResource();
				}
				else {
					throw new WildfhirClientException("Search for server Consent failed! No matching Consent found for identifier " +
							(consentIdentifer.hasSystem() ? consentIdentifer.getSystem() + "|" : "") + consentIdentifer.getValue());
				}
			}
			else {
				throw new WildfhirClientException("Search for server Consent failed! Response " + response.getStatusInfo().getReasonPhrase());
			}

			// Build revokeConsent Parameters with Consent and DocumentReference
			Parameters p = new Parameters();
			p.setId(UUIDUtil.getUUID());
			Meta meta = new Meta();
			meta.addProfile("http://hl7.org/fhir/us/consent-management/StructureDefinition/RevokeConsentParameters");
			p.setMeta(meta);

			// consent parameter
			ParametersParameterComponent param = new ParametersParameterComponent();
			param.setName("consent");
			Reference reference = new Reference();
			reference.setReference("Consent/" + consent.getId());
			reference.setIdentifier(consentIdentifer);
			param.setValue(reference);

			p.addParameter(param);

			// patient parameter
			param = new ParametersParameterComponent();
			param.setName("patient");
			param.setValue(consent.getPatient());

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

			docRef.setStatus(DocumentReferenceStatus.CURRENT);

			CodeableConcept codeableConcept = new CodeableConcept();
			Coding coding = new Coding();
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

			reference = consent.getPatient();
			docRef.setSubject(reference);
			docRef.setDate(new Date());
			docRef.addAuthor(reference);

			String consentPatientId = ServicesUtil.INSTANCE.extractResourceIdFromURL(consent.getPatient().getReference());
			Clientresource consentPatient = context.getClientresourceService().readClientResource("Patient", consentPatientId);
			Patient grantor = (Patient) context.getClientresourceService().readFHIRResource(consentPatient.getId());
			// Get Consent recipient (RelatedPersion)
			String recipientId = ServicesUtil.INSTANCE.extractResourceIdFromURL(consent.getProvision().getActorFirstRep().getReference().getReference());
			RelatedPerson recipient = (RelatedPerson) context.getClientresourceService().readFHIRResource("RelatedPerson", recipientId);

			DocumentReferenceContentComponent content = new DocumentReferenceContentComponent();
			Attachment attachment = new Attachment();
			attachment.setContentType("text/plain" + Constants.CHARSET_UTF8_EXT);
			sb = new StringBuilder("I, ");
			sb.append(grantor.getNameFirstRep().getNameAsSingleString());
			sb.append(", revoke my consent to ");
			sb.append(consent.getProvision().getType().toCode());
			sb.append(" ");
			sb.append(recipient.getNameFirstRep().getNameAsSingleString());
			sb.append(" access to my medical records.");
			attachment.setData(sb.toString().getBytes("UTF-8"));
			content.setAttachment(attachment);
			docRef.addContent(content);

			param.setResource(docRef);

			p.addParameter(param);

			// Send $fileConsent request
			JsonParser jsonParser = new JsonParser();
			jsonParser.setOutputStyle(OutputStyle.PRETTY);
			XmlParser xmlParser = new XmlParser();
			xmlParser.setOutputStyle(OutputStyle.PRETTY);
			ByteArrayOutputStream oOp = new ByteArrayOutputStream();
			if (formatType.equals("JSON")) {
				jsonParser.compose(oOp, p);

				context.setResourceString(oOp.toString());

				response = context.getResourceOperationClient().resourceOperation(p, null, context.getSelectedServerURL(), "Consent", Constants.FHIR_JSON_CONTENT, Constants.FHIR_JSON_CONTENT, null, "revokeConsent", null, null);
			}
			else {
				xmlParser.compose(oOp, p, true);

				context.setResourceString(oOp.toString());

				response = context.getResourceOperationClient().resourceOperation(p, null, context.getSelectedServerURL(), "Consent", Constants.FHIR_XML_CONTENT, Constants.FHIR_XML_CONTENT, null, "revokeConsent", null, null);
			}

			if (response != null) {
				wrapper = new ResourceResponseWrapper(response);

				if (formatType.equals("JSON")) {
					context.setResponseString(wrapper.getResourceJSON());
				}
				else {
					context.setResponseString(wrapper.getResourceXML());
				}

				if (wrapper.getResponseStatus() < 400) {
					FacesContext.getCurrentInstance().addMessage("tabView:fastconsentTabView:revokeConsentForm", new FacesMessage(FacesMessage.SEVERITY_INFO, "$revokeConsent request successfully processed.", ""));

					consent.setStatus(ConsentState.INACTIVE);
				}
				else {
					FacesContext.getCurrentInstance().addMessage("tabView:fastconsentTabView:revokeConsentForm", new FacesMessage(FacesMessage.SEVERITY_ERROR, "$revokeConsent response failure [" + wrapper.getResponseStatus() + "].", ""));
				}
			}
			else {
				context.setResponseString("ERROR: Response is empty!");
				FacesContext.getCurrentInstance().addMessage("tabView:fastconsentTabView:revokeConsentForm",
					new FacesMessage(FacesMessage.SEVERITY_ERROR, "Error processing $revokeConsent! Response is empty.", ""));
			}

			// Save updated Consent and generated DocumentReference to client resources
			Clientresource clientresource = context.getClientresourceService().read(Integer.valueOf(clientConsentId));
			oOp = new ByteArrayOutputStream();
			jsonParser.compose(oOp, consent);
			clientresource.setResourceContents(oOp.toByteArray());
			context.getClientresourceService().update(clientresource, consent);

			clientresource = new Clientresource();
			clientresource.setResourceId(docRefId);
			clientresource.setResourceType("DocumentReference");
			oOp = new ByteArrayOutputStream();
			jsonParser.compose(oOp, docRef);
			clientresource.setResourceContents(oOp.toByteArray());
			context.getClientresourceService().create(clientresource, docRef);
		}
		catch (WildfhirClientException e) {
			FacesContext.getCurrentInstance().addMessage("tabView:fastconsentTabView:revokeConsentForm",
					new FacesMessage(FacesMessage.SEVERITY_ERROR, "Error processing $revokeConsent! " + e.getMessage(), "Error processing $revokeConsent! " + e.getMessage()));
		}
		catch (Exception e) {
			FacesContext.getCurrentInstance().addMessage("tabView:fastconsentTabView:revokeConsentForm",
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
		log.info("Create AuditEvent Disclosure info: ");

		UTCDateUtil utcDateUtil;

		try {
			utcDateUtil = new UTCDateUtil();
			String formatType = context.getSelectedFormatType();
			log.info("Selected Format Type: " + formatType);
			log.info("BasePath for Create AuditEvent Disclosure: " + context.getSelectedServerURL());
			Response response = null;
			String fhirInteraction = context.getSelectedFhirInteraction();
			Date startDate = context.getStartDate();
			Date endDate = context.getEndDate();

			String clientPatientId = context.getSelectedPatientId();
			String clientPatientConsentId = context.getSelectedPatientConsentId();

			StringBuilder sb = new StringBuilder();
			sb.append("Patient id: ").append(clientPatientId);
			sb.append("; Consent id: ").append(clientPatientConsentId);
			sb.append("; Interaction: ").append(fhirInteraction);
			sb.append("; Start: ").append((startDate != null ? utcDateUtil.formatDate(startDate, UTCDateUtil.DATE_ONLY_FORMAT_UTC) : "null"));
			sb.append("; End: ").append((startDate != null ? utcDateUtil.formatDate(endDate, UTCDateUtil.DATE_ONLY_FORMAT_UTC) : "null"));

			context.setResourceString(sb.toString());
			context.setResponseString("TBD");

			// Get selected client resources
			Patient patient = (Patient) context.getClientresourceService().readFHIRResource(Integer.valueOf(clientPatientId));
			Consent consent = (Consent) context.getClientresourceService().readFHIRResource(Integer.valueOf(clientPatientConsentId));

			// Build AuditEvent Disclosure from Consent and Patient
			AuditEvent disclosure = new AuditEvent();
			String disclosureId = UUIDUtil.getUUID();
			disclosure.setId(disclosureId);
			Meta meta = new Meta();
			meta.addProfile("http://hl7.org/fhir/us/consent-management/StructureDefinition/FASTConsentAuditEvent");
			disclosure.setMeta(meta);

			// POPULATE AUDITEVENT ELEMENTS
			//type (fixed)
			Coding coding = new Coding();
			coding.setSystem("http://dicom.nema.org/resources/ontology/DCM");
			coding.setCode("110113");
			coding.setDisplay("Security Alert");
			disclosure.setType(coding);

			//subtype (fixed)
			coding = new Coding();
			coding.setSystem("https://profiles.ihe.net/ITI/BALP/CodeSystem/AuthZsubType");
			coding.setCode("AuthZ-Consent");
			disclosure.addSubtype(coding);

			//subtype (choice - restful get interactions) - NO LONGER ALLOWED BASED ON FAST AUDITEVENT PROFILE
//			coding = new Coding();
//			coding.setSystem("http://hl7.org/fhir/restful-interaction");
//			coding.setCode(fhirInteraction);
//			disclosure.addSubtype(coding);

			//action (fixed)
			disclosure.setAction(AuditEventAction.E);

			//period (set start and end)
			if (startDate != null || endDate != null) {
				Period period = new Period();
				if (startDate != null) {
					period.setStart(startDate);
				}
				if (endDate != null) {
					period.setEnd(endDate);
				}
				if (startDate != null && endDate != null) {
					if (endDate.compareTo(startDate) <= 0) {
						//throw new WildfhirClientException("AuditEvent period error! End date must be greater than start date.");
						// set endDate to startDate + 1 minute
						Calendar calendar = Calendar.getInstance();
						calendar.setTime(startDate);
						calendar.add(Calendar.MINUTE, 1);
						endDate = calendar.getTime();
						period.setEnd(endDate);
					}
				}
				disclosure.setPeriod(period);
			}

			//recorded (current datetime)
			disclosure.setRecorded(new Date());

			//outcome (fixed 0)
			disclosure.setOutcome(AuditEventOutcome._0);

			//purposeOfEvent (fixed Care Management)
			CodeableConcept cc = new CodeableConcept();
			coding = new Coding();
			coding.setSystem("http://terminology.hl7.org/CodeSystem/v3-ActReason");
			coding.setCode("CAREMGT");
			cc.addCoding(coding);
			disclosure.addPurposeOfEvent(cc);

			// AGENT SLICES
			//agent (slice client - the system used to make the request)
			AuditEventAgentComponent agent = new AuditEventAgentComponent();

			//type (fixed)
			cc = new CodeableConcept();
			coding = new Coding();
			coding.setSystem("http://dicom.nema.org/resources/ontology/DCM");
			coding.setCode("110150");
			cc.addCoding(coding);
			agent.setType(cc);

			//agent.who (fixed - consent-client-ri-site)
			Reference reference = new Reference();
			Identifier identifier = new Identifier();
			identifier.setSystem("http://example.org/identifiers");
			identifier.setValue("consent-client-ri-site");
			reference.setIdentifier(identifier);
			agent.setWho(reference);

			//agent.requestor (false)
			agent.setRequestor(false);

			//network (fixed 192.168.0.1 - local server)
			AuditEventAgentNetworkComponent network = new AuditEventAgentNetworkComponent();
			network.setAddress("192.168.0.1");
			agent.setNetwork(network);

			disclosure.addAgent(agent);

			//agent (slice user - the requesting person - fictitious provider)
			agent = new AuditEventAgentComponent();

			//type (fixed)
			cc = new CodeableConcept();
			coding = new Coding();
			coding.setSystem("http://terminology.hl7.org/CodeSystem/v3-ParticipationType");
			coding.setCode("IRCP");
			cc.addCoding(coding);
			agent.setType(cc);

			//agent.who (fixed - consent-client-ri-user)
			reference = new Reference();
			identifier = new Identifier();
			identifier.setSystem("http://hl7.org/fhir/sid/us-npi");
			identifier.setValue("consent-client-ri-user");
			reference.setIdentifier(identifier);
			agent.setWho(reference);

			//agent.requestor (true)
			agent.setRequestor(true);

			disclosure.addAgent(agent);

			//agent (slice userorg - the requesting healthcare provider)
			agent = new AuditEventAgentComponent();

			//type (fixed)
			cc = new CodeableConcept();
			coding = new Coding();
			coding.setSystem("http://terminology.hl7.org/CodeSystem/v3-RoleClass");
			coding.setCode("PROV");
			cc.addCoding(coding);
			agent.setType(cc);

			//agent.who (fixed - consent-client-ri-userorg)
			reference = new Reference();
			identifier = new Identifier();
			identifier.setSystem("http://hl7.org/fhir/sid/us-npi");
			identifier.setValue("consent-client-ri-userorg");
			reference.setIdentifier(identifier);
			agent.setWho(reference);

			//agent.requestor (false)
			agent.setRequestor(false);

			disclosure.addAgent(agent);

			//agent (slice authorizer - the system authorizing the request)
			agent = new AuditEventAgentComponent();

			//type (fixed)
			cc = new CodeableConcept();
			coding = new Coding();
			coding.setSystem("http://terminology.hl7.org/CodeSystem/extra-security-role-type");
			coding.setCode("authserver");
			cc.addCoding(coding);
			agent.setType(cc);

			//agent.who (fixed - consent-client-ri-authorizer)
			reference = new Reference();
			identifier = new Identifier();
			identifier.setSystem("http://hl7.org/fhir/sid/us-npi");
			identifier.setValue("consent-client-ri-authorizer");
			reference.setIdentifier(identifier);
			agent.setWho(reference);

			//agent.requestor (false)
			agent.setRequestor(false);

			disclosure.addAgent(agent);

			//source
			AuditEventSourceComponent source = new AuditEventSourceComponent();

			//source.observer (fixed - consent-client-ri-authorizer - MUST match agent slice authorizer)
			reference = new Reference();
			identifier = new Identifier();
			identifier.setSystem("http://hl7.org/fhir/sid/us-npi");
			identifier.setValue("consent-client-ri-authorizer");
			reference.setIdentifier(identifier);
			source.setObserver(reference);

			//source.type (fixed - 3 Web Server)
			coding = new Coding();
			coding.setSystem("http://terminology.hl7.org/CodeSystem/security-source-type");
			coding.setCode("3");
			coding.setDisplay("Web Server");
			source.addType(coding);

			disclosure.setSource(source);

			// ENTITY SLICES
			//entity (slice patient)
			AuditEventEntityComponent entity = new AuditEventEntityComponent();

			//entity.what (selected Patient)
			reference = new Reference();
			reference.setIdentifier(patient.getIdentifierFirstRep());
			entity.setWhat(reference);

			//entity.type (fixed - 1 person)
			coding = new Coding();
			coding.setSystem("http://terminology.hl7.org/CodeSystem/audit-entity-type");
			coding.setCode("1");
			entity.setType(coding);

			//entity.role (fixed - 1 Patient)
			coding = new Coding();
			coding.setSystem("http://terminology.hl7.org/CodeSystem/object-role");
			coding.setCode("1");
			coding.setDisplay("Patient");
			entity.setRole(coding);

			disclosure.addEntity(entity);

			//entity (slice consent)
			entity = new AuditEventEntityComponent();

			//entity.what (selected Consent)
			reference = new Reference();
			reference.setIdentifier(consent.getIdentifierFirstRep());
			entity.setWhat(reference);

			//entity.type (fixed - Consent)
			coding = new Coding();
			coding.setSystem("http://hl7.org/fhir/resource-types");
			coding.setCode("Consent");
			entity.setType(coding);

			//entity.role (4 Domain Resource)
			coding = new Coding();
			coding.setSystem("http://terminology.hl7.org/CodeSystem/object-role");
			coding.setCode("4");
			coding.setDisplay("Domain Resource");
			entity.setRole(coding);

			//entity.lifecycle (6 Access / Use)
			coding = new Coding();
			coding.setSystem("http://terminology.hl7.org/CodeSystem/dicom-audit-lifecycle");
			coding.setCode("6");
			coding.setDisplay("Access / Use");
			entity.setLifecycle(coding);

			disclosure.addEntity(entity);

			// Send AuditEvent Disclosure create request
			JsonParser jsonParser = new JsonParser();
			jsonParser.setOutputStyle(OutputStyle.PRETTY);
			XmlParser xmlParser = new XmlParser();
			xmlParser.setOutputStyle(OutputStyle.PRETTY);
			ByteArrayOutputStream oOp = new ByteArrayOutputStream();
			if (formatType.equals("JSON")) {
				jsonParser.compose(oOp, disclosure);

				context.setResourceString(oOp.toString());

				response = context.getResourceRESTClient().create(disclosure, context.getSelectedServerURL(), "AuditEvent", Constants.FHIR_JSON_CONTENT, null, null, null, null);
			}
			else {
				xmlParser.compose(oOp, disclosure, true);

				context.setResourceString(oOp.toString());

				response = context.getResourceRESTClient().create(disclosure, context.getSelectedServerURL(), "AuditEvent", Constants.FHIR_XML_CONTENT, null, null, null, null);
			}

			if (response != null) {
				String contentType = response.getHeaderString("Content-Type");
				if (contentType != null) {
					if (contentType.toUpperCase().contains("XML")) {
						context.setReturnedFormatType("XML");
					}
					else if (contentType.toUpperCase().contains("JSON")) {
						context.setReturnedFormatType("JSON");
					}
					else {
						context.setReturnedFormatType(formatType);
					}
				}
				else {
					context.setReturnedFormatType(formatType);
				}

				ResourceResponseWrapper wrapper;

				if ((response.getStatus() == Response.Status.OK.getStatusCode()) || (response.getStatus() == Response.Status.CREATED.getStatusCode())) {
					try {
						wrapper = new ResourceResponseWrapper(response);

						if (context.getReturnedFormatType().equals("XML")) {
							context.setResponseString(wrapper.getResourceXML());
						}
						else {
							context.setResponseString(wrapper.getResourceJSON());
						}

						FacesContext.getCurrentInstance().addMessage(
								"tabView:fastconsentTabView:recordDisclosureForm",
								new FacesMessage(FacesMessage.SEVERITY_INFO, "AuditEvent with ID: " + wrapper.getResourceBean().getResourceId() + " successfully created.", "AuditEvent with ID: " + wrapper.getResourceBean().getResourceId()
										+ " successfully created."));
					}
					catch (Exception e) {
						log.severe(e.getMessage());
						FacesContext.getCurrentInstance().addMessage("tabView:fastconsentTabView:recordDisclosureForm",
								new FacesMessage(FacesMessage.SEVERITY_ERROR, "Response resource json parsing failed! Please check the client logs.", "Response resource json parsing failed! Please check the client logs."));
						e.printStackTrace();
					}
				}
				else {
					try {
						wrapper = new ResourceResponseWrapper(response);

						if (context.getReturnedFormatType().equals("XML")) {
							context.setResponseString(wrapper.getResourceXML());
						}
						else {
							context.setResponseString(wrapper.getResourceJSON());
						}

						FacesContext.getCurrentInstance().addMessage("tabView:fastconsentTabView:recordDisclosureForm", new FacesMessage(FacesMessage.SEVERITY_INFO, "Response " + response.getStatus() + " - Failed to create new Resource entry.", "Response " + response.getStatus() + " - Failed to create new Resource entry."));
					}
					catch (Exception e) {
						log.severe(e.getMessage());
						FacesContext.getCurrentInstance().addMessage("tabView:fastconsentTabView:recordDisclosureForm",
								new FacesMessage(FacesMessage.SEVERITY_ERROR, "Response resource xml parsing failed! Please check the client logs.", "Response resource xml parsing failed! Please check the client logs."));
						e.printStackTrace();
					}
				}
			}
			else {
				context.setResponseString("ERROR: Response is empty!");
				FacesContext.getCurrentInstance().addMessage("tabView:fastconsentTabView:recordDisclosureForm",
					new FacesMessage(FacesMessage.SEVERITY_ERROR, "Error processing Create AuditEvent Disclosure! Response is empty.", ""));

				context.setResourceString("Response is empty.");
			}
		}
		catch (Exception e) {
			FacesContext.getCurrentInstance().addMessage("tabView:fastconsentTabView:recordDisclosureForm",
					new FacesMessage(FacesMessage.SEVERITY_ERROR, "Error processing Create AuditEvent Disclosure! Please check the client logs.", "Error processing Create AuditEvent Disclosure! Please check the client logs."));

			context.setResourceString(e.getMessage());
			
			e.printStackTrace();
		}
		finally {
			utcDateUtil = null;
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
		StringBuilder responseString = new StringBuilder();

		try {
			context.getClientresourceService().clientresourcePurgeAll();

			log.info("Purge clientresource complete.");

			loadPath = context.getCodeService().getCodeValue("initializeClientPath");

			log.info("initializeClient path: " + loadPath);

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

			responseString.append("Reset Client Test Data successfully processed ").append(loadCount).append(" files.");

			FacesContext.getCurrentInstance().addMessage("tabView:adminTabView:initializeClientForm",
					new FacesMessage(FacesMessage.SEVERITY_INFO, responseString.toString(), responseString.toString()));

			Serverdirectory localServer = context.getServerDirectoryService().read(1);

			Response response = context.getResourceOperationClient().resourceOperation(null, null, localServer.getBasePath(), null, Constants.FHIR_JSON_CONTENT, Constants.FHIR_JSON_CONTENT, null, "purge-all", null, null);

			if (response != null) {
				ResourceResponseWrapper wrapper = new ResourceResponseWrapper(response);

				if (wrapper.getResponseStatus() < 400) {
					FacesContext.getCurrentInstance().addMessage("tabView:adminTabView:initializeClientForm", new FacesMessage(FacesMessage.SEVERITY_INFO, "Purge server data request successfully processed.", ""));
					responseString.append(" Purge server data request successfully processed.");

					// Finally, re-create FAST SubscriptionTopic in server repository
					loadPath = context.getCodeService().getCodeValue("initializeServerPath");

					log.info("initializeServer path: " + loadPath);

					loadDir = FileSystems.getDefault().getPath(loadPath);

					result = new ArrayList<>();
					try (DirectoryStream<Path> stream = Files.newDirectoryStream(loadDir, "*.json")) {

						for (Path entry : stream) {
							log.info("Path Found: " + entry.toString());
							File serverFile = new File(entry.toString());
							if (entry.toString().contains("SubscriptionTopic")) {
								if (createServerResourceR5(serverFile, localServer)) {
									result.add(entry);
								}
							}
							else {
								if (createServerResourceR4(serverFile, localServer)) {
									result.add(entry);
								}
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

					FacesContext.getCurrentInstance().addMessage("tabView:adminTabView:initializeClientForm", new FacesMessage(FacesMessage.SEVERITY_INFO, "Reset Server Test Data successfully processed.", ""));
					responseString.append("Reset Server Test Data successfully processed ").append(loadCount).append(" files.");
				}
				else {
					FacesContext.getCurrentInstance().addMessage("tabView:adminTabView:initializeClientForm", new FacesMessage(FacesMessage.SEVERITY_ERROR, "Purge server data response failure [" + wrapper.getResponseStatus() + "].", ""));
					responseString.append(" Purge server data response failure [").append(wrapper.getResponseStatus()).append("].");
				}
			}
			else {
				responseString.append(" ERROR: Purge server data returned empty response!");
				FacesContext.getCurrentInstance().addMessage("tabView:adminTabView:initializeClientForm",
					new FacesMessage(FacesMessage.SEVERITY_ERROR, "ERROR: Purge server data returned empty response!", "ERROR: Purge server data returned empty response!"));
			}

			context.setResponseString(responseString.toString());
		}
		catch (NoSuchFileException e) {
			responseString.append("Error processing Reset Test Data! ").append(loadPath).append(" is not found.");

			FacesContext.getCurrentInstance().addMessage("tabView:adminTabView:initializeClientForm",
					new FacesMessage(FacesMessage.SEVERITY_ERROR, responseString.toString(), responseString.toString()));

			context.setResponseString(responseString.toString());

			e.printStackTrace();
		}
		catch (Exception e) {
			responseString.append("Error processing Reset Test Data! Please check the client logs.");

			FacesContext.getCurrentInstance().addMessage("tabView:adminTabView:initializeClientForm",
					new FacesMessage(FacesMessage.SEVERITY_ERROR, responseString.toString(), responseString.toString()));

			responseString.append(" ").append(e.getMessage());
			context.setResponseString(responseString.toString());

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
			bResult = false;
		}

		log.fine("[END] ApplicationController.writeClientResource()");

		return bResult;
	}

	private boolean createServerResourceR4(File serverFile, Serverdirectory localServer) {

		JsonParser jsonParser = null;
		ByteArrayInputStream iResource = null;
		Resource resource = null;
		Response response = null;
		ResourceResponseWrapper wrapper = null;
		boolean bResult = false;
		String serverFileName = "";

		try {
			serverFileName = serverFile.getName();

			log.fine("[START] ApplicationController.createServerResourceR4() for " + serverFileName);

			// Read contents of serverFileName and create in RI repository
			String serverContents = stringBuilder(serverFile.getPath());

			jsonParser = new JsonParser();
			iResource = new ByteArrayInputStream(serverContents.getBytes());
			resource = jsonParser.parse(iResource);

			response = context.getResourceRESTClient().update(resource.getId(), resource, localServer.getBasePath(), resource.getResourceType().name(), Constants.FHIR_JSON_CONTENT, null, null, null, null, null);

			wrapper = new ResourceResponseWrapper(response);

			if (wrapper.getResponseStatus() < 400) {
				bResult = true;
			}
			else {
				bResult = false;
			}
		}
		catch (Exception e) {
			// Swallow exception to allow processing to continue
			log.severe("Error processing serverFile '" + serverFile.getName() + "' - " + e.getMessage());
			bResult = false;
		}
		finally {
			jsonParser = null;
			iResource = null;
			resource = null;
			response = null;
			wrapper = null;
		}

		log.fine("[END] ApplicationController.createServerResourceR4()");

		return bResult;
	}

	private boolean createServerResourceR5(File serverFile, Serverdirectory localServer) {

		org.hl7.fhir.r5.formats.JsonParser jsonParser = null;
		ByteArrayInputStream iResource = null;
		org.hl7.fhir.r5.model.Resource resource = null;
		Response response = null;
		ResourceResponseWrapper wrapper = null;
		boolean bResult = false;
		String serverFileName = "";

		try {
			serverFileName = serverFile.getName();

			log.fine("[START] ApplicationController.createServerResourceR5() for " + serverFileName);

			// Read contents of serverFileName and create in RI repository
			String serverContents = stringBuilder(serverFile.getPath());

			jsonParser = new org.hl7.fhir.r5.formats.JsonParser();
			iResource = new ByteArrayInputStream(serverContents.getBytes());
			resource = jsonParser.parse(iResource);

			response = context.getResourceRESTClient().updateR5(resource.getId(), resource, localServer.getBasePath(), resource.getResourceType().name(), Constants.FHIR_JSON_CONTENT, null, null, null, null, null);

			wrapper = new ResourceResponseWrapper(response);

			if (wrapper.getResponseStatus() < 400) {
				bResult = true;
			}
			else {
				bResult = false;
			}
		}
		catch (Exception e) {
			// Swallow exception to allow processing to continue
			log.severe("Error processing serverFile '" + serverFile.getName() + "' - " + e.getMessage());
			bResult = false;
		}
		finally {
			jsonParser = null;
			iResource = null;
			resource = null;
			response = null;
			wrapper = null;
		}

		log.fine("[END] ApplicationController.createServerResourceR5()");

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
			log.fine("[Start] ApplicationController.manageServer() - update Existing Server");
			try {

				Serverdirectory server = context.getServerDirectoryService().update(context.getSelectedServer());
				if (server != null) {
					FacesContext.getCurrentInstance().addMessage("tabView:adminTabView:serversForm", new FacesMessage(FacesMessage.SEVERITY_INFO, "Server: " + server.getName() + " updated.", ""));
				}
			}
			catch (Exception e) {
				log.severe(e.getMessage());
				FacesContext.getCurrentInstance().addMessage("tabView:adminTabView:serversForm", new FacesMessage(FacesMessage.SEVERITY_ERROR, "Error updating Server.", ""));
			}
		}

		if (serverType.equalsIgnoreCase("new")) {
			log.fine("[Start] ApplicationController.manageServer() - Create new Server");
			try {
				Serverdirectory server = context.getServerDirectoryService().create(context.getNewServer());
				if (server != null) {
					FacesContext.getCurrentInstance().addMessage("tabView:adminTabView:serversForm", new FacesMessage(FacesMessage.SEVERITY_INFO, "Server: " + server.getName() + " successfully created.", ""));
					context.setNewServer(new Serverdirectory());
					context.setAvailableServers(context.getServerDirectoryService().findAll());
				}
			}
			catch (Exception e) {
				log.severe(e.getMessage());
				FacesContext.getCurrentInstance().addMessage("tabView:adminTabView:serversForm", new FacesMessage(FacesMessage.SEVERITY_ERROR, "Error creating new Server.", ""));
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
		log.fine("serverToManage name: " + context.getSelectedServer().getName());
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

	/*
	 * Subscription manage methods
	 */

	/**
	 * Perform a FHIR create on the selected server with a Subscription resource
	 * generated from the user data
	 *
	 * @return
	 */
	public void processCreateSubscription(ActionEvent event) {
		log.fine("[START] ApplicationController.processCreateSubscription()");

		try {
			log.info("BasePath for FHIR create (Subscription): " + context.getSelectedServerURL());

			Response resourceResponse = null;
			ResourceResponseWrapper wrapper = null;

			String formatType = context.getSelectedFormatType();

			Date endDate = context.getEndDate();
			String subscriptionReason = context.getSubscriptionReason();
			String subscriptionCriteria = context.getSubscriptionCriteria();
			String subscriptionEndpoint = context.getSubscriptionEndpoint();
			String selectedSubscriptionPayloadType = context.getSelectedSubscriptionPayloadType();
			String selectedSubscriptionContentType = context.getSelectedSubscriptionContentType();

			// Create new Subscription resource instance from user input data
			Subscription subscription = new Subscription();

			Meta meta = new Meta();
			meta.addProfile("http://hl7.org/fhir/us/consent-management/StructureDefinition/FASTSubscription");
			subscription.setMeta(meta);

			subscription.setStatus(SubscriptionStatus.REQUESTED);

			if (endDate != null) {
				subscription.setEnd(endDate);
			}

			subscription.setReason(subscriptionReason);

			subscription.setCriteria("http://hl7.org/fhir/us/consent-management/SubscriptionTopic/FASTConsentSubscriptionTopic");
			Extension extension = new Extension();
			extension.setUrl("http://hl7.org/fhir/uv/subscriptions-backport/StructureDefinition/backport-filter-criteria");
			StringType stringValue = new StringType(subscriptionCriteria);
			extension.setValue(stringValue);
			subscription.getCriteriaElement().addExtension(extension);

			SubscriptionChannelComponent channel = new SubscriptionChannelComponent();
			channel.setType(SubscriptionChannelType.RESTHOOK);
			channel.setEndpoint(subscriptionEndpoint);
			channel.setPayload(selectedSubscriptionPayloadType);
			extension = new Extension();
			extension.setUrl("http://hl7.org/fhir/uv/subscriptions-backport/StructureDefinition/backport-payload-content");
			CodeType codeType = new CodeType();
			codeType.setValue(selectedSubscriptionContentType);
			extension.setValue(codeType);
			channel.getPayloadElement().addExtension(extension);
			subscription.setChannel(channel);

			String resourceString = null;
			ByteArrayOutputStream oOp = new ByteArrayOutputStream();
			XmlParser xmlParser = new XmlParser();
			JsonParser jsonParser = new JsonParser();

			if (formatType.equals("XML")) {
				try {
					xmlParser.setOutputStyle(OutputStyle.PRETTY);
					xmlParser.compose(oOp, subscription, true);

					resourceString = oOp.toString();
				}
				catch (Exception e) {
					resourceString = "Exception parsing Subscription resource to XML format! " + e.getMessage();
					throw new Exception(resourceString);
				}
			}
			else {
				try {
					jsonParser.setOutputStyle(OutputStyle.PRETTY);
					jsonParser.compose(oOp, subscription);

					resourceString = oOp.toString();
				}
				catch (Exception e) {
					resourceString = "Exception parsing Subscription resource to JSON format! " + e.getMessage();
					throw new Exception(resourceString);
				}
			}

			context.setResourceString(resourceString);

			resourceResponse = context.getResourceRESTClient().create(subscription, context.getSelectedServerURL(), "Subscription", formatType, null, null, null, null);

			if (resourceResponse != null) {
				String contentType = resourceResponse.getHeaderString("Content-Type");
				if (contentType != null) {
					if (contentType.toUpperCase().contains("XML")) {
						context.setReturnedFormatType("XML");
					}
					else if (contentType.toUpperCase().contains("JSON")) {
						context.setReturnedFormatType("JSON");
					}
					else {
						context.setReturnedFormatType(formatType);
					}
				}
				else {
					context.setReturnedFormatType(formatType);
				}

				if (resourceResponse.getStatus() == (Response.Status.CREATED.getStatusCode())  ||
						resourceResponse.getStatus() == (Response.Status.OK.getStatusCode())) {
					try {
						wrapper = new ResourceResponseWrapper(resourceResponse);

						if (context.getReturnedFormatType().equals("XML")) {
							context.setResponseString(wrapper.getResourceXML());
						}
						else {
							context.setResponseString(wrapper.getResourceJSON());
						}

						subscription.setId(wrapper.getResource().getId());

						// Save generated Subscription to client resources
						Clientresource clientresource = new Clientresource();
						clientresource.setResourceId(wrapper.getResource().getId());
						clientresource.setResourceType("Subscription");
						oOp = new ByteArrayOutputStream();
						jsonParser.compose(oOp, subscription);
						clientresource.setResourceContents(oOp.toByteArray());
						context.getClientresourceService().create(clientresource, subscription);

						FacesContext.getCurrentInstance().addMessage("tabView:subscriptionsTabView:createSubscriptionForm",
								new FacesMessage(FacesMessage.SEVERITY_INFO, "Subscription successfully created.", null));
					}
					catch (Exception e) {
						FacesContext.getCurrentInstance().addMessage("tabView:subscriptionsTabView:createSubscriptionForm",
								new FacesMessage(FacesMessage.SEVERITY_ERROR, "Resource parsing failed! Please check the client logs. " + e.getMessage(), null));
						e.printStackTrace();
					}

				}
				else {
					try {
						wrapper = new ResourceResponseWrapper(resourceResponse);

						if (context.getReturnedFormatType().equals("XML")) {
							context.setResponseString(wrapper.getResourceXML());
						}
						else {
							context.setResponseString(wrapper.getResourceJSON());
						}

						FacesContext.getCurrentInstance().addMessage("tabView:subscriptionsTabView:createSubscriptionForm",
								new FacesMessage(FacesMessage.SEVERITY_WARN, "Response " + resourceResponse.getStatus() + " - Failed to create new Subscription.", null));
					}
					catch (Exception e) {
						FacesContext.getCurrentInstance().addMessage("tabView:subscriptionsTabView:createSubscriptionForm",
								new FacesMessage(FacesMessage.SEVERITY_ERROR, "Resource parsing failed! Please check the client logs." + e.getMessage(), null));
						e.printStackTrace();
					}
				}
			}
		}
		catch (Exception e) {
			FacesContext.getCurrentInstance().addMessage("tabView:subscriptionsTabView:createSubscriptionForm",
					new FacesMessage(FacesMessage.SEVERITY_ERROR, "Error creating Subscription! Please check the client logs. " + e.getMessage(), null));
			e.printStackTrace();
		}

		log.fine("[END] ApplicationController.processCreateSubscription()");
	}

	/**
	 * Perform a FHIR update on the selected server with the selected Subscription resource
	 *
	 * @return
	 */
	public void processUpdateSubscription(ActionEvent event) {
		log.fine("[START] ApplicationController.processUpdateSubscription()");

		try {
			log.info("BasePath for FHIR update (Subscription): " + context.getSelectedServerURL());

			Response resourceResponse = null;
			ResourceResponseWrapper wrapper = null;

			String formatType = context.getSelectedFormatType();

			String clientSubscriptionId = context.getSelectedSubscriptionId();
			String subscriptionStatus = context.getSelectedSubscriptionStatus();
			Date endDate = context.getEndDate();
			String subscriptionReason = context.getSubscriptionReason();
			String subscriptionCriteria = context.getSubscriptionCriteria();
			String subscriptionEndpoint = context.getSubscriptionEndpoint();
			String selectedSubscriptionPayloadType = context.getSelectedSubscriptionPayloadType();
			String selectedSubscriptionContentType = context.getSelectedSubscriptionContentType();

			Subscription subscription = (Subscription) context.getClientresourceService().readFHIRResource(Integer.valueOf(clientSubscriptionId));

			subscription.setStatus(SubscriptionStatus.fromCode(subscriptionStatus));
			subscription.setEnd(endDate);
			subscription.setReason(subscriptionReason);

			Extension extension = subscription.getCriteriaElement().getExtensionByUrl("http://hl7.org/fhir/uv/subscriptions-backport/StructureDefinition/backport-filter-criteria");
			StringType criteria = (StringType)extension.getValue();
			criteria.setValue(subscriptionCriteria);

			SubscriptionChannelComponent channel = subscription.getChannel();
			channel.setEndpoint(subscriptionEndpoint);
			channel.setPayload(selectedSubscriptionPayloadType);
			extension = channel.getPayloadElement().getExtensionByUrl("http://hl7.org/fhir/uv/subscriptions-backport/StructureDefinition/backport-payload-content");
			CodeType codeType = (CodeType)extension.getValue();
			codeType.setValue(selectedSubscriptionContentType);

			// Send Subscription update to target server
			JsonParser jsonParser = new JsonParser();
			jsonParser.setOutputStyle(OutputStyle.PRETTY);
			XmlParser xmlParser = new XmlParser();
			xmlParser.setOutputStyle(OutputStyle.PRETTY);
			ByteArrayOutputStream oOp = new ByteArrayOutputStream();
			if (formatType.equals("JSON")) {
				jsonParser.compose(oOp, subscription);

				context.setResourceString(oOp.toString());

				resourceResponse = context.getResourceRESTClient().update(subscription.getId(), subscription, context.getSelectedServerURL(), "Subscription", Constants.FHIR_JSON_CONTENT, null, null, null, null, null);
			}
			else {
				xmlParser.compose(oOp, subscription, true);

				context.setResourceString(oOp.toString());

				resourceResponse = context.getResourceRESTClient().update(subscription.getId(), subscription, context.getSelectedServerURL(), "Subscription", Constants.FHIR_XML_CONTENT, null, null, null, null, null);
			}

			if (resourceResponse != null) {
				wrapper = new ResourceResponseWrapper(resourceResponse);

				if (formatType.equals("JSON")) {
					context.setResponseString(wrapper.getResourceJSON());
				}
				else {
					context.setResponseString(wrapper.getResourceXML());
				}

				if (wrapper.getResponseStatus() < 400) {
					FacesContext.getCurrentInstance().addMessage("tabView:subscriptionsTabView:updateSubscriptionForm",
							new FacesMessage(FacesMessage.SEVERITY_INFO, "Subscription successfully updated.", null));

					// Save updated Consent and generated DocumentReference to client resources
					Clientresource clientresource = context.getClientresourceService().read(Integer.valueOf(clientSubscriptionId));
					oOp = new ByteArrayOutputStream();
					jsonParser.compose(oOp, subscription);
					clientresource.setResourceContents(oOp.toByteArray());
					context.getClientresourceService().update(clientresource, subscription);
				}
				else {
					FacesContext.getCurrentInstance().addMessage("tabView:subscriptionsTabView:updateSubscriptionForm",
							new FacesMessage(FacesMessage.SEVERITY_ERROR, "Subscription update response failure [" + wrapper.getResponseStatus() + "].", null));
				}
			}
			else {
				context.setResponseString("ERROR: Response is empty!");
				FacesContext.getCurrentInstance().addMessage("tabView:subscriptionsTabView:updateSubscriptionForm",
					new FacesMessage(FacesMessage.SEVERITY_ERROR, "Error processing Subscription update! Response is empty.", null));
			}


		}
		catch (Exception e) {
			FacesContext.getCurrentInstance().addMessage("tabView:subscriptionsTabView:updateSubscriptionForm",
					new FacesMessage(FacesMessage.SEVERITY_ERROR, "Error updating Subscription! Please check the client logs. " + e.getMessage(), null));
			e.printStackTrace();
		}

		log.fine("[END] ApplicationController.processUpdateSubscription()");
	}

	public void handleCreateSubscriptionTopicChange() throws Exception {
		try {
			// Get selected subscription topic
			String subscriptionTopicURL = context.getSelectedSubscriptionTopic();

			if (subscriptionTopicURL != null) {
				// Populate createSubscription criteria value that can be changed
				String subscriptionCriteria = null;

				if (subscriptionTopicURL.contains("FASTConsentSubscriptionTopic")) {
					subscriptionCriteria = "Consent?patient:identifier=PATIENTID{&actor:identifier=ACTORID&controller:identifier=CONTROLLERID&status=STATUS&category=CATEGORY}";
				}

				context.setSubscriptionCriteria(subscriptionCriteria);
			}
		}
		catch (Exception e) {
			FacesContext.getCurrentInstance().addMessage("tabView:subscriptionsTabView:createSubscriptionForm",
					new FacesMessage(FacesMessage.SEVERITY_ERROR, "Error processing Create Scription handle ScriptionTopic change! Please check the client logs. " + e.getMessage(), ""));
			e.printStackTrace();
		}
	}

	public void handleUpdateSubscriptionChange() throws Exception {
		try {
			// Get selected subscription resource
			String subscriptionConsentId = context.getSelectedSubscriptionId();
			Subscription subscription = (Subscription) context.getClientresourceService().readFHIRResource(Integer.valueOf(subscriptionConsentId));

			if (subscription != null) {
				// Populate updateSubscription values that can be changed

				String selectedSubscriptionTopicURL = subscription.getCriteria();
				context.setSelectedSubscriptionTopic(context.getSubscriptionTopicByURL(selectedSubscriptionTopicURL));

				String selectedSubscriptionStatus = subscription.getStatus().toCode();
				context.setSelectedSubscriptionStatus(selectedSubscriptionStatus);

				String subscriptionReason = subscription.getReason();
				context.setSubscriptionReason(subscriptionReason);

				StringType subscriptionCriteria = (StringType)subscription.getCriteriaElement().getExtensionByUrl("http://hl7.org/fhir/uv/subscriptions-backport/StructureDefinition/backport-filter-criteria").getValue();
				context.setSubscriptionCriteria(subscriptionCriteria.getValue());

				String subscriptionEndpoint = subscription.getChannel().getEndpoint();
				context.setSubscriptionEndpoint(subscriptionEndpoint);

				String selectedSubscriptionPayloadType = subscription.getChannel().getPayload();
				context.setSelectedSubscriptionPayloadType(selectedSubscriptionPayloadType);

				StringType selectedSubscriptionContentType = (StringType)subscription.getChannel().getPayloadElement().getExtensionByUrl("http://hl7.org/fhir/uv/subscriptions-backport/StructureDefinition/backport-payload-content").getValue();
				context.setSelectedSubscriptionContentType(selectedSubscriptionContentType.getValue());

				Date endDate = subscription.getEnd();
				context.setEndDate(endDate);

				if (subscription.getChannel().hasExtension("http://hl7.org/fhir/uv/subscriptions-backport/StructureDefinition/backport-heartbeat-period")) {
					UnsignedIntType uIntType = (UnsignedIntType)subscription.getChannel().getExtensionByUrl("http://hl7.org/fhir/uv/subscriptions-backport/StructureDefinition/backport-heartbeat-period").getValue();
					context.setSubscriptionHeartbeat(uIntType.getValue());
				}
				else {
					context.setSubscriptionHeartbeat(null);
				}

				if (subscription.getChannel().hasExtension("http://hl7.org/fhir/uv/subscriptions-backport/StructureDefinition/backport-timeout")) {
					UnsignedIntType uIntType = (UnsignedIntType)subscription.getChannel().getExtensionByUrl("http://hl7.org/fhir/uv/subscriptions-backport/StructureDefinition/backport-timeout").getValue();
					context.setSubscriptionTimeout(uIntType.getValue());
				}
				else {
					context.setSubscriptionTimeout(null);
				}

				if (subscription.getChannel().hasExtension("http://hl7.org/fhir/uv/subscriptions-backport/StructureDefinition/backport-max-count")) {
					PositiveIntType pIntType = (PositiveIntType)subscription.getChannel().getExtensionByUrl("http://hl7.org/fhir/uv/subscriptions-backport/StructureDefinition/backport-max-count").getValue();
					context.setSubscriptionMaxCount(pIntType.getValue());
				}
				else {
					context.setSubscriptionMaxCount(null);
				}
			}
		}
		catch (Exception e) {
			FacesContext.getCurrentInstance().addMessage("tabView:subscriptionsTabView:updateSubscriptionForm",
					new FacesMessage(FacesMessage.SEVERITY_ERROR, "Error processing Update Scription handle Scription change! Please check the client logs. " + e.getMessage(), ""));
			e.printStackTrace();
		}
	}

	public void handleSubscriptionHandshakeChange() throws Exception {
		try {
			// Get selected subscription resource
			String subscriptionId = context.getSelectedSubscriptionId();
			Subscription subscription = (Subscription) context.getResourceService().readFHIRResourceForTypeId("Subscription", subscriptionId);

			if (subscription != null) {
				// Populate updateSubscription values that can be changed

				String selectedSubscriptionTopicURL = subscription.getCriteria();
				context.setSelectedSubscriptionTopic(context.getSubscriptionTopicByURL(selectedSubscriptionTopicURL));

				String subscriptionReason = subscription.getReason();
				context.setSubscriptionReason(subscriptionReason);

				StringType subscriptionCriteria = (StringType)subscription.getCriteriaElement().getExtensionByUrl("http://hl7.org/fhir/uv/subscriptions-backport/StructureDefinition/backport-filter-criteria").getValue();
				context.setSubscriptionCriteria(subscriptionCriteria.getValue());

				String subscriptionEndpoint = subscription.getChannel().getEndpoint();
				context.setSubscriptionEndpoint(subscriptionEndpoint);
			}
		}
		catch (Exception e) {
			FacesContext.getCurrentInstance().addMessage("tabView:subscriptionsTabView:subscriptionHandshakeForm",
					new FacesMessage(FacesMessage.SEVERITY_ERROR, "Error processing Scription Handshake handle Scription change! Please check the client logs. " + e.getMessage(), ""));
			e.printStackTrace();
		}
	}

	/**
	 * Start the Subscription Service using the passed in since date
	 *
	 * @param event
	 */
	public void processSubscriptions(ActionEvent event) {
		log.fine("[START] ApplicationController.processSubscriptions()");

		Date datePicker = null;
		List<LabelKeyValueBean> results = null;
		UTCDateUtil utcDateUtil;

		try {
			utcDateUtil = new UTCDateUtil();
			if (context.getCodeService().isSupported("subscriptionServiceEnabled")) {
				LocalDateTime dateTimePicker = context.getDateTimePicker();

				if (dateTimePicker == null) {
					// Use current subscriptionLastProcessed
					datePicker = context.getSubscriptionLastProcessed();

					log.fine("last processed = " + utcDateUtil.formatDate(datePicker, UTCDateUtil.DATETIME_ONLY_PARAMETER_FORMAT));
				}
				else {
					// Convert from LocalDateTime to Date in current time zone
					datePicker = Date.from(dateTimePicker.atZone(ZoneId.of("GMT")).toInstant());

					log.fine("datePicker = " + utcDateUtil.formatDate(datePicker, UTCDateUtil.DATETIME_ONLY_PARAMETER_FORMAT));
				}

				Date newLastProcessed = new Date();
				context.setSubscriptionLastProcessed(newLastProcessed);
				String newLastProcessedMsg = "Subscription processing successfully executed. Last processed date time is now " + utcDateUtil.formatDate(newLastProcessed, UTCDateUtil.DATETIME_ONLY_PARAMETER_FORMAT) + ".";

				results = context.getSubscriptionServiceR5().processSubscriptions(datePicker);

				if (results == null) {
					results = new ArrayList<LabelKeyValueBean>();
				}
				if (results.isEmpty()) {
					results.add(new LabelKeyValueBean("No active subscriptions found.","",""));
				}

				context.setListLabelKeyValue(results);

				FacesContext.getCurrentInstance().addMessage("tabView:subscriptionClientTab:subscriptionClientForm",
						new FacesMessage(FacesMessage.SEVERITY_INFO, newLastProcessedMsg, newLastProcessedMsg));
			}
			else {
				FacesContext.getCurrentInstance().addMessage("tabView:subscriptionClientTab:subscriptionClientForm",
						new FacesMessage(FacesMessage.SEVERITY_WARN, "Subscription processing is not enabled.", "Subscription processing is not enabled."));
			}
		}
		catch (Exception e1) {
			log.fine(e1.getMessage());
			FacesContext.getCurrentInstance().addMessage("tabView:subscriptionClientTab:subscriptionClientForm",
					new FacesMessage(FacesMessage.SEVERITY_ERROR, "Error executing subscription processing! Please check the client logs.", "Error executing subscription processing! Please check the client logs."));

			e1.printStackTrace();
		}
		finally {
			utcDateUtil = null;
		}

		log.fine("[END] ApplicationController.fhirProcessSubscriptions()");
	}

	/**
	 * Send a Handshake notification for the selected Subscription
	 *
	 * @param event
	 */
	public void processSubscriptionHandshake(ActionEvent event) {
		log.info("[START] ApplicationController.processSubscriptionHandshake()");

		List<LabelKeyValueBean> results = new ArrayList<LabelKeyValueBean>();

		try {
			if (context.getCodeService().isSupported("subscriptionServiceEnabled")) {

				String subscriptionId = context.getSelectedSubscriptionId();

				Subscription subscription = (Subscription) context.getResourceService().readFHIRResourceForTypeId("Subscription", subscriptionId);

				if (subscription == null) {

					FacesContext.getCurrentInstance().addMessage("tabView:subscriptionsTabView:subscriptionHandshakeForm",
							new FacesMessage(FacesMessage.SEVERITY_WARN, "Please select a Subscription.", "Please select a Subscription."));

				}
				else {
					StringBuffer returnedDetails = new StringBuffer();

					LabelKeyValueBean result = context.getSubscriptionServiceR5().sendHandshake(subscription, returnedDetails);

					results.add(result);

					context.setListLabelKeyValue(results);

					FacesContext.getCurrentInstance().addMessage("tabView:subscriptionsTabView:subscriptionHandshakeForm",
							new FacesMessage(FacesMessage.SEVERITY_INFO, "Subscription handshake processing complete.", (returnedDetails != null ? returnedDetails.toString() : "Subscription handshake processing complete.")));
				}
			}
			else {
				FacesContext.getCurrentInstance().addMessage("tabView:subscriptionsTabView:subscriptionHandshakeForm",
						new FacesMessage(FacesMessage.SEVERITY_WARN, "Subscription processing is not enabled.", "Subscription processing is not enabled."));
			}
		}
		catch (Exception e1) {
			log.info(e1.getMessage());
			FacesContext.getCurrentInstance().addMessage("tabView:subscriptionsTabView:subscriptionHandshakeForm",
					new FacesMessage(FacesMessage.SEVERITY_ERROR, "Error executing subscription handshake processing! Please check the client logs.", "Error executing subscription handshake processing! Please check the client logs."));

			e1.printStackTrace();
		}

		log.info("[END] ApplicationController.processSubscriptionHandshake()");
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
