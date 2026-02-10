/*******************************************************************************
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
 *******************************************************************************/
package org.apache.ofbiz.shipment.thirdparty.dhl;

import java.io.IOException;
import java.io.StringWriter;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
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
import org.apache.ofbiz.base.util.StringUtil;
import org.apache.ofbiz.base.util.UtilDateTime;
import org.apache.ofbiz.base.util.UtilMisc;
import org.apache.ofbiz.base.util.UtilProperties;
import org.apache.ofbiz.base.util.UtilValidate;
import org.apache.ofbiz.base.util.UtilXml;
import org.apache.ofbiz.content.content.ContentWorker;
import org.apache.ofbiz.entity.Delegator;
import org.apache.ofbiz.entity.GenericEntityException;
import org.apache.ofbiz.entity.GenericValue;
import org.apache.ofbiz.entity.util.EntityUtilProperties;
import org.apache.ofbiz.persistence.dao.CarrierShipmentMethodDao;
import org.apache.ofbiz.persistence.dao.DaoRegistry;
import org.apache.ofbiz.persistence.dao.PostalAddressDao;
import org.apache.ofbiz.persistence.dao.ShipmentDao;
import org.apache.ofbiz.persistence.dao.ShipmentGatewayDhlDao;
import org.apache.ofbiz.persistence.dao.ShipmentRouteSegmentDao;
import org.apache.ofbiz.persistence.entity.CarrierShipmentMethodEntity;
import org.apache.ofbiz.persistence.entity.PostalAddressEntity;
import org.apache.ofbiz.persistence.entity.ShipmentEntity;
import org.apache.ofbiz.persistence.entity.ShipmentGatewayDhlEntity;
import org.apache.ofbiz.persistence.entity.ShipmentRouteSegmentEntity;
import org.apache.ofbiz.service.DispatchContext;
import org.apache.ofbiz.service.GenericServiceException;
import org.apache.ofbiz.service.LocalDispatcher;
import org.apache.ofbiz.service.ModelService;
import org.apache.ofbiz.service.ServiceUtil;
import org.apache.ofbiz.shipment.shipment.ShipmentServices;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.xml.sax.SAXException;


import com.landawn.abacus.query.Filters;
import com.landawn.abacus.util.Beans;

import org.apache.ofbiz.persistence.entity.x;
import org.apache.ofbiz.model.ServiceContext;
import org.apache.ofbiz.model.DhlServicesContext;
/**
 * DHL ShipmentServices
 *
 * <p>Implementation of DHL US domestic shipment interface using DHL ShipIT XML APi.</p>
 *
 * Shipment services not supported in DHL ShipIT 1.1
 * <ul>
 * <li>Multiple Piece shipping (Shipment must not have more than one
 * ShipmentPackage)</li>
 * <li>Dynamic editing of previously submitted shipment (void first then
 * resubmit instead)</li>
 * <li>Label size 4"x6"</li>
 * <li>Out of origin shipping</li>
 * </ul>
 *
 * TODO: International
 */
public class DhlServices {

    private static final String MODULE = DhlServices.class.getName();
    public static final String SHIPMENT_PROPERTIES_FILE = x.shipment_properties;
    public static final String DHL_WEIGHT_UOM_ID = x.WT_lb; // weight Uom used by DHL
    private static final String RES_ERROR = x.ProductUiLabels;

    /**
     * Opens a URL to DHL and makes a request.
     * @param xmlString Name of the DHL service to invoke
     * @param delegator the delegator
     * @param shipmentGatewayConfigId the shipment gateway config id
     * @param resource the RESOURCE file (i.e. shipment.properties)
     * @param locale locale in use
     * @return XML string response from DHL
     * @throws DhlConnectException
     */
    public static String sendDhlRequest(String xmlString, Delegator delegator, String shipmentGatewayConfigId,
            String resource, Locale locale) throws DhlConnectException {
        String conStr = getShipmentGatewayConfigValue(delegator, shipmentGatewayConfigId, x.connectUrl, resource, x.shipment_dhl_connect_url);
        if (conStr.isEmpty()) {
            throw new DhlConnectException(UtilProperties.getMessage(RES_ERROR,
                    x.FacilityShipmentDhlConnectUrlIncomplete, locale));
        }

        // xmlString should contain the auth document at the beginning
        // all documents require an <?xml version="1.0"?> header
        if (xmlString == null) {
            throw new DhlConnectException(UtilProperties.getMessage(RES_ERROR,
                    x.FacilityShipmentDhlXmlCannotBeNull, locale));
        }

        // prepare the connect string
        conStr = conStr.trim();

        String timeOutStr = getShipmentGatewayConfigValue(delegator, shipmentGatewayConfigId, x.connectTimeout,
                resource, x.shipment_dhl_connect_timeout, x._60);
        int timeout = 60;
        try {
            timeout = Integer.parseInt(timeOutStr);
        } catch (NumberFormatException e) {
            Debug.logError(e, x.Unable_to_set_timeout_to + timeOutStr + x.using_default + timeout);
        }

        if (Debug.verboseOn()) {
            Debug.logVerbose(x.DHL_Connect_URL + conStr, MODULE);
            Debug.logVerbose(x.DHL_XML_String + xmlString, MODULE);
        }

        HttpClient http = new HttpClient(conStr);
        http.setTimeout(timeout * 1000);
        String response = null;
        try {
            response = http.post(xmlString);
        } catch (HttpClientException e) {
            Debug.logError(e, x.Problem_connecting_with_DHL_server, MODULE);
            throw new DhlConnectException(UtilProperties.getMessage(RES_ERROR,
                    x.FacilityShipmentDhlConnectUrlProblem, UtilMisc.toMap(x.errorString, e), locale), e);
        }

        if (response == null) {
            throw new DhlConnectException(UtilProperties.getMessage(RES_ERROR,
                    x.FacilityShipmentDhlReceivedNullResponse, locale));
        }
        if (Debug.verboseOn()) {
            Debug.logVerbose(x.DHL_Response + response, MODULE);
        }

        return response;
    }


