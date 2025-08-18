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
package net.aegis.fhir.client;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import javax.annotation.PostConstruct;
import javax.faces.bean.ManagedBean;
import javax.faces.bean.SessionScoped;
import javax.inject.Inject;

import org.hl7.fhir.r4.model.OperationOutcome;
import org.hl7.fhir.r4.model.Patient;

import net.aegis.fhir.client.model.ResourceResponseWrapper;
import net.aegis.fhir.model.Clientresource;
import net.aegis.fhir.model.LabelKeyValueBean;
import net.aegis.fhir.model.OperationOutcomeWrapper;
import net.aegis.fhir.model.ResourceType;
import net.aegis.fhir.model.Serverdirectory;
import net.aegis.fhir.model.Subscriptionactivity;
import net.aegis.fhir.service.ClientresourceService;
import net.aegis.fhir.service.CodeService;
import net.aegis.fhir.service.ServerdirectoryService;
import net.aegis.fhir.service.SubscriptionactivityService;
import net.aegis.fhir.service.client.ConformanceResourceRESTClient;
import net.aegis.fhir.service.client.ResourceOperationRESTClient;
import net.aegis.fhir.service.client.ResourceRESTClient;
import net.aegis.fhir.service.subscription.r5.SubscriptionServiceR5;

/**
 * <p>
 * This class defines the context for the application. It is injected into controllers to expose attributes and services
 * available.
 * </p>
 *
 * @author richard.ettema
 *
 */
@ManagedBean(name = "context", eager = true)
@SessionScoped
public class ApplicationContext implements Serializable {

	private static final long serialVersionUID = 3579939563564600852L;

	private @Inject Logger log;

	private @Inject ClientresourceService clientresourceService;

	private @Inject CodeService codeService;

	private @Inject ServerdirectoryService serverDirectoryService;

	private @Inject SubscriptionactivityService subscriptionactivityService;

	private @Inject SubscriptionServiceR5 subscriptionServiceR5;

	private ResourceRESTClient resourceRESTClient;

	private ResourceOperationRESTClient resourceOperationClient;

	private ConformanceResourceRESTClient conformanceResourceRESTClient;

	private String resourceId;
	private String resourceVersion;

	// Conditional options passed as HTTP Headers
	private String ifMatch;
	private String ifModifiedSince;
	private String ifNoneExist;
	private String ifNoneMatch;
	private String prefer;
	private String updateQuery;

	// Conditional options passed as URL parameters
	private String _count;
	private String _format;
	private String _since;
	private String _summary;

	private List<String> summaryTypes;

	private List<String> summarySearchTypes;

	// Paging parameters
	private List<LabelKeyValueBean> pageReference;

	private String returnedFormatType;

	private Serverdirectory selectedServer;

	// stores details of new server to be created
	private Serverdirectory newServer;

	// list of available servers
	private List<Serverdirectory> availableServers;

	// URL for webservice that needs to be called
	private String selectedServerURL;

	// a lot of the contextual variables are shared across views and
	// UI components are generally made visible based on the view.
	// This ensures that only components for the current view are enabled
	private String currentView;

	private String resourceString;

	private String resource2String;

	private String responseString;

	private String selectedResourceType;

	// List of resource type specific criteria
	private List<LabelKeyValueBean> resourceCriteria;

	// result lists
	private List<ResourceResponseWrapper> resourceResults;

	Map<String, String> criteriaMap;

	// LabelKeyValueBean list
	private List<LabelKeyValueBean> listLabelKeyValue;

	private Date datePicker;

	private String selectedFormatType;

	private List<String> formatTypes;

	private String selectedHttpOperation;

	private String selectedFhirInteraction;

	private List<String> fhirInteractions;

	private OperationOutcomeWrapper validateOperationOutcome;

	// Subscription Activity
	private List<Subscriptionactivity> subscriptionactivity;

	private List<Clientresource> clientSubscriptions;
	private String selectedSubscriptionId;

