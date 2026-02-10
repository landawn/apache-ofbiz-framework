/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.apache.ofbiz.shipment.thirdparty.fedex;

import java.io.IOException;
import java.io.StringWriter;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.sql.Timestamp;
import java.util.Base64;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import javax.xml.parsers.ParserConfigurationException;

import org.apache.ofbiz.base.util.Debug;
import org.apache.ofbiz.base.util.GeneralException;
import org.apache.ofbiz.base.util.HttpClient;
import org.apache.ofbiz.base.util.HttpClientException;
import org.apache.ofbiz.base.util.UtilDateTime;
import org.apache.ofbiz.base.util.UtilMisc;
import org.apache.ofbiz.base.util.UtilProperties;
import org.apache.ofbiz.base.util.UtilValidate;
import org.apache.ofbiz.base.util.UtilXml;
import org.apache.ofbiz.base.util.template.FreeMarkerWorker;
import org.apache.ofbiz.entity.Delegator;
import org.apache.ofbiz.entity.GenericEntityException;
import org.apache.ofbiz.entity.GenericValue;
import org.apache.ofbiz.entity.condition.EntityCondition;
import org.apache.ofbiz.entity.condition.EntityOperator;
import org.apache.ofbiz.entity.util.EntityUtil;
import org.apache.ofbiz.entity.util.EntityUtilProperties;
import org.apache.ofbiz.persistence.dao.CarrierShipmentBoxTypeDao;
import org.apache.ofbiz.persistence.dao.DaoRegistry;
import org.apache.ofbiz.persistence.dao.ShipmentGatewayFedexDao;
import org.apache.ofbiz.persistence.dao.UserLoginDao;
import org.apache.ofbiz.persistence.entity.CarrierShipmentBoxTypeEntity;
import org.apache.ofbiz.persistence.entity.ShipmentGatewayFedexEntity;
import org.apache.ofbiz.party.party.PartyHelper;
import org.apache.ofbiz.service.DispatchContext;
import org.apache.ofbiz.service.GenericServiceException;
import org.apache.ofbiz.service.LocalDispatcher;
import org.apache.ofbiz.service.ServiceUtil;
import org.apache.ofbiz.shipment.shipment.ShipmentServices;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.xml.sax.SAXException;


import com.landawn.abacus.util.Beans;
import org.apache.ofbiz.persistence.entity.x;
import org.apache.ofbiz.model.ServiceContext;
import org.apache.ofbiz.model.FedexServicesContext;
/**
 * Fedex Shipment Services
 *
 * Implementation of Fedex shipment interface using Ship Manager Direct API
 *
 * TODO: FDXShipDeleteRequest/Reply (on error and via service call)
 * TODO: FDXCloseRequest/Reply
 * TODO: FDXRateRequest/Reply
 * TODO: FDXTrackRequest/Reply
 * TODO: International shipments
 * TODO: Multi-piece shipments
 * TODO: Freight shipments
 */
public class FedexServices {

    private static final String MODULE = FedexServices.class.getName();
    public static final String SHIPMENT_PROPERTIES_FILE = x.shipment_properties;
    private static final String RES_ERROR = x.ProductUiLabels;

    /**
     * Opens a URL to Fedex and makes a request.
     * @param xmlString XML message to send
     * @param delegator the delegator
     * @param shipmentGatewayConfigId the shipmentGatewayConfigId
     * @param resource RESOURCE file name
     * @return XML string response from FedEx
     * @throws FedexConnectException
     */
    public static String sendFedexRequest(String xmlString, Delegator delegator, String shipmentGatewayConfigId,
            String resource, Locale locale) throws FedexConnectException {
        String url = getShipmentGatewayConfigValue(delegator, shipmentGatewayConfigId, x.connectUrl, resource, x.shipment_fedex_connect_url);
        if (UtilValidate.isEmpty(url)) {
            throw new FedexConnectException(UtilProperties.getMessage(RES_ERROR,
                    x.FacilityShipmentFedexConnectUrlIncomplete, locale));
        }

        // xmlString should contain the auth document at the beginning
        // all documents require an <?xml version="1.0" encoding="UTF-8" ?> header
        if (!xmlString.matches(x.s_xml_s_version_1_0_s_encoding_UTF_8_s)) {
            throw new FedexConnectException(UtilProperties.getMessage(RES_ERROR,
                    x.FacilityShipmentFedexXmlHeaderMalformed, locale));
        }

        // prepare the connect string
        url = url.trim();

        String timeOutStr = getShipmentGatewayConfigValue(delegator, shipmentGatewayConfigId, x.connectTimeout,
                resource, x.shipment_fedex_connect_timeout, x._60);
        int timeout = 60;
        try {
            timeout = Integer.parseInt(timeOutStr);
        } catch (NumberFormatException e) {
            Debug.logError(e, x.Unable_to_set_timeout_to + timeOutStr + x.using_default + timeout);
        }

        if (Debug.verboseOn()) {
            Debug.logVerbose(x.Fedex_Connect_URL + url, MODULE);
            Debug.logVerbose(x.Fedex_XML_String + xmlString, MODULE);
        }

        HttpClient http = new HttpClient(url);
        http.setTimeout(timeout * 1000);
        String response = null;
        try {
            response = http.post(xmlString);
        } catch (HttpClientException e) {
            Debug.logError(e, x.Problem_connecting_to_Fedex_server, MODULE);
            throw new FedexConnectException(UtilProperties.getMessage(RES_ERROR, x.FacilityShipmentFedexConnectUrlProblem,
                    UtilMisc.toMap(x.errorString, e.toString()), locale));
        }

        if (response == null) {
            throw new FedexConnectException(UtilProperties.getMessage(RES_ERROR,
                    x.FacilityShipmentFedexReceivedNullResponse, locale));
        }
        if (Debug.verboseOn()) {
            Debug.logVerbose(x.Fedex_Response + response, MODULE);
        }

        return response;
    }