    /*
     * Service to obtain a rate estimate from DHL for a shipment. Notes: Only one package per shipment currently supported by DHL ShipIT.
     * If this service returns a null shippingEstimateAmount, then the shipment has not been processed
     */
    public static Map<String, Object> dhlRateEstimate(DispatchContext dctx, DhlServicesContext context) {
        Delegator delegator = dctx.getDelegator();
        LocalDispatcher dispatcher = dctx.getDispatcher();
        Locale locale = (Locale) context.get(x.locale);

        // some of these can be refactored
        String carrierPartyId = (String) context.get(x.carrierPartyId);
        String shipmentMethodTypeId = (String) context.get(x.shipmentMethodTypeId);
        String shippingContactMechId = (String) context.get(x.shippingContactMechId);
        BigDecimal shippableWeight = (BigDecimal) context.get(x.shippableWeight);

        if (x.NO_SHIPPING.equals(shipmentMethodTypeId)) {
            Map<String, Object> result = ServiceUtil.returnSuccess();
            result.put(x.shippingEstimateAmount, null);
            return result;
        }

        // translate shipmentMethodTypeId to DHL service code
        String dhlShipmentDetailCode = null;
        try {
            CarrierShipmentMethodDao carrierShipmentMethodDao = DaoRegistry.getDao(delegator, x.CarrierShipmentMethod,
                    CarrierShipmentMethodDao.class);
            CarrierShipmentMethodEntity carrierShipmentMethodEntity = carrierShipmentMethodDao.list(Filters.and(
                    Filters.eq(x.shipmentMethodTypeId, shipmentMethodTypeId),
                    Filters.eq(x.partyId, carrierPartyId),
                    Filters.eq(x.roleTypeId, x.CARRIER))).stream().findFirst().orElse(null);
            GenericValue carrierShipmentMethod = carrierShipmentMethodEntity == null ? null
                    : delegator.makeValue(x.CarrierShipmentMethod, Beans.beanToMap(carrierShipmentMethodEntity));
            if (carrierShipmentMethod == null) {
                return ServiceUtil.returnError(UtilProperties.getMessage(RES_ERROR,
                        x.FacilityShipmentDhlNoCarrierShipmentMethod,
                        UtilMisc.toMap(x.carrierPartyId, carrierPartyId, x.shipmentMethodTypeId, shipmentMethodTypeId), locale));
            }
            dhlShipmentDetailCode = carrierShipmentMethod.getString(x.carrierServiceCode);
        } catch (Exception e) {
            Debug.logError(e, x.Failed_to_get_rate_estimate + e.getMessage(), MODULE);
        }

        String resource = (String) context.get(x.serviceConfigProps);
        String shipmentGatewayConfigId = (String) context.get(x.shipmentGatewayConfigId);

        // shipping credentials (configured in properties)
        String userid = getShipmentGatewayConfigValue(delegator, shipmentGatewayConfigId,
                x.accessUserId, resource, x.shipment_dhl_access_userid);
        String password = getShipmentGatewayConfigValue(delegator, shipmentGatewayConfigId,
                x.accessPassword, resource, x.shipment_dhl_access_password);
        String shippingKey = getShipmentGatewayConfigValue(delegator, shipmentGatewayConfigId,
                x.accessShippingKey, resource, x.shipment_dhl_access_shippingKey);
        String accountNbr = getShipmentGatewayConfigValue(delegator, shipmentGatewayConfigId,
                x.accessAccountNbr, resource, x.shipment_dhl_access_accountNbr);
        if ((shippingKey.isEmpty()) || (accountNbr.isEmpty())) {
            return ServiceUtil.returnError(UtilProperties.getMessage(RES_ERROR,
                    x.FacilityShipmentDhlGatewayNotAvailable, locale));
        }

        // obtain the ship-to address
        GenericValue shipToAddress = null;
        if (shippingContactMechId != null) {
            try {
                PostalAddressDao postalAddressDao = DaoRegistry.getDao(delegator, x.PostalAddress, PostalAddressDao.class);
                PostalAddressEntity postalAddressEntity = postalAddressDao.get(shippingContactMechId).orElse(null);
                shipToAddress = postalAddressEntity == null ? null : delegator.makeValue(x.PostalAddress, Beans.beanToMap(postalAddressEntity));
                if (shipToAddress == null) {
                    return ServiceUtil.returnError(UtilProperties.getMessage(RES_ERROR,
                            x.FacilityShipmentUnableFoundShipToAddresss, locale));
                }
            } catch (Exception e) {
                Debug.logError(e, MODULE);
            }
        }

        if ((shippableWeight == null) || (shippableWeight.compareTo(BigDecimal.ZERO) <= 0)) {
            String tmpValue = EntityUtilProperties.getPropertyValue(SHIPMENT_PROPERTIES_FILE, x.shipment_default_weight_value, delegator);
            if (tmpValue != null) {
                try {
                    shippableWeight = new BigDecimal(tmpValue);
                } catch (Exception e) {
                    return ServiceUtil.returnError(UtilProperties.getMessage(RES_ERROR,
                            x.FacilityShipmentDhlDefaultShippableWeightNotConfigured, locale));
                }
            }
        }

        // TODO: if a weight UOM is passed in, use convertUom service to convert it here
        if (shippableWeight.compareTo(BigDecimal.ONE) < 0) {
            Debug.logWarning(x.DHL_Estimate_Weight_is_less_than_1_lb_submitting_DHL_minimum_of_1_lb_for_estimate, MODULE);
            shippableWeight = BigDecimal.ONE;
        }
        if ((x.G.equals(dhlShipmentDetailCode) && shippableWeight.compareTo(new BigDecimal(x._999)) > 0)
                || (shippableWeight.compareTo(new BigDecimal(x._150)) > 0)) {
            return ServiceUtil.returnError(UtilProperties.getMessage(RES_ERROR,
                    x.FacilityShipmentDhlShippableWeightExceed, locale));
        }
        String weight = shippableWeight.toString();

        // create AccessRequest XML doc using FreeMarker template
        String templateName = getShipmentGatewayConfigValue(delegator, shipmentGatewayConfigId,
                x.rateEstimateTemplate, resource, x.shipment_dhl_template_rate_estimate);
        if (templateName.trim().isEmpty()) {
            return ServiceUtil.returnError(UtilProperties.getMessage(RES_ERROR,
                    x.FacilityShipmentDhlShipmentTemplateLocationNotFound, locale));
        }
        StringWriter outWriter = new StringWriter();
        Map<String, Object> inContext = new HashMap<>();
        inContext.put(x.action, x.RateEstimate);
        inContext.put(x.userid, userid);
        inContext.put(x.password, password);
        inContext.put(x.accountNbr, accountNbr);
        inContext.put(x.shippingKey, shippingKey);
        inContext.put(x.shipDate, UtilDateTime.nowTimestamp());
        inContext.put(x.dhlShipmentDetailCode, dhlShipmentDetailCode);
        inContext.put(x.weight, weight);
        inContext.put(x.state, shipToAddress.getString(x.stateProvinceGeoId));
        // DHL ShipIT API does not accept ZIP+4
        if ((shipToAddress.getString(x.postalCode) != null) && (shipToAddress.getString(x.postalCode).length() > 5)) {
            inContext.put(x.postalCode, shipToAddress.getString(x.postalCode).substring(0, 5));
        } else {
            inContext.put(x.postalCode, shipToAddress.getString(x.postalCode));
        }
        try {
            ContentWorker.renderContentAsText(dispatcher, templateName, outWriter, inContext, locale, x.text_plain, null, null, false);
        } catch (Exception e) {
            Debug.logError(e, x.Cannot_get_DHL_Estimate_Failed_to_render_DHL_XML_Request, MODULE);
            return ServiceUtil.returnError(UtilProperties.getMessage(RES_ERROR,
                    x.FacilityShipmentDhlShipmentTemplateError, locale));
        }
        String requestString = outWriter.toString();
        if (Debug.verboseOn()) {
            Debug.logVerbose(requestString, MODULE);
        }

        // send the request
        String rateResponseString = null;
        try {
            rateResponseString = sendDhlRequest(requestString, delegator, shipmentGatewayConfigId, resource, locale);
            if (Debug.verboseOn()) {
                Debug.logVerbose(rateResponseString, MODULE);
            }
        } catch (DhlConnectException e) {
            String uceErrMsg = x.Error_sending_DHL_request_for_DHL_Service_Rate + e.toString();
            Debug.logError(e, uceErrMsg, MODULE);
            return ServiceUtil.returnError(UtilProperties.getMessage(RES_ERROR,
                    x.FacilityShipmentDhlShipmentTemplateSendingError,
                    UtilMisc.toMap(x.errorString, e.toString()), locale));
        }

        Document rateResponseDocument = null;
        try {
            rateResponseDocument = UtilXml.readXmlDocument(rateResponseString, false);
            return handleDhlRateResponse(rateResponseDocument, locale);
        } catch (SAXException | IOException | ParserConfigurationException e2) {
            String excErrMsg = x.Error_parsing_the_RatingServiceResponse + e2.toString();
            Debug.logError(e2, excErrMsg, MODULE);
            return ServiceUtil.returnError(UtilProperties.getMessage(RES_ERROR,
                    x.FacilityShipmentFedexShipmentTemplateParsingError,
                    UtilMisc.toMap(x.errorString, e2.toString()), locale));
        }
    }

