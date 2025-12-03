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
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
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
import org.hl7.fhir.r4.model.AuditEvent;
import org.hl7.fhir.r4.model.AuditEvent.AuditEventAction;
import org.hl7.fhir.r4.model.AuditEvent.AuditEventAgentComponent;
import org.hl7.fhir.r4.model.AuditEvent.AuditEventEntityComponent;
import org.hl7.fhir.r4.model.AuditEvent.AuditEventOutcome;
import org.hl7.fhir.r4.model.AuditEvent.AuditEventSourceComponent;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.Bundle.BundleLinkComponent;
import org.hl7.fhir.r4.model.CodeType;
import org.hl7.fhir.r4.model.CodeableConcept;
import org.hl7.fhir.r4.model.Coding;
import org.hl7.fhir.r4.model.Consent;
import org.hl7.fhir.r4.model.Extension;
import org.hl7.fhir.r4.model.Identifier;
import org.hl7.fhir.r4.model.Meta;
import org.hl7.fhir.r4.model.OperationOutcome;
import org.hl7.fhir.r4.model.Parameters;
import org.hl7.fhir.r4.model.Parameters.ParametersParameterComponent;
import org.hl7.fhir.r4.model.Patient;
import org.hl7.fhir.r4.model.Period;
import org.hl7.fhir.r4.model.Reference;
import org.hl7.fhir.r4.model.RelatedPerson;
import org.hl7.fhir.r4.model.Resource;
import org.hl7.fhir.r4.model.StringType;
import org.hl7.fhir.r4.model.Subscription;
import org.hl7.fhir.r4.model.Subscription.SubscriptionChannelComponent;
import org.hl7.fhir.r4.model.Subscription.SubscriptionChannelType;
import org.hl7.fhir.r4.model.Subscription.SubscriptionStatus;
import org.hl7.fhir.r4.model.Consent.ConsentProvisionType;
import org.hl7.fhir.r4.model.Consent.ConsentState;
import org.hl7.fhir.r4.model.Consent.ProvisionComponent;
import org.hl7.fhir.r4.model.Consent.provisionActorComponent;
import org.hl7.fhir.r4.model.DocumentReference;
import org.hl7.fhir.r4.model.DocumentReference.DocumentReferenceContentComponent;
import org.hl7.fhir.r4.model.Enumerations.DocumentReferenceStatus;
import org.primefaces.event.TabChangeEvent;