    /**
     * Fedex subscription request map. Register a Fedex account for shipping by obtaining the meter number
     * @param dctx the dctx
     * @param context the context
     * @return the map
     */
    public static Map<String, Object> fedexSubscriptionRequest(DispatchContext dctx, FedexServicesContext context) {
        Delegator delegator = dctx.getDelegator();
        String shipmentGatewayConfigId = (String) context.get(x.shipmentGatewayConfigId);
        String resource = (String) context.get(x.configProps);
        Locale locale = (Locale) context.get(x.locale);
        List<Object> errorList = new LinkedList<>();

        Boolean replaceMeterNumber = (Boolean) context.get(x.replaceMeterNumber);

        if (!replaceMeterNumber) {
            String meterNumber = getShipmentGatewayConfigValue(delegator, shipmentGatewayConfigId, x.accessMeterNumber,
                    resource, x.shipment_fedex_access_meterNumber);
            if (UtilValidate.isNotEmpty(meterNumber)) {
                return ServiceUtil.returnError(UtilProperties.getMessage(RES_ERROR,
                        x.FacilityShipmentFedexMeterNumberAlreadyExists,
                        UtilMisc.toMap(x.meterNumber, meterNumber), locale));
            }
        }

        String companyPartyId = (String) context.get(x.companyPartyId);
        String contactPartyName = (String) context.get(x.contactPartyName);

        Map<String, Object> result = new HashMap<>();

        String accountNumber = getShipmentGatewayConfigValue(delegator, shipmentGatewayConfigId, x.accessAccountNbr,
                resource, x.shipment_fedex_access_accountNbr);
        if (UtilValidate.isEmpty(accountNumber)) {
            return ServiceUtil.returnError(UtilProperties.getMessage(RES_ERROR,
                    x.FacilityShipmentFedexAccountNumberNotFound, locale));
        }

        if (UtilValidate.isEmpty(contactPartyName)) {
            return ServiceUtil.returnError(UtilProperties.getMessage(RES_ERROR,
                    x.FacilityShipmentFedexContactNameCannotBeEmpty, locale));
        }

        String companyName = null;
        GenericValue postalAddress = null;
        String phoneNumber = null;
        String faxNumber = null;
        String emailAddress = null;
        try {
            // Make sure the company exists
            UserLoginDao partyDao = DaoRegistry.getDao(delegator, x.Party, UserLoginDao.class);
            GenericValue companyParty = partyDao.findOne(delegator, x.Party, UtilMisc.toMap(x.partyId, companyPartyId), true);
            if (companyParty == null) {
                String errorMessage = x.Party_with_partyId + companyPartyId + x.does_not_exist;
                Debug.logError(errorMessage, MODULE);
                return ServiceUtil.returnError(UtilProperties.getMessage(RES_ERROR,
                        x.FacilityShipmentFedexCompanyPartyDoesNotExists,
                        UtilMisc.toMap(x.companyPartyId, companyPartyId), locale));
            }

            // Get the company name (required by Fedex)
            companyName = PartyHelper.getPartyName(companyParty);
            if (UtilValidate.isEmpty(companyName)) {
                String errorMessage = x.Party_with_partyId + companyPartyId + x.has_no_name;
                Debug.logError(errorMessage, MODULE);
                return ServiceUtil.returnError(UtilProperties.getMessage(RES_ERROR,
                        x.FacilityShipmentFedexCompanyPartyHasNoName,
                        UtilMisc.toMap(x.companyPartyId, companyPartyId), locale));
            }

            // Get the contact information for the company
            UserLoginDao partyContactDetailByPurposeDao = DaoRegistry.getDao(delegator, x.PartyContactDetailByPurpose, UserLoginDao.class);
            List<GenericValue> partyContactDetails = partyContactDetailByPurposeDao.findByAnd(delegator, x.PartyContactDetailByPurpose,
                    UtilMisc.toMap(x.partyId, companyPartyId), null, false);
            Timestamp nowTimestamp = UtilDateTime.nowTimestamp();
            partyContactDetails = EntityUtil.filterByDate(partyContactDetails, nowTimestamp, x.fromDate, x.thruDate, true);
            partyContactDetails = EntityUtil.filterByDate(partyContactDetails, nowTimestamp, x.purposeFromDate, x.purposeThruDate, true);

            // Get the first valid postal address (address1, city, postalCode and countryGeoId are required by Fedex)
            List<EntityCondition> postalAddressConditions = new LinkedList<>();
            postalAddressConditions.add(EntityCondition.makeCondition(x.contactMechTypeId, EntityOperator.EQUALS, x.POSTAL_ADDRESS));
            postalAddressConditions.add(EntityCondition.makeCondition(x.address1, EntityOperator.NOT_EQUAL, null));
            postalAddressConditions.add(EntityCondition.makeCondition(x.address1, EntityOperator.NOT_EQUAL, x.emptyString));
            postalAddressConditions.add(EntityCondition.makeCondition(x.city, EntityOperator.NOT_EQUAL, null));
            postalAddressConditions.add(EntityCondition.makeCondition(x.city, EntityOperator.NOT_EQUAL, x.emptyString));
            postalAddressConditions.add(EntityCondition.makeCondition(x.postalCode, EntityOperator.NOT_EQUAL, null));
            postalAddressConditions.add(EntityCondition.makeCondition(x.postalCode, EntityOperator.NOT_EQUAL, x.emptyString));
            postalAddressConditions.add(EntityCondition.makeCondition(x.countryGeoId, EntityOperator.NOT_EQUAL, null));
            postalAddressConditions.add(EntityCondition.makeCondition(x.countryGeoId, EntityOperator.NOT_EQUAL, x.emptyString));
            List<GenericValue> postalAddresses = EntityUtil.filterByCondition(partyContactDetails,
                    EntityCondition.makeCondition(postalAddressConditions, EntityOperator.AND));

            // Fedex requires USA or Canada addresses to have a state/province ID, so filter out the ones without
            postalAddressConditions.clear();
            postalAddressConditions.add(EntityCondition.makeCondition(x.countryGeoId, EntityOperator.IN, UtilMisc.toList(x.CAN, x.USA)));
            postalAddressConditions.add(EntityCondition.makeCondition(x.stateProvinceGeoId, EntityOperator.EQUALS, null));
            postalAddresses = EntityUtil.filterOutByCondition(postalAddresses, EntityCondition.makeCondition(postalAddressConditions,
                    EntityOperator.AND));
            postalAddressConditions.clear();
            postalAddressConditions.add(EntityCondition.makeCondition(x.countryGeoId, EntityOperator.IN, UtilMisc.toList(x.CAN, x.USA)));
            postalAddressConditions.add(EntityCondition.makeCondition(x.stateProvinceGeoId, EntityOperator.EQUALS, x.emptyString));
            postalAddresses = EntityUtil.filterOutByCondition(postalAddresses, EntityCondition.makeCondition(postalAddressConditions,
                    EntityOperator.AND));

            postalAddress = EntityUtil.getFirst(postalAddresses);
            if (UtilValidate.isEmpty(postalAddress)) {
                String errorMessage = x.Party_with_partyId + companyPartyId + x.does_not_have_a_current_fully_populated_postal_address;
                Debug.logError(errorMessage, MODULE);
                return ServiceUtil.returnError(UtilProperties.getMessage(RES_ERROR,
                        x.FacilityShipmentFedexCompanyPartyHasNotPostalAddress,
                        UtilMisc.toMap(x.companyPartyId, companyPartyId), locale));
            }
            UserLoginDao geoDao = DaoRegistry.getDao(delegator, x.Geo, UserLoginDao.class);
            GenericValue countryGeo = geoDao.findOne(delegator, x.Geo, UtilMisc.toMap(x.geoId, postalAddress.getString(x.countryGeoId)), true);
            String countryCode = countryGeo.getString(x.geoCode);
            String stateOrProvinceCode = null;
            // Only add the StateOrProvinceCode element if the address is in USA or Canada
            if (x.CA.equals(countryCode) || x.US.equals(countryCode)) {
                GenericValue stateProvinceGeo = geoDao.findOne(delegator, x.Geo,
                        UtilMisc.toMap(x.geoId, postalAddress.getString(x.stateProvinceGeoId)), true);
                stateOrProvinceCode = stateProvinceGeo.getString(x.geoCode);
            }

            // Get the first valid primary phone number (required by Fedex)
            List<EntityCondition> phoneNumberConditions = new LinkedList<>();
            phoneNumberConditions.add(EntityCondition.makeCondition(x.contactMechTypeId, EntityOperator.EQUALS, x.TELECOM_NUMBER));
            phoneNumberConditions.add(EntityCondition.makeCondition(x.contactMechPurposeTypeId, EntityOperator.EQUALS, x.PRIMARY_PHONE));
            phoneNumberConditions.add(EntityCondition.makeCondition(x.areaCode, EntityOperator.NOT_EQUAL, null));
            phoneNumberConditions.add(EntityCondition.makeCondition(x.areaCode, EntityOperator.NOT_EQUAL, x.emptyString));
            phoneNumberConditions.add(EntityCondition.makeCondition(x.contactNumber, EntityOperator.NOT_EQUAL, null));
            phoneNumberConditions.add(EntityCondition.makeCondition(x.contactNumber, EntityOperator.NOT_EQUAL, x.emptyString));
            List<GenericValue> phoneNumbers = EntityUtil.filterByCondition(partyContactDetails, EntityCondition.makeCondition(phoneNumberConditions,
                    EntityOperator.AND));
            GenericValue phoneNumberValue = EntityUtil.getFirst(phoneNumbers);
            if (UtilValidate.isEmpty(phoneNumberValue)) {
                String errorMessage = x.Party_with_partyId + companyPartyId + x.does_not_have_a_current_fully_populated_primary_phone_number;
                Debug.logError(errorMessage, MODULE);
                return ServiceUtil.returnError(UtilProperties.getMessage(RES_ERROR,
                        x.FacilityShipmentFedexCompanyPartyHasNotPrimaryPhoneNumber,
                        UtilMisc.toMap(x.companyPartyId, companyPartyId), locale));
            }
            phoneNumber = phoneNumberValue.getString(x.areaCode) + phoneNumberValue.getString(x.contactNumber);
            // Fedex doesn't want the North American country code
            if (UtilValidate.isNotEmpty(phoneNumberValue.getString(x.countryCode)) && !(x.CA.equals(countryCode) || x.US.equals(countryCode))) {
                phoneNumber = phoneNumberValue.getString(x.countryCode) + phoneNumber;
            }
            phoneNumber = phoneNumber.replaceAll(x.d_9527c7ba, x.emptyString);

            // Get the first valid fax number
            List<EntityCondition> faxNumberConditions = new LinkedList<>();
            faxNumberConditions.add(EntityCondition.makeCondition(x.contactMechTypeId, EntityOperator.EQUALS, x.TELECOM_NUMBER));
            faxNumberConditions.add(EntityCondition.makeCondition(x.contactMechPurposeTypeId, EntityOperator.EQUALS, x.FAX_NUMBER));
            faxNumberConditions.add(EntityCondition.makeCondition(x.areaCode, EntityOperator.NOT_EQUAL, null));
            faxNumberConditions.add(EntityCondition.makeCondition(x.areaCode, EntityOperator.NOT_EQUAL, x.emptyString));
            faxNumberConditions.add(EntityCondition.makeCondition(x.contactNumber, EntityOperator.NOT_EQUAL, null));
            faxNumberConditions.add(EntityCondition.makeCondition(x.contactNumber, EntityOperator.NOT_EQUAL, x.emptyString));
            List<GenericValue> faxNumbers = EntityUtil.filterByCondition(partyContactDetails, EntityCondition.makeCondition(faxNumberConditions,
                    EntityOperator.AND));
            GenericValue faxNumberValue = EntityUtil.getFirst(faxNumbers);
            if (!UtilValidate.isEmpty(faxNumberValue)) {
                faxNumber = faxNumberValue.getString(x.areaCode) + faxNumberValue.getString(x.contactNumber);
                // Fedex doesn't want the North American country code
                if (UtilValidate.isNotEmpty(faxNumberValue.getString(x.countryCode)) && !(x.CA.equals(countryCode) || x.US.equals(countryCode))) {
                    faxNumber = faxNumberValue.getString(x.countryCode) + faxNumber;
                }
                faxNumber = faxNumber.replaceAll(x.d_9527c7ba, x.emptyString);
            }

            // Get the first valid email address
            List<EntityCondition> emailConditions = new LinkedList<>();
            emailConditions.add(EntityCondition.makeCondition(x.contactMechTypeId, EntityOperator.EQUALS, x.EMAIL_ADDRESS));
            emailConditions.add(EntityCondition.makeCondition(x.infoString, EntityOperator.NOT_EQUAL, null));
            emailConditions.add(EntityCondition.makeCondition(x.infoString, EntityOperator.NOT_EQUAL, x.emptyString));
            List<GenericValue> emailAddresses = EntityUtil.filterByCondition(partyContactDetails, EntityCondition.makeCondition(emailConditions,
                    EntityOperator.AND));
            GenericValue emailAddressValue = EntityUtil.getFirst(emailAddresses);
            if (!UtilValidate.isEmpty(emailAddressValue)) {
                emailAddress = emailAddressValue.getString(x.infoString);
            }

            // Get the location of the Freemarker (XML) template for the FDXSubscriptionRequest
            String templateLocation = getShipmentGatewayConfigValue(delegator, shipmentGatewayConfigId, x.templateSubscription, resource,
                    x.shipment_fedex_template_subscription_location);
            if (UtilValidate.isEmpty(templateLocation)) {
                return ServiceUtil.returnError(UtilProperties.getMessage(RES_ERROR,
                        x.FacilityShipmentFedexSubscriptionTemplateLocationNotFound,
                        UtilMisc.toMap(x.templateLocation, templateLocation), locale));
            }

            // Populate the Freemarker context
            Map<String, Object> subscriptionRequestContext = new HashMap<>();
            subscriptionRequestContext.put(x.AccountNumber, accountNumber);
            subscriptionRequestContext.put(x.PersonName, contactPartyName);
            subscriptionRequestContext.put(x.CompanyName, companyName);
            subscriptionRequestContext.put(x.PhoneNumber, phoneNumber);
            if (UtilValidate.isNotEmpty(faxNumber)) {
                subscriptionRequestContext.put(x.FaxNumber, faxNumber);
            }
            if (UtilValidate.isNotEmpty(emailAddress)) {
                subscriptionRequestContext.put(x.EMailAddress, emailAddress);
            }
            subscriptionRequestContext.put(x.Line1, postalAddress.getString(x.address1));
            if (UtilValidate.isNotEmpty(postalAddress.getString(x.address2))) {
                subscriptionRequestContext.put(x.Line2, postalAddress.getString(x.address2));
            }
            subscriptionRequestContext.put(x.City, postalAddress.getString(x.city));
            if (UtilValidate.isNotEmpty(stateOrProvinceCode)) {
                subscriptionRequestContext.put(x.StateOrProvinceCode, stateOrProvinceCode);
            }
            subscriptionRequestContext.put(x.PostalCode, postalAddress.getString(x.postalCode));
            subscriptionRequestContext.put(x.CountryCode, countryCode);

            StringWriter outWriter = new StringWriter();
            try {
                FreeMarkerWorker.renderTemplate(templateLocation, subscriptionRequestContext, outWriter);
            } catch (Exception e) {
                String errorMessage = x.Cannot_send_Fedex_subscription_request_Failed_to_render_Fedex_XML_Subscription_Request_Template
                        + templateLocation + x.str_76d00394;
                Debug.logError(e, errorMessage, MODULE);
                return ServiceUtil.returnError(UtilProperties.getMessage(RES_ERROR,
                        x.FacilityShipmentFedexSubscriptionTemplateError,
                        UtilMisc.toMap(x.templateLocation, templateLocation, x.errorString, e.getMessage()), locale));
            }
            String fDXSubscriptionRequestString = outWriter.toString();

            // Send the request
            String fDXSubscriptionReplyString = null;
            try {
                fDXSubscriptionReplyString = sendFedexRequest(fDXSubscriptionRequestString, delegator, shipmentGatewayConfigId, resource, locale);
                Debug.logInfo(x.Fedex_response_for_FDXSubscriptionRequest + fDXSubscriptionReplyString, MODULE);
            } catch (FedexConnectException e) {
                String errorMessage = x.Error_sending_Fedex_request_for_FDXSubscriptionRequest + e.toString();
                Debug.logError(e, errorMessage, MODULE);
                return ServiceUtil.returnError(UtilProperties.getMessage(RES_ERROR,
                        x.FacilityShipmentFedexSubscriptionTemplateSendingError,
                        UtilMisc.toMap(x.errorString, e.toString()), locale));
            }

            Document fDXSubscriptionReplyDocument = null;
            try {
                fDXSubscriptionReplyDocument = UtilXml.readXmlDocument(fDXSubscriptionReplyString, false);
                Debug.logInfo(x.Fedex_response_for_FDXSubscriptionRequest + fDXSubscriptionReplyString, MODULE);
            } catch (SAXException | ParserConfigurationException | IOException e) {
                String errorMessage = x.Error_parsing_the_FDXSubscriptionRequest_response + e.toString();
                Debug.logError(e, errorMessage, MODULE);
                return ServiceUtil.returnError(UtilProperties.getMessage(RES_ERROR,
                        x.FacilityShipmentFedexSubscriptionTemplateParsingError,
                        UtilMisc.toMap(x.errorString, e.toString()), locale));
            }

            Element fedexSubscriptionReplyElement = fDXSubscriptionReplyDocument.getDocumentElement();
            handleErrors(fedexSubscriptionReplyElement, errorList, locale);

            if (UtilValidate.isNotEmpty(errorList)) {
                return ServiceUtil.returnError(errorList);
            }

            String meterNumber = UtilXml.childElementValue(fedexSubscriptionReplyElement, x.MeterNumber);

            result.put(x.meterNumber, meterNumber);

        } catch (GenericEntityException e) {
            Debug.logError(e, MODULE);
            return ServiceUtil.returnError(e.getMessage());
        }

        return result;
    }