    /*
     * Parses an XML document from DHL to get the rate estimate
     */
    public static Map<String, Object> handleDhlRateResponse(Document rateResponseDocument, Locale locale) {
        List<Object> errorList = new LinkedList<>();
        Map<String, Object> dhlRateCodeMap = new HashMap<>();
        // process RateResponse
        Element rateResponseElement = rateResponseDocument.getDocumentElement();
        DhlServices.handleErrors(rateResponseElement, errorList, locale);
        if (UtilValidate.isNotEmpty(errorList)) {
            return ServiceUtil.returnError(errorList);
        }
        // handle Response element info
        Element responseElement = UtilXml.firstChildElement(rateResponseElement, x.Shipment);
        Element responseEstimateDetailElement = UtilXml.firstChildElement(responseElement, x.EstimateDetail);

        DhlServices.handleErrors(responseElement, errorList, locale);
        if (UtilValidate.isNotEmpty(errorList)) {
            return ServiceUtil.returnError(errorList);
        }

        String dateGenerated = UtilXml.childElementValue(
                responseEstimateDetailElement, x.DateGenerated);

        Element responseServiceLevelCommitmentElement = UtilXml.firstChildElement(responseEstimateDetailElement,
                    x.ServiceLevelCommitment);
        String responseServiceLevelCommitmentDescription = UtilXml.childElementValue(responseServiceLevelCommitmentElement,
                    x.Desc);

        Element responseRateEstimateElement = UtilXml.firstChildElement(
                responseEstimateDetailElement, x.RateEstimate);
        String responseTotalChargeEstimate = UtilXml.childElementValue(
                responseRateEstimateElement, x.TotalChargeEstimate);
        Element responseChargesElement = UtilXml.firstChildElement(
                responseRateEstimateElement, x.Charges);
        List<? extends Element> chargeNodeList = UtilXml.childElementList(responseChargesElement,
                x.Charge);

        List<Map<String, String>> chargeList = new LinkedList<>();
        if (UtilValidate.isNotEmpty(chargeNodeList)) {
            for (Element responseChargeElement: chargeNodeList) {
                Map<String, String> charge = new HashMap<>();

                Element responseChargeTypeElement = UtilXml.firstChildElement(
                        responseChargeElement, x.Type);

                String responseChargeTypeCode = UtilXml.childElementValue(
                        responseChargeTypeElement, x.Code);
                String responseChargeTypeDesc = UtilXml.childElementValue(
                        responseChargeTypeElement, x.Desc);
                String responseChargeValue = UtilXml.childElementValue(
                        responseChargeElement, x.Value);

                charge.put(x.chargeTypeCode, responseChargeTypeCode);
                charge.put(x.chargeTypeDesc, responseChargeTypeDesc);
                charge.put(x.chargeValue, responseChargeValue);
                chargeList.add(charge);
            }
        }
        BigDecimal shippingEstimateAmount = new BigDecimal(responseTotalChargeEstimate);
        dhlRateCodeMap.put(x.dateGenerated, dateGenerated);
        dhlRateCodeMap.put(x.serviceLevelCommitment,
                responseServiceLevelCommitmentDescription);
        dhlRateCodeMap.put(x.totalChargeEstimate, responseTotalChargeEstimate);
        dhlRateCodeMap.put(x.chargeList, chargeList);

        Map<String, Object> result = ServiceUtil.returnSuccess();
        result.put(x.shippingEstimateAmount, shippingEstimateAmount);
        result.put(x.dhlRateCodeMap, dhlRateCodeMap);
        return result;
    }

