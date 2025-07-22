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
package net.aegis.fhir.service;

import java.io.ByteArrayInputStream;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.logging.Logger;

import javax.annotation.Resource;
import javax.ejb.Stateless;
import javax.ejb.TransactionManagement;
import javax.ejb.TransactionManagementType;
import javax.enterprise.event.Event;
import javax.inject.Inject;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.Query;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;
import javax.transaction.UserTransaction;

import org.hl7.fhir.r4.formats.JsonParser;
import org.hl7.fhir.r4.model.Coding;
import org.hl7.fhir.r4.model.Condition;
import org.hl7.fhir.r4.model.Consent;
import org.hl7.fhir.r4.model.Coverage;
import org.hl7.fhir.r4.model.DocumentReference;
import org.hl7.fhir.r4.model.Endpoint;
import org.hl7.fhir.r4.model.Extension;
import org.hl7.fhir.r4.model.Observation;
import org.hl7.fhir.r4.model.Organization;
import org.hl7.fhir.r4.model.Patient;
import org.hl7.fhir.r4.model.Reference;
import org.hl7.fhir.r4.model.RelatedPerson;
import org.hl7.fhir.r4.model.ResourceType;

import net.aegis.fhir.model.Clientresource;
import net.aegis.fhir.service.util.UTCDateUtil;

/**
 * ClientresourceService services for basic data operations: findAll, create,
 * delete, read and update.
 *
 * The @Stateless annotation eliminates the need for manual transaction
 * demarcation
 *
 * @author richard.ettema
 *
 */
@Stateless
@TransactionManagement(TransactionManagementType.BEAN)
public class ClientresourceService {

	@Inject
	private Logger log;

	@PersistenceContext
	private EntityManager em;

	@Resource
	private UserTransaction userTransaction;

	@Inject
	private Event<Clientresource> clientresourceEventSrc;

	@Inject
	UTCDateUtil utcDateUtil;

	/**
	 * The create interaction creates a new Client Resource record.
	 *
	 * @param clientresource
	 * @return <code>Clientresource</code>
	 * @throws Exception
	 */
	public Clientresource create(Clientresource clientresource, org.hl7.fhir.r4.model.Resource resource) throws Exception {

		log.fine("[START] ClientresourceService.create");

		Clientresource newClientresource = null;

		try {
			newClientresource = clientresource.clone(false);
			newClientresource.setStatus("valid");
			newClientresource.setLastUser("system");
			newClientresource.setLastUpdate(new Date());

			// Build description based on expected resource type
			newClientresource.setDescription(this.buildClientresourceDescription(resource));

			/*
			 * TRANSACTION BEGIN
			 */
			userTransaction.begin();

			em.persist(newClientresource);

			clientresourceEventSrc.fire(newClientresource);

			/*
			 * TRANSACTION COMMIT(END)
			 */
			userTransaction.commit();
		} catch (Exception e) {
			// Exception caught
			e.printStackTrace();
			throw e;
		}

		return newClientresource;
	}

	/**
	 * The read interaction returns a single Client Resource record.
	 *
	 * @param id
	 * @return <code>Clientresource</code>
	 * @throws Exception
	 */
	public Clientresource read(Integer id) throws Exception {

		log.fine("[START] ClientresourceService.read");

		Clientresource clientresource = null;

		try {
			/*
			 * TRANSACTION BEGIN
			 */
			userTransaction.begin();

			clientresource = em.find(Clientresource.class, id);

			/*
			 * TRANSACTION COMMIT(END)
			 */
			userTransaction.commit();
		} catch (Exception e) {
			// Exception caught
			e.printStackTrace();
			throw e;
		}

		return clientresource;
	}

