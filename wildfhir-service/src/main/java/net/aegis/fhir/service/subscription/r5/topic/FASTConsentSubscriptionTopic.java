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
package net.aegis.fhir.service.subscription.r5.topic;

import java.util.logging.Logger;

import jakarta.ws.rs.core.MultivaluedMap;
import jakarta.ws.rs.core.Response;

import java.io.ByteArrayOutputStream;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.apache.http.NameValuePair;
import org.apache.http.client.utils.URLEncodedUtils;
import org.hl7.fhir.r4.formats.XmlParser;
import org.hl7.fhir.r4.formats.IParser.OutputStyle;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.Bundle.BundleEntryComponent;
import org.hl7.fhir.r4.model.Bundle.BundleEntryRequestComponent;
import org.hl7.fhir.r4.model.Bundle.BundleEntryResponseComponent;
import org.hl7.fhir.r4.model.Bundle.BundleType;
import org.hl7.fhir.r4.model.Bundle.HTTPVerb;
import org.hl7.fhir.r4.model.CodeType;
import org.hl7.fhir.r4.model.Consent;
import org.hl7.fhir.r4.model.Extension;
import org.hl7.fhir.r4.model.Meta;
import org.hl7.fhir.r4.model.Parameters;
import org.hl7.fhir.r4.model.Resource;
import org.hl7.fhir.r4.model.ResourceType;
import org.hl7.fhir.r4.model.StringType;
import org.hl7.fhir.r4.model.Subscription;
import org.hl7.fhir.r5.model.Enumerations.SubscriptionStatusCodes;
import org.hl7.fhir.r5.model.Reference;
import org.hl7.fhir.r5.model.SubscriptionStatus;
import org.hl7.fhir.r5.model.SubscriptionStatus.SubscriptionNotificationType;
import org.hl7.fhir.r5.model.SubscriptionStatus.SubscriptionStatusNotificationEventComponent;

import net.aegis.fhir.model.ResourceContainer;
import net.aegis.fhir.model.client.ResourceResponseWrapper;
import net.aegis.fhir.service.CodeService;
import net.aegis.fhir.service.ResourceService;
import net.aegis.fhir.service.ResourcemetadataService;
import net.aegis.fhir.service.audit.AuditEventService;
import net.aegis.fhir.service.client.ResourceRESTClient;
import net.aegis.fhir.service.provenance.ProvenanceService;
import net.aegis.fhir.service.util.ServicesUtil;
import net.aegis.fhir.service.util.UTCDateUtil;
import net.aegis.fhir.service.util.UUIDUtil;

/**
 * @author richard.ettema
 *
 */
public class FASTConsentSubscriptionTopic extends SubscriptionTopicProxy {

	private Logger log = Logger.getLogger("FASTConsentSubscriptionTopic");

	private UTCDateUtil utcDateUtil = new UTCDateUtil();