    /**
     * Send a FDXShipRequest via the Ship Manager Direct API
     */
    public static Map<String, Object> fedexShipRequest(DispatchContext dctx, FedexServicesContext context) {
        Delegator delegator = dctx.getDelegator();
        LocalDispatcher dispatcher = dctx.getDispatcher();
        Locale locale = (Locale) context.get(x.locale);
        String shipmentId = (String) context.get(x.shipmentId);
        String shipmentRouteSegmentId = (String) context.get(x.shipmentRouteSegmentId);

        Map<String, Object> shipmentGatewayConfig = ShipmentServices.getShipmentGatewayConfigFromShipment(delegator, shipmentId, locale);
        String shipmentGatewayConfigId = (String) shipmentGatewayConfig.get(x.shipmentGatewayConfigId);
        String resource = (String) shipmentGatewayConfig.get(x.configProps);
        if (UtilValidate.isEmpty(shipmentGatewayConfigId) && UtilValidate.isEmpty(resource)) {
            return ServiceUtil.returnError(UtilProperties.getMessage(RES_ERROR, x.FacilityShipmentFedexGatewayNotAvailable, locale));
        }

        // Get the location of the Freemarker (XML) template for the FDXShipRequest
        String templateLocation = getShipmentGatewayConfigValue(delegator, shipmentGatewayConfigId, x.templateShipment,
                resource, x.shipment_fedex_template_ship_location);
        if (UtilValidate.isEmpty(templateLocation)) {
            return ServiceUtil.returnError(UtilProperties.getMessage(RES_ERROR, x.FacilityShipmentFedexShipmentTemplateLocationNotFound,
                    UtilMisc.toMap(x.templateLocation, templateLocation), locale));
        }

        // Get the Fedex account number
        String accountNumber = getShipmentGatewayConfigValue(delegator, shipmentGatewayConfigId, x.accessAccountNbr,
                resource, x.shipment_fedex_access_accountNbr);
        if (UtilValidate.isEmpty(accountNumber)) {
            return ServiceUtil.returnError(UtilProperties.getMessage(RES_ERROR, x.FacilityShipmentFedexAccountNumberNotFound, locale));
        }

        // Get the Fedex meter number
        String meterNumber = getShipmentGatewayConfigValue(delegator, shipmentGatewayConfigId, x.accessMeterNumber,
                resource, x.shipment_fedex_access_meterNumber);
        if (UtilValidate.isEmpty(meterNumber)) {
            return ServiceUtil.returnError(UtilProperties.getMessage(RES_ERROR, x.FacilityShipmentFedexMeterNumberNotFound,
                    UtilMisc.toMap(x.meterNumber, meterNumber), locale));
        }

        // Get the weight units to be used in the request
        String weightUomId = EntityUtilProperties.getPropertyValue(SHIPMENT_PROPERTIES_FILE, x.shipment_default_weight_uom, delegator);
        if (UtilValidate.isEmpty(weightUomId)) {
            return ServiceUtil.returnError(UtilProperties.getMessage(RES_ERROR, x.FacilityShipmentDefaultWeightUomIdNotFound, locale));
        } else if (!(x.WT_lb.equals(weightUomId) || x.WT_kg.equals(weightUomId))) {
            return ServiceUtil.returnError(UtilProperties.getMessage(RES_ERROR,
                    x.FacilityShipmentDefaultWeightUomIdNotValid, locale));
        }

        // Get the dimension units to be used in the request
        String dimensionsUomId = EntityUtilProperties.getPropertyValue(SHIPMENT_PROPERTIES_FILE, x.shipment_default_dimension_uom, delegator);
        if (UtilValidate.isEmpty(dimensionsUomId)) {
            return ServiceUtil.returnError(UtilProperties.getMessage(RES_ERROR, x.FacilityShipmentDefaultDimensionUomIdNotFound, locale));
        } else if (!(x.LEN_in.equals(dimensionsUomId) || x.LEN_cm.equals(dimensionsUomId))) {
            return ServiceUtil.returnError(UtilProperties.getMessage(RES_ERROR, x.FacilityShipmentDefaultDimensionUomIdNotValid, locale));
        }

        // Get the label image type to be returned
        String labelImageType = getShipmentGatewayConfigValue(delegator, shipmentGatewayConfigId, x.labelImageType,
                resource, x.shipment_fedex_labelImageType);
        if (UtilValidate.isEmpty(labelImageType)) {
            return ServiceUtil.returnError(UtilProperties.getMessage(RES_ERROR, x.FacilityShipmentFedexLabelImageTypeNotFound, locale));
        } else if (!(x.PDF.equals(labelImageType) || x.PNG.equals(labelImageType))) {
            return ServiceUtil.returnError(UtilProperties.getMessage(RES_ERROR, x.FacilityShipmentFedexLabelImageTypeNotValid, locale));
        }

        // Get the default dropoff type
        String dropoffType = getShipmentGatewayConfigValue(delegator, shipmentGatewayConfigId, x.defaultDropoffType,
                resource, x.shipment_fedex_default_dropoffType);
        if (UtilValidate.isEmpty(dropoffType)) {
            return ServiceUtil.returnError(UtilProperties.getMessage(RES_ERROR, x.FacilityShipmentFedexDropoffTypeNotFound, locale));
        }

        try {
            Map<String, Object> shipRequestContext = new HashMap<>();

            // Get the shipment and the shipmentRouteSegment
            UserLoginDao shipmentDao = DaoRegistry.getDao(delegator, x.Shipment, UserLoginDao.class);
            GenericValue shipment = shipmentDao.findOne(delegator, x.Shipment, UtilMisc.toMap(x.shipmentId, shipmentId), false);
            if (UtilValidate.isEmpty(shipment)) {
                return ServiceUtil.returnError(UtilProperties.getMessage(RES_ERROR,
                        x.ProductShipmentNotFoundId, locale) + shipmentId);
            }
            UserLoginDao shipmentRouteSegmentDao = DaoRegistry.getDao(delegator, x.ShipmentRouteSegment, UserLoginDao.class);
            GenericValue shipmentRouteSegment = shipmentRouteSegmentDao.findOne(delegator, x.ShipmentRouteSegment,
                    UtilMisc.toMap(x.shipmentId, shipmentId, x.shipmentRouteSegmentId, shipmentRouteSegmentId), false);
            if (UtilValidate.isEmpty(shipmentRouteSegment)) {
                return ServiceUtil.returnError(UtilProperties.getMessage(RES_ERROR, x.ProductShipmentRouteSegmentNotFound,
                        UtilMisc.toMap(x.shipmentId, shipmentId, x.shipmentRouteSegmentId, shipmentRouteSegmentId), locale));
            }

            // Determine the Fedex carrier
            String carrierPartyId = shipmentRouteSegment.getString(x.carrierPartyId);
            if (!x.FEDEX.equals(carrierPartyId)) {
                return ServiceUtil.returnError(UtilProperties.getMessage(RES_ERROR, x.FacilityShipmentFedexNotRouteSegmentCarrier,
                        UtilMisc.toMap(x.shipmentRouteSegmentId, shipmentRouteSegmentId, x.shipmentId, shipmentId), locale));
            }

            // Check the shipmentRouteSegment's carrier status
            if (UtilValidate.isNotEmpty(shipmentRouteSegment.getString(x.carrierServiceStatusId))
                    && !x.SHRSCS_NOT_STARTED.equals(shipmentRouteSegment.getString(x.carrierServiceStatusId))) {
                return ServiceUtil.returnError(UtilProperties.getMessage(RES_ERROR, x.FacilityShipmentFedexRouteSegmentStatusNotStarted,
                        UtilMisc.toMap(x.shipmentRouteSegmentId, shipmentRouteSegmentId, x.shipmentId, shipmentId, x.shipmentRouteSegmentStatus,
                                shipmentRouteSegment.getString(x.carrierServiceStatusId)), locale));
            }

            // Translate shipmentMethodTypeId to Fedex service code and carrier code
            String shipmentMethodTypeId = shipmentRouteSegment.getString(x.shipmentMethodTypeId);
            UserLoginDao carrierShipmentMethodDao = DaoRegistry.getDao(delegator, x.CarrierShipmentMethod, UserLoginDao.class);
            GenericValue carrierShipmentMethod = carrierShipmentMethodDao.findOne(delegator, x.CarrierShipmentMethod,
                    UtilMisc.toMap(x.shipmentMethodTypeId, shipmentMethodTypeId, x.partyId, x.FEDEX, x.roleTypeId, x.CARRIER), false);
            if (UtilValidate.isEmpty(carrierShipmentMethod)) {
                return ServiceUtil.returnError(UtilProperties.getMessage(RES_ERROR, x.FacilityShipmentFedexRouteSegmentCarrierShipmentMethodNotFound,
                        UtilMisc.toMap(x.shipmentId, shipmentId, x.shipmentRouteSegmentId, shipmentRouteSegmentId, x.carrierPartyId,
                                carrierPartyId, x.shipmentMethodTypeId, shipmentMethodTypeId), locale));
            }
            if (UtilValidate.isEmpty(carrierShipmentMethod.getString(x.carrierServiceCode))) {
                return ServiceUtil.returnError(UtilProperties.getMessage(RES_ERROR,
                        x.FacilityShipmentFedexNoCarrieServiceCode,
                        UtilMisc.toMap(x.shipmentMethodTypeId, shipmentMethodTypeId), locale));
            }
            String service = carrierShipmentMethod.getString(x.carrierServiceCode);

            // CarrierCode is FDXG only for FEDEXGROUND and GROUNDHOMEDELIVERY services.
            boolean isGroundService = x.FEDEXGROUND.equals(service) || x.GROUNDHOMEDELIVERY.equals(service);
            String carrierCode = isGroundService ? x.FDXG : x.FDXE;

            // Determine the currency by trying the shipmentRouteSegment, then the Shipment, then the framework's default currency,
            // and finally default to USD
            String currencyCode = null;
            if (UtilValidate.isNotEmpty(shipmentRouteSegment.getString(x.currencyUomId))) {
                currencyCode = shipmentRouteSegment.getString(x.currencyUomId);
            } else if (UtilValidate.isNotEmpty(shipment.getString(x.currencyUomId))) {
                currencyCode = shipment.getString(x.currencyUomId);
            } else {
                currencyCode = EntityUtilProperties.getPropertyValue(x.general, x.currency_uom_id_default, x.USD, delegator);
            }

            // Get and validate origin postal address
            GenericValue originPostalAddress = shipmentRouteSegment.getRelatedOne(x.OriginPostalAddress, false);
            if (UtilValidate.isEmpty(originPostalAddress)) {
                return ServiceUtil.returnError(UtilProperties.getMessage(RES_ERROR,
                        x.FacilityShipmentRouteSegmentOriginPostalAddressNotFound,
                        UtilMisc.toMap(x.shipmentId, shipmentId, x.shipmentRouteSegmentId, shipmentRouteSegmentId), locale));
            } else if (UtilValidate.isEmpty(originPostalAddress.getString(x.address1))
                       || UtilValidate.isEmpty(originPostalAddress.getString(x.city))
                       || UtilValidate.isEmpty(originPostalAddress.getString(x.postalCode))
                       || UtilValidate.isEmpty(originPostalAddress.getString(x.countryGeoId))) {
                return ServiceUtil.returnError(UtilProperties.getMessage(RES_ERROR,
                        x.FacilityShipmentRouteSegmentOriginPostalAddressNotComplete,
                        UtilMisc.toMap(x.shipmentId, shipmentId, x.shipmentRouteSegmentId, shipmentRouteSegmentId), locale));
            }
            GenericValue originCountryGeo = originPostalAddress.getRelatedOne(x.CountryGeo, false);
            if (UtilValidate.isEmpty(originCountryGeo)) {
                return ServiceUtil.returnError(UtilProperties.getMessage(RES_ERROR,
                        x.FacilityShipmentRouteSegmentOriginCountryGeoNotFound,
                        UtilMisc.toMap(x.shipmentId, shipmentId, x.shipmentRouteSegmentId, shipmentRouteSegmentId), locale));
            }

            String originAddressCountryCode = originCountryGeo.getString(x.geoCode);
            String originAddressStateOrProvinceCode = null;

            // Only add the StateOrProvinceCode element if the address is in USA or Canada
            if (x.CA.equals(originAddressCountryCode) || x.US.equals(originAddressCountryCode)) {
                if (UtilValidate.isEmpty(originPostalAddress.getString(x.stateProvinceGeoId))) {
                    return ServiceUtil.returnError(UtilProperties.getMessage(RES_ERROR,
                            x.FacilityShipmentRouteSegmentOriginStateProvinceGeoIdRequired,
                            UtilMisc.toMap(x.contactMechId, originPostalAddress.getString(x.contactMechId),
                                    x.shipmentId, shipmentId, x.shipmentRouteSegmentId, shipmentRouteSegmentId), locale));
                }
                UserLoginDao geoDao = DaoRegistry.getDao(delegator, x.Geo, UserLoginDao.class);
                GenericValue stateProvinceGeo = geoDao.findOne(delegator, x.Geo,
                        UtilMisc.toMap(x.geoId, originPostalAddress.getString(x.stateProvinceGeoId)), true);
                originAddressStateOrProvinceCode = stateProvinceGeo.getString(x.geoCode);
            }

            // Get and validate origin telecom number
            GenericValue originTelecomNumber = shipmentRouteSegment.getRelatedOne(x.OriginTelecomNumber, false);
            if (UtilValidate.isEmpty(originTelecomNumber)) {
                return ServiceUtil.returnError(UtilProperties.getMessage(RES_ERROR,
                        x.FacilityShipmentRouteSegmentOriginTelecomNumberNotFound,
                        UtilMisc.toMap(x.shipmentId, shipmentId, x.shipmentRouteSegmentId, shipmentRouteSegmentId), locale));
            }
            String originContactPhoneNumber = originTelecomNumber.getString(x.areaCode) + originTelecomNumber.getString(x.contactNumber);

            // Fedex doesn't want the North American country code
            if (UtilValidate.isNotEmpty(originTelecomNumber.getString(x.countryCode)) && !(x.CA.equals(originAddressCountryCode)
                    || x.US.equals(originAddressCountryCode))) {
                originContactPhoneNumber = originTelecomNumber.getString(x.countryCode) + originContactPhoneNumber;
            }
            originContactPhoneNumber = originContactPhoneNumber.replaceAll(x.d_9527c7ba, x.emptyString);

            // Get the origin contact name from the owner of the origin facility
            GenericValue partyFrom = null;
            GenericValue originFacility = shipment.getRelatedOne(x.OriginFacility, false);
            if (UtilValidate.isEmpty(originFacility)) {
                return ServiceUtil.returnError(UtilProperties.getMessage(RES_ERROR,
                        x.FacilityShipmentFedexOriginFacilityRequired,
                        UtilMisc.toMap(x.shipmentId, shipmentId, x.shipmentRouteSegmentId, shipmentRouteSegmentId), locale));
            } else {
                partyFrom = originFacility.getRelatedOne(x.OwnerParty, false);
                if (UtilValidate.isEmpty(partyFrom)) {
                    return ServiceUtil.returnError(UtilProperties.getMessage(RES_ERROR,
                            x.FacilityShipmentFedexOwnerPartyRequired,
                            UtilMisc.toMap(x.shipmentId, shipmentId, x.shipmentRouteSegmentId, shipmentRouteSegmentId,
                                    x.facilityId, originFacility.getString(x.facilityId)), locale));
                }
            }

            String originContactKey = x.PERSON.equals(partyFrom.getString(x.partyTypeId)) ? x.OriginContactPersonName : x.OriginContactCompanyName;
            String originContactName = PartyHelper.getPartyName(partyFrom, false);
            if (UtilValidate.isEmpty(originContactName)) {
                return ServiceUtil.returnError(UtilProperties.getMessage(RES_ERROR,
                        x.FacilityShipmentFedexPartyFromHasNoName,
                        UtilMisc.toMap(x.shipmentId, shipmentId, x.shipmentRouteSegmentId, shipmentRouteSegmentId), locale));
            }

            // Get and validate destination postal address
            GenericValue destinationPostalAddress = shipmentRouteSegment.getRelatedOne(x.DestPostalAddress, false);
            if (UtilValidate.isEmpty(destinationPostalAddress)) {
                return ServiceUtil.returnError(UtilProperties.getMessage(RES_ERROR,
                        x.FacilityShipmentRouteSegmentDestPostalAddressNotFound,
                        UtilMisc.toMap(x.shipmentId, shipmentId, x.shipmentRouteSegmentId, shipmentRouteSegmentId), locale));
            } else if (UtilValidate.isEmpty(destinationPostalAddress.getString(x.address1))
                       || UtilValidate.isEmpty(destinationPostalAddress.getString(x.city))
                       || UtilValidate.isEmpty(destinationPostalAddress.getString(x.postalCode))
                       || UtilValidate.isEmpty(destinationPostalAddress.getString(x.countryGeoId))) {
                return ServiceUtil.returnError(UtilProperties.getMessage(RES_ERROR,
                        x.FacilityShipmentRouteSegmentDestPostalAddressIncomplete,
                        UtilMisc.toMap(x.shipmentId, shipmentId, x.shipmentRouteSegmentId, shipmentRouteSegmentId), locale));
            }
            GenericValue destinationCountryGeo = destinationPostalAddress.getRelatedOne(x.CountryGeo, false);
            if (UtilValidate.isEmpty(destinationCountryGeo)) {
                return ServiceUtil.returnError(UtilProperties.getMessage(RES_ERROR,
                        x.FacilityShipmentRouteSegmentDestCountryGeoNotFound,
                        UtilMisc.toMap(x.shipmentId, shipmentId, x.shipmentRouteSegmentId, shipmentRouteSegmentId), locale));
            }
            String destinationAddressCountryCode = destinationCountryGeo.getString(x.geoCode);
            String destinationAddressStateOrProvinceCode = null;

            // Only add the StateOrProvinceCode element if the address is in USA or Canada
            if (x.CA.equals(destinationAddressCountryCode) || x.US.equals(destinationAddressCountryCode)) {
                if (UtilValidate.isEmpty(destinationPostalAddress.getString(x.stateProvinceGeoId))) {
                    return ServiceUtil.returnError(UtilProperties.getMessage(RES_ERROR,
                            x.FacilityShipmentRouteSegmentDestStateProvinceGeoIdNotFound,
                            UtilMisc.toMap(x.contactMechId, destinationPostalAddress.getString(x.contactMechId),
                                    x.shipmentId, shipmentId, x.shipmentRouteSegmentId, shipmentRouteSegmentId), locale));
                }
                UserLoginDao geoDao = DaoRegistry.getDao(delegator, x.Geo, UserLoginDao.class);
                GenericValue stateProvinceGeo = geoDao.findOne(delegator, x.Geo,
                        UtilMisc.toMap(x.geoId, destinationPostalAddress.getString(x.stateProvinceGeoId)), true);
                destinationAddressStateOrProvinceCode = stateProvinceGeo.getString(x.geoCode);
            }

            // Get and validate destination telecom number
            GenericValue destinationTelecomNumber = shipmentRouteSegment.getRelatedOne(x.DestTelecomNumber, false);
            if (UtilValidate.isEmpty(destinationTelecomNumber)) {
                return ServiceUtil.returnError(UtilProperties.getMessage(RES_ERROR,
                        x.FacilityShipmentRouteSegmentDestTelecomNumberNotFound,
                        UtilMisc.toMap(x.shipmentId, shipmentId, x.shipmentRouteSegmentId, shipmentRouteSegmentId), locale));
            }
            String destinationContactPhoneNumber = destinationTelecomNumber.getString(x.areaCode)
                    + destinationTelecomNumber.getString(x.contactNumber);

            // Fedex doesn't want the North American country code
            if (UtilValidate.isNotEmpty(destinationTelecomNumber.getString(x.countryCode)) && !(x.CA.equals(destinationAddressCountryCode)
                    || x.US.equals(destinationAddressCountryCode))) {
                destinationContactPhoneNumber = destinationTelecomNumber.getString(x.countryCode) + destinationContactPhoneNumber;
            }
            destinationContactPhoneNumber = destinationContactPhoneNumber.replaceAll(x.d_9527c7ba, x.emptyString);

            // Get the destination contact name
            String destinationPartyId = shipment.getString(x.partyIdTo);
            if (UtilValidate.isEmpty(destinationPartyId)) {
                return ServiceUtil.returnError(UtilProperties.getMessage(RES_ERROR,
                        x.FacilityShipmentFedexPartyToRequired,
                        UtilMisc.toMap(x.shipmentId, shipmentId, x.shipmentRouteSegmentId, shipmentRouteSegmentId), locale));
            }
            UserLoginDao partyDao = DaoRegistry.getDao(delegator, x.Party, UserLoginDao.class);
            GenericValue partyTo = partyDao.findOne(delegator, x.Party, UtilMisc.toMap(x.partyId, destinationPartyId), false);
            String destinationContactKey = x.PERSON.equals(partyTo.getString(x.partyTypeId)) ? x.DestinationContactPersonName
                    : x.DestinationContactCompanyName;
            String destinationContactName = PartyHelper.getPartyName(partyTo, false);
            if (UtilValidate.isEmpty(destinationContactName)) {
                return ServiceUtil.returnError(UtilProperties.getMessage(RES_ERROR,
                        x.FacilityShipmentFedexPartyToHasNoName,
                        UtilMisc.toMap(x.shipmentId, shipmentId, x.shipmentRouteSegmentId, shipmentRouteSegmentId), locale));
            }

            String homeDeliveryType = null;
            Timestamp homeDeliveryDate = null;
            if (x.GROUNDHOMEDELIVERY.equals(service)) {

                // Determine the home-delivery instructions
                homeDeliveryType = shipmentRouteSegment.getString(x.homeDeliveryType);
                if (UtilValidate.isNotEmpty(homeDeliveryType)) {
                    if (!(x.DATECERTAIN.equals(homeDeliveryType) || x.EVENING.equals(homeDeliveryType) || x.APPOINTMENT.equals(homeDeliveryType))) {
                        return ServiceUtil.returnError(UtilProperties.getMessage(RES_ERROR,
                                x.FacilityShipmentFedexHomeDeliveryTypeInvalid,
                                UtilMisc.toMap(x.shipmentId, shipmentId, x.shipmentRouteSegmentId, shipmentRouteSegmentId), locale));
                    }
                }
                homeDeliveryDate = shipmentRouteSegment.getTimestamp(x.homeDeliveryDate);
                if (UtilValidate.isEmpty(homeDeliveryDate)) {
                    return ServiceUtil.returnError(UtilProperties.getMessage(RES_ERROR,
                            x.FacilityShipmentFedexHomeDeliveryDateRequired,
                            UtilMisc.toMap(x.shipmentId, shipmentId, x.shipmentRouteSegmentId, shipmentRouteSegmentId), locale));
                } else if (homeDeliveryDate.before(UtilDateTime.nowTimestamp())) {
                    return ServiceUtil.returnError(UtilProperties.getMessage(RES_ERROR,
                            x.FacilityShipmentFedexHomeDeliveryDateBeforeCurrentDate,
                            UtilMisc.toMap(x.shipmentId, shipmentId, x.shipmentRouteSegmentId, shipmentRouteSegmentId), locale));
                }
            }

            List<GenericValue> shipmentPackageRouteSegs = shipmentRouteSegment.getRelated(x.ShipmentPackageRouteSeg, null,
                    UtilMisc.toList(x.shipmentPackageSeqId_39d5d38d), false);
            if (UtilValidate.isEmpty(shipmentPackageRouteSegs)) {
                return ServiceUtil.returnError(UtilProperties.getMessage(RES_ERROR, x.FacilityShipmentPackageRouteSegsNotFound,
                        UtilMisc.toMap(x.shipmentId, shipmentId, x.shipmentRouteSegmentId, shipmentRouteSegmentId), locale));
            }
            if (shipmentPackageRouteSegs.size() != 1) {
                return ServiceUtil.returnError(UtilProperties.getMessage(RES_ERROR,
                        x.FacilityShipmentFedexMultiplePackagesNotSupported, locale));
            }

            // TODO: Multi-piece shipments, including logic to cancel packages 1-n if FDXShipRequest n+1 fails

            // Populate the Freemarker context with the non-package-related information
            shipRequestContext.put(x.AccountNumber, accountNumber);
            shipRequestContext.put(x.MeterNumber, meterNumber);
            shipRequestContext.put(x.CarrierCode, carrierCode);
            shipRequestContext.put(x.ShipDate, UtilDateTime.nowTimestamp());
            shipRequestContext.put(x.ShipTime, UtilDateTime.nowTimestamp());
            shipRequestContext.put(x.DropoffType, dropoffType);
            shipRequestContext.put(x.Service, service);
            shipRequestContext.put(x.WeightUnits, x.WT_kg.equals(weightUomId) ? x.KGS : x.LBS);
            shipRequestContext.put(x.CurrencyCode, currencyCode);
            shipRequestContext.put(x.PayorType, x.SENDER);
            shipRequestContext.put(originContactKey, originContactName);
            shipRequestContext.put(x.OriginContactPhoneNumber, originContactPhoneNumber);
            shipRequestContext.put(x.OriginAddressLine1, originPostalAddress.getString(x.address1));
            if (UtilValidate.isNotEmpty(originPostalAddress.getString(x.address2))) {
                shipRequestContext.put(x.OriginAddressLine2, originPostalAddress.getString(x.address2));
            }
            shipRequestContext.put(x.OriginAddressCity, originPostalAddress.getString(x.city));
            if (UtilValidate.isNotEmpty(originAddressStateOrProvinceCode)) {
                shipRequestContext.put(x.OriginAddressStateOrProvinceCode, originAddressStateOrProvinceCode);
            }
            shipRequestContext.put(x.OriginAddressPostalCode, originPostalAddress.getString(x.postalCode));
            shipRequestContext.put(x.OriginAddressCountryCode, originAddressCountryCode);
            shipRequestContext.put(destinationContactKey, destinationContactName);
            shipRequestContext.put(x.DestinationContactPhoneNumber, destinationContactPhoneNumber);
            shipRequestContext.put(x.DestinationAddressLine1, destinationPostalAddress.getString(x.address1));
            if (UtilValidate.isNotEmpty(destinationPostalAddress.getString(x.address2))) {
                shipRequestContext.put(x.DestinationAddressLine2, destinationPostalAddress.getString(x.address2));
            }
            shipRequestContext.put(x.DestinationAddressCity, destinationPostalAddress.getString(x.city));
            if (UtilValidate.isNotEmpty(destinationAddressStateOrProvinceCode)) {
                shipRequestContext.put(x.DestinationAddressStateOrProvinceCode, destinationAddressStateOrProvinceCode);
            }
            shipRequestContext.put(x.DestinationAddressPostalCode, destinationPostalAddress.getString(x.postalCode));
            shipRequestContext.put(x.DestinationAddressCountryCode, destinationAddressCountryCode);
            shipRequestContext.put(x.LabelType, x._2DCOMMON);
            // Required type for FDXShipRequest. Not directly in the FTL because it shouldn't be changed.
            shipRequestContext.put(x.LabelImageType, labelImageType);
            if (UtilValidate.isNotEmpty(homeDeliveryType)) {
                shipRequestContext.put(x.HomeDeliveryType, homeDeliveryType);
            }
            if (homeDeliveryDate != null) {
                shipRequestContext.put(x.HomeDeliveryDate, homeDeliveryDate);
            }

            // Get the weight from the ShipmentRouteSegment first, which overrides all later weight computations
            boolean hasBillingWeight = false;
            BigDecimal billingWeight = shipmentRouteSegment.getBigDecimal(x.billingWeight);
            String billingWeightUomId = shipmentRouteSegment.getString(x.billingWeightUomId);
            if ((billingWeight != null) && (billingWeight.compareTo(BigDecimal.ZERO) > 0)) {
                hasBillingWeight = true;
                if (billingWeightUomId == null) {
                    Debug.logWarning(x.Shipment_Route_Segment_missing_billingWeightUomId_in_shipmentId + shipmentId
                            + x.assuming_default_shipment_fedex_weightUomId_of + weightUomId + x._from_0b70336f + SHIPMENT_PROPERTIES_FILE, MODULE);
                    billingWeightUomId = weightUomId;
                }

                // Convert the weight if necessary
                if (!billingWeightUomId.equals(weightUomId)) {
                    Map<String, Object> results = dispatcher.runSync(x.convertUom, UtilMisc.<String, Object>toMap(x.uomId,
                            billingWeightUomId, x.uomIdTo, weightUomId, x.originalValue, billingWeight));
                    if (ServiceUtil.isError(results) || (results.get(x.convertedValue) == null)) {
                        Debug.logWarning(x.Unable_to_convert_billing_weights_for_shipmentId + shipmentId, MODULE);

                        // Try getting the weight from package instead
                        hasBillingWeight = false;
                    } else {
                        billingWeight = (BigDecimal) results.get(x.convertedValue);
                    }
                }
            }

            // Loop through Shipment segments (NOTE: only one supported, loop is here for future refactoring reference)
            for (GenericValue shipmentPackageRouteSeg: shipmentPackageRouteSegs) {
                GenericValue shipmentPackage = shipmentPackageRouteSeg.getRelatedOne(x.ShipmentPackage, false);
                GenericValue shipmentBoxType = shipmentPackage.getRelatedOne(x.ShipmentBoxType, false);

                // FedEx requires the packaging type
                String packaging = null;
                if (UtilValidate.isEmpty(shipmentBoxType)) {
                    packaging = getShipmentGatewayConfigValue(delegator, shipmentGatewayConfigId, x.defaultPackagingType,
                            resource, x.shipment_fedex_default_packagingType);
                    if (UtilValidate.isEmpty(packaging)) {
                        return ServiceUtil.returnError(UtilProperties.getMessage(RES_ERROR,
                                x.FacilityShipmentFedexPackingTypeNotConfigured,
                                UtilMisc.toMap(x.shipmentPackageSeqId, shipmentPackage.getString(x.shipmentPackageSeqId),
                                        x.shipmentId, shipmentId), locale));
                    }
                    Debug.logWarning(x.Package_43c8c349 + shipmentPackage.getString(x.shipmentPackageSeqId) + x.of_shipment + shipmentId
                            + x.has_no_packaging_type_set_defaulting_to + packaging, MODULE);
                } else {
                    packaging = shipmentBoxType.getString(x.shipmentBoxTypeId);
                }

                // Make sure that the packaging type is valid for FedEx
                CarrierShipmentBoxTypeDao carrierShipmentBoxTypeDao = DaoRegistry.getDao(delegator, x.CarrierShipmentBoxType,
                        CarrierShipmentBoxTypeDao.class);
                CarrierShipmentBoxTypeEntity carrierShipmentBoxType;
                try {
                    carrierShipmentBoxType = carrierShipmentBoxTypeDao.get(CarrierShipmentBoxTypeEntity.builder()
                            .partyId(x.FEDEX)
                            .shipmentBoxTypeId(packaging)
                            .build()).orElse(null);
                } catch (java.sql.SQLException e) {
                    throw new GenericEntityException(e);
                }
                if (UtilValidate.isEmpty(carrierShipmentBoxType)) {
                    return ServiceUtil.returnError(UtilProperties.getMessage(RES_ERROR,
                            x.FacilityShipmentFedexPackingTypeInvalid,
                            UtilMisc.toMap(x.shipmentPackageSeqId, shipmentPackage.getString(x.shipmentPackageSeqId),
                                    x.shipmentId, shipmentId), locale));
                } else if (UtilValidate.isEmpty(carrierShipmentBoxType.getPackagingTypeCode())) {
                    return ServiceUtil.returnError(UtilProperties.getMessage(RES_ERROR,
                            x.FacilityShipmentFedexPackingTypeMissing,
                            UtilMisc.toMap(x.shipmentPackageSeqId, shipmentPackage.getString(x.shipmentPackageSeqId),
                                    x.shipmentId, shipmentId), locale));
                }
                packaging = carrierShipmentBoxType.getPackagingTypeCode();

                // Determine the dimensions of the package
                BigDecimal dimensionsLength = null;
                BigDecimal dimensionsWidth = null;
                BigDecimal dimensionsHeight = null;
                if (shipmentBoxType != null) {
                    dimensionsLength = shipmentBoxType.getBigDecimal(x.boxLength);
                    dimensionsWidth = shipmentBoxType.getBigDecimal(x.boxWidth);
                    dimensionsHeight = shipmentBoxType.getBigDecimal(x.boxHeight);

                    String boxDimensionsUomId = null;
                    GenericValue boxDimensionsUom = shipmentBoxType.getRelatedOne(x.DimensionUom, false);
                    if (!UtilValidate.isEmpty(boxDimensionsUom)) {
                        boxDimensionsUomId = boxDimensionsUom.getString(x.uomId);
                    } else {
                        Debug.logWarning(x.Packaging_type_for_package + shipmentPackage.getString(x.shipmentPackageSeqId)
                                + x.of_shipmentRouteSegment + shipmentRouteSegmentId + x.of_shipment + shipmentId
                                + x.is_missing_dimensionUomId_assuming_default_shipment_default_dimension_uom_of + dimensionsUomId
                                + x._from_0b70336f + SHIPMENT_PROPERTIES_FILE, MODULE);
                        boxDimensionsUomId = dimensionsUomId;
                    }
                    if (dimensionsLength != null && dimensionsLength.compareTo(BigDecimal.ZERO) > 0) {
                        if (!boxDimensionsUomId.equals(dimensionsUomId)) {
                            Map<String, Object> results = dispatcher.runSync(x.convertUom, UtilMisc.<String, Object>toMap(x.uomId,
                                    boxDimensionsUomId, x.uomIdTo, dimensionsUomId, x.originalValue, dimensionsLength));
                            if (ServiceUtil.isError(results) || (results.get(x.convertedValue) == null)) {
                                Debug.logWarning(x.Unable_to_convert_length_for_package + shipmentPackage.getString(x.shipmentPackageSeqId)
                                        + x.of_shipmentRouteSegment + shipmentRouteSegmentId + x.of_shipment + shipmentId, MODULE);
                                dimensionsLength = null;
                            } else {
                                dimensionsLength = (BigDecimal) results.get(x.convertedValue);
                            }
                        }

                    }
                    if (dimensionsWidth != null && dimensionsWidth.compareTo(BigDecimal.ZERO) > 0) {
                        if (!boxDimensionsUomId.equals(dimensionsUomId)) {
                            Map<String, Object> results = dispatcher.runSync(x.convertUom, UtilMisc.<String, Object>toMap(x.uomId,
                                    boxDimensionsUomId, x.uomIdTo, dimensionsUomId, x.originalValue, dimensionsWidth));
                            if (ServiceUtil.isError(results) || (results.get(x.convertedValue) == null)) {
                                Debug.logWarning(x.Unable_to_convert_width_for_package + shipmentPackage.getString(x.shipmentPackageSeqId)
                                        + x.of_shipmentRouteSegment + shipmentRouteSegmentId + x.of_shipment + shipmentId, MODULE);
                                dimensionsWidth = null;
                            } else {
                                dimensionsWidth = (BigDecimal) results.get(x.convertedValue);
                            }
                        }

                    }
                    if (dimensionsHeight != null && dimensionsHeight.compareTo(BigDecimal.ZERO) > 0) {
                        if (!boxDimensionsUomId.equals(dimensionsUomId)) {
                            Map<String, Object> results = dispatcher.runSync(x.convertUom, UtilMisc.<String, Object>toMap(x.uomId,
                                    boxDimensionsUomId, x.uomIdTo, dimensionsUomId, x.originalValue, dimensionsHeight));
                            if (ServiceUtil.isError(results) || (results.get(x.convertedValue) == null)) {
                                Debug.logWarning(x.Unable_to_convert_height_for_package + shipmentPackage.getString(x.shipmentPackageSeqId)
                                        + x.of_shipmentRouteSegment + shipmentRouteSegmentId + x.of_shipment + shipmentId, MODULE);
                                dimensionsHeight = null;
                            } else {
                                dimensionsHeight = (BigDecimal) results.get(x.convertedValue);
                            }
                        }

                    }
                }

                // Determine the package weight (possibly overriden by route segment billing weight)
                BigDecimal packageWeight = null;
                if (!hasBillingWeight) {
                    if (UtilValidate.isNotEmpty(shipmentPackage.getString(x.weight))) {
                        packageWeight = shipmentPackage.getBigDecimal(x.weight);
                    } else {

                        // Use default weight if available
                        try {
                            packageWeight = EntityUtilProperties.getPropertyAsBigDecimal(SHIPMENT_PROPERTIES_FILE, x.shipment_default_weight_value,
                                    BigDecimal.ZERO);
                        } catch (NumberFormatException ne) {
                            Debug.logWarning(x.Default_shippable_weight_not_configured_shipment_default_weight_value_assuming_1_0
                                    + weightUomId, MODULE);
                            packageWeight = BigDecimal.ONE;
                        }
                    }

                    // Convert weight if necessary
                    String packageWeightUomId = shipmentPackage.getString(x.weightUomId);
                    if (UtilValidate.isEmpty(packageWeightUomId)) {
                        Debug.logWarning(x.Shipment_Route_Segment_missing_weightUomId_in_shipmentId + shipmentId
                                + x.assuming_shipment_default_weight_uom_of + weightUomId + x._from_0b70336f + SHIPMENT_PROPERTIES_FILE, MODULE);
                        packageWeightUomId = weightUomId;
                    }
                    if (!packageWeightUomId.equals(weightUomId)) {
                        Map<String, Object> results = dispatcher.runSync(x.convertUom, UtilMisc.<String, Object>toMap(x.uomId,
                                packageWeightUomId, x.uomIdTo, weightUomId, x.originalValue, packageWeight));
                        if (ServiceUtil.isError(results) || (results.get(x.convertedValue) == null)) {
                            return ServiceUtil.returnError(UtilProperties.getMessage(RES_ERROR,
                                    x.FacilityShipmentFedexWeightOfPackageCannotBeConverted,
                                    UtilMisc.toMap(x.shipmentPackageSeqId, shipmentPackage.getString(x.shipmentPackageSeqId),
                                            x.shipmentRouteSegmentId, shipmentRouteSegmentId, x.shipmentId, shipmentId), locale));
                        } else {
                            packageWeight = (BigDecimal) results.get(x.convertedValue);
                        }
                    }
                }
                BigDecimal weight = hasBillingWeight ? billingWeight : packageWeight;
                if (weight == null || weight.compareTo(BigDecimal.ZERO) < 0) {
                    return ServiceUtil.returnError(UtilProperties.getMessage(RES_ERROR,
                            x.FacilityShipmentFedexWeightOfPackageNotAvailable,
                            UtilMisc.toMap(x.shipmentPackageSeqId, shipmentPackage.getString(x.shipmentPackageSeqId),
                                    x.shipmentRouteSegmentId, shipmentRouteSegmentId, x.shipmentId, shipmentId), locale));
                }

                // Populate the Freemarker context with package-related information
                shipRequestContext.put(x.CustomerReference, shipmentId + x.str_05a79f06 + shipmentRouteSegmentId + x.str_05a79f06 + shipmentPackage.getString(
                        x.shipmentPackageSeqId));
                shipRequestContext.put(x.DropoffType, dropoffType);
                shipRequestContext.put(x.Packaging, packaging);
                if (UtilValidate.isNotEmpty(dimensionsUomId)
                        && dimensionsLength != null && dimensionsLength.setScale(0, RoundingMode.HALF_UP).compareTo(BigDecimal.ZERO) > 0
                        && dimensionsWidth != null && dimensionsWidth.setScale(0, RoundingMode.HALF_UP).compareTo(BigDecimal.ZERO) > 0
                        && dimensionsHeight != null && dimensionsHeight.setScale(0, RoundingMode.HALF_UP).compareTo(BigDecimal.ZERO) > 0) {
                    shipRequestContext.put(x.DimensionsUnits, x.LEN_in.equals(dimensionsUomId) ? x.IN : x.CM);
                    shipRequestContext.put(x.DimensionsLength, dimensionsLength.setScale(0, RoundingMode.HALF_UP).toString());
                    shipRequestContext.put(x.DimensionsWidth, dimensionsWidth.setScale(0, RoundingMode.HALF_UP).toString());
                    shipRequestContext.put(x.DimensionsHeight, dimensionsHeight.setScale(0, RoundingMode.HALF_UP).toString());
                }
                shipRequestContext.put(x.Weight_69c0b815, weight.setScale(1, RoundingMode.UP).toString());
            }

            StringWriter outWriter = new StringWriter();
            try {
                FreeMarkerWorker.renderTemplate(templateLocation, shipRequestContext, outWriter);
            } catch (Exception e) {
                String errorMessage = x.Cannot_confirm_Fedex_shipment_Failed_to_render_Fedex_XML_Ship_Request_Template + templateLocation + x.str_76d00394;
                Debug.logError(e, errorMessage, MODULE);
                return ServiceUtil.returnError(UtilProperties.getMessage(RES_ERROR,
                        x.FacilityShipmentFedexShipmentTemplateError,
                        UtilMisc.toMap(x.templateLocation, templateLocation, x.errorString, e.getMessage()), locale));
            }

            // Pass the request string to the sending method
            String fDXShipRequestString = outWriter.toString();
            String fDXShipReplyString = null;
            try {
                fDXShipReplyString = sendFedexRequest(fDXShipRequestString, delegator, shipmentGatewayConfigId, resource, locale);
                if (Debug.verboseOn()) {
                    Debug.logVerbose(fDXShipReplyString, MODULE);
                }
            } catch (FedexConnectException e) {
                String errorMessage = x.Error_sending_Fedex_request_for_FDXShipRequest;
                Debug.logError(e, errorMessage, MODULE);
                return ServiceUtil.returnError(UtilProperties.getMessage(RES_ERROR,
                        x.FacilityShipmentFedexShipmentTemplateSendingError,
                        UtilMisc.toMap(x.errorString, e.toString()), locale));
            }

            // Pass the reply to the handler method
            return handleFedexShipReply(fDXShipReplyString, shipmentRouteSegment, shipmentPackageRouteSegs, locale);

        } catch (GenericEntityException | GenericServiceException e) {
            Debug.logError(e, MODULE);
            return ServiceUtil.returnError(UtilProperties.getMessage(RES_ERROR,
                    x.FacilityShipmentFedexShipmentTemplateServiceError,
                    UtilMisc.toMap(x.errorString, e.toString()), locale));
        }
    }