	/**
	 * The read interaction returns a single Client Resource record.
	 *
	 * @param resourceType
	 * @param resourceId
	 * @return <code>Clientresource</code>
	 * @throws Exception
	 */
	public Clientresource readClientResource(String resourceType, String resourceId) throws Exception {

		log.fine("[START] ClientresourceService.readClientResource");

		Clientresource clientresource = null;

		try {
			CriteriaBuilder cb = em.getCriteriaBuilder();
			CriteriaQuery<Clientresource> criteria = cb.createQuery(Clientresource.class);
			Root<Clientresource> resource = criteria.from(Clientresource.class);
			List<Predicate> predicateList = new ArrayList<Predicate>();
			predicateList.add(cb.equal(resource.get("resourceId"), resourceId));
			predicateList.add(cb.equal(resource.get("resourceType"), resourceType));

			criteria.select(resource)
				.where(cb.and(predicateList.toArray(new Predicate[predicateList.size()])));

			List<Clientresource> resources = em.createQuery(criteria).getResultList();

			if (resources != null && resources.size() > 0) {
				// Expecting only one match; return the first one
				clientresource = resources.get(0);
			}

		} catch (Exception e) {
			// Exception caught
			e.printStackTrace();
			throw e;
		}

		return clientresource;
	}

	/**
	 * Return the FHIR Resource instance from the repository resource.resourceContents
	 * converted to a FHIR Resource object
	 *
	 * @param id
	 * @return <code>org.hl7.fhir.r4.model.Resource</code>
	 * @throws Exception
	 */
	public org.hl7.fhir.r4.model.Resource readFHIRResource(Integer id) throws Exception {

		log.fine("[START] ResourceService.readFHIRResource(" + id + ")");

		Clientresource clientresource = null;
		org.hl7.fhir.r4.model.Resource foundFHIRResource = null;

		try {
			clientresource = this.read(id);

			if (clientresource != null && clientresource.getResourceContents() != null) {
				// Convert JSON contents to FHIR Resource object
				ByteArrayInputStream iResource = new ByteArrayInputStream(clientresource.getResourceContents());
				JsonParser jsonP = new JsonParser();
				foundFHIRResource = (org.hl7.fhir.r4.model.Resource)jsonP.parse(iResource);
			}

		} catch (Exception e) {
			// Exception caught
			e.printStackTrace();
			throw e;
		}

		return foundFHIRResource;
	}

	/**
	 * The update interaction modifies an existing Client Resource record.
	 *
	 * @param updateClientresource
	 * @return <code>Clientresource</code>
	 * @throws Exception
	 */
	public Clientresource update(Clientresource updateClientresource, org.hl7.fhir.r4.model.Resource resource) throws Exception {

		log.fine("[START] ClientresourceService.update");

		Clientresource clientresource = null;

		try {
			clientresource = read(updateClientresource.getId());

			clientresource.setResourceId(updateClientresource.getResourceId());
			clientresource.setResourceType(updateClientresource.getResourceType());
			//clientresource.setDescription(updateClientresource.getDescription());
			clientresource.setStatus(updateClientresource.getStatus());
			clientresource.setLastUser("system");
			clientresource.setLastUpdate(new Date());
			clientresource.setResourceContents(updateClientresource.getResourceContents());

			// Build description based on expected resource type
			clientresource.setDescription(this.buildClientresourceDescription(resource));

			/*
			 * TRANSACTION BEGIN
			 */
			userTransaction.begin();

			em.merge(clientresource);
			clientresourceEventSrc.fire(clientresource);

			/*
			 * TRANSACTION COMMIT(END)
			 */
			userTransaction.commit();
		} catch (Exception e) {
			// Exception caught
			e.printStackTrace();
			throw e;
		}

		return clientresource;
	}

	/**
	 * The delete interaction deletes a Client Resource record.
	 *
	 * @param id
	 * @throws Exception
	 */
	public int delete(Integer id) throws Exception {

		log.fine("[START] ClientresourceService.delete");

		int result = 0;

		try {
			Clientresource clientresource = em.find(Clientresource.class, id);

			if (clientresource != null) {
				/*
				 * TRANSACTION BEGIN
				 */
				userTransaction.begin();

				em.remove(clientresource);
				result = 1;

				/*
				 * TRANSACTION COMMIT(END)
				 */
				userTransaction.commit();
			}
		} catch (Exception e) {
			// Exception caught
			e.printStackTrace();
			throw e;
		}

		return result;
	}