	private List<LabelKeyValueBean> listSubscriptionStatuses;
	private String selectedSubscriptionStatus;

	private String subscriptionReason;
	private String subscriptionCriteria;
	private String subscriptionEndpoint;

	private List<LabelKeyValueBean> listSubscriptionPayloadTypes;
	private String selectedSubscriptionPayloadType;
	private List<LabelKeyValueBean> listSubscriptionContentTypes;
	private String selectedSubscriptionContentType;

	// Consent operations
	private List<Clientresource> clientConsents;
	private String selectedConsentId;
	private List<Clientresource> clientPatientConsents;
	private String selectedPatientConsentId;
	private List<Clientresource> clientPatients;
	private String selectedPatientId;
	private List<Clientresource> clientRelatedPersons;
	private String selectedRelatedPersonId;
	private List<Clientresource> clientOrganizations;
	private String selectedOrganizationId;
	private List<LabelKeyValueBean> listProvisionTypes;
	private String selectedProvisionType;
	private Date endDate;
	private Date startDate;

	public ApplicationContext() {

	}

	@PostConstruct
	public void init() {
		log.fine("[START] - ApplicationContext.init()");
		this.resourceRESTClient = new ResourceRESTClient(codeService);
		this.resourceOperationClient = new ResourceOperationRESTClient(codeService);
		this.conformanceResourceRESTClient = new ConformanceResourceRESTClient(codeService);
		this.newServer = new Serverdirectory();
		this.currentView = "";
		this.selectedHttpOperation = "GET";
		this.resourceString = null;
		this.resource2String = null;
		this.responseString = null;
		this.resourceCriteria = null;
		this.criteriaMap = new HashMap<String, String>();
		this.resourceId = "";
		this.resourceVersion = "";
		this.ifModifiedSince = "";
		this.ifNoneExist = "";
		this.ifNoneMatch = "";
		this.prefer = "";
		this.updateQuery = "";
		this._count = "";
		this._format = "";
		this._since = "";
		this._summary = "";
		this.pageReference = null;
		this.resourceResults = null;
		this.datePicker = null;
		this.validateOperationOutcome = null;
		this.selectedSubscriptionId = null;
		this.selectedSubscriptionStatus = null;
		this.subscriptionReason = null;
		this.subscriptionCriteria = null;
		this.subscriptionEndpoint = null;
		this.selectedSubscriptionPayloadType = null;
		this.selectedSubscriptionContentType = null;
		this.selectedConsentId = null;
		this.selectedPatientConsentId = null;
		this.selectedPatientId = null;
		this.selectedRelatedPersonId = null;
		this.selectedOrganizationId = null;
		this.selectedProvisionType = null;
		this.endDate = null;
		this.startDate = null;
		try {
			this.clientSubscriptions = clientresourceService.findClientresourceByResourceType("Subscription");
			this.clientConsents = clientresourceService.findClientresourceByResourceType("Consent");
			this.clientPatients = clientresourceService.findClientresourceByResourceType("Patient");
			this.clientRelatedPersons = new ArrayList<Clientresource>();
			this.clientOrganizations = clientresourceService.findClientresourceByResourceType("Organization");
		}
		catch (Exception e) {
			e.printStackTrace();
		}
	}

