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

import net.aegis.fhir.model.Clientresource;
import net.aegis.fhir.model.LabelKeyValueBean;
import net.aegis.fhir.model.Serverdirectory;
import net.aegis.fhir.service.ClientresourceService;
import net.aegis.fhir.service.CodeService;
import net.aegis.fhir.service.ServerdirectoryService;
import net.aegis.fhir.service.client.ResourceOperationRESTClient;
import net.aegis.fhir.service.client.ResourceRESTClient;
import net.aegis.fhir.service.subscription.SubscriptionServiceR5;

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

	private @Inject SubscriptionServiceR5 subscriptionServiceR5;

	private ResourceRESTClient resourceRESTClient;

	private ResourceOperationRESTClient resourceOperationClient;

	private Serverdirectory selectedServer;

	// stores details of new server to be created
	private Serverdirectory newServer;

	// list of available servers
	private List<Serverdirectory> availableServers;

	// URL for webservice that needs to be called
	private String selectedServerURL;

	// a lot of the contextual variables are shared across views and
	// UI components are generated made visible based on the view.
	// This ensures that only components for the current view are enabled
	private String currentView;

	private String resourceString;

	private String resource2String;

	private String responseString;

	Map<String, String> criteriaMap;

	// LabelKeyValueBean list
	private List<LabelKeyValueBean> listLabelKeyValue;

	private Date datePicker;

	private String selectedFormatType;

	private List<String> formatTypes;

	// Consent operations
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
		this.newServer = new Serverdirectory();
		this.currentView = "";
		this.resourceString = null;
		this.resource2String = null;
		this.responseString = null;
		this.criteriaMap = new HashMap<String, String>();
		this.datePicker = null;
		try {
			this.availableServers = serverDirectoryService.findAll();
			this.clientPatients = clientresourceService.findClientresourceByResourceType("Patient");
			this.clientRelatedPersons = clientresourceService.findClientresourceByResourceType("RelatedPerson");
			this.clientOrganizations = clientresourceService.findClientresourceByResourceType("Organization");
			listProvisionTypes = new ArrayList<LabelKeyValueBean>();
			listProvisionTypes.add(new LabelKeyValueBean("Opt In", "permit", "Opt In"));
			listProvisionTypes.add(new LabelKeyValueBean("Opt Out", "deny", "Opt Out"));
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
		this.criteriaMap = new HashMap<String, String>();
		this.datePicker = null;
		try {
			this.availableServers = serverDirectoryService.findAll();
			this.clientPatients = clientresourceService.findClientresourceByResourceType("Patient");
			this.clientRelatedPersons = clientresourceService.findClientresourceByResourceType("RelatedPerson");
			this.clientOrganizations = clientresourceService.findClientresourceByResourceType("Organization");
			listProvisionTypes = new ArrayList<LabelKeyValueBean>();
			listProvisionTypes.add(new LabelKeyValueBean("Opt In", "permit", "Opt In"));
			listProvisionTypes.add(new LabelKeyValueBean("Opt Out", "deny", "Opt Out"));
		}
		catch (Exception e) {
			e.printStackTrace();
		}
	}

	public ClientresourceService getClientresourceService() {
		return clientresourceService;
	}

	public void setClientresourceService(ClientresourceService clientresourceService) {
		this.clientresourceService = clientresourceService;
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