	/**
	 * The findAll interaction returns all Client Resource records sorted by name
	 * ascending.
	 *
	 * @return <code>List<Clientresource></code>
	 * @throws Exception
	 */
	public List<Clientresource> findAll() throws Exception {

		log.fine("[START] ClientresourceService.findAll");

		List<Clientresource> result = new ArrayList<Clientresource>();

		try {
			CriteriaBuilder cb = em.getCriteriaBuilder();
			CriteriaQuery<Clientresource> criteria = cb.createQuery(Clientresource.class);
			Root<Clientresource> rootClientresource = criteria.from(Clientresource.class);

			criteria.select(rootClientresource).orderBy(cb.asc(rootClientresource.get("description")));

			result = em.createQuery(criteria).getResultList();

		} catch (Exception e) {
			// Exception caught
			e.printStackTrace();
			throw e;
		}

		return result;
	}

	/**
	 * Get the Client Resource records for a specific ResourceType
	 *
	 * @param basePath
	 * @return <code>List<Clientresource></code>
	 * @throws Exception
	 */
	@SuppressWarnings("unchecked")
	public List<Clientresource> findClientresourceByResourceType(String resourceType) throws Exception {

		log.fine("[START] ClientresourceService.findClientresourceByResourceType");

		Query clientresourceQuery = null;
		List<Clientresource> result = new ArrayList<Clientresource>();

		/*
		 * Generate the Clientresource list of all records for a resourceType value
		 */
		try {
			clientresourceQuery = em.createNamedQuery("findClientresourceByResourceType").setParameter("resourceType",
					resourceType);

			result = (List<Clientresource>) clientresourceQuery.getResultList();
		} catch (Exception e) {
			// Exception caught
			e.printStackTrace();
			throw e;
		}

		return result;
	}

	/**
	 * Execute a DDL truncate on the clientresource table
	 *
	 * @return <code>int</code> Number of entities updated or deleted
	 * @throws Exception
	 */
	public int clientresourcePurgeAll() throws Exception {

		log.fine("[START] ClientresourceService.clientresourcePurgeAll");

		Query clientresourceQuery = null;
		int result = 0;

		try {
			/*
			 *  TRANSACTION BEGIN
			 */
			userTransaction.begin();

			// Build native query for truncate clientresource
			String sQuery = "truncate clientresource";

			log.info("Native Query: " + sQuery);

			clientresourceQuery = em.createNativeQuery(sQuery);

			result = clientresourceQuery.executeUpdate();

			/*
			 *  TRANSACTION COMMIT(END)
			 */
			userTransaction.commit();

		} catch (Exception e) {
			// Exception caught
			e.printStackTrace();
			throw e;
		}

		return result;
	}

	/*
	 * Private methods
	 */

