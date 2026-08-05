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
package net.aegis.fhir.service.subscription.r5;

import java.io.ByteArrayOutputStream;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.logging.Logger;

import org.apache.http.NameValuePair;
import org.apache.http.client.utils.URLEncodedUtils;
import org.hl7.fhir.r4.formats.IParser.OutputStyle;
import org.hl7.fhir.r4.formats.JsonParser;
import org.hl7.fhir.r4.formats.XmlParser;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.Bundle.BundleEntryComponent;
import org.hl7.fhir.r4.model.Bundle.BundleEntryRequestComponent;
import org.hl7.fhir.r4.model.Bundle.BundleEntryResponseComponent;
import org.hl7.fhir.r4.model.Bundle.BundleType;
import org.hl7.fhir.r4.model.Bundle.HTTPVerb;
import org.hl7.fhir.r4.model.Meta;
import org.hl7.fhir.r4.model.OperationOutcome;
import org.hl7.fhir.r4.model.Parameters;
import org.hl7.fhir.r4.model.ResourceType;
import org.hl7.fhir.r4.model.StringType;
import org.hl7.fhir.r4.model.Subscription;
import org.hl7.fhir.r4.model.Subscription.SubscriptionStatus;
import org.hl7.fhir.r5.model.CodeableConcept;
import org.hl7.fhir.r5.model.Coding;
import org.hl7.fhir.r5.model.Enumerations.SubscriptionStatusCodes;
import org.hl7.fhir.r5.model.SubscriptionStatus.SubscriptionNotificationType;

import jakarta.ejb.Stateless;
import jakarta.inject.Inject;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.MultivaluedMap;
import jakarta.ws.rs.core.Response;
import net.aegis.fhir.model.Clientresource;
import net.aegis.fhir.model.LabelKeyValueBean;
import net.aegis.fhir.model.ResourceContainer;
import net.aegis.fhir.service.ClientresourceService;
import net.aegis.fhir.service.CodeService;
import net.aegis.fhir.service.ResourceService;
import net.aegis.fhir.service.ResourcemetadataService;
import net.aegis.fhir.service.audit.AuditEventService;
import net.aegis.fhir.service.client.ResourceRESTClient;
import net.aegis.fhir.service.provenance.ProvenanceService;
import net.aegis.fhir.service.subscription.r5.topic.SubscriptionTopicProxy;
import net.aegis.fhir.service.subscription.r5.topic.SubscriptionTopicProxyObjectFactory;
import net.aegis.fhir.service.util.ServicesUtil;
import net.aegis.fhir.service.util.UTCDateUtil;
import net.aegis.fhir.service.util.UUIDUtil;

/**
 * Subscription Service for performing the set of functions supporting the
 * FHIR R5 Backport to R4 Subscriptions Framework with Topics.
 *
 * Supported channel types: rest-hook
 *
 * The @Stateless annotation eliminates the need for manual transaction demarcation
 *
 * @author richard.ettema
 *
 */
@Stateless
public class SubscriptionServiceR5 {

	private Logger log = Logger.getLogger("SubscriptionServiceR5");

	@Inject
	AuditEventService auditEventService;

	@Inject
	ClientresourceService clientresourceService;

    @Inject
    CodeService codeService;

	@Inject
    ProvenanceService provenanceService;

    @Inject
	private ResourceService resourceService;

	@Inject
	ResourcemetadataService resourcemetadataService;

	@Inject
	private UTCDateUtil utcDateUtil;

	private ResourceRESTClient resourceClient;

	/*
	 * Public Methods
	 */

	public SubscriptionServiceR5() throws Exception {
		this.resourceClient = new ResourceRESTClient(codeService);
	}