	public void clear() {
		log.fine("[START] - ApplicationContext.clear()");
		this.newServer = new Serverdirectory();
		this.currentView = "";
		this.resourceString = null;
		this.resource2String = null;
		this.responseString = null;
		this.resourceCriteria = null;
		this.criteriaMap = new HashMap<String, String>();
		this.resourceId = "";
		this.resourceVersion = "";
		this.ifModifiedSince = "";
		this.ifNoneExist = "";
		this.ifNoneMatch = "";
		this.prefer = "";
		this.updateQuery = "";
		this._count = "";
		this._format = "";
		this._since = "";
		this._summary = "";
		this.pageReference = null;
		this.resourceResults = null;
		this.datePicker = null;
		this.validateOperationOutcome = null;
		this.selectedSubscriptionId = null;
		this.selectedSubscriptionStatus = null;
		this.subscriptionReason = null;
		this.subscriptionCriteria = null;
		this.subscriptionEndpoint = null;
		this.selectedSubscriptionPayloadType = null;
		this.selectedSubscriptionContentType = null;
		this.selectedConsentId = null;
		this.selectedPatientConsentId = null;
		this.selectedPatientId = null;
		this.selectedRelatedPersonId = null;
		this.selectedOrganizationId = null;
		this.selectedProvisionType = null;
		this.endDate = null;
		this.startDate = null;
		try {
			this.clientSubscriptions = clientresourceService.findClientresourceByResourceType("Subscription");
			this.clientConsents = clientresourceService.findClientresourceByResourceType("Consent");
			this.clientPatients = clientresourceService.findClientresourceByResourceType("Patient");
			this.clientOrganizations = clientresourceService.findClientresourceByResourceType("Organization");
		}
		catch (Exception e) {
			e.printStackTrace();
		}
	}

	public List<String> getSummaryTypes() {
		if (summaryTypes == null) {
			summaryTypes = new ArrayList<String>();
			summaryTypes.add("true");
			summaryTypes.add("false");
			summaryTypes.add("text");
			summaryTypes.add("data");
		}
		return summaryTypes;
	}

	public List<String> getSummarySearchTypes() {
		if (summarySearchTypes == null) {
			summarySearchTypes = new ArrayList<String>();
			summarySearchTypes.add("true");
			summarySearchTypes.add("false");
			summarySearchTypes.add("text");
			summarySearchTypes.add("data");
			summarySearchTypes.add("count");
		}
		return summarySearchTypes;
	}

	public List<String> getResourceTypes() {
		return ResourceType.getResourceTypes();
	}

	public List<String> getSupportedResourceTypes() {
		return ResourceType.getSupportedResourceTypes();
	}

	public ClientresourceService getClientresourceService() {
		return clientresourceService;
	}

	public void setClientresourceService(ClientresourceService clientresourceService) {
		this.clientresourceService = clientresourceService;
	}

	public ConformanceResourceRESTClient getConformanceResourceRESTClient() {
		return conformanceResourceRESTClient;
	}

	public void setConformanceResourceRESTClient(ConformanceResourceRESTClient conformanceResourceRESTClient) {
		this.conformanceResourceRESTClient = conformanceResourceRESTClient;
	}

	public CodeService getCodeService() {
		return this.codeService;
	}

	public void setCodeService(CodeService codeService) {
		this.codeService = codeService;
	}

	public ServerdirectoryService getServerDirectoryService() {
		return serverDirectoryService;
	}

	public void setServerDirectoryService(ServerdirectoryService serverDirectoryService) {
		this.serverDirectoryService = serverDirectoryService;
	}

	public SubscriptionactivityService getSubscriptionactivityService() {
		return subscriptionactivityService;
	}

	public void setSubscriptionactivityService(SubscriptionactivityService subscriptionactivityService) {
		this.subscriptionactivityService = subscriptionactivityService;
	}

	public SubscriptionServiceR5 getSubscriptionServiceR5() {
		return subscriptionServiceR5;
	}

	public void setSubscriptionServiceR5(SubscriptionServiceR5 subscriptionServiceR5) {
		this.subscriptionServiceR5 = subscriptionServiceR5;
	}

	public ResourceRESTClient getResourceRESTClient() {
		return resourceRESTClient;
	}

	public void setResourceRESTClient(ResourceRESTClient resourceRESTClient) {
		this.resourceRESTClient = resourceRESTClient;
	}

	public ResourceOperationRESTClient getResourceOperationClient() {
		return resourceOperationClient;
	}

	public void setResourceOperationClient(ResourceOperationRESTClient resourceOperationClient) {
		this.resourceOperationClient = resourceOperationClient;
	}

	public String getResourceId() {
		return resourceId;
	}

	public void setResourceId(String resourceId) {
		this.resourceId = resourceId;
	}

