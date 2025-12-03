/*
-- Insert default resourcemetadata
*/
INSERT INTO wildfhirr4.resourcemetadata (id,resourceJoinId,paramName,paramType,paramValue,systemValue,codeValue,textValue,paramValueU,textValueU) VALUES
(1,1,'_id','TOKEN','FASTConsentSubscriptionTopic',NULL,NULL,NULL,'FASTCONSENTSUBSCRIPTIONTOPIC',NULL),
(2,1,'_lastUpdated','DATE','20250627150128',NULL,'20250627110128',NULL,'20250627150128',NULL),
(3,1,'derived-or-self','URI','http://hl7.org/fhir/us/consent-management/SubscriptionTopic/FASTConsentSubscriptionTopic',NULL,NULL,NULL,'HTTP://HL7.ORG/FHIR/US/CONSENT-MANAGEMENT/SUBSCRIPTIONTOPIC/FASTCONSENTSUBSCRIPTIONTOPIC',NULL),
(4,1,'url','URI','http://hl7.org/fhir/us/consent-management/SubscriptionTopic/FASTConsentSubscriptionTopic',NULL,NULL,NULL,'HTTP://HL7.ORG/FHIR/US/CONSENT-MANAGEMENT/SUBSCRIPTIONTOPIC/FASTCONSENTSUBSCRIPTIONTOPIC',NULL),
(5,1,'version','TOKEN','1.0.0',NULL,NULL,NULL,'1.0.0',NULL),
(6,1,'title','STRING','FAST Consent Subscription Topic',NULL,NULL,NULL,'FAST CONSENT SUBSCRIPTION TOPIC',NULL),
(7,1,'status','TOKEN','active',NULL,NULL,NULL,'ACTIVE',NULL),
(8,1,'date','DATE','20250301050000',NULL,'20250301000000',NULL,'20250301050000',NULL);