	/**
	 * Process all active Subscription resources based on since datetime
	 *
	 * @param since
	 * @return List<LabelKeyValueBean> - Results
	 * @throws Exception
	 */
	public List<LabelKeyValueBean> processSubscriptions(Date since) throws Exception {

		log.fine("[START] SubscriptionServiceR5.processSubscriptions()");

		// Verify since date is not null
		if (since == null) {
			throw new Exception("processSubscriptions requires a valid, non-null date defining the date since the last processing execution!");
		}

		List<LabelKeyValueBean> results = new ArrayList<LabelKeyValueBean>();
		LabelKeyValueBean result = null;

		/*
		 * Search for all Subscriptions with status = active
		 * For each Subscription
		 *   Perform search using criteria adding _lastUpdated=gt:since parameter
		 *   For each matched resource instance
		 *     Send FHIR update to Subscription endpoint with matched resource instance as the payload
		 * Save new since date
		 */
		try {
			// Construct _lastUpdated parameters string
			StringBuilder sbSinceParams = new StringBuilder("_lastUpdated=ge")
					.append(utcDateUtil.formatDate(since, UTCDateUtil.DATETIME_ONLY_PARAMETER_FORMAT));

			log.fine("sbSinceParams [" + sbSinceParams.toString() + "]");

			// Convert search parameter string into queryParams map
			List<NameValuePair> params = URLEncodedUtils.parse("status=active", Charset.defaultCharset());
			MultivaluedMap<String, String> queryParams = ServicesUtil.INSTANCE.listNameValuePairToMultivaluedMapString(params);

			// Search for all Subscriptions with status = active; return as searchset Bundle
			ResourceContainer rcSubscriptions = resourceService.search(queryParams, null, null, "Subscription", "INTERNAL", null, null, null, false);

			// Check for matched Subscription resources
			if (rcSubscriptions != null && rcSubscriptions.getBundle() != null && !rcSubscriptions.getBundle().getEntry().isEmpty()) {

				ByteArrayOutputStream oResource = null;
				XmlParser xmlParser = new XmlParser();
				xmlParser.setOutputStyle(OutputStyle.PRETTY);
				JsonParser jsonParse = new JsonParser();
				jsonParse.setOutputStyle(OutputStyle.PRETTY);
				String payload = null;

				// For each Subscription entry
				Subscription subscription = null;

				for (BundleEntryComponent subscriptionEntry : rcSubscriptions.getBundle().getEntry()) {

					subscription = (Subscription)subscriptionEntry.getResource();

					log.fine("Processing Subscription [" + subscription.getId() + "] with criteria [" + subscription.getCriteria() + "] for channel type [" + subscription.getChannel().getType().name() + "]");

					// Initialize result bean
					result = new LabelKeyValueBean(subscription.getId(), subscription.getChannel().getType().name() + "; " + subscription.getChannel().getEndpoint(),
							subscription.getCriteria(), "", "processing", "");

					/*
					 * Use Factory Pattern for execution of SubscriptionTopic operation
					 */
					SubscriptionTopicProxyObjectFactory topicFactory = new SubscriptionTopicProxyObjectFactory();
					SubscriptionTopicProxy topicProxy = topicFactory.getSubscriptionTopicProxy(subscription.getCriteria());

					// Check for non-null topic proxy
					if (topicProxy != null) {
						StringBuffer returnedDetails = new StringBuffer();

						Bundle subscriptionBundle = topicProxy.processTopic(resourceService, resourcemetadataService, codeService, auditEventService, provenanceService, subscription, since, returnedDetails);

						// Select processing based on channel.type
						switch (subscription.getChannel().getType()) {
						case EMAIL:
							result.setPath("Email channel type not supported");
							log.fine("Email channel type not currently supported.");
							break;
						case MESSAGE:
							result.setPath("FHIR messaging channel not supported");
							log.fine("FHIR messaging channel type not currently supported.");
							break;
						case NULL:
							result.setPath("NULL channel type not supported");
							log.fine("NULL channel type not currently supported.");
							break;
						case RESTHOOK:
							/*
							 * If returnedDetails not empty topic processing either did not find any updated resources or failed, record outcome in result
							 *
							 * Else, post subscriptionBundle to subscription end point
							 */
							boolean okToPost = false;
							Response response = null;
							if (returnedDetails != null && returnedDetails.length() > 0) {
								result.setType(returnedDetails.toString());
								log.fine("REST Hook " + returnedDetails.toString());
							}
							else {
								// Parse subscriptionBundle to XML or JSON String based on the Subscription payload
								oResource = new ByteArrayOutputStream();
								if (subscription.getChannel().getPayload().contains("xml")) {
									xmlParser.compose(oResource, subscriptionBundle, true);
									payload = oResource.toString();
									okToPost = true;
								}
								else if (subscription.getChannel().getPayload().contains("json")) {
									jsonParse.compose(oResource, subscriptionBundle);
									payload = oResource.toString();
									okToPost = true;
								}
								else {
									// Unsupported mime type format
									result.setType("Invalid channel payload mime type '" + subscription.getChannel().getPayload() + "'!");
									log.fine("REST Hook Invalid channel payload mime type '" + subscription.getChannel().getPayload() + "'!");
								}
							}

							if (okToPost == true) {
								// Process HTTP headers if present
								List<String> headers = new ArrayList<String>();
								for (StringType header : subscription.getChannel().getHeader()) {
									headers.add(header.asStringValue());
								}
								response = resourceClient.post(subscription.getChannel().getEndpoint(), null, payload, subscription.getChannel().getPayload(), headers);

								result.setRefType(payload);
							}

							// Update Subscription status based on current result and post response
							this.setSubscriptionStatus(subscriptionBundle, subscription, response, result);

							result.setPath("complete");
							break;
						case SMS:
							result.setPath("SMS channel type not supported");
							log.fine("SMS channel type not currently supported.");
							break;
						case WEBSOCKET:
							result.setPath("Websocket channel type not supported");
							log.fine("Websocket channel type not currently supported.");
							break;
						default:
							result.setPath("Unknown channel type");
							log.fine("Unknown channel type!");
							break;
						}
					}
					else {
						result.setType("Unsupported subscription topic '" + subscription.getCriteria() + "'!");
					}

					// Add result bean to results
					results.add(result);
				}
			}
			else {
				log.fine("SubscriptionServiceR5.processSubscriptions() - No active subscriptions found.");
				result = new LabelKeyValueBean("", "", "", "No active subscriptions found.", "complete");
				results.add(result);
			}

		} catch (Exception e) {
			e.printStackTrace();
			throw e;
		}

		return results;
	}