    /**
     * Extract the tracking number and shipping label from the FDXShipReply XML string
     * @param fDXShipReplyString
     * @param shipmentRouteSegment
     * @param shipmentPackageRouteSegs
     * @throws GenericEntityException
     */
    public static Map<String, Object> handleFedexShipReply(String fDXShipReplyString, GenericValue shipmentRouteSegment,
            List<GenericValue> shipmentPackageRouteSegs, Locale locale) throws GenericEntityException {
        List<Object> errorList = new LinkedList<>();
        GenericValue shipmentPackageRouteSeg = shipmentPackageRouteSegs.get(0);

        Document fdxShipReplyDocument = null;
        try {
            fdxShipReplyDocument = UtilXml.readXmlDocument(fDXShipReplyString, false);
        } catch (Exception e) {
            String errorMessage = x.Error_parsing_the_FDXShipReply + e.toString();
            Debug.logError(e, errorMessage, MODULE);
            // TODO Cancel the package
        }

        if (UtilValidate.isEmpty(fdxShipReplyDocument)) {
            return ServiceUtil.returnError(UtilProperties.getMessage(RES_ERROR,
                    x.FacilityShipmentFedexShipmentTemplateParsingError, locale));
        }

        // Tracking number: Tracking/TrackingNumber
        Element rootElement = fdxShipReplyDocument.getDocumentElement();

        handleErrors(rootElement, errorList, locale);

        if (UtilValidate.isNotEmpty(errorList)) {
            return ServiceUtil.returnError(errorList);
        }

        Element trackingElement = UtilXml.firstChildElement(rootElement, x.Tracking);
        String trackingNumber = UtilXml.childElementValue(trackingElement, x.TrackingNumber);

        // Label: Labels/OutboundLabel
        Element labelElement = UtilXml.firstChildElement(rootElement, x.Labels);
        String encodedImageString = UtilXml.childElementValue(labelElement, x.OutboundLabel);
        if (UtilValidate.isEmpty(encodedImageString)) {
            Debug.logError(x.Cannot_find_FDXShipReply_label_FDXShipReply_document_is + fDXShipReplyString, MODULE);
            return ServiceUtil.returnError(UtilProperties.getMessage(RES_ERROR,
                    x.FacilityShipmentFedexShipmentTemplateLabelNotFound,
                    UtilMisc.toMap(x.shipmentPackageRouteSeg, shipmentPackageRouteSeg,
                            x.fDXShipReplyString, fDXShipReplyString), locale));
        }

        byte[] labelBytes = Base64.getMimeDecoder().decode(encodedImageString.getBytes(StandardCharsets.UTF_8));

        if (labelBytes != null) {

            // Store in db blob
            shipmentPackageRouteSeg.setBytes(x.labelImage, labelBytes);
        } else {
            Debug.logInfo(x.Failed_to_either_decode_returned_FedEx_label_or_no_data_found_in_Labels_OutboundLabel, MODULE);
            // TODO: Cancel the package
        }

        shipmentPackageRouteSeg.set(x.trackingCode, trackingNumber);
        shipmentPackageRouteSeg.set(x.labelHtml, encodedImageString);
        shipmentPackageRouteSeg.store();

        shipmentRouteSegment.set(x.trackingIdNumber, trackingNumber);
        shipmentRouteSegment.put(x.carrierServiceStatusId, x.SHRSCS_CONFIRMED);
        shipmentRouteSegment.store();

        return ServiceUtil.returnSuccess(UtilProperties.getMessage(RES_ERROR,
                x.FacilityShipmentFedexShipmentConfirmed, locale));
    }