	public String getResourceVersion() {
		return resourceVersion;
	}

	public void setResourceVersion(String resourceVersion) {
		this.resourceVersion = resourceVersion;
	}

	public String getIfMatch() {
		return ifMatch;
	}

	public void setIfMatch(String ifMatch) {
		this.ifMatch = ifMatch;
	}

	public String getIfModifiedSince() {
		return ifModifiedSince;
	}

	public void setIfModifiedSince(String ifModifiedSince) {
		this.ifModifiedSince = ifModifiedSince;
	}

	public String getIfNoneExist() {
		return ifNoneExist;
	}

	public void setIfNoneExist(String ifNoneExist) {
		this.ifNoneExist = ifNoneExist;
	}

	public String getIfNoneMatch() {
		return ifNoneMatch;
	}

	public void setIfNoneMatch(String ifNoneMatch) {
		this.ifNoneMatch = ifNoneMatch;
	}

	public String getPrefer() {
		return prefer;
	}

	public void setPrefer(String prefer) {
		this.prefer = prefer;
	}

	public String getUpdateQuery() {
		return updateQuery;
	}

	public void setUpdateQuery(String updateQuery) {
		this.updateQuery = updateQuery;
	}

	public String get_count() {
		return _count;
	}

	public void set_count(String _count) {
		this._count = _count;
	}

	public String get_format() {
		return _format;
	}

	public void set_format(String _format) {
		this._format = _format;
	}

	public String get_since() {
		return _since;
	}

	public void set_since(String _since) {
		this._since = _since;
	}

	public String get_summary() {
		return _summary;
	}

	public void set_summary(String _summary) {
		this._summary = _summary;
	}

	public List<LabelKeyValueBean> getPageReference() {
		if (pageReference == null) {
			pageReference = new ArrayList<LabelKeyValueBean>();
		}
		return pageReference;
	}

	public void setPageReference(List<LabelKeyValueBean> pageReference) {
		this.pageReference = pageReference;
	}

	public String getReturnedFormatType() {
		return returnedFormatType;
	}

	public void setReturnedFormatType(String returnedFormatType) {
		this.returnedFormatType = returnedFormatType;
	}

	public Serverdirectory getSelectedServer() {
		return selectedServer;
	}

	public void setSelectedServer(Serverdirectory selectedServer) {
		this.selectedServer = selectedServer;
	}

	public Serverdirectory getNewServer() {
		return newServer;
	}

	public void setNewServer(Serverdirectory newServer) {
		this.newServer = newServer;
	}

	public List<Serverdirectory> getAvailableServers() {
		try {
			availableServers = serverDirectoryService.findAll();
		}
		catch (Exception e) {
			e.printStackTrace();
		}
		return availableServers;
	}

	public void setAvailableServers(List<Serverdirectory> availableServers) {
		this.availableServers = availableServers;
	}

	public String getSelectedServerURL() {
		return selectedServerURL;
	}

	public void setSelectedServerURL(String selectedServerURL) {
		this.selectedServerURL = selectedServerURL;
	}

	public String getCurrentView() {
		return currentView;
	}

	public void setCurrentView(String currentView) {
		this.currentView = currentView;
	}

	public String getResourceString() {
		return resourceString;
	}

	public void setResourceString(String resourceString) {
		this.resourceString = resourceString;
	}

	public String getResource2String() {
		return resource2String;
	}

	public void setResource2String(String resource2String) {
		this.resource2String = resource2String;
	}

	public String getResponseString() {
		return responseString;
	}

	public void setResponseString(String responseString) {
		this.responseString = responseString;
	}

	public String getSelectedResourceType() {
		return selectedResourceType;
	}

	public void setSelectedResourceType(String selectedResourceType) {
		this.selectedResourceType = selectedResourceType;
	}

	public List<LabelKeyValueBean> getResourceCriteria() {
		return resourceCriteria;
	}