	public LabelKeyValueBean sendHandshake(Subscription subscription, StringBuffer returnedDetails) throws Exception {

		log.info("[START] SubscriptionServiceR5.sendHandshake()");

		LabelKeyValueBean result = new LabelKeyValueBean("NULL", "NULL", "NULL", "", "start", "");

		try {
			if (subscription != null) {

				// Initialize result bean
				result.setLabel(subscription.getId());
				result.setKey(
						subscription.getChannel().getType().name() + "; " + subscription.getChannel().getEndpoint());
				result.setValue(subscription.getCriteria());
				result.setPath("processing");

				if (!subscription.getStatus().equals(SubscriptionStatus.OFF)) {

					ByteArrayOutputStream oResource = null;
					XmlParser xmlParser = new XmlParser();
					xmlParser.setOutputStyle(OutputStyle.PRETTY);
					JsonParser jsonParse = new JsonParser();
					jsonParse.setOutputStyle(OutputStyle.PRETTY);
					String payload = null;

					/*
					 * Use Factory Pattern to verify support for the SubscriptionTopic
					 */
					SubscriptionTopicProxyObjectFactory topicFactory = new SubscriptionTopicProxyObjectFactory();
					SubscriptionTopicProxy topicProxy = topicFactory
							.getSubscriptionTopicProxy(subscription.getCriteria());

					// Check for non-null topic proxy
					if (topicProxy != null) {
						if (returnedDetails == null) {
							returnedDetails = new StringBuffer();
						}

						Bundle subscriptionBundle = generateHandshake(resourceService, codeService, subscription,
								returnedDetails);

						// Select processing based on channel.type
						switch (subscription.getChannel().getType()) {
						case EMAIL:
							result.setPath("Email channel type not supported");
							log.info("Email channel type not currently supported.");
							break;
						case MESSAGE:
							result.setPath("FHIR messaging channel not supported");
							log.info("FHIR messaging channel type not currently supported.");
							break;
						case NULL:
							result.setPath("NULL channel type not supported");
							log.info("NULL channel type not currently supported.");
							break;
						case RESTHOOK:
							/*
							 * If returnedDetails not empty topic processing either did not find any updated
							 * resources or failed, record outcome in result
							 *
							 * Else, post subscriptionBundle to subscription end point
							 */
							boolean okToPost = false;
							Response response = null;
							if (returnedDetails != null && returnedDetails.length() > 0) {
								result.setType(returnedDetails.toString());
								log.info("REST Hook " + returnedDetails.toString());
							} else {
								// Parse subscriptionBundle to XML or JSON String based on the Subscription
								// payload
								oResource = new ByteArrayOutputStream();
								if (subscription.getChannel().getPayload().contains("xml")) {
									xmlParser.compose(oResource, subscriptionBundle, true);
									payload = oResource.toString();
									okToPost = true;
								} else if (subscription.getChannel().getPayload().contains("json")) {
									jsonParse.compose(oResource, subscriptionBundle);
									payload = oResource.toString();
									okToPost = true;
								} else {
									// Unsupported mime type format
									result.setType("Invalid channel payload mime type '"
											+ subscription.getChannel().getPayload() + "'!");
									log.info("REST Hook Invalid channel payload mime type '"
											+ subscription.getChannel().getPayload() + "'!");
								}
							}

							if (okToPost == true) {
								// Process HTTP headers if present
								List<String> headers = new ArrayList<String>();
								for (StringType header : subscription.getChannel().getHeader()) {
									headers.add(header.asStringValue());
								}
								response = resourceClient.post(subscription.getChannel().getEndpoint(), null, payload,
										subscription.getChannel().getPayload(), headers);

								result.setRefType(payload);
							}

							// Update Subscription status based on current result and post response
							this.setSubscriptionStatus(subscriptionBundle, subscription, response, result);

							result.setPath("handshake complete");
							break;
						case SMS:
							result.setPath("SMS channel type not supported");
							log.info("SMS channel type not currently supported.");
							break;
						case WEBSOCKET:
							result.setPath("Websocket channel type not supported");
							log.info("Websocket channel type not currently supported.");
							break;
						default:
							result.setPath("Unknown channel type");
							log.info("Unknown channel type!");
							break;
						}
					} else {
						result.setType("Unsupported subscription topic '" + subscription.getCriteria() + "'!");
					}

				} else {
					result.setType("Cannot generate handshake notification for a Subscription with a status of OFF!");
				}
			} else {
				result.setType("Cannot generate handshake notification for NULL Subscription!");
			}

		} catch (Exception e) {
			log.severe("Exception sending handshake notification! " + e.getMessage());
			result.setType("Exception sending handshake notification! " + e.getMessage());

			throw e;
		}

		return result;
	}