	/* (non-Javadoc)
	 * @see net.aegis.fhir.service.subscription.r5.topic.SubscriptionTopicProxy#processTopic(net.aegis.fhir.service.ResourceService, net.aegis.fhir.service.ResourcemetadataService, net.aegis.fhir.service.CodeService, net.aegis.fhir.service.audit.AuditEventService, net.aegis.fhir.service.provenance.ProvenanceService, org.hl7.fhir.r4.model.Subscription, java.util.Date, java.lang.StringBuffer)
	 */
	@Override
	public Bundle processTopic(ResourceService resourceService,
			ResourcemetadataService resourcemetadataService, CodeService codeService,
			AuditEventService auditEventService, ProvenanceService provenanceService,
			Subscription subscription, Date since, StringBuffer returnedDetails)
			throws Exception {

		log.info("[START] FASTConsentSubscriptionTopic.processTopic()");

		Bundle subscriptionBundle = null;
		BundleEntryComponent subscriptionEntry = null;
		SubscriptionStatus subscriptionStatus = null;
		SubscriptionStatus existingStatus = null;
		Parameters pSubscriptionStatus = null;
		String payloadContent = "full-resource"; // Default

		try {
			// Get subscription.channel backport-payload-content code value
			if (subscription.hasChannel() && subscription.getChannel().hasPayload() &&
					subscription.getChannel().getPayloadElement().hasExtension()) {

				for (Extension payloadExt : subscription.getChannel().getPayloadElement().getExtension()) {
					if (payloadExt.hasUrl() && payloadExt.getUrl().equals("http://hl7.org/fhir/uv/subscriptions-backport/StructureDefinition/backport-payload-content")) {
						payloadContent = ((CodeType) payloadExt.getValue()).getCode();
						break;
					}
				}
			}

			/*
			 *  Get current SubscriptionStatus; if not found, create
			 */

			// Convert search parameter string into queryParams map
			String paramsString = "subscription=Subscription/" + subscription.getId() + "&type=event-notification&_sort=-_lastUpdated&_count=1";
			List<NameValuePair> params = URLEncodedUtils.parse(paramsString, Charset.defaultCharset());
			MultivaluedMap<String, String> queryParams = ServicesUtil.INSTANCE.listNameValuePairToMultivaluedMapString(params);

			// Search for all SubscriptionStatus with subscription = current Subscription; return as searchset Bundle
			ResourceContainer rc = resourceService.search(queryParams, null, null, "SubscriptionStatus", "INTERNAL", null, null, null, false);

			// Check for matched SubscriptionStatus resources
			if (rc != null && rc.getBundle() != null && !rc.getBundle().getEntry().isEmpty()) {

				// Should only be one SubscriptionStatus so take the first entry
				existingStatus = (SubscriptionStatus) ServicesUtil.INSTANCE.convertR4ParametersToR5SubscriptionStatus(rc.getBundle().getEntryFirstRep().getResource());
			}

			/*
			 *  Build Consent search parameters
			 *  - _lastUpdated=ge since date
			 *  - subscription.criteria backport-filter-criteria extension(s)
			 *  ! NEED TO ACCOUNT FOR CRITERIA DEFINED WITH RESOURCE TYPE !
			 */
			StringBuilder sbParams = new StringBuilder("_lastUpdated=ge")
					.append(utcDateUtil.formatDate(since, UTCDateUtil.DATE_PARAMETER_FORMAT));

			if (subscription.hasCriteria() && subscription.getCriteriaElement().hasExtension()) {

				String filterCriteria = null;
				int qPos = -1;
				// Expect only one backport-filter-criteria extension but, many are allowed
				for (Extension criteriaExt : subscription.getCriteriaElement().getExtension()) {

					if (criteriaExt.hasUrl() && criteriaExt.getUrl().equals("http://hl7.org/fhir/uv/subscriptions-backport/StructureDefinition/backport-filter-criteria")) {

						filterCriteria = ((StringType) criteriaExt.getValue()).getValueAsString();

						// Check for '?' and save the sub-string after
						qPos = filterCriteria.indexOf("?");
						if (qPos > 0) {
							filterCriteria = filterCriteria.substring(qPos + 1);
						}

						sbParams.append("&").append(filterCriteria);
					}
				}
			}

			params = URLEncodedUtils.parse(sbParams.toString(), Charset.defaultCharset());
			queryParams = ServicesUtil.INSTANCE.listNameValuePairToMultivaluedMapString(params);

			// Search for all Consent matching criteria; return as searchset Bundle
			rc = resourceService.search(queryParams, null, null, "Consent", "INTERNAL", null, null, null, false);

			// Check for matched Consent resources
			if (rc != null && rc.getBundle() != null && !rc.getBundle().getEntry().isEmpty()) {

				int consentCount = 0;
				for (BundleEntryComponent consentEntry : rc.getBundle().getEntry()) {
					if (consentEntry.hasResource() && consentEntry.getResource().getResourceType().equals(ResourceType.Consent)) {
						consentCount++;
					}
				}

				if (consentCount > 0) {
					// Subscription Notification Bundle
					subscriptionBundle = new Bundle();
					subscriptionBundle.setId(UUIDUtil.getUUID());
					Meta meta = new Meta();
					meta.addProfile("http://hl7.org/fhir/uv/subscriptions-backport/StructureDefinition/backport-subscription-notification-r4");
					subscriptionBundle.setMeta(meta);
					subscriptionBundle.setType(BundleType.HISTORY);
					subscriptionBundle.setTimestamp(new Date());

					String baseUrl = codeService.findCodeValueByName("baseUrl");

					// SubscriptionStatus for Subscription
					subscriptionStatus = newSubscriptionStatus(subscription, existingStatus, rc.getBundle(), baseUrl, payloadContent);

					pSubscriptionStatus = (Parameters) ServicesUtil.INSTANCE.convertR5SubscriptionStatusToR4Parameters(subscriptionStatus);

					// Convert the Resource to XML byte[]
					ByteArrayOutputStream oResource = new ByteArrayOutputStream();
					XmlParser xmlParser = new XmlParser();
					xmlParser.setOutputStyle(OutputStyle.PRETTY);
					xmlParser.compose(oResource, pSubscriptionStatus, true);
					byte[] bResource = oResource.toByteArray();

					// Initialize a Resource to be created
					net.aegis.fhir.model.Resource aegisResource = new net.aegis.fhir.model.Resource();
					aegisResource.setResourceType("SubscriptionStatus");
					aegisResource.setResourceContents(bResource);

					// Create new SubscriptionStatus
					ResourceContainer rcStatus = resourceService.create(aegisResource, null, baseUrl);

					aegisResource = rcStatus.getResource();

					// Add R4 Parameters (SubscriptionStatus) to subscription notification bundle
					subscriptionEntry = new BundleEntryComponent();
					subscriptionEntry.setFullUrl(baseUrl + "/Parameters/" + aegisResource.getResourceId());
					pSubscriptionStatus.setId(aegisResource.getResourceId());
					subscriptionEntry.setResource(pSubscriptionStatus);
					// Set request and response
					BundleEntryRequestComponent entryRequest = new BundleEntryRequestComponent();
					entryRequest.setMethod(HTTPVerb.GET);
					entryRequest.setUrl(baseUrl + "/Subscription/" + subscription.getId() + "/$status");
					subscriptionEntry.setRequest(entryRequest);
					BundleEntryResponseComponent entryResponse = new BundleEntryResponseComponent();
					entryResponse.setStatus("200");
					subscriptionEntry.setResponse(entryResponse);
					subscriptionBundle.addEntry(subscriptionEntry);

					// Iterate over all matched Consent; add to subscription notification bundle if payloadContent equal "full-resource"
					if (payloadContent.equals("full-resource")) {
						String responseCode = null;

						for (BundleEntryComponent consentEntry : rc.getBundle().getEntry()) {

							// Only add matched Consent resource(s)
							if (consentEntry.hasResource() && consentEntry.getResource().getResourceType().equals(ResourceType.Consent)) {
								responseCode = "200";
								subscriptionEntry = new BundleEntryComponent();
								subscriptionEntry.setFullUrl(baseUrl + "/Consent/" + consentEntry.getResource().getId());
								subscriptionEntry.setResource(consentEntry.getResource());
								// Set request and response
								entryRequest = new BundleEntryRequestComponent();
								if (consentEntry.getResource().hasMeta() && consentEntry.getResource().getMeta().hasVersionId()) {
									if (consentEntry.getResource().getMeta().getVersionId().equals("1")) {
										responseCode = "201";
									}
								}
								if (responseCode.equals("201")) {
									entryRequest.setMethod(HTTPVerb.POST);
									entryRequest.setUrl("Consent");
								}
								else {
									entryRequest.setMethod(HTTPVerb.PUT);
									entryRequest.setUrl("Consent/" + consentEntry.getResource().getId());
								}
								subscriptionEntry.setRequest(entryRequest);
								entryResponse = new BundleEntryResponseComponent();
								entryResponse.setStatus(responseCode);
								subscriptionEntry.setResponse(entryResponse);
								subscriptionBundle.addEntry(subscriptionEntry);
							}
						}
					}
				}
				else {
					// If zero matches, returnDetails = "0 matches"
					if (returnedDetails == null) {
						returnedDetails = new StringBuffer();
					}
					returnedDetails.append("No matched Consent resources found for Subscription criteria.");
				}

			}
			else {
				// If zero matches, returnDetails = "0 matches"
				if (returnedDetails == null) {
					returnedDetails = new StringBuffer();
				}
				returnedDetails.append("No matched Consent resources found for Subscription criteria.");
			}

		} catch (Exception e) {
			throw e;
		}

		return subscriptionBundle;
	}