	public void setResourceCriteria(List<LabelKeyValueBean> resourceCriteria) {
		this.resourceCriteria = resourceCriteria;
	}

	public List<ResourceResponseWrapper> getResourceResults() {
		return resourceResults;
	}

	public void setResourceResults(List<ResourceResponseWrapper> resourceResults) {
		this.resourceResults = resourceResults;
	}

	public Map<String, String> getCriteriaMap() {
		return criteriaMap;
	}

	public void setCriteriaMap(Map<String, String> criteriaMap) {
		this.criteriaMap = criteriaMap;
	}

	public List<LabelKeyValueBean> getListLabelKeyValue() {
		if (listLabelKeyValue == null) {
			listLabelKeyValue = new ArrayList<LabelKeyValueBean>();
		}
		return listLabelKeyValue;
	}

	public void setListLabelKeyValue(List<LabelKeyValueBean> listLabelKeyValue) {
		this.listLabelKeyValue = listLabelKeyValue;
	}

	public Date getDatePicker() {
		return datePicker;
	}

	public void setDatePicker(Date datePicker) {
		this.datePicker = datePicker;
	}

	public String getSelectedFormatType() {
		if (selectedFormatType == null) {
			selectedFormatType = "JSON";
		}
		return selectedFormatType;
	}

	public void setSelectedFormatType(String selectedFormatType) {
		this.selectedFormatType = selectedFormatType;
	}

	public List<String> getFormatTypes() {
		if (formatTypes == null) {
			formatTypes = new ArrayList<String>();
			formatTypes.add("JSON");
			formatTypes.add("XML");
		}
		return formatTypes;
	}

	public String getSelectedHttpOperation() {
		return selectedHttpOperation;
	}

	public void setSelectedHttpOperation(String selectedHttpOperation) {
		this.selectedHttpOperation = selectedHttpOperation;
	}

	public String getSelectedFhirInteraction() {
		return selectedFhirInteraction;
	}

	public void setSelectedFhirInteraction(String selectedFhirInteraction) {
		this.selectedFhirInteraction = selectedFhirInteraction;
	}

	public List<String> getFhirInteractions() {
		if (fhirInteractions == null) {
			fhirInteractions = new ArrayList<String>();
			fhirInteractions.add("read");
			fhirInteractions.add("search");
			fhirInteractions.add("vread");
			fhirInteractions.add("history");
			fhirInteractions.add("operation");
		}
		return fhirInteractions;
	}

	public void setFhirInteractions(List<String> fhirInteractions) {
		this.fhirInteractions = fhirInteractions;
	}

	public OperationOutcomeWrapper getValidateOperationOutcome() {
		return validateOperationOutcome;
	}

	public void setValidateOperationOutcome(OperationOutcomeWrapper validateOperationOutcome) {
		this.validateOperationOutcome = validateOperationOutcome;
	}

	public void setValidateOperationOutcome(OperationOutcome validateOperationOutcome) {
		this.validateOperationOutcome = new OperationOutcomeWrapper(validateOperationOutcome);
	}

	public List<Subscriptionactivity> getSubscriptionactivity() {
		return subscriptionactivity;
	}

	public List<Subscriptionactivity> getSubscriptionactivity(String subscriptionId) {
		try {
			subscriptionactivity = subscriptionactivityService.findSubscriptionactivityById(subscriptionId);
		}
		catch (Exception e) {
			e.printStackTrace();
		}
		return subscriptionactivity;
	}

	public void setSubscriptionactivity(List<Subscriptionactivity> subscriptionactivity) {
		this.subscriptionactivity = subscriptionactivity;
	}

	public List<Clientresource> getClientSubscriptions() {
		return clientSubscriptions;
	}

	public void setClientSubscriptions(List<Clientresource> clientSubscriptions) {
		this.clientSubscriptions = clientSubscriptions;
	}

	public String getSelectedSubscriptionId() {
		return selectedSubscriptionId;
	}

	public void setSelectedSubscriptionId(String selectedSubscriptionId) {
		this.selectedSubscriptionId = selectedSubscriptionId;
	}