    public static void handleErrors(Element rootElement, List<Object> errorList, Locale locale) {
        Element errorElement = null;
        if (x.Error.equalsIgnoreCase(rootElement.getNodeName())) {
            errorElement = rootElement;
        } else {
            errorElement = UtilXml.firstChildElement(rootElement, x.Error);
        }
        if (UtilValidate.isNotEmpty(errorElement)) {
            Element errorCodeElement = UtilXml.firstChildElement(errorElement, x.Code);
            Element errorMessageElement = UtilXml.firstChildElement(errorElement, x.Message);
            if (errorCodeElement != null || errorMessageElement != null) {
                String errorCode = UtilXml.childElementValue(errorElement, x.Code);
                String errorMessage = UtilXml.childElementValue(errorElement, x.Message);
                if (UtilValidate.isNotEmpty(errorCode) || UtilValidate.isNotEmpty(errorMessage)) {
                    errorList.add(UtilProperties.getMessage(RES_ERROR, x.FacilityShipmentFedexErrorMessage,
                            UtilMisc.toMap(x.errorCode, errorCode, x.errorMessage, errorMessage), locale));
                }
            }
        }
    }

    private static String getShipmentGatewayConfigValue(Delegator delegator, String shipmentGatewayConfigId,
            String shipmentGatewayConfigParameterName, String resource, String parameterName) {
        String returnValue = x.emptyString;
        if (UtilValidate.isNotEmpty(shipmentGatewayConfigId)) {
            try {
                ShipmentGatewayFedexDao shipmentGatewayFedexDao = DaoRegistry.getDao(delegator, x.ShipmentGatewayFedex,
                        ShipmentGatewayFedexDao.class);
                ShipmentGatewayFedexEntity fedexEntity = shipmentGatewayFedexDao.get(shipmentGatewayConfigId).orElse(null);
                GenericValue fedex = fedexEntity == null ? null : delegator.makeValue(x.ShipmentGatewayFedex, Beans.beanToMap(fedexEntity));
                if (fedex != null) {
                    Object fedexField = fedex.get(shipmentGatewayConfigParameterName);
                    if (fedexField != null) {
                        returnValue = fedexField.toString().trim();
                    }
                }
            } catch (Exception e) {
                Debug.logError(e, MODULE);
            }
        } else {
            String value = EntityUtilProperties.getPropertyValue(resource, parameterName, delegator);
            if (value != null) {
                returnValue = value.trim();
            }
        }
        return returnValue;
    }

    private static String getShipmentGatewayConfigValue(Delegator delegator, String shipmentGatewayConfigId, String
            shipmentGatewayConfigParameterName,
            String resource, String parameterName, String defaultValue) {
        String returnValue = getShipmentGatewayConfigValue(delegator, shipmentGatewayConfigId, shipmentGatewayConfigParameterName,
                resource, parameterName);
        if (UtilValidate.isEmpty(returnValue)) {
            returnValue = defaultValue;
        }
        return returnValue;
    }
}

@SuppressWarnings(x.serial)
class FedexConnectException extends GeneralException {
    FedexConnectException() {
        super();
    }

    FedexConnectException(String msg) {
        super(msg);
    }

    FedexConnectException(Throwable t) {
        super(t);
    }

    FedexConnectException(String msg, Throwable t) {
        super(msg, t);
    }
}