    /*
     * Register a DHL account for shipping by obtaining the DHL shipping key
     */
    public static Map<String, Object> dhlRegisterInquire(DispatchContext dctx, DhlServicesContext context) {
        Delegator delegator = dctx.getDelegator();
        String resource = (String) context.get(x.serviceConfigProps);
        String shipmentGatewayConfigId = (String) context.get(x.shipmentGatewayConfigId);
        Locale locale = (Locale) context.get(x.locale);
        Map<String, Object> result;
        String postalCode = (String) context.get(x.postalCode);
        String accountNbr = getShipmentGatewayConfigValue(delegator, shipmentGatewayConfigId, x.accessAccountNbr,
                resource, x.shipment_dhl_access_accountNbr);
        if (accountNbr.isEmpty()) {
            return ServiceUtil.returnError(UtilProperties.getMessage(RES_ERROR,
                    x.FacilityShipmentDhlAccessAccountNbrMandotoryForRegisterAccount, locale));
        }
        // create AccessRequest XML doc
        Document requestDocument = createAccessRequestDocument(delegator, shipmentGatewayConfigId, resource);
        String requestString = null;
        Element requesElement = requestDocument.getDocumentElement();

        Element registerRequestElement = UtilXml.addChildElement(requesElement, x.Register, requestDocument);
        registerRequestElement.setAttribute(x.version, x._1_0);
        registerRequestElement.setAttribute(x.action, x.ShippingKey);
        UtilXml.addChildElementValue(registerRequestElement, x.AccountNbr, accountNbr, requestDocument);
        UtilXml.addChildElementValue(registerRequestElement, x.PostalCode, postalCode, requestDocument);

        try {
            requestString = UtilXml.writeXmlDocument(requestDocument);
            Debug.logInfo(x.AccessRequest_XML_Document + requestString, MODULE);
        } catch (IOException e) {
            String ioeErrMsg = x.Error_writing_the_AccessRequest_XML_Document_to_a_String + e.toString();
            Debug.logError(e, ioeErrMsg, MODULE);
            return ServiceUtil.returnError(UtilProperties.getMessage(RES_ERROR,
                    x.FacilityShipmentDhlErrorAccessRequestXmlToString,
                    UtilMisc.toMap(x.errorString, e.toString()), locale));
        }
        // send the request
        String registerResponseString = null;
        try {
            registerResponseString = sendDhlRequest(requestString, delegator, shipmentGatewayConfigId, resource, locale);
            Debug.logInfo(x.DHL_request_for_DHL_Register_Account + registerResponseString, MODULE);
        } catch (DhlConnectException e) {
            String uceErrMsg = x.Error_sending_DHL_request_for_DHL_Register_Account + e.toString();
            Debug.logError(e, uceErrMsg, MODULE);
            return ServiceUtil.returnError(UtilProperties.getMessage(RES_ERROR,
                    x.FacilityShipmentDhlErrorSendingRequestRegisterAccount,
                    UtilMisc.toMap(x.errorString, e.toString()), locale));
        }

        Document registerResponseDocument = null;
        try {
            registerResponseDocument = UtilXml.readXmlDocument(registerResponseString, false);
            result = handleDhlRegisterResponse(registerResponseDocument, locale);
            Debug.logInfo(x.DHL_response_for_DHL_Register_Account + registerResponseString, MODULE);
        } catch (SAXException | IOException | ParserConfigurationException e2) {
            String excErrMsg = x.Error_parsing_the_RegisterAccountServiceSelectionResponse + e2.toString();
            Debug.logError(e2, excErrMsg, MODULE);
            return ServiceUtil.returnError(UtilProperties.getMessage(RES_ERROR,
                    x.FacilityShipmentDhlErrorParsingRegisterAccountResponse,
                    UtilMisc.toMap(x.errorString, e2.toString()), locale));
        }

        return result;
    }

    /*
     * Parse response from DHL registration request to get shipping key
     */
    public static Map<String, Object> handleDhlRegisterResponse(Document registerResponseDocument, Locale locale) {
        List<Object> errorList = new LinkedList<>();
        // process RegisterResponse
        Element registerResponseElement = registerResponseDocument.getDocumentElement();
        DhlServices.handleErrors(registerResponseElement, errorList, locale);
        if (UtilValidate.isNotEmpty(errorList)) {
            return ServiceUtil.returnError(errorList);
        }
        // handle Response element info
        Element responseElement = UtilXml.firstChildElement(registerResponseElement, x.Register);
        DhlServices.handleErrors(responseElement, errorList, locale);
        if (UtilValidate.isNotEmpty(errorList)) {
            return ServiceUtil.returnError(errorList);
        }
        String responseShippingKey = UtilXml.childElementValue(responseElement, x.ShippingKey);
        Map<String, Object> result = ServiceUtil.returnSuccess();
        result.put(x.shippingKey, responseShippingKey);
        return result;
    }

    /*
     * Pass a shipment request to DHL via ShipIT and get a tracking number and a label back, among other things
     */