import net.aegis.fhir.client.ApplicationContext;
import net.aegis.fhir.client.model.BundleWrapper;
import net.aegis.fhir.client.model.ResourceResponseWrapper;
import net.aegis.fhir.client.util.WildfhirClientException;
import net.aegis.fhir.model.Clientresource;
import net.aegis.fhir.model.Constants;
import net.aegis.fhir.model.LabelKeyValueBean;
import net.aegis.fhir.model.ResourceType;
import net.aegis.fhir.model.Serverdirectory;
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
	 * Perform a FHIR read for the supplied resource id
	 *
	 * @return
	 */
	public void fhirRead(ActionEvent event) {
		log.fine("[START] ApplicationController.fhirRead()");

		try {
			log.info("BasePath for FHIR read: " + context.getSelectedServerURL());
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
						log.info(e.getMessage());
						FacesContext.getCurrentInstance().addMessage("tabView:interactionsTabView:fhirReadForm",
								new FacesMessage(FacesMessage.SEVERITY_ERROR, "Resource parsing failed! Please check the client logs.", "Resource parsing failed! Please check the client logs."));
						e.printStackTrace();
					}

				}
				else if (resourceResponse.getStatus() == (Response.Status.NOT_MODIFIED.getStatusCode())) {
					log.info(Integer.toString(resourceResponse.getStatus()));
					FacesContext.getCurrentInstance().addMessage("tabView:interactionsTabView:fhirReadForm", new FacesMessage(FacesMessage.SEVERITY_INFO, "RESOURCE " + resourceId + " NOT MODIFIED", "RESOURCE " + resourceId + " NOT MODIFIED"));
				}
				else {
					log.info(Integer.toString(resourceResponse.getStatus()));
					FacesContext.getCurrentInstance().addMessage("tabView:interactionsTabView:fhirReadForm", new FacesMessage(FacesMessage.SEVERITY_INFO, "Response " + resourceResponse.getStatus() + " - No Resource found matching ID " + resourceId, "Response " + resourceResponse.getStatus() + " - No Resource found matching ID " + resourceId));
				}
			}
		}
		catch (Exception e) {
			log.info(e.getMessage());
			FacesContext.getCurrentInstance().addMessage("tabView:interactionsTabView:fhirReadForm", new FacesMessage(FacesMessage.SEVERITY_ERROR, "Error reading resource! Please check the client logs.", "Error reading resource! Please check the client logs."));
			e.printStackTrace();
		}

		log.fine("[END] ApplicationController.fhirRead()");
	}

	/**
	 * Performs a FHIR history read for specified patient record in List of patient results, Updates messages for the UI
	 * form whose id is supplied
	 *
	 * @param event
	 */
	public void fhirHistory(ActionEvent event) {
		log.fine("[START] ApplicationController.fhirHistory()");
		log.info("BasePath for FHIR history: " + context.getSelectedServerURL());

		String formatType = context.getSelectedFormatType();

		String _format = context.get_format();
		String _count = context.get_count();
		String _since = context.get_since();

		if (!StringUtils.isNullOrEmpty(_since)) {
			try {
				utcDateUtil.parseXMLDate(_since);
				log.info("fhirHistory _since = " + _since);
			}
			catch (Exception e) {
				log.severe("Exception parsing _since parameter to UTC Date! " + e.getMessage());
				FacesContext.getCurrentInstance().addMessage("tabView:homeTabView:historyForm",
						new FacesMessage(FacesMessage.SEVERITY_ERROR, "_since parameter is not a UTC Date! Example '2015-12-09T15:53:18Z'.", "_since parameter is not a UTC Date! Example '2015-12-09T15:53:18Z'."));
				return;
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
		log.info("BasePath for FHIR delete: " + context.getSelectedServerURL());

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
							"tabView:homeTabView:historyForm",
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
		log.info("BasePath for FHIR search: " + context.getSelectedServerURL());
		log.info("Search Criteria: ");

		Map<String, String> criteriaToSend = new HashMap<String, String>();

		for (LabelKeyValueBean lkvb : context.getResourceCriteria()) {
			if (!lkvb.getValue().isEmpty()) {
				log.info(lkvb.getKey() + " = " + lkvb.getValue());
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
			log.info(e1.getMessage());
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
					log.info(e1.getMessage());
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
		log.info("BasePath for FHIR delete: " + context.getSelectedServerURL());

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
					log.info(e1.getMessage());
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
			log.info("No Resource found matching ID: " + context.getResourceId());
			String form = "tabView:interactionsTabView:" + operation + "Form";
			FacesContext.getCurrentInstance().addMessage(form, new FacesMessage(FacesMessage.SEVERITY_WARN, "No Resource found matching ID: " + context.getResourceId(), ""));
			context.setResourceString(null);
		}

		log.info("resource id: " + context.getResourceId());
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
	 * Execute the $validate operation
	 *
	 * @param event
	 */
	public void fhirValidate(ActionEvent event) {
		log.fine("[START] ApplicationController.fhirValidate()");
		log.info("BasePath for FHIR validate: " + context.getSelectedServerURL());

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
				FacesContext.getCurrentInstance().addMessage("tabView:interacionsTabView:validateForm", new FacesMessage(FacesMessage.SEVERITY_ERROR, "Validate operation requires input contained in a Parameters resource type.", "Validate operation requires input contained in a Parameters resource type."));
			}
		}
		catch (NumberFormatException e) {
			validateExceptionOutcomeString = ServicesUtil.INSTANCE.getOperationOutcome(OperationOutcome.IssueSeverity.FATAL, OperationOutcome.IssueType.EXCEPTION, e.getMessage(), null, null, formatType.toLowerCase());
			FacesContext.getCurrentInstance().addMessage("tabView:interacionsTabView:validateForm",
					new FacesMessage(FacesMessage.SEVERITY_ERROR, "Number format error validating resource request! Please check the client logs.", "Error reading resource! Please check the client logs."));
			e.printStackTrace();
		}
		catch (Exception e) {
			validateExceptionOutcomeString = ServicesUtil.INSTANCE.getOperationOutcome(OperationOutcome.IssueSeverity.FATAL, OperationOutcome.IssueType.EXCEPTION, e.getMessage(), null, null, formatType.toLowerCase());
			FacesContext.getCurrentInstance().addMessage("tabView:interacionsTabView:validateForm",
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

				FacesContext.getCurrentInstance().addMessage("tabView:interacionsTabView:validateForm", new FacesMessage(FacesMessage.SEVERITY_INFO, "Resource validate complete.", "Resource validate complete."));
			}
			catch (Exception e) {
				FacesContext.getCurrentInstance().addMessage("tabView:interacionsTabView:validateForm",
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

				FacesContext.getCurrentInstance().addMessage("tabView:interacionsTabView:validateForm", new FacesMessage(FacesMessage.SEVERITY_INFO, "Resource validate operation errors found.", "Resource validate operation errors found."));
			}
			catch (Exception e) {
				FacesContext.getCurrentInstance().addMessage("tabView:interacionsTabView:validateForm",
						new FacesMessage(FacesMessage.SEVERITY_ERROR, "Error reporting validate operations errors! Please check the client logs.", "Error reporting validate operations errors! Please check the client logs."));
				e.printStackTrace();
			}
		}
		else {
			FacesContext.getCurrentInstance().addMessage("tabView:interacionsTabView:validateForm", new FacesMessage(FacesMessage.SEVERITY_ERROR, "Validate operation did not report any results.", "Validate operation did not report any results."));
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
		log.info("BasePath for FHIR metadata " + context.getSelectedServerURL());

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
			log.info(e.getMessage());
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
		log.info("BasePath for FHIR metadata (new page) " + context.getSelectedServerURL());

		String conformanceUrl = context.getSelectedServerURL() + "/metadata";

		try {
			FacesContext.getCurrentInstance().getExternalContext().redirect(conformanceUrl);
		}
		catch (IOException e) {
			log.severe(e.getMessage());
			FacesContext.getCurrentInstance().addMessage(null, new FacesMessage(FacesMessage.SEVERITY_ERROR, "Error accessing server's metadata endpoint! " + e.getMessage(), ""));
		}

		log.fine("[END] ApplicationController.fhirMetadataNewPage");
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
			RelatedPerson grantee = (RelatedPerson) context.getClientresourceService().readFHIRResource(Integer.valueOf(clientRelatedPersonId));
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

			codeableConcept = new CodeableConcept();
			coding = new Coding();
			coding.setSystem("http://terminology.hl7.org/CodeSystem/consentpolicycodes");
			coding.setCode("hipaa-auth");
			codeableConcept.addCoding(coding);
			consent.setPolicyRule(codeableConcept);

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
			sb.append(consent.getProvision().getType().toCode());
			sb.append(" ");
			sb.append(grantee.getNameFirstRep().getNameAsSingleString());
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
					FacesContext.getCurrentInstance().addMessage("tabView:operationsTabView:fileConsentForm", new FacesMessage(FacesMessage.SEVERITY_INFO, "$fileConsent request successfully processed.", ""));
				}
				else {
					FacesContext.getCurrentInstance().addMessage("tabView:operationsTabView:fileConsentForm", new FacesMessage(FacesMessage.SEVERITY_ERROR, "$fileConsent response failure [" + wrapper.getResponseStatus() + "].", ""));

					consent.setStatus(ConsentState.INACTIVE);
				}
			}
			else {
				context.setResponseString("ERROR: Response is empty!");
				FacesContext.getCurrentInstance().addMessage("tabView:operationsTabView:fileConsentForm",
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
			FacesContext.getCurrentInstance().addMessage("tabView:operationsTabView:fileConsentForm",
					new FacesMessage(FacesMessage.SEVERITY_ERROR, "Error processing $fileConsent! " + e.getMessage(), ""));
		}
		catch (Exception e) {
			FacesContext.getCurrentInstance().addMessage("tabView:operationsTabView:fileConsentForm",
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
			FacesContext.getCurrentInstance().addMessage("tabView:operationsTabView:fileConsentForm",
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
				// Populate record-disclosure Consent list for selected Patient
				context.setClientPatientConsents(patient);
			}
		}
		catch (Exception e) {
			FacesContext.getCurrentInstance().addMessage("tabView:operationsTabView:recordDisclosureForm",
					new FacesMessage(FacesMessage.SEVERITY_ERROR, "Error processing $record-disclosure handle Patient change! Please check the client logs. " + e.getMessage(), ""));
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
			log.info("BasePath for $fileConsent: " + context.getSelectedServerURL());
			Response response = null;

			String clientConsentId = context.getSelectedConsentId();

			StringBuilder sb = new StringBuilder();
			sb.append("Consent id: ").append(clientConsentId);

			context.setResourceString(sb.toString());
			context.setResponseString("TBD");

			Consent consent = (Consent) context.getClientresourceService().readFHIRResource(Integer.valueOf(clientConsentId));

			// Build revokeConsent Parameters with Consent and DocumentReference
			Parameters p = new Parameters();
			p.setId(UUIDUtil.getUUID());
			Meta meta = new Meta();
			meta.addProfile("http://hl7.org/fhir/us/consent-management/StructureDefinition/RevokeConsentParameters");
			p.setMeta(meta);

			// consent parameter
			ParametersParameterComponent param = new ParametersParameterComponent();
			param.setName("consent");

			// Build consent Reference parameter value
			Reference reference = new Reference();
			reference.setReference("Consent/" + consent.getId());
			if (consent.hasIdentifier()) {
				reference.setIdentifier(consent.getIdentifierFirstRep());
			}
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
			// Get Consent grantee (RelatedPersion)
			String granteeId = ServicesUtil.INSTANCE.extractResourceIdFromURL(consent.getProvision().getActorFirstRep().getReference().getReference());
			RelatedPerson grantee = (RelatedPerson) context.getClientresourceService().readFHIRResource("RelatedPerson", granteeId);

			DocumentReferenceContentComponent content = new DocumentReferenceContentComponent();
			Attachment attachment = new Attachment();
			attachment.setContentType("text/plain" + Constants.CHARSET_UTF8_EXT);
			sb = new StringBuilder("I, ");
			sb.append(grantor.getNameFirstRep().getNameAsSingleString());
			sb.append(", revoke my consent to ");
			sb.append(consent.getProvision().getType().toCode());
			sb.append(" ");
			sb.append(grantee.getNameFirstRep().getNameAsSingleString());
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
				ResourceResponseWrapper wrapper = new ResourceResponseWrapper(response);

				if (formatType.equals("JSON")) {
					context.setResponseString(wrapper.getResourceJSON());
				}
				else {
					context.setResponseString(wrapper.getResourceXML());
				}

				if (wrapper.getResponseStatus() < 400) {
					FacesContext.getCurrentInstance().addMessage("tabView:operationsTabView:revokeConsentForm", new FacesMessage(FacesMessage.SEVERITY_INFO, "$revokeConsent request successfully processed.", ""));

					consent.setStatus(ConsentState.REJECTED);
				}
				else {
					FacesContext.getCurrentInstance().addMessage("tabView:operationsTabView:revokeConsentForm", new FacesMessage(FacesMessage.SEVERITY_ERROR, "$revokeConsent response failure [" + wrapper.getResponseStatus() + "].", ""));

					consent.setStatus(ConsentState.INACTIVE);
				}
			}
			else {
				context.setResponseString("ERROR: Response is empty!");
				FacesContext.getCurrentInstance().addMessage("tabView:operationsTabView:revokeConsentForm",
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
			String formatType = context.getSelectedFormatType();
			log.info("Selected Format Type: " + formatType);
			log.info("BasePath for $record-disclosure: " + context.getSelectedServerURL());
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

			// Build revokeConsent Parameters with Consent and DocumentReference
			Parameters p = new Parameters();
			p.setId(UUIDUtil.getUUID());
			Meta meta = new Meta();
			meta.addProfile("http://hl7.org/fhir/us/consent-management/StructureDefinition/RecordDisclosureParameters");
			p.setMeta(meta);

			// disclosure parameter
			ParametersParameterComponent param = new ParametersParameterComponent();
			param.setName("disclosure");

			AuditEvent disclosure = new AuditEvent();
			String disclosureId = UUIDUtil.getUUID();
			disclosure.setId(disclosureId);
			meta = new Meta();
			meta.addProfile("http://hl7.org/fhir/us/consent-management/StructureDefinition/FASTConsentAuditEvent");
			disclosure.setMeta(meta);

			// POPULATE AUDITEVENT ELEMENTS
			//type (fixed)
			Coding coding = new Coding();
			coding.setSystem("http://dicom.nema.org/resources/ontology/DCM");
			coding.setCode("110106");
			coding.setDisplay("Export");
			disclosure.setType(coding);

			//subtype (choice - restful get interactions)
			coding = new Coding();
			coding.setSystem("http://hl7.org/fhir/restful-interaction");
			coding.setCode(fhirInteraction);
			disclosure.addSubtype(coding);

			//action (fixed)
			disclosure.setAction(AuditEventAction.R);

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

			//agent
			AuditEventAgentComponent agent = new AuditEventAgentComponent();

			//agent.who (selected patient)
			Reference reference = new Reference();
			reference.setIdentifier(patient.getIdentifierFirstRep());
			agent.setWho(reference);

			//agent.requestor (true)
			agent.setRequestor(true);

			disclosure.addAgent(agent);

			//source
			AuditEventSourceComponent source = new AuditEventSourceComponent();

			//source.observer (fixed - consent-client-ri-site)
			reference = new Reference();
			Identifier identifier = new Identifier();
			identifier.setSystem("http://example.org/identifiers");
			identifier.setValue("consent-client-ri-site");
			reference.setIdentifier(identifier);
			source.setObserver(reference);

			//source.type (fixed - 3 Web Server)
			coding = new Coding();
			coding.setSystem("http://hl7.org/fhir/security-source-type");
			coding.setCode("3");
			coding.setDisplay("Web Server");
			source.addType(coding);

			disclosure.setSource(source);

			//entity
			AuditEventEntityComponent entity = new AuditEventEntityComponent();

			//entity.what (selected Consent)
			reference = new Reference();
			reference.setIdentifier(consent.getIdentifierFirstRep());
			entity.setWhat(reference);

			//entity.type (Consent)
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

			param.setResource(disclosure);

			p.addParameter(param);

			// consent parameter
			param = new ParametersParameterComponent();
			param.setName("consent");

			// Build consent Reference parameter value
			reference = new Reference();
			reference.setReference("Consent/" + consent.getId());
			if (consent.hasIdentifier()) {
				reference.setIdentifier(consent.getIdentifierFirstRep());
			}
			param.setValue(reference);

			p.addParameter(param);

			// Send $record-disclosure request
			JsonParser jsonParser = new JsonParser();
			jsonParser.setOutputStyle(OutputStyle.PRETTY);
			XmlParser xmlParser = new XmlParser();
			xmlParser.setOutputStyle(OutputStyle.PRETTY);
			ByteArrayOutputStream oOp = new ByteArrayOutputStream();
			if (formatType.equals("JSON")) {
				jsonParser.compose(oOp, p);

				context.setResourceString(oOp.toString());

				response = context.getResourceOperationClient().resourceOperation(p, null, context.getSelectedServerURL(), "AuditEvent", Constants.FHIR_JSON_CONTENT, Constants.FHIR_JSON_CONTENT, null, "record-disclosure", null, null);
			}
			else {
				xmlParser.compose(oOp, p, true);

				context.setResourceString(oOp.toString());

				response = context.getResourceOperationClient().resourceOperation(p, null, context.getSelectedServerURL(), "AuditEvent", Constants.FHIR_XML_CONTENT, Constants.FHIR_XML_CONTENT, null, "record-disclosure", null, null);
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
					FacesContext.getCurrentInstance().addMessage("tabView:operationsTabView:recordDisclosureForm", new FacesMessage(FacesMessage.SEVERITY_INFO, "$record-disclosure request successfully processed.", ""));

					consent.setStatus(ConsentState.REJECTED);
				}
				else {
					FacesContext.getCurrentInstance().addMessage("tabView:operationsTabView:recordDisclosureForm", new FacesMessage(FacesMessage.SEVERITY_ERROR, "$record-disclosure response failure [" + wrapper.getResponseStatus() + "].", ""));

					consent.setStatus(ConsentState.INACTIVE);
				}
			}
			else {
				context.setResponseString("ERROR: Response is empty!");
				FacesContext.getCurrentInstance().addMessage("tabView:operationsTabView:recordDisclosureForm",
					new FacesMessage(FacesMessage.SEVERITY_ERROR, "Error processing $record-disclosure! Response is empty.", ""));
			}
		}
		catch (Exception e) {
			FacesContext.getCurrentInstance().addMessage("tabView:operationsTabView:recordDisclosureForm",
					new FacesMessage(FacesMessage.SEVERITY_ERROR, "Error processing $recordDisclosure! Please check the client logs.", "Error processing $record-disclosure! Please check the client logs."));

			context.setResourceString(e.getMessage());
			
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
							if (createServerResource(serverFile, localServer)) {
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
				FacesContext.getCurrentInstance().addMessage("tabView:operationsTabView:fileConsentForm",
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

		log.fine("[END] ResourceTemplateService.writeResourceTemplate()");

		return bResult;
	}

	private boolean createServerResource(File serverFile, Serverdirectory localServer) {

		boolean bResult = false;
		String serverFileName = "";

		try {
			serverFileName = serverFile.getName();

			log.fine("[START] ApplicationController.createServerResource() for " + serverFileName);

			// Read contents of serverFileName and create in RI repository
			String serverContents = stringBuilder(serverFile.getPath());

			org.hl7.fhir.r5.formats.JsonParser jsonParser = new org.hl7.fhir.r5.formats.JsonParser();
			ByteArrayInputStream iResource = new ByteArrayInputStream(serverContents.getBytes());
			org.hl7.fhir.r5.model.Resource resource = jsonParser.parse(iResource);

			Response response = context.getResourceRESTClient().updateR5(resource.getId(), resource, localServer.getBasePath(), resource.getResourceType().name(), Constants.FHIR_JSON_CONTENT, null, null, null, null, null);

			ResourceResponseWrapper wrapper = new ResourceResponseWrapper(response);

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

		log.fine("[END] ResourceTemplateService.createServerResource()");

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

			subscription.setStatus(SubscriptionStatus.ACTIVE);

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

	public void handleUpdateSubscriptionChange() throws Exception {
		try {
			// Get selected subscription resource
			String subscriptionConsentId = context.getSelectedSubscriptionId();
			Subscription subscription = (Subscription) context.getClientresourceService().readFHIRResource(Integer.valueOf(subscriptionConsentId));

			if (subscription != null) {
				// Populate updateSubscription values that can be changed
				// We know these values exist because we populated them when performing fileConsent
				String selectedSubscriptionStatus = subscription.getStatus().toCode();
				context.setSelectedSubscriptionStatus(selectedSubscriptionStatus);

				Date endDate = subscription.getEnd();
				context.setEndDate(endDate);

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
			}
		}
		catch (Exception e) {
			FacesContext.getCurrentInstance().addMessage("tabView:subscriptionsTabView:updateSubscriptionForm",
					new FacesMessage(FacesMessage.SEVERITY_ERROR, "Error processing Update Scription handle Scription change! Please check the client logs. " + e.getMessage(), ""));
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