	private String buildClientresourceDescription(org.hl7.fhir.r4.model.Resource resource) {
		StringBuilder description = new StringBuilder();

		if (resource != null) {
			ResourceType type = resource.getResourceType();
			switch (type) {
			case Coverage:
				Coverage coverage = (Coverage)resource;
				description.append("(");
				description.append((coverage.hasIdentifier() ? coverage.getIdentifierFirstRep().getValue() : (coverage.hasId() ? coverage.getId() : "?")));
				description.append(") beneficiary: ");
				description.append((coverage.hasBeneficiary() ? (coverage.getBeneficiary().hasReference() ? coverage.getBeneficiary().getReference() : (coverage.getBeneficiary().hasIdentifier() ? coverage.getBeneficiary().getIdentifier().getValue() : "?")) : "?"));
				description.append("; subscriber: ");
				description.append((coverage.hasSubscriber() ? (coverage.getSubscriber().hasReference() ? coverage.getSubscriber().getReference() : (coverage.getSubscriber().hasIdentifier() ? coverage.getSubscriber().getIdentifier().getValue() : "?")) : "?"));
				break;
			case Condition:
				Condition condition = (Condition)resource;
				description.append("(");
				description.append((condition.hasIdentifier() ? condition.getIdentifierFirstRep().getValue() : (condition.hasId() ? condition.getId() : "?")));
				description.append(") patient: ");
				description.append((condition.hasSubject() ? (condition.getSubject().hasReference() ? condition.getSubject().getReference() : (condition.getSubject().hasIdentifier() ? condition.getSubject().getIdentifier().getValue() : "?")) : "?"));
				description.append("; cat: ");
				if (condition.hasCategory() && condition.getCategoryFirstRep().hasCoding()) {
					Coding catCoding = condition.getCategoryFirstRep().getCodingFirstRep();
					description.append(catCoding.hasDisplay() ? catCoding.getDisplay() : (catCoding.hasCode() ? catCoding.getCode() : "?"));
				}
				else {
					description.append("?");
				}
				description.append("; code: ");
				if (condition.hasCode() && condition.getCode().hasCoding()) {
					Coding codCoding = condition.getCode().getCodingFirstRep();
					description.append(codCoding.hasDisplay() ? codCoding.getDisplay() : (codCoding.hasCode() ? codCoding.getCode() : "?"));
				}
				else {
					description.append("?");
				}
				break;
			case Consent:
				Consent consent = (Consent)resource;
				description.append("(");
				description.append((consent.hasIdentifier() ? consent.getIdentifierFirstRep().getValue() : (consent.hasId() ? consent.getId() : "?")));
				description.append(") patient: ");
				description.append((consent.hasPatient() ? (consent.getPatient().hasReference() ? consent.getPatient().getReference() : (consent.getPatient().hasIdentifier() ? consent.getPatient().getIdentifier().getValue() : "?")) : "?"));
				description.append("; prov: ");
				description.append(consent.getProvision().getType().getDisplay());
				description.append("; grantee: ");
				Extension grantee = null;
				if (consent.hasExtension("http://hl7.org/fhir/5.0/StructureDefinition/extension-Consent.grantee")) {
					grantee = consent.getExtensionByUrl("http://hl7.org/fhir/5.0/StructureDefinition/extension-Consent.grantee");
					if (grantee.hasValue() && grantee.getValue() instanceof Reference) {
						Reference gRef = (Reference)grantee.getValue();
						description.append((gRef.hasReference() ? gRef.getReference() : (gRef.hasIdentifier() ? gRef.getIdentifier().getValue() : "?")));
					}
					else {
						description.append("?");
					}
				}
				else {
					description.append("?");
				}
				break;
			case DocumentReference:
				DocumentReference docRef = (DocumentReference)resource;
				description.append("(");
				description.append((docRef.hasIdentifier() ? docRef.getIdentifierFirstRep().getValue() : (docRef.hasId() ? docRef.getId() : "?")));
				description.append(") subject: ");
				description.append((docRef.hasSubject() ? (docRef.getSubject().hasReference() ? docRef.getSubject().getReference() : (docRef.getSubject().hasIdentifier() ? docRef.getSubject().getIdentifier().getValue() : "?")) : "?"));
				description.append("; type: ");
				description.append((docRef.hasType() ? (docRef.getType().hasCoding() ? (docRef.getType().getCodingFirstRep().getSystem()) : "?") : "?"));
				description.append("|");
				description.append((docRef.hasType() ? (docRef.getType().hasCoding() ? (docRef.getType().getCodingFirstRep().getCode()) : "?") : "?"));
				break;
			case Endpoint:
				Endpoint endpoint = (Endpoint)resource;
				description.append("(");
				description.append((endpoint.hasIdentifier() ? endpoint.getIdentifierFirstRep().getValue() : (endpoint.hasId() ? endpoint.getId() : "?")));
				description.append(") ");
				description.append((endpoint.hasName() ? endpoint.getName() : "?"));
				break;
			case Observation:
				Observation observation = (Observation)resource;
				description.append("(");
				description.append((observation.hasIdentifier() ? observation.getIdentifierFirstRep().getValue() : (observation.hasId() ? observation.getId() : "?")));
				description.append(") patient: ");
				description.append((observation.hasSubject() ? (observation.getSubject().hasReference() ? observation.getSubject().getReference() : (observation.getSubject().hasIdentifier() ? observation.getSubject().getIdentifier().getValue() : "?")) : "?"));
				description.append("; cat: ");
				if (observation.hasCategory() && observation.getCategoryFirstRep().hasCoding()) {
					Coding catCoding = observation.getCategoryFirstRep().getCodingFirstRep();
					description.append(catCoding.hasDisplay() ? catCoding.getDisplay() : (catCoding.hasCode() ? catCoding.getCode() : "?"));
				}
				else {
					description.append("?");
				}
				description.append("; code: ");
				if (observation.hasCode() && observation.getCode().hasCoding()) {
					Coding codCoding = observation.getCode().getCodingFirstRep();
					description.append(codCoding.hasDisplay() ? codCoding.getDisplay() : (codCoding.hasCode() ? codCoding.getCode() : "?"));
				}
				else {
					description.append("?");
				}
				break;
			case Organization:
				Organization organization = (Organization)resource;
				description.append("(");
				description.append((organization.hasIdentifier() ? organization.getIdentifierFirstRep().getValue() : (organization.hasId() ? organization.getId() : "?")));
				description.append(") ");
				description.append((organization.hasName() ? organization.getName() : "?"));
				break;
			case Patient:
				Patient patient = (Patient)resource;
				description.append((patient.hasName() ? (patient.getNameFirstRep().hasFamily() ? patient.getNameFirstRep().getFamily() + ", " + (patient.getNameFirstRep().hasGiven() ? patient.getNameFirstRep().getGivenAsSingleString() : "?") : "?") : "??"));
				description.append(" (");
				description.append((patient.hasIdentifier() ? patient.getIdentifierFirstRep().getValue() : (patient.hasId() ? patient.getId() : "?")));
				description.append("); g: ");
				description.append((patient.hasGender() ? patient.getGender().getDisplay() : "?"));
				description.append("; b: ");
				description.append((patient.hasBirthDate() ? utcDateUtil.formatDate(patient.getBirthDate(), UTCDateUtil.DATE_ONLY_FORMAT_UTC) : "?"));
				break;
			case RelatedPerson:
				RelatedPerson relatedPerson = (RelatedPerson)resource;
				description.append((relatedPerson.hasName() ? (relatedPerson.getNameFirstRep().hasFamily() ? relatedPerson.getNameFirstRep().getFamily() + ", " + (relatedPerson.getNameFirstRep().hasGiven() ? relatedPerson.getNameFirstRep().getGivenAsSingleString() : "?") : "?") : "??"));
				description.append(" (");
				description.append((relatedPerson.hasIdentifier() ? relatedPerson.getIdentifierFirstRep().getValue() : (relatedPerson.hasId() ? relatedPerson.getId() : "?")));
				description.append("); g: ");
				description.append((relatedPerson.hasGender() ? relatedPerson.getGender().getDisplay() : "?"));
				description.append("; b: ");
				description.append((relatedPerson.hasBirthDate() ? utcDateUtil.formatDate(relatedPerson.getBirthDate(), UTCDateUtil.DATE_ONLY_FORMAT_UTC) : "?"));
				break;
			default:
				description.append(type.name() + "; id: " + resource.getId());
				break;
			}
		}
		else {
			description.append("Resource contents null!");
		}

		return description.toString();
	}
}