	public boolean processSubscriptionNotification(HttpServletRequest request, HttpHeaders headers, String payload, String outcome) throws Exception {

		log.info("[START] SubscriptionServiceR5.processSubscriptionNotification()");

		boolean result = true;
		String contentType = null;
		String producesType = null;
		Parameters pSubscriptionStatus = null;
		org.hl7.fhir.r5.model.SubscriptionStatus existingSubscriptionStatus = null;

		try {
			// Get the produces type based on the request Accept
			producesType = ServicesUtil.INSTANCE.getProducesType(headers, request);

			// Get the content type based on the request Content-Type
			contentType = ServicesUtil.INSTANCE.getHttpHeader(headers, HttpHeaders.CONTENT_TYPE);

			// Initialize outcome to "default success"
			outcome = ServicesUtil.INSTANCE.getOperationOutcome(OperationOutcome.IssueSeverity.INFORMATION,
					OperationOutcome.IssueType.INFORMATIONAL, "default success", null, null, producesType);

			// Instantiate the Resource; this is the first, simple validation of the
			// resource
			Bundle notifyBundle = (Bundle) ServicesUtil.INSTANCE.convertToR4Resource(contentType, "Bundle", payload);

			// Get the SubscriptionStatus Bundle.entry
			for (BundleEntryComponent entry : notifyBundle.getEntry()) {
				if (entry.getResource().getResourceType().equals(ResourceType.Parameters)) {
					pSubscriptionStatus = (Parameters) entry.getResource();
					existingSubscriptionStatus = (org.hl7.fhir.r5.model.SubscriptionStatus) ServicesUtil.INSTANCE.convertR4ParametersToR5SubscriptionStatus(pSubscriptionStatus);
					break;
				}
			}

			if (existingSubscriptionStatus != null) {
				/*
				 * Process notification based on type: handshake, heat beat, event-notification, query-status, query-event
				 */
				SubscriptionNotificationType sNotifyType = existingSubscriptionStatus.getType();

				if (sNotifyType.equals(SubscriptionNotificationType.HANDSHAKE)) {
					/*
					 * Attempt to retrieve client Subscription resource, update to 'active' if status is 'requested'
					 */
					if (existingSubscriptionStatus.hasSubscription() && existingSubscriptionStatus.getSubscription().hasReference()) {
						String subscriptionRef = existingSubscriptionStatus.getSubscription().getReference();
						// Extract subscription id
						String subscriptionId = ServicesUtil.INSTANCE.extractResourceIdFromURL(subscriptionRef);

						Subscription subscription = (Subscription) clientresourceService.readFHIRResource("Subscription", subscriptionId);

						if (subscription != null) {
							subscription.setStatus(SubscriptionStatus.ACTIVE);

							// Save updated Subscription to client resources
							Clientresource clientresource = clientresourceService.findClientresourceByResourceTypeResourceId("Subscription", subscriptionId);
							JsonParser jsonParser = new JsonParser();
							jsonParser.setOutputStyle(OutputStyle.PRETTY);
							ByteArrayOutputStream oOp = new ByteArrayOutputStream();
							jsonParser.compose(oOp, subscription);
							clientresource.setResourceContents(oOp.toByteArray());
							clientresourceService.update(clientresource, subscription);

							// Set outcome to "Handshake success"
							outcome = ServicesUtil.INSTANCE.getOperationOutcome(OperationOutcome.IssueSeverity.INFORMATION,
									OperationOutcome.IssueType.INFORMATIONAL, "Handshake success", null, null, producesType);
						}
						else {
							throw new Exception("Notification Bundle SubscriptionStatus Subscription cannot be found locally!");
						}
					}
					else {
						throw new Exception("Notification Bundle SubscriptionStatus does not define a referenced Subscription!");
					}
				}
				if (sNotifyType.equals(SubscriptionNotificationType.HEARTBEAT)) {
					// Set outcome to "Heartbeat success"
					outcome = ServicesUtil.INSTANCE.getOperationOutcome(OperationOutcome.IssueSeverity.INFORMATION,
							OperationOutcome.IssueType.INFORMATIONAL, "Heartbeat success", null, null, producesType);
				}
				if (sNotifyType.equals(SubscriptionNotificationType.EVENTNOTIFICATION)) {
					// Set outcome to "Event notification success"
					outcome = ServicesUtil.INSTANCE.getOperationOutcome(OperationOutcome.IssueSeverity.INFORMATION,
							OperationOutcome.IssueType.INFORMATIONAL, "Event notification success", null, null, producesType);
				}
				if (sNotifyType.equals(SubscriptionNotificationType.QUERYSTATUS)) {
					// Set outcome to "Query status success"
					outcome = ServicesUtil.INSTANCE.getOperationOutcome(OperationOutcome.IssueSeverity.INFORMATION,
							OperationOutcome.IssueType.INFORMATIONAL, "Query status ok", null, null, producesType);
				}
				if (sNotifyType.equals(SubscriptionNotificationType.QUERYEVENT)) {
					// Set outcome to "Query event not implemented"
					outcome = ServicesUtil.INSTANCE.getOperationOutcome(OperationOutcome.IssueSeverity.INFORMATION,
							OperationOutcome.IssueType.INFORMATIONAL, "Query event not implemented", null, null, producesType);
				}
			}
			else {
				throw new Exception("Notification Bundle does not contain a SubscriptionStatus!");
			}
		} catch (Exception e) {
			result = false;
			outcome = ServicesUtil.INSTANCE.getOperationOutcome(OperationOutcome.IssueSeverity.ERROR,
					OperationOutcome.IssueType.EXCEPTION,
					"Exception processing subscription notification! " + e.getMessage(), null, null, producesType);
			log.severe("Exception processing subscription notification! " + e.getMessage());
		}

		return result;
	}