	@Override
	public boolean processEventNotification(ResourceService resourceService,
			ResourcemetadataService resourcemetadataService, CodeService codeService,
			AuditEventService auditEventService, ProvenanceService provenanceService, Bundle notification,
			org.hl7.fhir.r5.model.SubscriptionStatus subscriptionstatus, String producesType,
			StringBuffer returnedDetails) throws Exception {

		log.info("[START] FASTConsentSubscriptionTopic.processEventNotification()");

		boolean result = true; // default

		if (returnedDetails == null) {
			returnedDetails = new StringBuffer();
		}

		try {
			/*
			 * Process the FAST Consent Subscription Event Notification
			 * Check for Consent(s) in notification Bundle; if not present, read from focus reference
			 * Parse (each) Consent and check local server if it already exists via its identifier
			 * If yes, update
			 * If no, create
			 */

			ResourceRESTClient resourceRESTClient = new ResourceRESTClient(codeService);
			Response resourceResponse = null;
			ResourceResponseWrapper responseWrapper = null;
			Resource r4Resource = null;
			List<String> headers = new ArrayList<String>();
			headers.add("Accept: " + producesType);

			// Build List of all Consent resources in the notification Bundle
			List<Consent> notifyConsentList = new ArrayList<Consent>();

			for (BundleEntryComponent nEntry : notification.getEntry()) {
				if (nEntry.getResource().getResourceType().equals(ResourceType.Consent)) {
					notifyConsentList.add((Consent)nEntry.getResource());
				}
			}

			if (notifyConsentList.isEmpty()) {
				if (subscriptionstatus.hasNotificationEvent()) {
					// Iterate over subscription status notificationEvent(s) and read all Consent focus references
					// ABSOLUTE URLS TO CONSENT RESOURCES EXPECTED
					for (SubscriptionStatusNotificationEventComponent nEvent : subscriptionstatus.getNotificationEvent()) {
						if (nEvent.hasFocus() && nEvent.getFocus().hasReference() && nEvent.getFocus().getReference().contains("Consent")) {
							String consentRef = nEvent.getFocus().getReference();
							resourceResponse = resourceRESTClient.get(consentRef, null, headers);

							if (resourceResponse.getStatus() == (Response.Status.OK.getStatusCode())) {
								responseWrapper = new ResourceResponseWrapper(resourceResponse);

								if (responseWrapper != null && responseWrapper.getResource() != null) {
									r4Resource = responseWrapper.getResource();

									if (r4Resource.getResourceType().equals(ResourceType.Consent)) {
										notifyConsentList.add((Consent)r4Resource);
									}
									else {
										log.warning("Focus resource from '" + consentRef + "' is not a Consent!");
									}
								}
								else {
									log.warning("Attempt to parse Consent focus resource from '" + consentRef + "' response wrapper failed! Wrapper resource is null.");
								}
							}
							else {
								log.warning("Attempt to get Consent focus resource from '" + consentRef + "' failed! Response status: " + resourceResponse.getStatus());
							}
						}
						else {
							log.warning("Notification event does not have a focus reference! Event number: " + (nEvent.hasEventNumber() ? nEvent.getEventNumber() : "?"));
						}
					}

					if (notifyConsentList.isEmpty()) {
						result = false;
						returnedDetails.append("No Consent focus references found in the subscription notification events! ");
					}
				}
				else {
					result = false;
					returnedDetails.append("No subscription notification events found! ");
				}
			}

			if (result == true && !notifyConsentList.isEmpty()) {
				List<NameValuePair> params = null;
				MultivaluedMap<String, String> queryParams = null;
				ResourceContainer rc = null;
				String consentId = null;
				ByteArrayOutputStream oResource = null;
				XmlParser xmlParser = new XmlParser();
				xmlParser.setOutputStyle(OutputStyle.PRETTY);
				net.aegis.fhir.model.Resource newResource = null;

				// Process all Consent resources
				StringBuffer consentIdentifier = null;
				for (Consent notifyConsent : notifyConsentList) {
					consentIdentifier = new StringBuffer("identifier=");
					consentIdentifier.append(notifyConsent.hasIdentifier() && notifyConsent.getIdentifierFirstRep().hasSystem() ? notifyConsent.getIdentifierFirstRep().getSystem() : "");
					consentIdentifier.append(notifyConsent.hasIdentifier() && notifyConsent.getIdentifierFirstRep().hasSystem() ? "|" : "");
					consentIdentifier.append(notifyConsent.hasIdentifier() && notifyConsent.getIdentifierFirstRep().hasValue() ? notifyConsent.getIdentifierFirstRep().getValue() : "?");
					log.info("Processing Consent '" + consentIdentifier.toString() + "'");

					/*
					 *  Attempt to find (search) for local Consent by identifier.
					 *  If not found, create; else, update
					 */
					params = URLEncodedUtils.parse(consentIdentifier.toString(), Charset.defaultCharset());
					queryParams = ServicesUtil.INSTANCE.listNameValuePairToMultivaluedMapString(params);

					// Search for all Consent(s) with identifier = consentIdentifier; return as searchset Bundle; only expect zero or one matches
					rc = resourceService.search(queryParams, null, null, "Consent", "INTERNAL", null, null, null, false);

					consentId = null;

					// Check for matched Subscription resources
					if (rc != null && rc.getBundle() != null && !rc.getBundle().getEntry().isEmpty()) {

						for (BundleEntryComponent consentEntry : rc.getBundle().getEntry()) {
							if (consentEntry.hasResource() && consentEntry.getResource().getResourceType().equals(ResourceType.Consent)) {
								// Take first Consent and save its resource id
								consentId = consentEntry.getResource().getId();
								break;
							}
						}
						
					}

					// Convert the Resource to XML byte[]
					oResource = new ByteArrayOutputStream();
					xmlParser = new XmlParser();
					xmlParser.compose(oResource, notifyConsent, true);
					byte[] bResource = oResource.toByteArray();

					// Initialize a Resource to be created or updated
					newResource = new net.aegis.fhir.model.Resource();
					newResource.setResourceType("Consent");
					newResource.setResourceContents(bResource);

					if (consentId == null) {
						// If consentId is null, create Consent

						rc = resourceService.create(newResource, null, codeService.findCodeValueByName("baseUrl"));

						if (rc.getResponseStatus().equals(Response.Status.CREATED)) {
							returnedDetails.append("Created Consent " + consentIdentifier.toString() + ". ");
						}
						else {
							returnedDetails.append("Create of Consent " + consentIdentifier.toString() + " failed; " + rc.getResponseStatus().name() + "! ");
						}
					}
					else {
						// If consentId is not null, update Consent

						rc = resourceService.update(consentId, newResource, codeService.findCodeValueByName("baseUrl"));

						if (rc.getResponseStatus().equals(Response.Status.OK)) {
							returnedDetails.append("Updated Consent " + consentIdentifier.toString() + ". ");
						}
						else {
							returnedDetails.append("Update of Consent " + consentIdentifier.toString() + " failed; " + rc.getResponseStatus().name() + "! ");
						}
					}
				}
			}
			else {
				result = false;
				returnedDetails.append("No Consent resources found processing subscription notification!");
			}
		
		} catch (Exception e) {
			result = false;
			returnedDetails.append("Exception processing subscription notification! ").append(e.getMessage());
		}

		return result;
	}