	public List<LabelKeyValueBean> getListSubscriptionStatuses() {
		if (listSubscriptionStatuses == null) {
			listSubscriptionStatuses = new ArrayList<LabelKeyValueBean>();
			LabelKeyValueBean lkvb = new LabelKeyValueBean("Active", "active", "active");
			listSubscriptionStatuses.add(lkvb);
			lkvb = new LabelKeyValueBean("Error", "error", "error");
			listSubscriptionStatuses.add(lkvb);
			lkvb = new LabelKeyValueBean("Requested", "requested", "requested");
			listSubscriptionStatuses.add(lkvb);
			lkvb = new LabelKeyValueBean("Off", "off", "off");
			listSubscriptionStatuses.add(lkvb);
		}
		return listSubscriptionStatuses;
	}

	public void setListSubscriptionStatuses(List<LabelKeyValueBean> listSubscriptionStatuses) {
		this.listSubscriptionStatuses = listSubscriptionStatuses;
	}

	public String getSelectedSubscriptionStatus() {
		return selectedSubscriptionStatus;
	}

	public void setSelectedSubscriptionStatus(String selectedSubscriptionStatus) {
		this.selectedSubscriptionStatus = selectedSubscriptionStatus;
	}

	public String getSubscriptionReason() {
		return subscriptionReason;
	}

	public void setSubscriptionReason(String subscriptionReason) {
		this.subscriptionReason = subscriptionReason;
	}

	public String getSubscriptionCriteria() {
		return subscriptionCriteria;
	}

	public void setSubscriptionCriteria(String subscriptionCriteria) {
		this.subscriptionCriteria = subscriptionCriteria;
	}

	public String getSubscriptionEndpoint() {
		return subscriptionEndpoint;
	}

	public void setSubscriptionEndpoint(String subscriptionEndpoint) {
		this.subscriptionEndpoint = subscriptionEndpoint;
	}

	public List<LabelKeyValueBean> getListSubscriptionPayloadTypes() {
		if (listSubscriptionPayloadTypes == null) {
			listSubscriptionPayloadTypes = new ArrayList<LabelKeyValueBean>();
			LabelKeyValueBean lkvb = new LabelKeyValueBean("FHIR JSON (application/fhir+json)", "application/fhir+json", "application/fhir+json");
			listSubscriptionPayloadTypes.add(lkvb);
			lkvb = new LabelKeyValueBean("FHIR XML (application/fhir+xml)", "application/fhir+xml", "application/fhir+xml");
			listSubscriptionPayloadTypes.add(lkvb);
		}
		return listSubscriptionPayloadTypes;
	}

	public void setListSubscriptionPayloadTypes(List<LabelKeyValueBean> listSubscriptionPayloadTypes) {
		this.listSubscriptionPayloadTypes = listSubscriptionPayloadTypes;
	}

	public String getSelectedSubscriptionPayloadType() {
		return selectedSubscriptionPayloadType;
	}

	public void setSelectedSubscriptionPayloadType(String selectedSubscriptionPayloadType) {
		this.selectedSubscriptionPayloadType = selectedSubscriptionPayloadType;
	}

	public List<LabelKeyValueBean> getListSubscriptionContentTypes() {
		if (listSubscriptionContentTypes == null) {
			listSubscriptionContentTypes = new ArrayList<LabelKeyValueBean>();
			LabelKeyValueBean lkvb = new LabelKeyValueBean("Full Resource", "full-resource", "full-resource");
			listSubscriptionContentTypes.add(lkvb);
			lkvb = new LabelKeyValueBean("Id Only", "id-only", "id-only");
			listSubscriptionContentTypes.add(lkvb);
			lkvb = new LabelKeyValueBean("Empty", "empty", "empty");
			listSubscriptionContentTypes.add(lkvb);
		}
		return listSubscriptionContentTypes;
	}