    public static Map<String, Object> dhlShipmentConfirm(DispatchContext dctx, DhlServicesContext context) {
        Delegator delegator = dctx.getDelegator();
        LocalDispatcher dispatcher = dctx.getDispatcher();
        Locale locale = (Locale) context.get(x.locale);

        GenericValue userLogin = (GenericValue) context.get(x.userLogin);
        String shipmentId = (String) context.get(x.shipmentId);
        String shipmentRouteSegmentId = (String) context.get(x.shipmentRouteSegmentId);

        Map<String, Object> shipmentGatewayConfig = ShipmentServices.getShipmentGatewayConfigFromShipment(delegator, shipmentId, locale);
        String shipmentGatewayConfigId = (String) shipmentGatewayConfig.get(x.shipmentGatewayConfigId);
        String resource = (String) shipmentGatewayConfig.get(x.configProps);
        if (UtilValidate.isEmpty(shipmentGatewayConfigId) && UtilValidate.isEmpty(resource)) {
            return ServiceUtil.returnError(UtilProperties.getMessage(RES_ERROR,
                    x.FacilityShipmentDhlGatewayNotAvailable, locale));
        }

        try {
            ShipmentDao shipmentDao = DaoRegistry.getDao(delegator, x.Shipment, ShipmentDao.class);
            ShipmentEntity shipmentEntity = shipmentDao.get(shipmentId).orElse(null);
            GenericValue shipment = shipmentEntity == null ? null : delegator.makeValue(x.Shipment, Beans.beanToMap(shipmentEntity));
            if (shipment == null) {
                return ServiceUtil.returnError(UtilProperties.getMessage(RES_ERROR,
                        x.ProductShipmentNotFoundId, locale) + shipmentId);
            }
            ShipmentRouteSegmentDao shipmentRouteSegmentDao = DaoRegistry.getDao(delegator, x.ShipmentRouteSegment, ShipmentRouteSegmentDao.class);
            ShipmentRouteSegmentEntity shipmentRouteSegmentEntity = shipmentRouteSegmentDao.list(Filters.and(
                    Filters.eq(x.shipmentId, shipmentId),
                    Filters.eq(x.shipmentRouteSegmentId, shipmentRouteSegmentId))).stream().findFirst().orElse(null);
            GenericValue shipmentRouteSegment = shipmentRouteSegmentEntity == null ? null
                    : delegator.makeValue(x.ShipmentRouteSegment, Beans.beanToMap(shipmentRouteSegmentEntity));
            if (shipmentRouteSegment == null) {
                return ServiceUtil.returnError(UtilProperties.getMessage(RES_ERROR,
                        x.ProductShipmentRouteSegmentNotFound,
                        UtilMisc.toMap(x.shipmentId, shipmentId, x.shipmentRouteSegmentId, shipmentRouteSegmentId), locale));
            }

            if (!x.DHL.equals(shipmentRouteSegment.getString(x.carrierPartyId))) {
                return ServiceUtil.returnError(UtilProperties.getMessage(RES_ERROR,
                        x.FacilityShipmentDhlNotRouteSegmentCarrier,
                        UtilMisc.toMap(x.shipmentRouteSegmentId, shipmentRouteSegmentId, x.shipmentId, shipmentId), locale));
            }

            // add ShipmentRouteSegment carrierServiceStatusId, check before all DHL services
            if (UtilValidate.isNotEmpty(shipmentRouteSegment.getString(x.carrierServiceStatusId))
                    && !x.SHRSCS_NOT_STARTED.equals(shipmentRouteSegment.getString(x.carrierServiceStatusId))) {
                return ServiceUtil.returnError(UtilProperties.getMessage(RES_ERROR,
                        x.FacilityShipmentDhlRouteSegmentStatusNotStarted,
                        UtilMisc.toMap(x.shipmentRouteSegmentId, shipmentRouteSegmentId, x.shipmentId, shipmentId,
                                x.shipmentRouteSegmentStatus, shipmentRouteSegment.getString(x.carrierServiceStatusId)), locale));
            }

            // Get Origin Info
            GenericValue originPostalAddress = shipmentRouteSegment.getRelatedOne(x.OriginPostalAddress, false);
            if (originPostalAddress == null) {
                return ServiceUtil.returnError(UtilProperties.getMessage(RES_ERROR,
                        x.FacilityShipmentRouteSegmentOriginPostalAddressNotFound,
                        UtilMisc.toMap(x.shipmentId, shipmentId, x.shipmentRouteSegmentId, shipmentRouteSegmentId), locale));
            }
            GenericValue originTelecomNumber = shipmentRouteSegment.getRelatedOne(x.OriginTelecomNumber, false);
            if (originTelecomNumber == null) {
                return ServiceUtil.returnError(UtilProperties.getMessage(RES_ERROR,
                        x.FacilityShipmentRouteSegmentOriginTelecomNumberNotFound,
                        UtilMisc.toMap(x.shipmentId, shipmentId, x.shipmentRouteSegmentId, shipmentRouteSegmentId), locale));
            }
            String originPhoneNumber = originTelecomNumber.getString(x.areaCode) + originTelecomNumber.getString(x.contactNumber);
            // don't put on country code if not specified or is the US country code (UPS wants it this way and assuming DHL will accept this)
            if (UtilValidate.isNotEmpty(originTelecomNumber.getString(x.countryCode))
                    && !x._001.equals(originTelecomNumber.getString(x.countryCode))) {
                originPhoneNumber = originTelecomNumber.getString(x.countryCode) + originPhoneNumber;
            }
            originPhoneNumber = StringUtil.replaceString(originPhoneNumber, x.str_3bc15c8a, x.emptyString);
            originPhoneNumber = StringUtil.replaceString(originPhoneNumber, x.str_b858cb28, x.emptyString);

            // lookup the two letter country code (in the geoCode field)
            GenericValue originCountryGeo = originPostalAddress.getRelatedOne(x.CountryGeo, false);
            if (originCountryGeo == null) {
                return ServiceUtil.returnError(UtilProperties.getMessage(RES_ERROR,
                        x.FacilityShipmentRouteSegmentOriginCountryGeoNotFound,
                        UtilMisc.toMap(x.shipmentId, shipmentId, x.shipmentRouteSegmentId, shipmentRouteSegmentId), locale));
            }

            // Get Dest Info
            GenericValue destPostalAddress = shipmentRouteSegment.getRelatedOne(x.DestPostalAddress, false);
            if (destPostalAddress == null) {
                return ServiceUtil.returnError(UtilProperties.getMessage(RES_ERROR,
                        x.FacilityShipmentRouteSegmentDestPostalAddressNotFound,
                        UtilMisc.toMap(x.shipmentId, shipmentId, x.shipmentRouteSegmentId, shipmentRouteSegmentId), locale));
            }

            // DHL requires destination phone number, default to sender # if no customer number
            String destPhoneNumber = originPhoneNumber;
            GenericValue destTelecomNumber = shipmentRouteSegment.getRelatedOne(x.DestTelecomNumber, false);
            if (destTelecomNumber != null) {
                destPhoneNumber = destTelecomNumber.getString(x.areaCode) + destTelecomNumber.getString(x.contactNumber);
                // don't put on country code if not specified or is the US country code (UPS wants it this way)
                if (UtilValidate.isNotEmpty(destTelecomNumber.getString(x.countryCode))
                        && !x._001.equals(destTelecomNumber.getString(x.countryCode))) {
                    destPhoneNumber = destTelecomNumber.getString(x.countryCode) + destPhoneNumber;
                }
                destPhoneNumber = StringUtil.replaceString(destPhoneNumber, x.str_3bc15c8a, x.emptyString);
                destPhoneNumber = StringUtil.replaceString(destPhoneNumber, x.str_b858cb28, x.emptyString);
            }

            String recipientEmail = null;
            Map<String, Object> results = dispatcher.runSync(x.getPartyEmail, UtilMisc.toMap(x.partyId,
                    shipment.get(x.partyIdTo), x.userLogin, userLogin));
            if (ServiceUtil.isError(results)) {
                return ServiceUtil.returnError(ServiceUtil.getErrorMessage(results));
            }
            if (results.get(x.emailAddress) != null) {
                recipientEmail = (String) results.get(x.emailAddress);
            }

            // lookup the two letter country code (in the geoCode field)
            GenericValue destCountryGeo = destPostalAddress.getRelatedOne(x.CountryGeo, false);
            if (destCountryGeo == null) {
                return ServiceUtil.returnError(UtilProperties.getMessage(RES_ERROR,
                        x.FacilityShipmentRouteSegmentDestCountryGeoNotFound,
                        UtilMisc.toMap(x.shipmentId, shipmentId, x.shipmentRouteSegmentId, shipmentRouteSegmentId), locale));
            }

            List<GenericValue> shipmentPackageRouteSegs = shipmentRouteSegment.getRelated(x.ShipmentPackageRouteSeg,
                    null, UtilMisc.toList(x.shipmentPackageSeqId_39d5d38d), false);
            if (UtilValidate.isEmpty(shipmentPackageRouteSegs)) {
                return ServiceUtil.returnError(UtilProperties.getMessage(RES_ERROR,
                        x.FacilityShipmentPackageRouteSegsNotFound,
                        UtilMisc.toMap(x.shipmentId, shipmentId, x.shipmentRouteSegmentId, shipmentRouteSegmentId), locale));
            }
            if (shipmentPackageRouteSegs.size() != 1) {
                return ServiceUtil.returnError(UtilProperties.getMessage(RES_ERROR,
                        x.FacilityShipmentDhlMultiplePackagesNotSupported, locale));
            }

            // get the weight from the ShipmentRouteSegment first, which overrides all later weight computations
            boolean hasBillingWeight = false;  // for later overrides
            BigDecimal billingWeight = shipmentRouteSegment.getBigDecimal(x.billingWeight);
            String billingWeightUomId = shipmentRouteSegment.getString(x.billingWeightUomId);
            if ((billingWeight != null) && (billingWeight.compareTo(BigDecimal.ZERO) > 0)) {
                hasBillingWeight = true;
                if (billingWeightUomId == null) {
                    Debug.logWarning(x.Shipment_Route_Segment_missing_billingWeightUomId_in_shipmentId + shipmentId, MODULE);
                    billingWeightUomId = x.WT_lb; // TODO: this should be specified in a properties file
                }
                // convert
                results = dispatcher.runSync(x.convertUom, UtilMisc.<String, Object>toMap(x.uomId, billingWeightUomId, x.uomIdTo,
                        DHL_WEIGHT_UOM_ID, x.originalValue, billingWeight));
                if (ServiceUtil.isError(results) || (results.get(x.convertedValue) == null)) {
                    Debug.logWarning(x.Unable_to_convert_billing_weights_for_shipmentId + shipmentId, MODULE);
                    // try getting the weight from package instead
                    hasBillingWeight = false;
                } else {
                    billingWeight = (BigDecimal) results.get(x.convertedValue);
                }
            }

            // loop through Shipment segments (NOTE: only one supported, loop is here for future refactoring reference)
            BigDecimal packageWeight = null;
            for (GenericValue shipmentPackageRouteSeg: shipmentPackageRouteSegs) {
                GenericValue shipmentPackage = shipmentPackageRouteSeg.getRelatedOne(x.ShipmentPackage, false);
                GenericValue shipmentBoxType = shipmentPackage.getRelatedOne(x.ShipmentBoxType, false);

                //if (shipmentBoxType != null) {
                    // TODO: determine what default UoM is (assuming inches) - there should be a defaultDimensionUomId in Facility
                //}

                // next step is weight determination, so skip if we have a billing weight
                if (hasBillingWeight) continue;

                // compute total packageWeight (for now, just one package)
                if (shipmentPackage.getString(x.weight) != null) {
                    packageWeight = new BigDecimal(shipmentPackage.getString(x.weight));
                } else {
                    // use default weight if available
                    try {
                        packageWeight = EntityUtilProperties.getPropertyAsBigDecimal(SHIPMENT_PROPERTIES_FILE,
                                x.shipment_default_weight_value, BigDecimal.ZERO);
                    } catch (NumberFormatException ne) {
                        Debug.logWarning(x.Default_shippable_weight_not_configured_shipment_default_weight_value, MODULE);
                        packageWeight = BigDecimal.ONE;
                    }
                }
                // convert weight
                String weightUomId = (String) shipmentPackage.get(x.weightUomId);
                if (weightUomId == null) {
                    Debug.logWarning(x.Shipment_Route_Segment_missing_weightUomId_in_shipmentId + shipmentId, MODULE);
                    weightUomId = x.WT_lb; // TODO: this should be specified in a properties file
                }
                results = dispatcher.runSync(x.convertUom, UtilMisc.<String, Object>toMap(x.uomId, weightUomId, x.uomIdTo,
                        DHL_WEIGHT_UOM_ID, x.originalValue, packageWeight));
                if (ServiceUtil.isError(results)) {
                    return ServiceUtil.returnError(ServiceUtil.getErrorMessage(results));
                }
                if ((results == null) || (results.get(ModelService.RESPONSE_MESSAGE).equals(ModelService.RESPOND_ERROR))
                        || (results.get(x.convertedValue) == null)) {
                    Debug.logWarning(x.Unable_to_convert_weights_for_shipmentId + shipmentId, MODULE);
                    packageWeight = BigDecimal.ONE;
                } else {
                    packageWeight = (BigDecimal) results.get(x.convertedValue);
                }
            }

            // pick which weight to use and round it
            BigDecimal weight = null;
            if (hasBillingWeight) {
                weight = billingWeight;
            } else {
                weight = packageWeight;
            }
            // want the rounded weight as a string, so we use the "" + int shortcut
            String roundedWeight = weight.setScale(0, RoundingMode.HALF_UP).toPlainString();

            // translate shipmentMethodTypeId to DHL service code
            String shipmentMethodTypeId = shipmentRouteSegment.getString(x.shipmentMethodTypeId);
            String dhlShipmentDetailCode = null;
            CarrierShipmentMethodDao carrierShipmentMethodDao = DaoRegistry.getDao(delegator, x.CarrierShipmentMethod,
                    CarrierShipmentMethodDao.class);
            CarrierShipmentMethodEntity carrierShipmentMethodEntity = carrierShipmentMethodDao.list(Filters.and(
                    Filters.eq(x.shipmentMethodTypeId, shipmentMethodTypeId),
                    Filters.eq(x.partyId, x.DHL),
                    Filters.eq(x.roleTypeId, x.CARRIER))).stream().findFirst().orElse(null);
            GenericValue carrierShipmentMethod = carrierShipmentMethodEntity == null ? null
                    : delegator.makeValue(x.CarrierShipmentMethod, Beans.beanToMap(carrierShipmentMethodEntity));
            if (carrierShipmentMethod == null) {
                return ServiceUtil.returnError(UtilProperties.getMessage(RES_ERROR,
                        x.FacilityShipmentDhlNoCarrierShipmentMethod,
                        UtilMisc.toMap(x.carrierPartyId, x.DHL, x.shipmentMethodTypeId, shipmentMethodTypeId), locale));
            }
            dhlShipmentDetailCode = carrierShipmentMethod.getString(x.carrierServiceCode);

            // shipping credentials (configured in properties)
            String userid = getShipmentGatewayConfigValue(delegator, shipmentGatewayConfigId,
                    x.accessUserId, resource, x.shipment_dhl_access_userid);
            String password = getShipmentGatewayConfigValue(delegator, shipmentGatewayConfigId,
                    x.accessPassword, resource, x.shipment_dhl_access_password);
            String shippingKey = getShipmentGatewayConfigValue(delegator, shipmentGatewayConfigId,
                    x.accessShippingKey, resource, x.shipment_dhl_access_shippingKey);
            String accountNbr = getShipmentGatewayConfigValue(delegator, shipmentGatewayConfigId,
                    x.accessAccountNbr, resource, x.shipment_dhl_access_accountNbr);
            if ((shippingKey.isEmpty()) || (accountNbr.isEmpty())) {
                return ServiceUtil.returnError(UtilProperties.getMessage(RES_ERROR,
                        x.FacilityShipmentDhlGatewayNotAvailable, locale));
            }

            // label image preference (PNG or GIF)
            String labelImagePreference = getShipmentGatewayConfigValue(delegator, shipmentGatewayConfigId,
                    x.labelImageFormat, resource, x.shipment_dhl_label_image_format);
            if (labelImagePreference.isEmpty()) {
                Debug.logInfo(x.shipment_dhl_label_image_format_not_specified_assuming_PNG, MODULE);
                labelImagePreference = x.PNG;
            } else if (!(x.PNG.equals(labelImagePreference) || x.GIF.equals(labelImagePreference))) {
                Debug.logError(x.Illegal_shipment_dhl_label_image_format + labelImagePreference, MODULE);
                return ServiceUtil.returnError(UtilProperties.getMessage(RES_ERROR,
                        x.FacilityShipmentDhlUnknownLabelImageFormat,
                        UtilMisc.toMap(x.labelImagePreference, labelImagePreference), locale));
            }

            // create AccessRequest XML doc using FreeMarker template
            String templateName = getShipmentGatewayConfigValue(delegator, shipmentGatewayConfigId,
                    x.rateEstimateTemplate, resource, x.shipment_dhl_template_rate_estimate);
            if ((templateName.trim().isEmpty())) {
                return ServiceUtil.returnError(UtilProperties.getMessage(RES_ERROR,
                        x.FacilityShipmentDhlRateEstimateTemplateNotConfigured, locale));
            }
            StringWriter outWriter = new StringWriter();
            Map<String, Object> inContext = new HashMap<>();
            inContext.put(x.action, x.GenerateLabel);
            inContext.put(x.userid, userid);
            inContext.put(x.password, password);
            inContext.put(x.accountNbr, accountNbr);
            inContext.put(x.shippingKey, shippingKey);
            inContext.put(x.shipDate, UtilDateTime.nowTimestamp());
            inContext.put(x.dhlShipmentDetailCode, dhlShipmentDetailCode);
            inContext.put(x.weight, roundedWeight);
            inContext.put(x.senderPhoneNbr, originPhoneNumber);
            inContext.put(x.companyName, destPostalAddress.getString(x.toName));
            inContext.put(x.attnTo, destPostalAddress.getString(x.attnName));
            inContext.put(x.street, destPostalAddress.getString(x.address1));
            inContext.put(x.streetLine2, destPostalAddress.getString(x.address2));
            inContext.put(x.city, destPostalAddress.getString(x.city));
            inContext.put(x.state, destPostalAddress.getString(x.stateProvinceGeoId));

            // DHL ShipIT API does not accept ZIP+4
            if ((destPostalAddress.getString(x.postalCode) != null) && (destPostalAddress.getString(x.postalCode).length() > 5)) {
                inContext.put(x.postalCode, destPostalAddress.getString(x.postalCode).substring(0, 5));
            } else {
                inContext.put(x.postalCode, destPostalAddress.getString(x.postalCode));
            }
            inContext.put(x.phoneNbr, destPhoneNumber);
            inContext.put(x.labelImageType, labelImagePreference);
            inContext.put(x.shipperReference, shipment.getString(x.primaryOrderId) + x.str_3bc15c8a + shipment.getString(x.primaryShipGroupSeqId));
            inContext.put(x.notifyEmailAddress, recipientEmail);

            try {
                ContentWorker.renderContentAsText(dispatcher, templateName, outWriter, inContext, locale, x.text_plain, null, null, false);
            } catch (Exception e) {
                Debug.logError(e, x.Cannot_confirm_DHL_shipment_Failed_to_render_DHL_XML_Request, MODULE);
                return ServiceUtil.returnError(UtilProperties.getMessage(RES_ERROR,
                        x.FacilityShipmentFedexRateTemplateRenderingError, locale));
            }
            String requestString = outWriter.toString();
            if (Debug.verboseOn()) {
                Debug.logVerbose(requestString, MODULE);
            }

            // send the request
            String responseString = null;
            try {
                responseString = sendDhlRequest(requestString, delegator, shipmentGatewayConfigId, resource, locale);
                if (Debug.verboseOn()) {
                    Debug.logVerbose(responseString, MODULE);
                }
            } catch (DhlConnectException e) {
                String uceErrMsg = x.Error_sending_DHL_request_for_DHL_Service_Rate + e.toString();
                Debug.logError(e, uceErrMsg, MODULE);
                return ServiceUtil.returnError(UtilProperties.getMessage(RES_ERROR,
                        x.FacilityShipmentFedexRateTemplateSendingError,
                        UtilMisc.toMap(x.errorString, e.toString()), locale));
            }
            // pass to handler method
            return handleDhlShipmentConfirmResponse(responseString, shipmentRouteSegment, shipmentPackageRouteSegs, locale);
        } catch (Exception e) {
            Debug.logError(e, MODULE);
            return ServiceUtil.returnError(UtilProperties.getMessage(RES_ERROR,
                    x.FacilityShipmentFedexRateTemplateReadingError,
                    UtilMisc.toMap(x.errorString, e.toString()), locale));
        }
    }

