/*
-- Query: SELECT * FROM fastconsentr4.resourcemetadata
LIMIT 0, 2000

-- Date: 2026-09-10 10:29
*/
INSERT INTO fastconsentr4.resourcemetadata (id,resourceJoinId,paramName,paramType,paramValue,systemValue,codeValue,textValue,paramValueU,textValueU) VALUES
(1,1,'_id','TOKEN','organization-fastconsentri',NULL,NULL,NULL,'ORGANIZATION-FASTCONSENTRI',NULL),
(2,1,'_lastUpdated','DATE','20260910142512',NULL,'20260910102512',NULL,'20260910142512',NULL),
(3,1,'active','TOKEN','true',NULL,NULL,NULL,'TRUE',NULL),
(4,1,'identifier','TOKEN','organization-fastconsentri','http://example.org/identifiers',NULL,NULL,'ORGANIZATION-FASTCONSENTRI',NULL),
(5,1,'name','STRING','FAST Consent Administrative Server - Reference Implementation',NULL,NULL,NULL,'FAST CONSENT ADMINISTRATIVE SERVER - REFERENCE IMPLEMENTATION',NULL),
(6,1,'phonetic','STRING','FAST Consent Administrative Server - Reference Implementation',NULL,NULL,NULL,'FAST CONSENT ADMINISTRATIVE SERVER - REFERENCE IMPLEMENTATION',NULL),
(7,1,'type','TOKEN',NULL,'http://terminology.hl7.org/CodeSystem/organization-type',NULL,'Other',NULL,'OTHER'),
(8,2,'_id','TOKEN','FASTConsentSubscriptionTopic',NULL,NULL,NULL,'FASTCONSENTSUBSCRIPTIONTOPIC',NULL),
(9,2,'_lastUpdated','DATE','20260910142513',NULL,'20260910102513',NULL,'20260910142513',NULL),
(10,2,'derived-or-self','URI','http://hl7.org/fhir/us/consent-management/SubscriptionTopic/FASTConsentSubscriptionTopic',NULL,NULL,NULL,'HTTP://HL7.ORG/FHIR/US/CONSENT-MANAGEMENT/SUBSCRIPTIONTOPIC/FASTCONSENTSUBSCRIPTIONTOPIC',NULL),
(11,2,'url','URI','http://hl7.org/fhir/us/consent-management/SubscriptionTopic/FASTConsentSubscriptionTopic',NULL,NULL,NULL,'HTTP://HL7.ORG/FHIR/US/CONSENT-MANAGEMENT/SUBSCRIPTIONTOPIC/FASTCONSENTSUBSCRIPTIONTOPIC',NULL),
(12,2,'version','TOKEN','1.0.0-preview',NULL,NULL,NULL,'1.0.0-PREVIEW',NULL),
(13,2,'title','STRING','FAST Consent Subscription Topic',NULL,NULL,NULL,'FAST CONSENT SUBSCRIPTION TOPIC',NULL),
(14,2,'status','TOKEN','active',NULL,NULL,NULL,'ACTIVE',NULL),
(15,2,'date','DATE','20250301050000',NULL,'20250301000000',NULL,'20250301050000',NULL);
