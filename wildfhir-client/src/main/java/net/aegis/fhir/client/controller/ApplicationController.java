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
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.logging.Logger;

import javax.faces.application.FacesMessage;
import javax.faces.bean.ManagedBean;
import javax.faces.bean.ManagedProperty;
import javax.faces.bean.SessionScoped;
import javax.faces.context.FacesContext;
import javax.faces.event.ActionEvent;
import javax.inject.Inject;

import org.primefaces.event.TabChangeEvent;

import net.aegis.fhir.client.ApplicationContext;
import net.aegis.fhir.model.LabelKeyValueBean;
import net.aegis.fhir.model.Serverdirectory;
import net.aegis.fhir.service.util.UTCDateUtil;

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
	}

	public ApplicationContext getContext() {
		return context;
	}

	public void setContext(ApplicationContext context) {
		this.context = context;
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
					FacesContext.getCurrentInstance().addMessage(null, new FacesMessage(FacesMessage.SEVERITY_INFO, "Server: " + server.getName() + " updated.", ""));
				}
			}
			catch (Exception e) {
				log.severe(e.getMessage());
				FacesContext.getCurrentInstance().addMessage(null, new FacesMessage(FacesMessage.SEVERITY_ERROR, "Error updating Server.", ""));
			}
		}

		if (serverType.equalsIgnoreCase("new")) {
			log.info("[Start] ApplicationController.manageServer() - Create new Server");
			try {
				Serverdirectory server = context.getServerDirectoryService().create(context.getNewServer());
				if (server != null) {
					FacesContext.getCurrentInstance().addMessage(null, new FacesMessage(FacesMessage.SEVERITY_INFO, "Server: " + server.getName() + " successfully created.", ""));
					context.setNewServer(new Serverdirectory());
					context.setAvailableServers(context.getServerDirectoryService().findAll());
				}
			}
			catch (Exception e) {
				log.severe(e.getMessage());
				FacesContext.getCurrentInstance().addMessage(null, new FacesMessage(FacesMessage.SEVERITY_ERROR, "Error creating new Server.", ""));
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
				FacesContext.getCurrentInstance().addMessage(null, new FacesMessage(FacesMessage.SEVERITY_INFO, "Server with ID " + serverId + " successfully deleted.", ""));
				context.setAvailableServers(context.getServerDirectoryService().findAll());
			}
			else {
				throw new Exception("Error deleting Server");
			}
		}
		catch (NumberFormatException e) {
			log.severe(e.getMessage());
			FacesContext.getCurrentInstance().addMessage(null, new FacesMessage(FacesMessage.SEVERITY_ERROR, "Number format error deleting Server with ID " + serverId, ""));
		}
		catch (Exception e) {
			log.severe(e.getMessage());
			FacesContext.getCurrentInstance().addMessage(null, new FacesMessage(FacesMessage.SEVERITY_ERROR, "Error deleting Server with ID " + serverId, ""));
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

		Map<String, String> criteriaToSend = new HashMap<String, String>();

		for (Entry<String, String> e : context.getCriteriaMap().entrySet()) {
			log.info(e.getKey() + " = " + e.getValue());
			if (e.getValue() != null && !e.getValue().isEmpty()) {
				criteriaToSend.put(e.getKey(), e.getValue());
			}
		}

		List<LabelKeyValueBean> results = null;

		try {
			if (context.getCodeService().isSupported("subscriptionServiceEnabled")) {
				String sinceDateTime = criteriaToSend.get("sinceDateTime");

				if (sinceDateTime == null) {

					FacesContext.getCurrentInstance().addMessage("tabView:subscriptionClientTab:subscriptionClientForm",
							new FacesMessage(FacesMessage.SEVERITY_WARN, "Missing Criteria - please enter Since DateTime.", "Missing Criteria - please enter Since DateTime."));

				}
				else {
					Date since = utcDateUtil.parseDate(sinceDateTime, UTCDateUtil.DATE_PARAMETER_FORMAT, null);

					results = context.getSubscriptionServiceR5().processSubscriptions(since);

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