    // NOTE: Must VOID shipments on errors
    public static Map<String, Object> handleDhlShipmentConfirmResponse(String rateResponseString, GenericValue shipmentRouteSegment,
            List<GenericValue> shipmentPackageRouteSegs, Locale locale) throws GenericEntityException {
        GenericValue shipmentPackageRouteSeg = shipmentPackageRouteSegs.get(0);

        // TODO: figure out how to handle validation on return XML, which can be mangled
        // Ideas: try again right away, let user try again, etc.
        Document rateResponseDocument = null;
        try {
            rateResponseDocument = UtilXml.readXmlDocument(rateResponseString, false);
        } catch (SAXException | IOException | ParserConfigurationException e2) {
            String excErrMsg = x.Error_parsing_the_RatingServiceSelectionResponse + e2.toString();
            Debug.logError(e2, excErrMsg, MODULE);
            // TODO: VOID
        }

        // tracking number: Shipment/ShipmentDetail/AirbillNbr
        Element rootElement = rateResponseDocument.getDocumentElement();
        Element shipmentElement = UtilXml.firstChildElement(rootElement, x.Shipment);
        Element shipmentDetailElement = UtilXml.firstChildElement(shipmentElement, x.ShipmentDetail);
        String trackingNumber = UtilXml.childElementValue(shipmentDetailElement, x.AirbillNbr);

        // label: Shipment/Label/Image
        Element labelElement = UtilXml.firstChildElement(shipmentElement, x.Label);
        String encodedImageString = UtilXml.childElementValue(labelElement, x.Image);
        if (encodedImageString == null) {
            Debug.logError(x.Cannot_find_response_DHL_shipment_label_Rate_response_document_is + rateResponseString, MODULE);
            return ServiceUtil.returnError(UtilProperties.getMessage(RES_ERROR,
                    x.FacilityShipmentDhlShipmentLabelError,
                    UtilMisc.toMap(x.shipmentPackageRouteSeg, shipmentPackageRouteSeg,
                            x.rateResponseString, rateResponseString), locale));
        }

        // TODO: this is a temporary hack to replace the newlines so that Base64 likes the input This is NOT platform independent
        int size = encodedImageString.length();
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < size; i++) {
            if (encodedImageString.charAt(i) == '\n') {
                continue;
            }
            sb.append(encodedImageString.charAt(i));
        }
        byte[] labelBytes = Base64.getMimeDecoder().decode(sb.toString().getBytes(StandardCharsets.UTF_8));