	/*
	 * Private methods
	 */

	/**
	 * @param resourceService
	 * @param codeService
	 * @param subscription
	 * @param returnedDetails
	 * @return Subscription Handshake Bundle
	 * @throws Exception
	 */
	private Bundle generateHandshake(ResourceService resourceService, CodeService codeService,
			Subscription subscription, StringBuffer returnedDetails) throws Exception {

		log.info("[START] SubscriptionServiceR5.generateHandshake()");

		Bundle subscriptionBundle = null;
		BundleEntryComponent subscriptionEntry = null;
		org.hl7.fhir.r5.model.SubscriptionStatus subscriptionStatus = null;
		org.hl7.fhir.r5.model.SubscriptionStatus existingStatus = null;
		Parameters pSubscriptionStatus = null;
		long eventNumber = 0;

		try {

			if (subscription != null) {
				/*
				 * Get current SubscriptionStatus; if not found, create
				 */

				// Convert search parameter string into queryParams map
				String paramsString = "subscription=Subscription/" + subscription.getId()
						+ "&type=event-notification&_sort=-_lastUpdated&_count=1";
				List<NameValuePair> params = URLEncodedUtils.parse(paramsString, Charset.defaultCharset());
				MultivaluedMap<String, String> queryParams = ServicesUtil.INSTANCE
						.listNameValuePairToMultivaluedMapString(params);

				// Search for all SubscriptionStatus with subscription = current Subscription;
				// return as searchset Bundle
				ResourceContainer rc = resourceService.search(queryParams, null, null, "SubscriptionStatus", "INTERNAL",
						null, null, null, false);

				// Check for matched SubscriptionStatus resources
				if (rc != null && rc.getBundle() != null && !rc.getBundle().getEntry().isEmpty()) {

					// Should only be one SubscriptionStatus so take the first entry
					existingStatus = (org.hl7.fhir.r5.model.SubscriptionStatus) ServicesUtil.INSTANCE
							.convertR4ParametersToR5SubscriptionStatus(rc.getBundle().getEntryFirstRep().getResource());
					if (existingStatus != null) {
						eventNumber = existingStatus.getEventsSinceSubscriptionStart();
					}
				}

				// Subscription Notification Bundle
				subscriptionBundle = new Bundle();
				subscriptionBundle.setId(UUIDUtil.getUUID());
				Meta meta = new Meta();
				meta.addProfile(
						"http://hl7.org/fhir/uv/subscriptions-backport/StructureDefinition/backport-subscription-notification-r4");
				subscriptionBundle.setMeta(meta);
				subscriptionBundle.setType(BundleType.HISTORY);
				subscriptionBundle.setTimestamp(new Date());

				String baseUrl = codeService.findCodeValueByName("baseUrl");

				subscriptionStatus = new org.hl7.fhir.r5.model.SubscriptionStatus();

				String subscriptionStatusId = UUIDUtil.getUUID();
				subscriptionStatus.setId(subscriptionStatusId);

				org.hl7.fhir.r5.model.Reference reference = new org.hl7.fhir.r5.model.Reference();
				reference.setReference(baseUrl + "/Subscription/" + subscription.getId());
				subscriptionStatus.setSubscription(reference);

				subscriptionStatus.setTopic(subscription.getCriteria());

				subscriptionStatus.setStatus(SubscriptionStatusCodes.REQUESTED);

				subscriptionStatus.setType(SubscriptionNotificationType.HANDSHAKE);

				subscriptionStatus.setEventsSinceSubscriptionStart(eventNumber);

				pSubscriptionStatus = (Parameters) ServicesUtil.INSTANCE
						.convertR5SubscriptionStatusToR4Parameters(subscriptionStatus);

				// Add R4 Parameters (SubscriptionStatus) to subscription notification bundle
				subscriptionEntry = new BundleEntryComponent();
				subscriptionEntry.setFullUrl(UUIDUtil.UUID_PREFIX + subscriptionStatusId);
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
			} else {
				// Subscription is null, set returnDetails
				if (returnedDetails == null) {
					returnedDetails = new StringBuffer();
				}
				returnedDetails.append("Cannot generate handshake notification for NULL Subscription!");
			}

		} catch (Exception e) {
			log.severe("Exception generating handshake notification! " + e.getMessage());
			if (returnedDetails == null) {
				returnedDetails = new StringBuffer();
			}
			returnedDetails.append("Exception generating handshake notification! " + e.getMessage());
			throw e;
		}

		return subscriptionBundle;
	}