	/**
	 * @param subscription
	 * @param existingStatus
	 * @param consentSearch
	 * @param baseUrl
	 * @param payloadContent
	 * @return
	 * @throws Exception
	 */
	private SubscriptionStatus newSubscriptionStatus(Subscription subscription, SubscriptionStatus existingStatus, Bundle consentSearch, String baseUrl, String payloadContent) throws Exception {

		SubscriptionStatus subscriptionStatus = new SubscriptionStatus();

		long eventNumber = 1;
		if (existingStatus != null) {
			eventNumber = existingStatus.getEventsSinceSubscriptionStart() + 1;
		}

		org.hl7.fhir.r5.model.Reference reference = new org.hl7.fhir.r5.model.Reference();
		reference.setReference("Subscription/" + subscription.getId());
		subscriptionStatus.setSubscription(reference);

		subscriptionStatus.setTopic(subscription.getCriteria());

		subscriptionStatus.setStatus(SubscriptionStatusCodes.ACTIVE);

		subscriptionStatus.setType(SubscriptionNotificationType.EVENTNOTIFICATION);

		subscriptionStatus.setEventsSinceSubscriptionStart(eventNumber);

		SubscriptionStatusNotificationEventComponent ssne = new SubscriptionStatusNotificationEventComponent();

		ssne.setEventNumber(eventNumber);
		ssne.setTimestamp(new Date());

		if (!payloadContent.equals("empty")) {
			// Iterate over Consent search Bundle; add Consent reference(s)
			int iEntry = 0;
			Reference consentReference = null;
			for (BundleEntryComponent consentEntry : consentSearch.getEntry()) {
				consentReference = new Reference();
				consentReference.setReference(baseUrl + "/Consent/" + consentEntry.getResource().getId());

				if (iEntry == 0) {
					ssne.setFocus(consentReference);
				}
				else {
					ssne.addAdditionalContext(consentReference);
				}

				iEntry++;
			}
		}

		subscriptionStatus.addNotificationEvent(ssne);

		return subscriptionStatus;
	}

}