        if (labelBytes != null) {
            // store in db blob
            shipmentPackageRouteSeg.setBytes(x.labelImage, labelBytes);
        } else {
            Debug.logInfo(x.Failed_to_either_decode_returned_DHL_label_or_no_data_found_in_eCommerce_Shipment_Label_Image, MODULE);
            // TODO: VOID
        }

        shipmentPackageRouteSeg.set(x.trackingCode, trackingNumber);
        shipmentPackageRouteSeg.set(x.labelHtml, sb.toString());
        shipmentPackageRouteSeg.store();

        shipmentRouteSegment.set(x.trackingIdNumber, trackingNumber);
        shipmentRouteSegment.put(x.carrierServiceStatusId, x.SHRSCS_CONFIRMED);
        shipmentRouteSegment.store();

        return ServiceUtil.returnSuccess(UtilProperties.getMessage(RES_ERROR,
                x.FacilityShipmentDhlShipmentConfirmed, locale));
    }


    public static Document createAccessRequestDocument(Delegator delegator, String shipmentGatewayConfigId, String resource) {
        Document eCommerceRequestDocument = UtilXml.makeEmptyXmlDocument(x.eCommerce);
        Element eCommerceRequesElement = eCommerceRequestDocument.getDocumentElement();
        eCommerceRequesElement.setAttribute(x.version, getShipmentGatewayConfigValue(delegator, shipmentGatewayConfigId,
                x.headVersion, resource, x.shipment_dhl_head_version));
        eCommerceRequesElement.setAttribute(x.action, getShipmentGatewayConfigValue(delegator,
                shipmentGatewayConfigId, x.headAction, resource, x.shipment_dhl_head_action));
        Element requestorRequestElement = UtilXml.addChildElement(eCommerceRequesElement, x.Requestor, eCommerceRequestDocument);
        UtilXml.addChildElementValue(requestorRequestElement, x.ID, getShipmentGatewayConfigValue(delegator,
                shipmentGatewayConfigId, x.accessUserId, resource, x.shipment_dhl_access_userid),
                eCommerceRequestDocument);
        UtilXml.addChildElementValue(requestorRequestElement, x.Password, getShipmentGatewayConfigValue(delegator,
                shipmentGatewayConfigId, x.accessPassword, resource, x.shipment_dhl_access_password),
                eCommerceRequestDocument);
        return eCommerceRequestDocument;
    }

    public static void handleErrors(Element responseElement, List<Object> errorList, Locale locale) {
        Element faultsElement = UtilXml.firstChildElement(responseElement,
                x.Faults);
        List<? extends Element> faultElements = UtilXml.childElementList(faultsElement, x.Fault_9d5daffa);
        if (UtilValidate.isNotEmpty(faultElements)) {
            for (Element errorElement: faultElements) {
                StringBuilder errorMessageBuf = new StringBuilder();

                String errorCode = UtilXml.childElementValue(errorElement, x.Code);
                String errorDescription = UtilXml.childElementValue(errorElement, x.Desc);
                String errorSource = UtilXml.childElementValue(errorElement, x.Source);
                if (UtilValidate.isEmpty(errorSource)) {
                    errorSource = UtilXml.childElementValue(errorElement, x.Context);
                }
                errorMessageBuf.append(UtilProperties.getMessage(RES_ERROR, x.FacilityShipmentDhlErrorMessage,
                        UtilMisc.toMap(x.errorCode, errorCode, x.errorDescription, errorDescription), locale));
                if (UtilValidate.isNotEmpty(errorSource)) {
                    errorMessageBuf.append(UtilProperties.getMessage(RES_ERROR,
                            x.FacilityShipmentDhlErrorMessageElement,
                            UtilMisc.toMap(x.errorSource, errorSource), locale));
                }
                errorList.add(errorMessageBuf.toString());
            }
        }
    }

    private static String getShipmentGatewayConfigValue(Delegator delegator, String shipmentGatewayConfigId, String
            shipmentGatewayConfigParameterName, String resource, String parameterName) {
        String returnValue = x.emptyString;
        if (UtilValidate.isNotEmpty(shipmentGatewayConfigId)) {
            try {
                ShipmentGatewayDhlDao shipmentGatewayDhlDao = DaoRegistry.getDao(delegator, x.ShipmentGatewayDhl, ShipmentGatewayDhlDao.class);
                ShipmentGatewayDhlEntity shipmentGatewayDhlEntity = shipmentGatewayDhlDao.get(shipmentGatewayConfigId).orElse(null);
                GenericValue dhl = shipmentGatewayDhlEntity == null ? null
                        : delegator.makeValue(x.ShipmentGatewayDhl, Beans.beanToMap(shipmentGatewayDhlEntity));
                if (UtilValidate.isNotEmpty(dhl)) {
                    Object dhlField = dhl.get(shipmentGatewayConfigParameterName);
                    if (dhlField != null) {
                        returnValue = dhlField.toString().trim();
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
            shipmentGatewayConfigParameterName, String resource, String parameterName, String defaultValue) {
        String returnValue = getShipmentGatewayConfigValue(delegator, shipmentGatewayConfigId, shipmentGatewayConfigParameterName,
                resource, parameterName);
        if (UtilValidate.isEmpty(returnValue)) {
            returnValue = defaultValue;
        }
        return returnValue;
    }
}
@SuppressWarnings(x.serial)
class DhlConnectException extends GeneralException {
    DhlConnectException() {
        super();
    }

    DhlConnectException(String msg) {
        super(msg);
    }

    DhlConnectException(Throwable t) {
        super(t);
    }

    DhlConnectException(String msg, Throwable t) {
        super(msg, t);
    }
}