	/**
	 * Update Subscription status and result outcome (path) based on current result
	 * and post response
	 *
	 * @param subscriptionBundle
	 * @param subscription
	 * @param response
	 * @param result
	 * @return boolean true success; false error
	 * @throws Exception
	 */
	private boolean setSubscriptionStatus(Bundle subscriptionBundle, Subscription subscription, Response response,
			LabelKeyValueBean result) throws Exception {

		log.info("[START] SubscriptionServiceR5.setSubscriptionStatus()");

		boolean bResult = true; // Default true - success;
		StringBuilder msg = new StringBuilder();
		SubscriptionStatus newStatus = SubscriptionStatus.ACTIVE; // Default active
		String errorCode = "";
		org.hl7.fhir.r5.model.SubscriptionStatus errorSubscriptionStatus = null;
		org.hl7.fhir.r5.model.SubscriptionStatus existingSubscriptionStatus = null;
		Parameters pErrorSubscriptionStatus = null;
		Parameters pExistingSubscriptionStatus = null;

		if (response != null) {
			if (response.getStatus() < 400) {
				msg.append("Subscription '").append(subscription.getId()).append("' processed successfully.");

				// response success - assign Subscription.status = active
			} else {
				msg.append("Subscription '").append(subscription.getId())
						.append("' process failed! Response from notification request '").append(response.getStatus())
						.append("'.");

				// response failure! - update Subscription.status = error
				newStatus = SubscriptionStatus.ERROR;
				bResult = false;

				// create new SubscriptionStatus with response error
				errorCode = "error-response";

			}
		} else {
			msg.append("Subscription '").append(subscription.getId())
					.append("' process failed! Null response from notification request.");

			// response is null - update Subscription.status = error
			newStatus = SubscriptionStatus.ERROR;
			bResult = false;

			// create new SubscriptionStatus with no-response error
			errorCode = "no-response";
		}

		// Set Subscription.status based on response
		subscription.setStatus(newStatus);

		String baseUrl = codeService.findCodeValueByName("baseUrl");

		// Convert the Resource to XML byte[]
		ByteArrayOutputStream oResource = new ByteArrayOutputStream();
		XmlParser xmlParser = new XmlParser();
		xmlParser.setOutputStyle(OutputStyle.PRETTY);
		xmlParser.compose(oResource, subscription, true);
		byte[] bResource = oResource.toByteArray();

		// Initialize a Resource to be updated
		net.aegis.fhir.model.Resource aegisResource = new net.aegis.fhir.model.Resource();
		aegisResource.setResourceType("Subscription");
		aegisResource.setResourceContents(bResource);

		// Update Subscription
		ResourceContainer rcStatus = resourceService.update(subscription.getId(), aegisResource, baseUrl);

		if (!rcStatus.getResponseStatus().equals(Response.Status.OK)) {
			msg.append("Subscription '").append(subscription.getId())
					.append("' process failed! Response from Subscription update '")
					.append(rcStatus.getResponseStatus().toString()).append("'.");

			bResult = false;
		}

		// If error, create new SubscriptionStatus with error condition
		if (bResult == false) {
			for (BundleEntryComponent entry : subscriptionBundle.getEntry()) {
				if (entry.getResource().getResourceType().equals(ResourceType.Parameters)) {
					pExistingSubscriptionStatus = (Parameters) entry.getResource();
					existingSubscriptionStatus = (org.hl7.fhir.r5.model.SubscriptionStatus) ServicesUtil.INSTANCE
							.convertR4ParametersToR5SubscriptionStatus(pExistingSubscriptionStatus);
					break;
				}
			}
			errorSubscriptionStatus = this.errorSubscriptionStatus(subscription, existingSubscriptionStatus, baseUrl,
					errorCode, msg.toString());
			pErrorSubscriptionStatus = (Parameters) ServicesUtil.INSTANCE
					.convertR5SubscriptionStatusToR4Parameters(errorSubscriptionStatus);

			// Convert the Resource to XML byte[]
			oResource = new ByteArrayOutputStream();
			xmlParser.compose(oResource, pErrorSubscriptionStatus, true);
			bResource = oResource.toByteArray();

			// Initialize a Resource to be created
			aegisResource = new net.aegis.fhir.model.Resource();
			aegisResource.setResourceType("SubscriptionStatus");
			aegisResource.setResourceContents(bResource);

			// Create error SubscriptionStatus
			rcStatus = resourceService.create(aegisResource, null, baseUrl);

			if (!rcStatus.getResponseStatus().equals(Response.Status.OK)) {
				msg.append("Subscription '").append(subscription.getId())
						.append("' process failed! Create error SubscriptionStatus failed '")
						.append(rcStatus.getResponseStatus().toString()).append("'.");
			}
		}

		// Assign result message
		result.setType(msg.toString());

		return bResult;
	}