	public void setListSubscriptionContentTypes(List<LabelKeyValueBean> listSubscriptionContentTypes) {
		this.listSubscriptionContentTypes = listSubscriptionContentTypes;
	}

	public String getSelectedSubscriptionContentType() {
		return selectedSubscriptionContentType;
	}

	public void setSelectedSubscriptionContentType(String selectedSubscriptionContentType) {
		this.selectedSubscriptionContentType = selectedSubscriptionContentType;
	}

	public List<Clientresource> getClientConsents() {
		return clientConsents;
	}

	public void setClientConsents(List<Clientresource> clientConsents) {
		this.clientConsents = clientConsents;
	}

	public String getSelectedConsentId() {
		return selectedConsentId;
	}

	public void setSelectedConsentId(String selectedConsentId) {
		this.selectedConsentId = selectedConsentId;
	}

	public List<Clientresource> getClientPatientConsents() {
		return clientPatientConsents;
	}

	public void setClientPatientConsents(List<Clientresource> clientPatientConsents) {
		this.clientPatientConsents = clientPatientConsents;
	}

	public void setClientPatientConsents(Patient patient) throws Exception {
		this.clientPatientConsents = clientresourceService.findClientresourceByResourceTypePatient("Consent", patient);
	}

	public String getSelectedPatientConsentId() {
		return selectedPatientConsentId;
	}

	public void setSelectedPatientConsentId(String selectedPatientConsentId) {
		this.selectedPatientConsentId = selectedPatientConsentId;
	}

	public List<Clientresource> getClientPatients() {
		return clientPatients;
	}

	public void setClientPatients(List<Clientresource> clientPatients) {
		this.clientPatients = clientPatients;
	}

	public String getSelectedPatientId() {
		return selectedPatientId;
	}

	public void setSelectedPatientId(String selectedPatientId) {
		this.selectedPatientId = selectedPatientId;
	}

	public List<Clientresource> getClientRelatedPersons() {
		return clientRelatedPersons;
	}

	public void setClientRelatedPersons(List<Clientresource> clientRelatedPersons) {
		this.clientRelatedPersons = clientRelatedPersons;
	}

	public void setClientRelatedPersons(Patient patient) throws Exception {
		this.clientRelatedPersons = clientresourceService.findClientresourceByResourceTypePatient("RelatedPerson", patient);
	}

	public String getSelectedRelatedPersonId() {
		return selectedRelatedPersonId;
	}

	public void setSelectedRelatedPersonId(String selectedRelatedPersonId) {
		this.selectedRelatedPersonId = selectedRelatedPersonId;
	}

	public List<Clientresource> getClientOrganizations() {
		return clientOrganizations;
	}

	public void setClientOrganizations(List<Clientresource> clientOrganizations) {
		this.clientOrganizations = clientOrganizations;
	}

	public String getSelectedOrganizationId() {
		return selectedOrganizationId;
	}

	public void setSelectedOrganizationId(String selectedOrganizationId) {
		this.selectedOrganizationId = selectedOrganizationId;
	}

	public List<LabelKeyValueBean> getListProvisionTypes() {
		listProvisionTypes = new ArrayList<LabelKeyValueBean>();
		listProvisionTypes.add(new LabelKeyValueBean("Opt In", "permit", "Opt In"));
		listProvisionTypes.add(new LabelKeyValueBean("Opt Out", "deny", "Opt Out"));
		
		return listProvisionTypes;
	}

	public void setListProvisionTypes(List<LabelKeyValueBean> listProvisionTypes) {
		this.listProvisionTypes = listProvisionTypes;
	}

	public String getSelectedProvisionType() {
		return selectedProvisionType;
	}

	public void setSelectedProvisionType(String selectedProvisionType) {
		this.selectedProvisionType = selectedProvisionType;
	}

	public Date getEndDate() {
		return endDate;
	}

	public void setEndDate(Date endDate) {
		this.endDate = endDate;
	}

	public Date getStartDate() {
		return startDate;
	}

	public void setStartDate(Date startDate) {
		this.startDate = startDate;
	}

}