	/**
	 * @param subscription
	 * @param existingStatus
	 * @param baseUrl
	 * @param errorCode
	 * @param errorText
	 * @return
	 * @throws Exception
	 */
	private org.hl7.fhir.r5.model.SubscriptionStatus errorSubscriptionStatus(Subscription subscription,
			org.hl7.fhir.r5.model.SubscriptionStatus existingStatus, String baseUrl, String errorCode, String errorText)
			throws Exception {

		org.hl7.fhir.r5.model.SubscriptionStatus subscriptionStatus = new org.hl7.fhir.r5.model.SubscriptionStatus();

		long eventNumber = 0;
		if (existingStatus != null) {
			eventNumber = existingStatus.getEventsSinceSubscriptionStart();
		}

		org.hl7.fhir.r5.model.Reference reference = new org.hl7.fhir.r5.model.Reference();
		reference.setReference(baseUrl + "/Subscription/" + subscription.getId());
		subscriptionStatus.setSubscription(reference);

		subscriptionStatus.setTopic(subscription.getCriteria());

		subscriptionStatus.setStatus(SubscriptionStatusCodes.ERROR);

		subscriptionStatus.setType(SubscriptionNotificationType.QUERYSTATUS);

		subscriptionStatus.setEventsSinceSubscriptionStart(eventNumber);

		List<CodeableConcept> theError = new ArrayList<CodeableConcept>();
		CodeableConcept ccError = new CodeableConcept();
		Coding cError = new Coding();
		cError.setSystem("http://terminology.hl7.org/CodeSystem/subscription-error");
		cError.setCode(errorCode);
		ccError.addCoding(cError);
		ccError.setText(errorText);

		subscriptionStatus.setError(theError);

		return subscriptionStatus;
	}

}
