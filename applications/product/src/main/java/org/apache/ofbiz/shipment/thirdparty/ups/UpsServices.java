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
package org.apache.ofbiz.shipment.thirdparty.ups;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Base64;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import javax.xml.parsers.ParserConfigurationException;

import org.apache.ofbiz.base.util.Debug;
import org.apache.ofbiz.base.util.GeneralException;
import org.apache.ofbiz.base.util.HttpClient;
import org.apache.ofbiz.base.util.HttpClientException;
import org.apache.ofbiz.base.util.StringUtil;
import org.apache.ofbiz.base.util.UtilGenerics;
import org.apache.ofbiz.base.util.UtilMisc;
import org.apache.ofbiz.base.util.UtilNumber;
import org.apache.ofbiz.base.util.UtilProperties;
import org.apache.ofbiz.base.util.UtilValidate;
import org.apache.ofbiz.base.util.UtilXml;
import org.apache.ofbiz.base.util.string.FlexibleStringExpander;
import org.apache.ofbiz.entity.Delegator;
import org.apache.ofbiz.entity.GenericEntityException;
import org.apache.ofbiz.entity.GenericValue;
import org.apache.ofbiz.entity.condition.EntityCondition;
import org.apache.ofbiz.entity.condition.EntityOperator;
import org.apache.ofbiz.entity.util.EntityUtil;
import org.apache.ofbiz.entity.util.EntityUtilProperties;
import org.apache.ofbiz.party.contact.ContactMechWorker;
import org.apache.ofbiz.persistence.dao.CarrierShipmentMethodDao;
import org.apache.ofbiz.persistence.dao.DaoRegistry;
import org.apache.ofbiz.persistence.dao.OrderPaymentPreferenceDao;
import org.apache.ofbiz.persistence.dao.PostalAddressDao;
import org.apache.ofbiz.persistence.dao.ProductStoreShipmentMethDao;
import org.apache.ofbiz.persistence.dao.ShipmentDao;
import org.apache.ofbiz.persistence.dao.ShipmentGatewayUpsDao;
import org.apache.ofbiz.persistence.dao.ShipmentRouteSegmentDao;
import org.apache.ofbiz.product.store.ProductStoreWorker;
import org.apache.ofbiz.service.DispatchContext;
import org.apache.ofbiz.service.GenericServiceException;
import org.apache.ofbiz.service.LocalDispatcher;
import org.apache.ofbiz.service.ModelService;
import org.apache.ofbiz.service.ServiceUtil;
import org.apache.ofbiz.shipment.shipment.ShipmentServices;
import org.apache.ofbiz.shipment.shipment.ShipmentWorker;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.xml.sax.SAXException;


import org.apache.ofbiz.persistence.entity.x;
import org.apache.ofbiz.model.ServiceContext;
import org.apache.ofbiz.model.UpsServicesContext;
/**
 * UPS ShipmentServices
 */
public class UpsServices {

    private static final String MODULE = UpsServices.class.getName();
    private static final String RES_ERROR = x.ProductUiLabels;
    private static final String RES_ORDER = x.OrderUiLabels;

    private static final Map<String, String> UPS_TO_OFBIZ = new HashMap<>();
    private static final Map<String, String> OFBIZ_TO_UPS = new HashMap<>();
    private static final int DECIMALS = UtilNumber.getBigDecimalScale(x.order_decimals);
    private static final RoundingMode ROUNDING = UtilNumber.getRoundingMode(x.order_rounding);
    private static final int RET_SERVICE_CODE = 8;
    private static final String DATE_FORMAT = x.yyyyMMdd;

    static {
        UPS_TO_OFBIZ.put(x.LBS, x.WT_lb);
        UPS_TO_OFBIZ.put(x.KGS, x.WT_kg);
        for (Map.Entry<String, String> entry : UPS_TO_OFBIZ.entrySet()) {
            OFBIZ_TO_UPS.put(entry.getValue(), entry.getKey());
        }
    }

    public static Map<String, Object> upsShipmentConfirm(DispatchContext dctx, UpsServicesContext context) {
        Delegator delegator = dctx.getDelegator();
        LocalDispatcher dispatcher = dctx.getDispatcher();
        GenericValue userLogin = (GenericValue) context.get(x.userLogin);
        Locale locale = (Locale) context.get(x.locale);
        String shipmentId = (String) context.get(x.shipmentId);
        String shipmentRouteSegmentId = (String) context.get(x.shipmentRouteSegmentId);

        Map<String, Object> shipmentGatewayConfig = ShipmentServices.getShipmentGatewayConfigFromShipment(delegator, shipmentId, locale);
        String shipmentGatewayConfigId = (String) shipmentGatewayConfig.get(x.shipmentGatewayConfigId);
        String resource = (String) shipmentGatewayConfig.get(x.configProps);
        if (UtilValidate.isEmpty(shipmentGatewayConfigId) && UtilValidate.isEmpty(resource)) {
            return ServiceUtil.returnError(UtilProperties.getMessage(RES_ERROR, x.FacilityShipmentUpsGatewayNotAvailable, locale));
        }
        boolean shipmentUpsSaveCertificationInfo = x._true.equals(getShipmentGatewayConfigValue(delegator, shipmentGatewayConfigId, x.saveCertInfo,
                resource, x.shipment_ups_save_certification_info, x._true));
        String shipmentUpsSaveCertificationPath = FlexibleStringExpander.expandString(getShipmentGatewayConfigValue(delegator,
                shipmentGatewayConfigId, x.saveCertPath, resource, x.shipment_ups_save_certification_path, x.emptyString), context);
        File shipmentUpsSaveCertificationFile = null;
        if (shipmentUpsSaveCertificationInfo) {
            shipmentUpsSaveCertificationFile = new File(shipmentUpsSaveCertificationPath);
            if (!shipmentUpsSaveCertificationFile.exists()) {
                shipmentUpsSaveCertificationFile.mkdirs();
            }
        }

        String shipmentConfirmResponseString = null;

        try {
            ShipmentDao shipmentDao = DaoRegistry.getDao(delegator, x.Shipment, ShipmentDao.class);
            ShipmentRouteSegmentDao shipmentRouteSegmentDao = DaoRegistry.getDao(delegator, x.ShipmentRouteSegment,
                    ShipmentRouteSegmentDao.class);
            CarrierShipmentMethodDao carrierShipmentMethodDao = DaoRegistry.getDao(delegator, x.CarrierShipmentMethod,
                    CarrierShipmentMethodDao.class);
            OrderPaymentPreferenceDao orderPaymentPreferenceDao = DaoRegistry.getDao(delegator, x.OrderPaymentPreference,
                    OrderPaymentPreferenceDao.class);
            GenericValue shipment = shipmentDao.findOneByWhere(delegator, x.Shipment, UtilMisc.toMap(x.shipmentId, shipmentId), null, null,
                    false);
            if (shipment == null) {
                return ServiceUtil.returnError(UtilProperties.getMessage(RES_ERROR, x.ProductShipmentNotFoundId, locale) + x.str_b858cb28 + shipmentId);
            }
            GenericValue shipmentRouteSegment = shipmentRouteSegmentDao.findOneByWhere(delegator, x.ShipmentRouteSegment,
                    UtilMisc.toMap(x.shipmentId, shipmentId, x.shipmentRouteSegmentId, shipmentRouteSegmentId), null, null, false);
            if (shipmentRouteSegment == null) {
                return ServiceUtil.returnError(UtilProperties.getMessage(RES_ERROR, x.ProductShipmentRouteSegmentNotFound,
                        UtilMisc.toMap(x.shipmentId, shipmentId, x.shipmentRouteSegmentId, shipmentRouteSegmentId), locale));
            }

            if (!x.UPS.equals(shipmentRouteSegment.getString(x.carrierPartyId))) {
                return ServiceUtil.returnError(UtilProperties.getMessage(RES_ERROR, x.FacilityShipmentUpsNotRouteSegmentCarrier,
                        UtilMisc.toMap(x.shipmentRouteSegmentId, shipmentRouteSegmentId, x.shipmentId, shipmentId), locale));
            }

            // add ShipmentRouteSegment carrierServiceStatusId, check before all UPS services
            if (UtilValidate.isNotEmpty(shipmentRouteSegment.getString(x.carrierServiceStatusId))
                    && !x.SHRSCS_NOT_STARTED.equals(shipmentRouteSegment.getString(x.carrierServiceStatusId))) {
                return ServiceUtil.returnError(UtilProperties.getMessage(RES_ERROR, x.FacilityShipmentUpsRouteSegmentStatusNotStarted,
                        UtilMisc.toMap(x.shipmentRouteSegmentId, shipmentRouteSegmentId, x.shipmentId, shipmentId, x.shipmentRouteSegmentStatus,
                                shipmentRouteSegment.getString(x.carrierServiceStatusId)), locale));
            }

            // Get Origin Info
            GenericValue originPostalAddress = shipmentRouteSegment.getRelatedOne(x.OriginPostalAddress, false);
            if (originPostalAddress == null) {
                return ServiceUtil.returnError(UtilProperties.getMessage(RES_ERROR, x.FacilityShipmentRouteSegmentOriginPostalAddressNotFound,
                        UtilMisc.toMap(x.shipmentId, shipmentId, x.shipmentRouteSegmentId, shipmentRouteSegmentId), locale));
            }
            GenericValue originTelecomNumber = shipmentRouteSegment.getRelatedOne(x.OriginTelecomNumber, false);
            if (originTelecomNumber == null) {
                return ServiceUtil.returnError(UtilProperties.getMessage(RES_ERROR, x.FacilityShipmentRouteSegmentOriginTelecomNumberNotFound,
                        UtilMisc.toMap(x.shipmentId, shipmentId, x.shipmentRouteSegmentId, shipmentRouteSegmentId), locale));
            }
            String originPhoneNumber = originTelecomNumber.getString(x.areaCode) + originTelecomNumber.getString(x.contactNumber);
            // don't put on country code if not specified or is the US country code (UPS wants it this way)
            if (UtilValidate.isNotEmpty(originTelecomNumber.getString(x.countryCode))
                    && !x._001.equals(originTelecomNumber.getString(x.countryCode))) {
                originPhoneNumber = originTelecomNumber.getString(x.countryCode) + originPhoneNumber;
            }
            originPhoneNumber = StringUtil.replaceString(originPhoneNumber, x.str_3bc15c8a, x.emptyString);
            originPhoneNumber = StringUtil.replaceString(originPhoneNumber, x.str_b858cb28, x.emptyString);
            // lookup the two letter country code (in the geoCode field)
            GenericValue originCountryGeo = originPostalAddress.getRelatedOne(x.CountryGeo, false);
            if (originCountryGeo == null) {
                return ServiceUtil.returnError(UtilProperties.getMessage(RES_ERROR, x.FacilityShipmentRouteSegmentOriginCountryGeoNotFound,
                        UtilMisc.toMap(x.shipmentId, shipmentId, x.shipmentRouteSegmentId, shipmentRouteSegmentId), locale));
            }

            // Get Dest Info
            GenericValue destPostalAddress = shipmentRouteSegment.getRelatedOne(x.DestPostalAddress, false);
            if (destPostalAddress == null) {
                return ServiceUtil.returnError(UtilProperties.getMessage(RES_ERROR, x.FacilityShipmentRouteSegmentDestPostalAddressNotFound,
                        UtilMisc.toMap(x.shipmentId, shipmentId, x.shipmentRouteSegmentId, shipmentRouteSegmentId), locale));
            }

            GenericValue destTelecomNumber = shipmentRouteSegment.getRelatedOne(x.DestTelecomNumber, false);
            if (destTelecomNumber == null) {
                String missingErrMsg = x.DestTelecomNumber_not_found_for_ShipmentRouteSegment_with_shipmentId + shipmentId + x._and
                        + x.shipmentRouteSegmentId_59538687 + shipmentRouteSegmentId;
                Debug.logError(missingErrMsg, MODULE);
                // for now we won't require the dest phone number, but is it required?
            }
            String destPhoneNumber = null;
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

            // lookup the two letter country code (in the geoCode field)
            GenericValue destCountryGeo = destPostalAddress.getRelatedOne(x.CountryGeo, false);
            if (destCountryGeo == null) {
                return ServiceUtil.returnError(UtilProperties.getMessage(RES_ERROR, x.FacilityShipmentRouteSegmentDestCountryGeoNotFound,
                        UtilMisc.toMap(x.shipmentId, shipmentId, x.shipmentRouteSegmentId, shipmentRouteSegmentId), locale));
            }

            GenericValue carrierShipmentMethod = carrierShipmentMethodDao.findOneByWhere(delegator, x.CarrierShipmentMethod,
                    UtilMisc.toMap(x.partyId, shipmentRouteSegment.get(x.carrierPartyId), x.roleTypeId, x.CARRIER, x.shipmentMethodTypeId,
                            shipmentRouteSegment.get(x.shipmentMethodTypeId)), null, null, false);
            if (carrierShipmentMethod == null) {
                return ServiceUtil.returnError(UtilProperties.getMessage(RES_ERROR, x.FacilityShipmentUpsRouteSegmentCarrierShipmentMethodNotFound,
                        UtilMisc.toMap(x.shipmentId, shipmentId, x.shipmentRouteSegmentId, shipmentRouteSegmentId, x.carrierPartyId,
                                shipmentRouteSegment.get(x.carrierPartyId), x.shipmentMethodTypeId, shipmentRouteSegment.get(x.shipmentMethodTypeId)),
                        locale));
            }

            List<GenericValue> shipmentPackageRouteSegs = shipmentRouteSegment.getRelated(x.ShipmentPackageRouteSeg, null, UtilMisc.toList(
                    x.shipmentPackageSeqId_39d5d38d), false);
            if (UtilValidate.isEmpty(shipmentPackageRouteSegs)) {
                return ServiceUtil.returnError(UtilProperties.getMessage(RES_ERROR, x.FacilityShipmentPackageRouteSegsNotFound,
                        UtilMisc.toMap(x.shipmentId, shipmentId, x.shipmentRouteSegmentId, shipmentRouteSegmentId), locale));
            }

            List<GenericValue> itemIssuances = shipment.getRelated(x.ItemIssuance, null, null, false);
            Set<String> orderIdSet = new TreeSet<>();
            for (GenericValue itemIssuance : itemIssuances) {
                orderIdSet.add(itemIssuance.getString(x.orderId));
            }
            String ordersDescription = x.emptyString;
            if (orderIdSet.size() > 1) {

                StringBuilder odBuf = new StringBuilder(UtilProperties.getMessage(RES_ORDER, x.OrderOrders, locale) + x.str_b858cb28);
                for (String orderId : orderIdSet) {
                    if (odBuf.length() > 0) {
                        odBuf.append(x.str_d3bc9a37);
                    }
                    odBuf.append(orderId);
                }
                ordersDescription = odBuf.toString();
            } else if (!orderIdSet.isEmpty()) {
                ordersDescription = UtilProperties.getMessage(RES_ORDER, x.OrderOrder, locale) + x.str_b858cb28 + orderIdSet.iterator().next();
            }

            // COD Support
            boolean allowCOD = x._true.equalsIgnoreCase(getShipmentGatewayConfigValue(delegator, shipmentGatewayConfigId, x.codAllowCod, resource,
                    x.shipment_ups_cod_allowCOD, x._true));

            // COD only applies if all orders involved with the shipment were paid only with EXT_COD - anything else becomes too complicated
            if (allowCOD) {

                // Get the paymentMethodTypeIds of all the orderPaymentPreferences involved with the shipment
                List<GenericValue> opps = orderPaymentPreferenceDao.findListByWhere(delegator, x.OrderPaymentPreference,
                        EntityCondition.makeCondition(x.orderId, EntityOperator.IN, orderIdSet), null, null, false);
                List<String> paymentMethodTypeIds = EntityUtil.getFieldListFromEntityList(opps, x.paymentMethodTypeId, true);

                if (paymentMethodTypeIds.size() > 1 || !paymentMethodTypeIds.contains(x.EXT_COD)) {
                    allowCOD = false;
                }
            }

            String codSurchargeAmount = null;
            String codSurchargeCurrencyUomId = null;
            String codFundsCode = null;

            boolean codSurchargeApplyToFirstPackage = false;
            boolean codSurchargeApplyToAllPackages = false;
            boolean codSurchargeSplitBetweenPackages = false;
            boolean codSurchargeApplyToNoPackages = false;

            BigDecimal codSurchargePackageAmount = null;

            if (allowCOD) {
                codSurchargeAmount = getShipmentGatewayConfigValue(delegator, shipmentGatewayConfigId, x.codSurchargeAmount, resource, x.shipment
                        + x.ups_cod_surcharge_amount, x.emptyString);
                if (UtilValidate.isEmpty(codSurchargeAmount)) {
                    return ServiceUtil.returnError(UtilProperties.getMessage(RES_ERROR, x.FacilityShipmentUpsSurchargeAmountIsNotConfigurated,
                            locale));
                }
                codSurchargeCurrencyUomId = getShipmentGatewayConfigValue(delegator, shipmentGatewayConfigId, x.codSurchargeCurrencyUomId,
                        resource, x.shipment_ups_cod_surcharge_currencyUomId, x.emptyString);
                if (UtilValidate.isEmpty(codSurchargeCurrencyUomId)) {
                    return ServiceUtil.returnError(UtilProperties.getMessage(RES_ERROR, x.FacilityShipmentUpsSurchargeCurrencyIsNotConfigurated,
                            locale));
                }
                String codSurchargeApplyToPackages = getShipmentGatewayConfigValue(delegator, shipmentGatewayConfigId,
                        x.codSurchargeApplyToPackage, resource, x.shipment_ups_cod_surcharge_applyToPackages, x.emptyString);
                if (UtilValidate.isEmpty(codSurchargeApplyToPackages)) {
                    return ServiceUtil.returnError(UtilProperties.getMessage(RES_ERROR, x.FacilityShipmentUpsApplyToPackagesIsNotConfigured, locale));
                }
                codFundsCode = getShipmentGatewayConfigValue(delegator, shipmentGatewayConfigId, x.codFundsCode, resource, x.shipment_ups_cod
                        + x.codFundsCode_26907ab0, x.emptyString);
                if (UtilValidate.isEmpty(codFundsCode)) {
                    return ServiceUtil.returnError(UtilProperties.getMessage(RES_ERROR, x.FacilityShipmentUpsCodFundsCodeIsNotConfigured, locale));
                }

                codSurchargeApplyToFirstPackage = x.first.equalsIgnoreCase(codSurchargeApplyToPackages);
                codSurchargeApplyToAllPackages = x.all.equalsIgnoreCase(codSurchargeApplyToPackages);
                codSurchargeSplitBetweenPackages = x.split.equalsIgnoreCase(codSurchargeApplyToPackages);
                codSurchargeApplyToNoPackages = x.none.equalsIgnoreCase(codSurchargeApplyToPackages);

                if (codSurchargeApplyToNoPackages) {
                    codSurchargeAmount = x._0;
                }
                codSurchargePackageAmount = new BigDecimal(codSurchargeAmount).setScale(DECIMALS, ROUNDING);
                if (codSurchargeSplitBetweenPackages) {
                    codSurchargePackageAmount = codSurchargePackageAmount.divide(new BigDecimal(shipmentPackageRouteSegs.size()), DECIMALS, ROUNDING);
                }

                if (UtilValidate.isEmpty(destTelecomNumber)) {
                    Debug.logInfo(x.Voice_notification_service_will_not_be_requested_for_COD_shipmentId + shipmentId + x.shipmentRouteSegmentId_91aebf79
                            + shipmentRouteSegmentId + x.missing_destination_phone_number, MODULE);
                }
                if (UtilValidate.isEmpty(shipmentRouteSegment.get(x.homeDeliveryType))) {
                    Debug.logInfo(x.Voice_notification_service_will_not_be_requested_for_COD_shipmentId + shipmentId + x.shipmentRouteSegmentId_91aebf79
                            + shipmentRouteSegmentId + x.destination_address_is_not_residential, MODULE);
                }
            }

            // Determine the currency by trying the shipmentRouteSegment, then the Shipment, then the framework's default currency, and finally
            // default to USD
            String currencyCode = null;
            if (UtilValidate.isNotEmpty(shipmentRouteSegment.getString(x.currencyUomId))) {
                currencyCode = shipmentRouteSegment.getString(x.currencyUomId);
            } else if (UtilValidate.isNotEmpty(shipment.getString(x.currencyUomId))) {
                currencyCode = shipment.getString(x.currencyUomId);
            } else {
                currencyCode = EntityUtilProperties.getPropertyValue(x.general, x.currency_uom_id_default, x.USD, delegator);
            }

            // Okay, start putting the XML together...
            Document shipmentConfirmRequestDoc = UtilXml.makeEmptyXmlDocument(x.ShipmentConfirmRequest);
            Element shipmentConfirmRequestElement = shipmentConfirmRequestDoc.getDocumentElement();
            shipmentConfirmRequestElement.setAttribute(x.xml_lang, x.en_US);

            // Top Level Element: Request
            Element requestElement = UtilXml.addChildElement(shipmentConfirmRequestElement, x.Request, shipmentConfirmRequestDoc);

            Element transactionReferenceElement = UtilXml.addChildElement(requestElement, x.TransactionReference, shipmentConfirmRequestDoc);
            UtilXml.addChildElementValue(transactionReferenceElement, x.CustomerContext, x.Ship_Confirm_nonvalidate, shipmentConfirmRequestDoc);
            UtilXml.addChildElementValue(transactionReferenceElement, x.XpciVersion, x._1_0001, shipmentConfirmRequestDoc);

            UtilXml.addChildElementValue(requestElement, x.RequestAction, x.ShipConfirm, shipmentConfirmRequestDoc);
            UtilXml.addChildElementValue(requestElement, x.RequestOption, x.nonvalidate, shipmentConfirmRequestDoc);

            // Top Level Element: LabelSpecification
            Element labelSpecificationElement = UtilXml.addChildElement(shipmentConfirmRequestElement, x.LabelSpecification,
                    shipmentConfirmRequestDoc);

            Element labelPrintMethodElement = UtilXml.addChildElement(labelSpecificationElement, x.LabelPrintMethod, shipmentConfirmRequestDoc);
            UtilXml.addChildElementValue(labelPrintMethodElement, x.Code, x.GIF, shipmentConfirmRequestDoc);

            UtilXml.addChildElementValue(labelSpecificationElement, x.HTTPUserAgent, x.Mozilla_5_0, shipmentConfirmRequestDoc);

            Element labelImageFormatElement = UtilXml.addChildElement(labelSpecificationElement, x.LabelImageFormat, shipmentConfirmRequestDoc);
            UtilXml.addChildElementValue(labelImageFormatElement, x.Code, x.GIF, shipmentConfirmRequestDoc);

            // Top Level Element: Shipment
            Element shipmentElement = UtilXml.addChildElement(shipmentConfirmRequestElement, x.Shipment, shipmentConfirmRequestDoc);
            UtilXml.addChildElementValue(shipmentElement, x.Description,
                    x.Goods_for_Shipment + shipment.get(x.shipmentId) + x._from_0b70336f + ordersDescription, shipmentConfirmRequestDoc);

            // Child of Shipment: Shipper
            String shipperNumber = getShipmentGatewayConfigValue(delegator, shipmentGatewayConfigId, x.shipperNumber, resource, x.shipment_ups
                    + x.shipper_number, x.emptyString);
            Element shipperElement = UtilXml.addChildElement(shipmentElement, x.Shipper, shipmentConfirmRequestDoc);
            UtilXml.addChildElementValue(shipperElement, x.Name, UtilValidate.isNotEmpty(originPostalAddress.getString(x.toName))
                    ? originPostalAddress.getString(x.toName) : x.emptyString, shipmentConfirmRequestDoc);
            UtilXml.addChildElementValue(shipperElement, x.AttentionName, UtilValidate.isNotEmpty(originPostalAddress.getString(x.attnName))
                    ? originPostalAddress.getString(x.attnName) : x.emptyString, shipmentConfirmRequestDoc);
            UtilXml.addChildElementValue(shipperElement, x.PhoneNumber, originPhoneNumber, shipmentConfirmRequestDoc);
            UtilXml.addChildElementValue(shipperElement, x.ShipperNumber, shipperNumber, shipmentConfirmRequestDoc);

            Element shipperAddressElement = UtilXml.addChildElement(shipperElement, x.Address, shipmentConfirmRequestDoc);
            UtilXml.addChildElementValue(shipperAddressElement, x.AddressLine1, originPostalAddress.getString(x.address1), shipmentConfirmRequestDoc);
            if (UtilValidate.isNotEmpty(originPostalAddress.getString(x.address2))) {
                UtilXml.addChildElementValue(shipperAddressElement, x.AddressLine2, originPostalAddress.getString(x.address2),
                        shipmentConfirmRequestDoc);
            }
            UtilXml.addChildElementValue(shipperAddressElement, x.City, originPostalAddress.getString(x.city), shipmentConfirmRequestDoc);
            UtilXml.addChildElementValue(shipperAddressElement, x.StateProvinceCode, originPostalAddress.getString(x.stateProvinceGeoId),
                    shipmentConfirmRequestDoc);
            UtilXml.addChildElementValue(shipperAddressElement, x.PostalCode, originPostalAddress.getString(x.postalCode), shipmentConfirmRequestDoc);
            UtilXml.addChildElementValue(shipperAddressElement, x.CountryCode, originCountryGeo.getString(x.geoCode), shipmentConfirmRequestDoc);
            // How to determine this? Add to data model...? UtilXml.addChildElement(shipperAddressElement, "ResidentialAddress",
            // shipmentConfirmRequestDoc);

            // Child of Shipment: ShipTo
            Element shipToElement = UtilXml.addChildElement(shipmentElement, x.ShipTo, shipmentConfirmRequestDoc);
            UtilXml.addChildElementValue(shipToElement, x.CompanyName, UtilValidate.isNotEmpty(destPostalAddress.getString(x.toName))
                    ? destPostalAddress.getString(x.toName) : x.emptyString, shipmentConfirmRequestDoc);
            UtilXml.addChildElementValue(shipToElement, x.AttentionName, UtilValidate.isNotEmpty(destPostalAddress.getString(x.attnName))
                    ? destPostalAddress.getString(x.attnName) : x.emptyString, shipmentConfirmRequestDoc);
            if (UtilValidate.isNotEmpty(destPhoneNumber)) {
                UtilXml.addChildElementValue(shipToElement, x.PhoneNumber, destPhoneNumber, shipmentConfirmRequestDoc);
            }
            Element shipToAddressElement = UtilXml.addChildElement(shipToElement, x.Address, shipmentConfirmRequestDoc);
            UtilXml.addChildElementValue(shipToAddressElement, x.AddressLine1, destPostalAddress.getString(x.address1), shipmentConfirmRequestDoc);
            if (UtilValidate.isNotEmpty(destPostalAddress.getString(x.address2))) {
                UtilXml.addChildElementValue(shipToAddressElement, x.AddressLine2, destPostalAddress.getString(x.address2),
                        shipmentConfirmRequestDoc);
            }
            UtilXml.addChildElementValue(shipToAddressElement, x.City, destPostalAddress.getString(x.city), shipmentConfirmRequestDoc);
            UtilXml.addChildElementValue(shipToAddressElement, x.StateProvinceCode, destPostalAddress.getString(x.stateProvinceGeoId),
                    shipmentConfirmRequestDoc);
            UtilXml.addChildElementValue(shipToAddressElement, x.PostalCode, destPostalAddress.getString(x.postalCode), shipmentConfirmRequestDoc);
            UtilXml.addChildElementValue(shipToAddressElement, x.CountryCode, destCountryGeo.getString(x.geoCode), shipmentConfirmRequestDoc);
            if (UtilValidate.isNotEmpty(shipmentRouteSegment.getString(x.homeDeliveryType))) {
                UtilXml.addChildElement(shipToAddressElement, x.ResidentialAddress, shipmentConfirmRequestDoc);
            }

            // Child of Shipment: ShipFrom
            Element shipFromElement = UtilXml.addChildElement(shipmentElement, x.ShipFrom, shipmentConfirmRequestDoc);
            UtilXml.addChildElementValue(shipFromElement, x.CompanyName, originPostalAddress.getString(x.toName), shipmentConfirmRequestDoc);
            UtilXml.addChildElementValue(shipFromElement, x.AttentionName, originPostalAddress.getString(x.attnName), shipmentConfirmRequestDoc);
            UtilXml.addChildElementValue(shipFromElement, x.PhoneNumber, originPhoneNumber, shipmentConfirmRequestDoc);
            Element shipFromAddressElement = UtilXml.addChildElement(shipFromElement, x.Address, shipmentConfirmRequestDoc);
            UtilXml.addChildElementValue(shipFromAddressElement, x.AddressLine1, originPostalAddress.getString(x.address1),
                    shipmentConfirmRequestDoc);
            if (UtilValidate.isNotEmpty(originPostalAddress.getString(x.address2))) {
                UtilXml.addChildElementValue(shipFromAddressElement, x.AddressLine2, originPostalAddress.getString(x.address2),
                        shipmentConfirmRequestDoc);
            }
            UtilXml.addChildElementValue(shipFromAddressElement, x.City, originPostalAddress.getString(x.city), shipmentConfirmRequestDoc);
            UtilXml.addChildElementValue(shipFromAddressElement, x.StateProvinceCode, originPostalAddress.getString(x.stateProvinceGeoId),
                    shipmentConfirmRequestDoc);
            UtilXml.addChildElementValue(shipFromAddressElement, x.PostalCode, originPostalAddress.getString(x.postalCode),
                    shipmentConfirmRequestDoc);
            UtilXml.addChildElementValue(shipFromAddressElement, x.CountryCode, originCountryGeo.getString(x.geoCode), shipmentConfirmRequestDoc);

            // Child of Shipment: SoldTo
            Element soldToElement = UtilXml.addChildElement(shipmentElement, x.SoldTo, shipmentConfirmRequestDoc);
            UtilXml.addChildElementValue(soldToElement, x.CompanyName, UtilValidate.isNotEmpty(destPostalAddress.getString(x.toName))
                    ? destPostalAddress.getString(x.toName) : x.emptyString, shipmentConfirmRequestDoc);
            UtilXml.addChildElementValue(soldToElement, x.AttentionName, UtilValidate.isNotEmpty(destPostalAddress.getString(x.attnName))
                    ? destPostalAddress.getString(x.attnName) : x.emptyString, shipmentConfirmRequestDoc);
            if (UtilValidate.isNotEmpty(destPhoneNumber)) {
                UtilXml.addChildElementValue(soldToElement, x.PhoneNumber, destPhoneNumber, shipmentConfirmRequestDoc);
            }
            Element soldToAddressElement = UtilXml.addChildElement(soldToElement, x.Address, shipmentConfirmRequestDoc);
            UtilXml.addChildElementValue(soldToAddressElement, x.AddressLine1, destPostalAddress.getString(x.address1), shipmentConfirmRequestDoc);
            if (UtilValidate.isNotEmpty(destPostalAddress.getString(x.address2))) {
                UtilXml.addChildElementValue(soldToAddressElement, x.AddressLine2, destPostalAddress.getString(x.address2),
                        shipmentConfirmRequestDoc);
            }
            UtilXml.addChildElementValue(soldToAddressElement, x.City, destPostalAddress.getString(x.city), shipmentConfirmRequestDoc);
            UtilXml.addChildElementValue(soldToAddressElement, x.StateProvinceCode, destPostalAddress.getString(x.stateProvinceGeoId),
                    shipmentConfirmRequestDoc);
            UtilXml.addChildElementValue(soldToAddressElement, x.PostalCode, destPostalAddress.getString(x.postalCode), shipmentConfirmRequestDoc);
            UtilXml.addChildElementValue(soldToAddressElement, x.CountryCode, destCountryGeo.getString(x.geoCode), shipmentConfirmRequestDoc);

            // Child of Shipment: PaymentInformation
            Element paymentInformationElement = UtilXml.addChildElement(shipmentElement, x.PaymentInformation, shipmentConfirmRequestDoc);

            String thirdPartyAccountNumber = shipmentRouteSegment.getString(x.thirdPartyAccountNumber);

            if (UtilValidate.isEmpty(thirdPartyAccountNumber)) {

                // Paid by shipper
                Element prepaidElement = UtilXml.addChildElement(paymentInformationElement, x.Prepaid, shipmentConfirmRequestDoc);
                Element billShipperElement = UtilXml.addChildElement(prepaidElement, x.BillShipper, shipmentConfirmRequestDoc);

                // fill in BillShipper AccountNumber element
                String billShipperAccountNumber = getShipmentGatewayConfigValue(delegator, shipmentGatewayConfigId, x.billShipperAccountNumber,
                        resource, x.shipment_ups_bill_shipper_account_number, x.emptyString);
                UtilXml.addChildElementValue(billShipperElement, x.AccountNumber, billShipperAccountNumber, shipmentConfirmRequestDoc);
            } else {

                // Paid by another shipper (may be receiver or not)

                // UPS requires the postal code and country code of the third party
                String thirdPartyPostalCode = shipmentRouteSegment.getString(x.thirdPartyPostalCode);
                if (UtilValidate.isEmpty(thirdPartyPostalCode)) {
                    return ServiceUtil.returnError(UtilProperties.getMessage(RES_ERROR, x.FacilityShipmentUpsRouteSegmentThirdPartyPostalCodeNotFound,
                            UtilMisc.toMap(x.shipmentId, shipmentId, x.shipmentRouteSegmentId, shipmentRouteSegmentId), locale));
                }
                String thirdPartyCountryGeoCode = shipmentRouteSegment.getString(x.thirdPartyCountryGeoCode);
                if (UtilValidate.isEmpty(thirdPartyCountryGeoCode)) {
                    return ServiceUtil.returnError(UtilProperties.getMessage(RES_ERROR, x.FacilityShipmentUpsRouteSegmentThirdPartyCountryNotFound,
                            UtilMisc.toMap(x.shipmentId, shipmentId, x.shipmentRouteSegmentId, shipmentRouteSegmentId), locale));
                }

                Element billThirdPartyElement = UtilXml.addChildElement(paymentInformationElement, x.BillThirdParty, shipmentConfirmRequestDoc);
                Element billThirdPartyShipperElement = UtilXml.addChildElement(billThirdPartyElement, x.BillThirdPartyShipper,
                        shipmentConfirmRequestDoc);
                UtilXml.addChildElementValue(billThirdPartyShipperElement, x.AccountNumber, thirdPartyAccountNumber, shipmentConfirmRequestDoc);
                Element thirdPartyElement = UtilXml.addChildElement(billThirdPartyShipperElement, x.ThirdParty, shipmentConfirmRequestDoc);
                Element addressElement = UtilXml.addChildElement(thirdPartyElement, x.Address, shipmentConfirmRequestDoc);
                UtilXml.addChildElementValue(addressElement, x.PostalCode, thirdPartyPostalCode, shipmentConfirmRequestDoc);
                UtilXml.addChildElementValue(addressElement, x.CountryCode, thirdPartyCountryGeoCode, shipmentConfirmRequestDoc);
            }

            // Child of Shipment: Service
            Element serviceElement = UtilXml.addChildElement(shipmentElement, x.Service, shipmentConfirmRequestDoc);
            String carrierServiceCode = carrierShipmentMethod.getString(x.carrierServiceCode);
            UtilXml.addChildElementValue(serviceElement, x.Code, carrierServiceCode, shipmentConfirmRequestDoc);

            // Child of Shipment: ShipmentServiceOptions
            List<String> internationalServiceCodes = UtilMisc.toList(x._07, x._08, x._54, x._65);
            if (internationalServiceCodes.contains(carrierServiceCode)) {
                Element shipmentServiceOptionsElement = UtilXml.addChildElement(shipmentElement, x.ShipmentServiceOptions, shipmentConfirmRequestDoc);
                Element internationalFormsElement = UtilXml.addChildElement(shipmentServiceOptionsElement, x.InternationalForms,
                        shipmentConfirmRequestDoc);
                UtilXml.addChildElementValue(internationalFormsElement, x.FormType, x._01, shipmentConfirmRequestDoc);
                List<GenericValue> shipmentItems = shipment.getRelated(x.ShipmentItem, null, null, false);
                for (GenericValue shipmentItem : shipmentItems) {
                    Element productElement = UtilXml.addChildElement(internationalFormsElement, x.Product, shipmentConfirmRequestDoc);
                    UtilXml.addChildElementValue(productElement, x.Description, x.Product_Description, shipmentConfirmRequestDoc);
                    Element unitElement = UtilXml.addChildElement(productElement, x.Unit, shipmentConfirmRequestDoc);
                    BigDecimal productQuantity = shipmentItem.getBigDecimal(x.quantity).setScale(DECIMALS, ROUNDING);
                    UtilXml.addChildElementValue(unitElement, x.Number, String.valueOf(productQuantity.intValue()), shipmentConfirmRequestDoc);
                    List<GenericValue> shipmentItemIssuances = shipmentItem.getRelated(x.ItemIssuance, null, null, false);
                    GenericValue orderItem = EntityUtil.getFirst(shipmentItemIssuances).getRelatedOne(x.OrderItem, false);
                    UtilXml.addChildElementValue(unitElement, x.Value, orderItem.getBigDecimal(x.unitPrice).toString(), shipmentConfirmRequestDoc);
                    Element unitOfMeasurElement = UtilXml.addChildElement(unitElement, x.UnitOfMeasurement, shipmentConfirmRequestDoc);
                    UtilXml.addChildElementValue(unitOfMeasurElement, x.Code, x.EA, shipmentConfirmRequestDoc);
                    UtilXml.addChildElementValue(productElement, x.OriginCountryCode, x.US, shipmentConfirmRequestDoc);
                }
                SimpleDateFormat formatter = new SimpleDateFormat(DATE_FORMAT);
                String invoiceDate = formatter.format(shipment.getTimestamp(x.createdDate));
                UtilXml.addChildElementValue(internationalFormsElement, x.InvoiceDate, invoiceDate, shipmentConfirmRequestDoc);
                UtilXml.addChildElementValue(internationalFormsElement, x.ReasonForExport, x.SALE, shipmentConfirmRequestDoc);
                UtilXml.addChildElementValue(internationalFormsElement, x.CurrencyCode, currencyCode, shipmentConfirmRequestDoc);
            }

            // Child of Shipment: Package
            ListIterator<GenericValue> shipmentPackageRouteSegIter = shipmentPackageRouteSegs.listIterator();
            while (shipmentPackageRouteSegIter.hasNext()) {
                GenericValue shipmentPackageRouteSeg = shipmentPackageRouteSegIter.next();
                GenericValue shipmentPackage = shipmentPackageRouteSeg.getRelatedOne(x.ShipmentPackage, false);
                GenericValue shipmentBoxType = shipmentPackage.getRelatedOne(x.ShipmentBoxType, false);
                List<GenericValue> carrierShipmentBoxTypes = shipmentPackage.getRelated(x.CarrierShipmentBoxType, UtilMisc.toMap(x.partyId, x.UPS),
                        null, false);
                GenericValue carrierShipmentBoxType = null;
                if (!carrierShipmentBoxTypes.isEmpty()) {
                    carrierShipmentBoxType = carrierShipmentBoxTypes.get(0);
                }

                Element packageElement = UtilXml.addChildElement(shipmentElement, x.Package_7431e3df, shipmentConfirmRequestDoc);
                Element packagingTypeElement = UtilXml.addChildElement(packageElement, x.PackagingType, shipmentConfirmRequestDoc);
                if (carrierShipmentBoxType != null && carrierShipmentBoxType.get(x.packagingTypeCode) != null) {
                    UtilXml.addChildElementValue(packagingTypeElement, x.Code, carrierShipmentBoxType.getString(x.packagingTypeCode),
                            shipmentConfirmRequestDoc);
                } else {
                    // default to "02", plain old Package
                    UtilXml.addChildElementValue(packagingTypeElement, x.Code, x._02, shipmentConfirmRequestDoc);
                }
                if (shipmentBoxType != null) {
                    Element dimensionsElement = UtilXml.addChildElement(packageElement, x.Dimensions, shipmentConfirmRequestDoc);
                    Element unitOfMeasurementElement = UtilXml.addChildElement(dimensionsElement, x.UnitOfMeasurement, shipmentConfirmRequestDoc);
                    GenericValue dimensionUom = shipmentBoxType.getRelatedOne(x.DimensionUom, false);
                    if (dimensionUom != null) {
                        UtilXml.addChildElementValue(unitOfMeasurementElement, x.Code,
                                dimensionUom.getString(x.abbreviation).toUpperCase(Locale.getDefault()), shipmentConfirmRequestDoc);
                    } else {
                        // I guess we'll default to inches...
                        UtilXml.addChildElementValue(unitOfMeasurementElement, x.Code, ModelService.IN_PARAM, shipmentConfirmRequestDoc);
                    }
                    BigDecimal boxLength = shipmentBoxType.getBigDecimal(x.boxLength);
                    BigDecimal boxWidth = shipmentBoxType.getBigDecimal(x.boxWidth);
                    BigDecimal boxHeight = shipmentBoxType.getBigDecimal(x.boxHeight);
                    UtilXml.addChildElementValue(dimensionsElement, x.Length, UtilValidate.isNotEmpty(boxLength) ? x.emptyString + boxLength.intValue() : x.emptyString,
                            shipmentConfirmRequestDoc);
                    UtilXml.addChildElementValue(dimensionsElement, x.Width, UtilValidate.isNotEmpty(boxWidth) ? x.emptyString + boxWidth.intValue() : x.emptyString,
                            shipmentConfirmRequestDoc);
                    UtilXml.addChildElementValue(dimensionsElement, x.Height, UtilValidate.isNotEmpty(boxHeight) ? x.emptyString + boxHeight.intValue() : x.emptyString,
                            shipmentConfirmRequestDoc);
                } else if (UtilValidate.isNotEmpty(shipmentPackage.getBigDecimal(x.boxLength))
                        && UtilValidate.isNotEmpty(shipmentPackage.getBigDecimal(x.boxWidth))
                        && UtilValidate.isNotEmpty(shipmentPackage.getBigDecimal(x.boxHeight))) {
                    Element dimensionsElement = UtilXml.addChildElement(packageElement, x.Dimensions, shipmentConfirmRequestDoc);
                    Element unitOfMeasurementElement = UtilXml.addChildElement(dimensionsElement, x.UnitOfMeasurement, shipmentConfirmRequestDoc);
                    GenericValue dimensionUom = shipmentPackage.getRelatedOne(x.DimensionUom, false);
                    if (dimensionUom != null) {
                        UtilXml.addChildElementValue(unitOfMeasurementElement, x.Code,
                                dimensionUom.getString(x.abbreviation).toUpperCase(Locale.getDefault()), shipmentConfirmRequestDoc);
                    } else {
                        UtilXml.addChildElementValue(unitOfMeasurementElement, x.Code, ModelService.IN_PARAM, shipmentConfirmRequestDoc);
                    }
                    UtilXml.addChildElementValue(dimensionsElement, x.Length, x.emptyString + shipmentPackage.getBigDecimal(x.boxLength).intValue(),
                            shipmentConfirmRequestDoc);
                    UtilXml.addChildElementValue(dimensionsElement, x.Width, x.emptyString + shipmentPackage.getBigDecimal(x.boxWidth).intValue(),
                            shipmentConfirmRequestDoc);
                    UtilXml.addChildElementValue(dimensionsElement, x.Height, x.emptyString + shipmentPackage.getBigDecimal(x.boxHeight).intValue(),
                            shipmentConfirmRequestDoc);
                }

                Element packageWeightElement = UtilXml.addChildElement(packageElement, x.PackageWeight, shipmentConfirmRequestDoc);
                Element packageWeightUnitOfMeasurementElement = UtilXml.addChildElement(packageElement, x.UnitOfMeasurement,
                        shipmentConfirmRequestDoc);
                String weightUomUps = null;
                if (shipmentPackage.get(x.weightUomId) != null) {
                    weightUomUps = OFBIZ_TO_UPS.get(shipmentPackage.get(x.weightUomId));
                }
                if (weightUomUps != null) {
                    UtilXml.addChildElementValue(packageWeightUnitOfMeasurementElement, x.Code, weightUomUps, shipmentConfirmRequestDoc);
                } else {
                    // might as well default to LBS
                    UtilXml.addChildElementValue(packageWeightUnitOfMeasurementElement, x.Code, x.LBS, shipmentConfirmRequestDoc);
                }

                if (shipmentPackage.getString(x.weight) == null) {
                    return ServiceUtil.returnError(UtilProperties.getMessage(RES_ERROR, x.FacilityShipmentUpsWeightValueNotFound,
                            UtilMisc.toMap(x.shipmentId, shipmentId, x.shipmentRouteSegmentId, shipmentRouteSegmentId, x.shipmentPackageSeqId,
                                    shipmentPackage.getString(x.shipmentPackageSeqId)), locale));
                }
                BigDecimal boxWeight = shipmentPackage.getBigDecimal(x.weight);
                UtilXml.addChildElementValue(packageWeightElement, x.Weight_69c0b815, UtilValidate.isNotEmpty(boxWeight) ? x.emptyString + boxWeight.setScale(0,
                        RoundingMode.CEILING) : x.emptyString, shipmentConfirmRequestDoc);
                // Adding only when order is not an international order
                if (!internationalServiceCodes.contains(carrierServiceCode)) {
                    Element referenceNumberElement = UtilXml.addChildElement(packageElement, x.ReferenceNumber, shipmentConfirmRequestDoc);
                    UtilXml.addChildElementValue(referenceNumberElement, x.Code, x.MK, shipmentConfirmRequestDoc);
                    UtilXml.addChildElementValue(referenceNumberElement, x.Value, shipmentPackage.getString(x.shipmentPackageSeqId),
                            shipmentConfirmRequestDoc);
                }
                if (carrierShipmentBoxType != null && carrierShipmentBoxType.get(x.oversizeCode) != null) {
                    UtilXml.addChildElementValue(packageElement, x.OversizePackage, carrierShipmentBoxType.getString(x.oversizeCode),
                            shipmentConfirmRequestDoc);
                }

                Element packageServiceOptionsElement = UtilXml.addChildElement(packageElement, x.PackageServiceOptions, shipmentConfirmRequestDoc);

                // Package insured value
                BigDecimal insuredValue = shipmentPackage.getBigDecimal(x.insuredValue);
                if (!UtilValidate.isEmpty(insuredValue)) {

                    Element insuredValueElement = UtilXml.addChildElement(packageServiceOptionsElement, x.InsuredValue, shipmentConfirmRequestDoc);
                    UtilXml.addChildElementValue(insuredValueElement, x.MonetaryValue, insuredValue.setScale(2, RoundingMode.HALF_UP).toString(),
                            shipmentConfirmRequestDoc);
                    UtilXml.addChildElementValue(insuredValueElement, x.CurrencyCode, currencyCode, shipmentConfirmRequestDoc);
                }

                if (allowCOD) {
                    Element codElement = UtilXml.addChildElement(packageServiceOptionsElement, x.COD, shipmentConfirmRequestDoc);
                    UtilXml.addChildElementValue(codElement, x.CODCode, x._3, shipmentConfirmRequestDoc); // "3" is the only valid value for
                    // package-level COD
                    UtilXml.addChildElementValue(codElement, x.CODFundsCode, codFundsCode, shipmentConfirmRequestDoc);
                    Element codAmountElement = UtilXml.addChildElement(codElement, x.CODAmount, shipmentConfirmRequestDoc);
                    UtilXml.addChildElementValue(codAmountElement, x.CurrencyCode, currencyCode, shipmentConfirmRequestDoc);

                    // Get the value of the package by going back to the orderItems
                    Map<String, Object> getPackageValueResult = dispatcher.runSync(x.getShipmentPackageValueFromOrders, UtilMisc.toMap(x.shipmentId,
                            shipmentId, x.shipmentPackageSeqId, shipmentPackage.get(x.shipmentPackageSeqId), x.currencyUomId, currencyCode,
                            x.userLogin, userLogin, x.locale, locale));
                    if (ServiceUtil.isError(getPackageValueResult)) return getPackageValueResult;
                    BigDecimal packageValue = (BigDecimal) getPackageValueResult.get(x.packageValue);

                    // Convert the value of the COD surcharge to the shipment currency, if necessary
                    Map<String, Object> convertUomResult = dispatcher.runSync(x.convertUom, UtilMisc.<String, Object>toMap(x.uomId,
                            codSurchargeCurrencyUomId, x.uomIdTo, currencyCode, x.originalValue, codSurchargePackageAmount));
                    if (ServiceUtil.isError(convertUomResult)) return convertUomResult;
                    if (convertUomResult.containsKey(x.convertedValue)) {
                        codSurchargePackageAmount = ((BigDecimal) convertUomResult.get(x.convertedValue)).setScale(DECIMALS, ROUNDING);
                    }

                    // Add the amount of the surcharge for the package, if the surcharge should be on all packages or the first and this is the
                    // first package
                    if (codSurchargeApplyToAllPackages || codSurchargeSplitBetweenPackages || (codSurchargeApplyToFirstPackage
                            && shipmentPackageRouteSegIter.previousIndex() <= 0)) {
                        packageValue = packageValue.add(codSurchargePackageAmount);
                    }

                    UtilXml.addChildElementValue(codAmountElement, x.MonetaryValue, packageValue.setScale(DECIMALS, ROUNDING).toString(),
                            shipmentConfirmRequestDoc);
                }
            }

            String shipmentConfirmRequestString = null;
            try {
                shipmentConfirmRequestString = UtilXml.writeXmlDocument(shipmentConfirmRequestDoc);
            } catch (IOException e) {
                String ioeErrMsg = x.Error_writing_the_ShipmentConfirmRequest_XML_Document_to_a_String + e.toString();
                Debug.logError(e, ioeErrMsg, MODULE);
                return ServiceUtil.returnError(UtilProperties.getMessage(RES_ERROR, x.FacilityShipmentUpsErrorShipmentConfirmRequestXmlToString,
                        UtilMisc.toMap(x.errorString, e.toString()), locale));
            }

            // create AccessRequest XML doc
            Document accessRequestDocument = createAccessRequestDocument(delegator, shipmentGatewayConfigId, resource);
            String accessRequestString = null;
            try {
                accessRequestString = UtilXml.writeXmlDocument(accessRequestDocument);
            } catch (IOException e) {
                String ioeErrMsg = x.Error_writing_the_AccessRequest_XML_Document_to_a_String + e.toString();
                Debug.logError(e, ioeErrMsg, MODULE);
                return ServiceUtil.returnError(UtilProperties.getMessage(RES_ERROR, x.FacilityShipmentUpsErrorAccessRequestXmlToString,
                        UtilMisc.toMap(x.errorString, e.toString()), locale));
            }

            // connect to UPS server, send AccessRequest to auth
            // send ShipmentConfirmRequest String
            // get ShipmentConfirmResponse String back
            StringBuilder xmlString = new StringBuilder();
            xmlString.append(accessRequestString);
            xmlString.append(shipmentConfirmRequestString);

            if (shipmentUpsSaveCertificationInfo) {
                String outFileName =
                        shipmentUpsSaveCertificationPath + x.UpsShipmentConfirmRequest + shipmentId + x.str_53a0acfa + shipmentRouteSegment.getString(
                         x.shipmentRouteSegmentId) + x.xml_657e4752;
                try (FileOutputStream fileOut = new FileOutputStream(outFileName)) {
                    fileOut.write(xmlString.toString().getBytes(StandardCharsets.UTF_8));
                    fileOut.flush();
                } catch (IOException e) {
                    Debug.logInfo(e, x.Could_not_save_UPS_XML_file + xmlString.toString() + x.to_file + outFileName, MODULE);
                }
            }

            try {
                shipmentConfirmResponseString = sendUpsRequest(x.ShipConfirm, xmlString.toString(), shipmentGatewayConfigId, resource, delegator,
                        locale);
            } catch (UpsConnectException e) {
                String uceErrMsg = x.Error_sending_UPS_request_for_UPS_Service_ShipConfirm + e.toString();
                Debug.logError(e, uceErrMsg, MODULE);
                return ServiceUtil.returnError(UtilProperties.getMessage(RES_ERROR, x.FacilityShipmentUpsErrorSendingShipConfirm,
                        UtilMisc.toMap(x.errorString, e.toString()), locale));
            }

            if (shipmentUpsSaveCertificationInfo) {
                String outFileName =
                        shipmentUpsSaveCertificationPath + x.UpsShipmentConfirmResponse + shipmentId + x.str_53a0acfa + shipmentRouteSegment.getString(
                         x.shipmentRouteSegmentId) + x.xml_657e4752;
                try (FileOutputStream fileOut = new FileOutputStream(outFileName)) {
                    fileOut.write(shipmentConfirmResponseString.getBytes(StandardCharsets.UTF_8));
                    fileOut.flush();
                } catch (IOException e) {
                    Debug.logInfo(e, x.Could_not_save_UPS_XML_file + xmlString.toString() + x.to_file + outFileName, MODULE);
                }
            }

            Document shipmentConfirmResponseDocument = null;
            try {
                shipmentConfirmResponseDocument = UtilXml.readXmlDocument(shipmentConfirmResponseString, false);
            } catch (SAXException | IOException | ParserConfigurationException e2) {
                String excErrMsg = x.Error_parsing_the_ShipmentConfirmResponse + e2.toString();
                Debug.logError(e2, excErrMsg, MODULE);
                return ServiceUtil.returnError(UtilProperties.getMessage(RES_ERROR, x.FacilityShipmentUpsErrorParsingShipmentConfirm,
                        UtilMisc.toMap(x.errorString, e2.toString()), locale));
            }

            return handleUpsShipmentConfirmResponse(shipmentConfirmResponseDocument, shipmentRouteSegment, locale);
        } catch (GenericServiceException e) {
            Debug.logError(e, MODULE);
            return ServiceUtil.returnError(UtilProperties.getMessage(RES_ERROR, x.FacilityShipmentUpsErrorDataShipmentConfirm,
                    UtilMisc.toMap(x.errorString, e.toString()), locale));
        } catch (GenericEntityException e) {
            Debug.logError(e, MODULE);
            if (shipmentConfirmResponseString != null) {
                Debug.logError(x.Got_XML_ShipmentConfirmRespose + shipmentConfirmResponseString, MODULE);
                return ServiceUtil.returnError(UtilMisc.toList(
                        UtilProperties.getMessage(RES_ERROR, x.FacilityShipmentUpsErrorDataShipmentConfirm,
                                UtilMisc.toMap(x.errorString, e.toString()), locale),
                        UtilProperties.getMessage(RES_ERROR, x.FacilityShipmentUpsShipmentConfirmResposeWasReceived,
                                UtilMisc.toMap(x.shipmentConfirmResponseString, shipmentConfirmResponseString), locale)));
            } else {
                return ServiceUtil.returnError(UtilProperties.getMessage(RES_ERROR, x.FacilityShipmentUpsErrorDataShipmentConfirm,
                        UtilMisc.toMap(x.errorString, e.toString()), locale));
            }
        }
    }

    public static Map<String, Object> handleUpsShipmentConfirmResponse(Document shipmentConfirmResponseDocument, GenericValue shipmentRouteSegment,
                                                                       Locale locale) throws GenericEntityException {
        // process ShipmentConfirmResponse, update data as needed
        Element shipmentConfirmResponseElement = shipmentConfirmResponseDocument.getDocumentElement();

        // handle Response element info
        Element responseElement = UtilXml.firstChildElement(shipmentConfirmResponseElement, x.Response_6e617e4f);
        String responseStatusCode = UtilXml.childElementValue(responseElement, x.ResponseStatusCode);
        List<Object> errorList = new LinkedList<>();
        UpsServices.handleErrors(responseElement, errorList, locale);

        if (x._1.equals(responseStatusCode)) {
            // handle ShipmentCharges element info
            Element shipmentChargesElement = UtilXml.firstChildElement(shipmentConfirmResponseElement, x.ShipmentCharges);

            Element transportationChargesElement = UtilXml.firstChildElement(shipmentChargesElement, x.TransportationCharges);
            String transportationMonetaryValue = UtilXml.childElementValue(transportationChargesElement, x.MonetaryValue);

            Element serviceOptionsChargesElement = UtilXml.firstChildElement(shipmentChargesElement, x.ServiceOptionsCharges);
            String serviceOptionsMonetaryValue = UtilXml.childElementValue(serviceOptionsChargesElement, x.MonetaryValue);

            Element totalChargesElement = UtilXml.firstChildElement(shipmentChargesElement, x.TotalCharges);
            String totalCurrencyCode = UtilXml.childElementValue(totalChargesElement, x.CurrencyCode);
            String totalMonetaryValue = UtilXml.childElementValue(totalChargesElement, x.MonetaryValue);

            if (UtilValidate.isNotEmpty(totalCurrencyCode)) {
                if (UtilValidate.isEmpty(shipmentRouteSegment.getString(x.currencyUomId))) {
                    shipmentRouteSegment.set(x.currencyUomId, totalCurrencyCode);
                } else if (!totalCurrencyCode.equals(shipmentRouteSegment.getString(x.currencyUomId))) {
                    shipmentRouteSegment.set(x.currencyUomId, totalCurrencyCode);
                    errorList.add(UtilProperties.getMessage(RES_ERROR, x.FacilityShipmentUpsCurrencyDoesNotMatch,
                            UtilMisc.toMap(x.currency1, totalCurrencyCode, x.currency2, shipmentRouteSegment.getString(x.currencyUomId)), locale));
                }
            }

            try {
                shipmentRouteSegment.set(x.actualTransportCost, new BigDecimal(transportationMonetaryValue));
            } catch (NumberFormatException e) {
                String excErrMsg = x.Error_parsing_the_transportationMonetaryValue + transportationMonetaryValue + x.str_89222ecc + e.toString();
                Debug.logError(e, excErrMsg, MODULE);
                errorList.add(UtilProperties.getMessage(RES_ERROR, x.FacilityShipmentUpsErrorParsingTransportationMonetaryValue,
                        UtilMisc.toMap(x.transportationMonetaryValue, transportationMonetaryValue, x.errorString, e.toString()), locale));
            }
            try {
                shipmentRouteSegment.set(x.actualServiceCost, new BigDecimal(serviceOptionsMonetaryValue));
            } catch (NumberFormatException e) {
                String excErrMsg = x.Error_parsing_the_serviceOptionsMonetaryValue + serviceOptionsMonetaryValue + x.str_89222ecc + e.toString();
                Debug.logError(e, excErrMsg, MODULE);
                errorList.add(UtilProperties.getMessage(RES_ERROR, x.FacilityShipmentUpsErrorParsingServiceOptionsMonetaryValue,
                        UtilMisc.toMap(x.serviceOptionsMonetaryValue, serviceOptionsMonetaryValue, x.errorString, e.toString()), locale));
            }
            try {
                shipmentRouteSegment.set(x.actualCost, new BigDecimal(totalMonetaryValue));
            } catch (NumberFormatException e) {
                String excErrMsg = x.Error_parsing_the_totalMonetaryValue + totalMonetaryValue + x.str_89222ecc + e.toString();
                Debug.logError(e, excErrMsg, MODULE);
                errorList.add(UtilProperties.getMessage(RES_ERROR, x.FacilityShipmentUpsErrorParsingTotalMonetaryValue,
                        UtilMisc.toMap(x.totalMonetaryValue, totalMonetaryValue, x.errorString, e.toString()), locale));
            }

            // handle BillingWeight element info
            Element billingWeightElement = UtilXml.firstChildElement(shipmentConfirmResponseElement, x.BillingWeight);
            Element billingWeightUnitOfMeasurementElement = UtilXml.firstChildElement(billingWeightElement, x.UnitOfMeasurement);
            String billingWeightUnitOfMeasurement = UtilXml.childElementValue(billingWeightUnitOfMeasurementElement, x.Code);
            String billingWeight = UtilXml.childElementValue(billingWeightElement, x.Weight_69c0b815);
            try {
                shipmentRouteSegment.set(x.billingWeight, new BigDecimal(billingWeight));
            } catch (NumberFormatException e) {
                String excErrMsg = x.Error_parsing_the_billingWeight + billingWeight + x.str_89222ecc + e.toString();
                Debug.logError(e, excErrMsg, MODULE);
                errorList.add(UtilProperties.getMessage(RES_ERROR, x.FacilityShipmentUpsErrorParsingBillingWeight,
                        UtilMisc.toMap(x.billingWeight, billingWeight, x.errorString, e.toString()), locale));
            }
            shipmentRouteSegment.set(x.billingWeightUomId, UPS_TO_OFBIZ.get(billingWeightUnitOfMeasurement));

            // store the ShipmentIdentificationNumber and ShipmentDigest
            String shipmentIdentificationNumber = UtilXml.childElementValue(shipmentConfirmResponseElement, x.ShipmentIdentificationNumber);
            String shipmentDigest = UtilXml.childElementValue(shipmentConfirmResponseElement, x.ShipmentDigest);
            shipmentRouteSegment.set(x.trackingIdNumber, shipmentIdentificationNumber);
            shipmentRouteSegment.set(x.trackingDigest, shipmentDigest);

            // set ShipmentRouteSegment carrierServiceStatusId after each UPS service applicable
            shipmentRouteSegment.put(x.carrierServiceStatusId, x.SHRSCS_CONFIRMED);

            // write/store all modified value objects
            shipmentRouteSegment.store();

            // -=-=-=- Okay, now done with that, just return any extra info...
            StringBuilder successString = new StringBuilder(UtilProperties.getMessage(RES_ERROR,
                    x.FacilityShipmentUpsShipmentConfirmSucceeded, locale));

            if (!errorList.isEmpty()) {
                // this shouldn't happen much, but handle it anyway
                successString.append(UtilProperties.getMessage(RES_ERROR, x.FacilityShipmentUpsShipmentConfirmError, locale));
                Iterator<Object> errorListIter = errorList.iterator();
                while (errorListIter.hasNext()) {
                    successString.append(errorListIter.next());
                    if (errorListIter.hasNext()) {
                        successString.append(x.str_d3bc9a37);
                    }
                }
            }
            return ServiceUtil.returnSuccess(successString.toString());
        } else {
            errorList.add(0, UtilProperties.getMessage(RES_ERROR, x.FacilityShipmentUpsShipmentConfirmFailed, locale));
            return ServiceUtil.returnError(errorList);
        }
    }

    public static Map<String, Object> upsShipmentAccept(DispatchContext dctx, UpsServicesContext context) {
        Delegator delegator = dctx.getDelegator();
        String shipmentId = (String) context.get(x.shipmentId);
        String shipmentRouteSegmentId = (String) context.get(x.shipmentRouteSegmentId);
        Locale locale = (Locale) context.get(x.locale);

        Map<String, Object> shipmentGatewayConfig = ShipmentServices.getShipmentGatewayConfigFromShipment(delegator, shipmentId, locale);
        String shipmentGatewayConfigId = (String) shipmentGatewayConfig.get(x.shipmentGatewayConfigId);
        String resource = (String) shipmentGatewayConfig.get(x.configProps);
        if (UtilValidate.isEmpty(shipmentGatewayConfigId) && UtilValidate.isEmpty(resource)) {
            return ServiceUtil.returnError(UtilProperties.getMessage(RES_ERROR, x.FacilityShipmentUpsGatewayNotAvailable, locale));
        }
        boolean shipmentUpsSaveCertificationInfo = x._true.equals(getShipmentGatewayConfigValue(delegator, shipmentGatewayConfigId, x.saveCertInfo,
                resource, x.shipment_ups_save_certification_info, x._true));
        String shipmentUpsSaveCertificationPath = FlexibleStringExpander.expandString(getShipmentGatewayConfigValue(delegator,
                shipmentGatewayConfigId, x.saveCertPath, resource, x.shipment_ups_save_certification_path, x.emptyString), context);
        File shipmentUpsSaveCertificationFile = null;
        if (shipmentUpsSaveCertificationInfo) {
            shipmentUpsSaveCertificationFile = new File(shipmentUpsSaveCertificationPath);
            if (!shipmentUpsSaveCertificationFile.exists()) {
                shipmentUpsSaveCertificationFile.mkdirs();
            }
        }

        String shipmentAcceptResponseString = null;

        try {
            ShipmentRouteSegmentDao shipmentRouteSegmentDao = DaoRegistry.getDao(delegator, x.ShipmentRouteSegment,
                    ShipmentRouteSegmentDao.class);
            GenericValue shipmentRouteSegment = shipmentRouteSegmentDao.findOneByWhere(delegator, x.ShipmentRouteSegment,
                    UtilMisc.toMap(x.shipmentId, shipmentId, x.shipmentRouteSegmentId, shipmentRouteSegmentId), null, null, false);

            if (!x.UPS.equals(shipmentRouteSegment.getString(x.carrierPartyId))) {
                return ServiceUtil.returnError(UtilProperties.getMessage(RES_ERROR, x.FacilityShipmentUpsNotRouteSegmentCarrier, UtilMisc.toMap(
                        x.shipmentRouteSegmentId, shipmentRouteSegmentId, x.shipmentId, shipmentId), locale));
            }

            // add ShipmentRouteSegment carrierServiceStatusId, check before all UPS services
            if (!x.SHRSCS_CONFIRMED.equals(shipmentRouteSegment.getString(x.carrierServiceStatusId))) {
                return ServiceUtil.returnError(UtilProperties.getMessage(RES_ERROR, x.FacilityShipmentUpsRouteSegmentStatusNotConfirmed,
                        UtilMisc.toMap(x.shipmentRouteSegmentId, shipmentRouteSegmentId, x.shipmentId, shipmentId, x.shipmentRouteSegmentStatus,
                                shipmentRouteSegment.getString(x.carrierServiceStatusId)), locale));
            }

            List<GenericValue> shipmentPackageRouteSegs = shipmentRouteSegment.getRelated(x.ShipmentPackageRouteSeg, null, UtilMisc.toList(
                    x.shipmentPackageSeqId_39d5d38d), false);
            if (UtilValidate.isEmpty(shipmentPackageRouteSegs)) {
                return ServiceUtil.returnError(UtilProperties.getMessage(RES_ERROR, x.FacilityShipmentUpsPackageRouteSegsNotFound,
                        UtilMisc.toMap(x.shipmentId, shipmentId, x.shipmentRouteSegmentId, shipmentRouteSegmentId), locale));
            }

            if (UtilValidate.isEmpty(shipmentRouteSegment.getString(x.trackingDigest))) {
                return ServiceUtil.returnError(UtilProperties.getMessage(RES_ERROR,
                        x.FacilityShipmentUpsTrackingDigestWasNotSet, locale));
            }

            Document shipmentAcceptRequestDoc = UtilXml.makeEmptyXmlDocument(x.ShipmentAcceptRequest);
            Element shipmentAcceptRequestElement = shipmentAcceptRequestDoc.getDocumentElement();
            shipmentAcceptRequestElement.setAttribute(x.xml_lang, x.en_US);

            // Top Level Element: Request
            Element requestElement = UtilXml.addChildElement(shipmentAcceptRequestElement, x.Request, shipmentAcceptRequestDoc);

            Element transactionReferenceElement = UtilXml.addChildElement(requestElement, x.TransactionReference, shipmentAcceptRequestDoc);
            UtilXml.addChildElementValue(transactionReferenceElement, x.CustomerContext, x.ShipAccept_01, shipmentAcceptRequestDoc);
            UtilXml.addChildElementValue(transactionReferenceElement, x.XpciVersion, x._1_0001, shipmentAcceptRequestDoc);

            UtilXml.addChildElementValue(requestElement, x.RequestAction, x.ShipAccept, shipmentAcceptRequestDoc);
            UtilXml.addChildElementValue(requestElement, x.RequestOption, x._01, shipmentAcceptRequestDoc);

            UtilXml.addChildElementValue(shipmentAcceptRequestElement, x.ShipmentDigest, shipmentRouteSegment.getString(x.trackingDigest),
                    shipmentAcceptRequestDoc);


            String shipmentAcceptRequestString = null;
            try {
                shipmentAcceptRequestString = UtilXml.writeXmlDocument(shipmentAcceptRequestDoc);
            } catch (IOException e) {
                String ioeErrMsg = x.Error_writing_the_ShipmentAcceptRequest_XML_Document_to_a_String + e.toString();
                Debug.logError(e, ioeErrMsg, MODULE);
                return ServiceUtil.returnError(UtilProperties.getMessage(RES_ERROR,
                        x.FacilityShipmentUpsErrorShipmentAcceptRequestXmlToString,
                        UtilMisc.toMap(x.errorString, e.toString()), locale));
            }

            // create AccessRequest XML doc
            Document accessRequestDocument = createAccessRequestDocument(delegator, shipmentGatewayConfigId, resource);
            String accessRequestString = null;
            try {
                accessRequestString = UtilXml.writeXmlDocument(accessRequestDocument);
            } catch (IOException e) {
                String ioeErrMsg = x.Error_writing_the_AccessRequest_XML_Document_to_a_String + e.toString();
                Debug.logError(e, ioeErrMsg, MODULE);
                return ServiceUtil.returnError(UtilProperties.getMessage(RES_ERROR, x.FacilityShipmentUpsErrorAccessRequestXmlToString,
                        UtilMisc.toMap(x.errorString, e.toString()), locale));
            }

            // connect to UPS server, send AccessRequest to auth
            // send ShipmentConfirmRequest String
            // get ShipmentConfirmResponse String back
            StringBuilder xmlString = new StringBuilder();
            xmlString.append(accessRequestString);
            xmlString.append(shipmentAcceptRequestString);

            if (shipmentUpsSaveCertificationInfo) {
                String outFileName =
                        shipmentUpsSaveCertificationPath + x.UpsShipmentAcceptRequest + shipmentId + x.str_53a0acfa + shipmentRouteSegment.getString(
                         x.shipmentRouteSegmentId) + x.xml_657e4752;
                try (FileOutputStream fileOut = new FileOutputStream(outFileName)) {
                    fileOut.write(xmlString.toString().getBytes(StandardCharsets.UTF_8));
                    fileOut.flush();
                } catch (IOException e) {
                    Debug.logInfo(e, x.Could_not_save_UPS_XML_file + xmlString.toString() + x.to_file + outFileName, MODULE);
                }
            }

            try {
                shipmentAcceptResponseString = sendUpsRequest(x.ShipAccept, xmlString.toString(), shipmentGatewayConfigId, resource, delegator,
                        locale);
            } catch (UpsConnectException e) {
                String uceErrMsg = x.Error_sending_UPS_request_for_UPS_Service_ShipAccept + e.toString();
                Debug.logError(e, uceErrMsg, MODULE);
                return ServiceUtil.returnError(UtilProperties.getMessage(RES_ERROR, x.FacilityShipmentUpsErrorSendingShipAccept,
                        UtilMisc.toMap(x.errorString, e.toString()), locale));
            }

            if (shipmentUpsSaveCertificationInfo) {
                String outFileName =
                        shipmentUpsSaveCertificationPath + x.UpsShipmentAcceptResponse + shipmentId + x.str_53a0acfa + shipmentRouteSegment.getString(
                         x.shipmentRouteSegmentId) + x.xml_657e4752;
                try (FileOutputStream fileOut = new FileOutputStream(outFileName)) {
                    fileOut.write(shipmentAcceptResponseString.getBytes(StandardCharsets.UTF_8));
                    fileOut.flush();
                } catch (IOException e) {
                    Debug.logInfo(e, x.Could_not_save_UPS_XML_file + xmlString.toString() + x.to_file + outFileName, MODULE);
                }
            }

            Document shipmentAcceptResponseDocument = null;
            try {
                shipmentAcceptResponseDocument = UtilXml.readXmlDocument(shipmentAcceptResponseString, false);
            } catch (SAXException | IOException | ParserConfigurationException e2) {
                String excErrMsg = x.Error_parsing_the_ShipmentAcceptResponse + e2.toString();
                Debug.logError(e2, excErrMsg, MODULE);
                return ServiceUtil.returnError(UtilProperties.getMessage(RES_ERROR, x.FacilityShipmentUpsErrorParsingShipmentAcceptResponse,
                        UtilMisc.toMap(x.errorString, e2.toString()), locale));
            }

            return handleUpsShipmentAcceptResponse(shipmentAcceptResponseDocument, shipmentRouteSegment, shipmentPackageRouteSegs, delegator,
                    shipmentGatewayConfigId, resource, context, locale);
        } catch (GenericEntityException e) {
            Debug.logError(e, MODULE);
            return ServiceUtil.returnError(UtilProperties.getMessage(RES_ERROR, x.FacilityShipmentUpsErrorDataShipmentAccept,
                    UtilMisc.toMap(x.errorString, e.toString()), locale));
        }
    }

    public static Map<String, Object> handleUpsShipmentAcceptResponse(Document shipmentAcceptResponseDocument, GenericValue shipmentRouteSegment,
                                                                      List<GenericValue> shipmentPackageRouteSegs,
                                                                      Delegator delegator, String shipmentGatewayConfigId, String resource,
                                                                      UpsServicesContext context, Locale locale)
            throws GenericEntityException {
        boolean shipmentUpsSaveCertificationInfo = x._true.equals(getShipmentGatewayConfigValue(delegator, shipmentGatewayConfigId, x.saveCertInfo,
                resource, x.shipment_ups_save_certification_info, x._true));
        String shipmentUpsSaveCertificationPath = FlexibleStringExpander.expandString(getShipmentGatewayConfigValue(delegator,
                shipmentGatewayConfigId, x.saveCertPath, resource, x.shipment_ups_save_certification_path, x.emptyString), context);
        File shipmentUpsSaveCertificationFile = null;
        if (shipmentUpsSaveCertificationInfo) {
            shipmentUpsSaveCertificationFile = new File(shipmentUpsSaveCertificationPath);
            if (!shipmentUpsSaveCertificationFile.exists()) {
                shipmentUpsSaveCertificationFile.mkdirs();
            }
        }

        // process ShipmentAcceptResponse, update data as needed
        Element shipmentAcceptResponseElement = shipmentAcceptResponseDocument.getDocumentElement();

        // handle Response element info
        Element responseElement = UtilXml.firstChildElement(shipmentAcceptResponseElement, x.Response_6e617e4f);
        String responseStatusCode = UtilXml.childElementValue(responseElement, x.ResponseStatusCode);
        List<Object> errorList = new LinkedList<>();
        UpsServices.handleErrors(responseElement, errorList, locale);

        if (x._1.equals(responseStatusCode)) {
            Element shipmentResultsElement = UtilXml.firstChildElement(shipmentAcceptResponseElement, x.ShipmentResults);

            // This information is returned in both the ShipmentConfirmResponse and
            //the ShipmentAcceptResponse. So, we'll go ahead and store it here again
            //and warn of changes or something...


            // handle ShipmentCharges element info
            Element shipmentChargesElement = UtilXml.firstChildElement(shipmentResultsElement, x.ShipmentCharges);

            Element transportationChargesElement = UtilXml.firstChildElement(shipmentChargesElement, x.TransportationCharges);
            String transportationMonetaryValue = UtilXml.childElementValue(transportationChargesElement, x.MonetaryValue);

            Element serviceOptionsChargesElement = UtilXml.firstChildElement(shipmentChargesElement, x.ServiceOptionsCharges);
            String serviceOptionsMonetaryValue = UtilXml.childElementValue(serviceOptionsChargesElement, x.MonetaryValue);

            Element totalChargesElement = UtilXml.firstChildElement(shipmentChargesElement, x.TotalCharges);
            String totalCurrencyCode = UtilXml.childElementValue(totalChargesElement, x.CurrencyCode);
            String totalMonetaryValue = UtilXml.childElementValue(totalChargesElement, x.MonetaryValue);

            if (UtilValidate.isNotEmpty(totalCurrencyCode)) {
                if (UtilValidate.isEmpty(shipmentRouteSegment.getString(x.currencyUomId))) {
                    shipmentRouteSegment.set(x.currencyUomId, totalCurrencyCode);
                } else if (!totalCurrencyCode.equals(shipmentRouteSegment.getString(x.currencyUomId))) {
                    shipmentRouteSegment.set(x.currencyUomId, totalCurrencyCode);
                    errorList.add(UtilProperties.getMessage(RES_ERROR, x.FacilityShipmentUpsCurrencyDoesNotMatch,
                            UtilMisc.toMap(x.currency1, totalCurrencyCode, x.currency2, shipmentRouteSegment.getString(x.currencyUomId)), locale));
                }
            }

            try {
                shipmentRouteSegment.set(x.actualTransportCost, new BigDecimal(transportationMonetaryValue));
            } catch (NumberFormatException e) {
                String excErrMsg = x.Error_parsing_the_transportationMonetaryValue + transportationMonetaryValue + x.str_89222ecc + e.toString();
                Debug.logError(e, excErrMsg, MODULE);
                errorList.add(UtilProperties.getMessage(RES_ERROR, x.FacilityShipmentUpsErrorParsingTransportationMonetaryValue,
                        UtilMisc.toMap(x.transportationMonetaryValue, transportationMonetaryValue, x.errorString, e.toString()), locale));
            }
            try {
                shipmentRouteSegment.set(x.actualServiceCost, new BigDecimal(serviceOptionsMonetaryValue));
            } catch (NumberFormatException e) {
                String excErrMsg = x.Error_parsing_the_serviceOptionsMonetaryValue + serviceOptionsMonetaryValue + x.str_89222ecc + e.toString();
                Debug.logError(e, excErrMsg, MODULE);
                errorList.add(UtilProperties.getMessage(RES_ERROR, x.FacilityShipmentUpsErrorParsingServiceOptionsMonetaryValue,
                        UtilMisc.toMap(x.serviceOptionsMonetaryValue, serviceOptionsMonetaryValue, x.errorString, e.toString()), locale));
            }
            try {
                shipmentRouteSegment.set(x.actualCost, new BigDecimal(totalMonetaryValue));
            } catch (NumberFormatException e) {
                String excErrMsg = x.Error_parsing_the_totalMonetaryValue + totalMonetaryValue + x.str_89222ecc + e.toString();
                Debug.logError(e, excErrMsg, MODULE);
                errorList.add(UtilProperties.getMessage(RES_ERROR, x.FacilityShipmentUpsErrorParsingTotalMonetaryValue,
                        UtilMisc.toMap(x.totalMonetaryValue, totalMonetaryValue, x.errorString, e.toString()), locale));
            }

            // handle BillingWeight element info
            Element billingWeightElement = UtilXml.firstChildElement(shipmentResultsElement, x.BillingWeight);
            Element billingWeightUnitOfMeasurementElement = UtilXml.firstChildElement(billingWeightElement, x.UnitOfMeasurement);
            String billingWeightUnitOfMeasurement = UtilXml.childElementValue(billingWeightUnitOfMeasurementElement, x.Code);
            String billingWeight = UtilXml.childElementValue(billingWeightElement, x.Weight_69c0b815);
            try {
                shipmentRouteSegment.set(x.billingWeight, new BigDecimal(billingWeight));
            } catch (NumberFormatException e) {
                String excErrMsg = x.Error_parsing_the_billingWeight + billingWeight + x.str_89222ecc + e.toString();
                Debug.logError(e, excErrMsg, MODULE);
                errorList.add(UtilProperties.getMessage(RES_ERROR, x.FacilityShipmentUpsErrorParsingBillingWeight,
                        UtilMisc.toMap(x.billingWeight, billingWeight, x.errorString, e.toString()), locale));
            }
            shipmentRouteSegment.set(x.billingWeightUomId, UPS_TO_OFBIZ.get(billingWeightUnitOfMeasurement));

            // store the ShipmentIdentificationNumber and ShipmentDigest
            String shipmentIdentificationNumber = UtilXml.childElementValue(shipmentResultsElement, x.ShipmentIdentificationNumber);
            // should compare to trackingIdNumber, should always be the same right?
            shipmentRouteSegment.set(x.trackingIdNumber, shipmentIdentificationNumber);

            // set ShipmentRouteSegment carrierServiceStatusId after each UPS service applicable
            shipmentRouteSegment.put(x.carrierServiceStatusId, x.SHRSCS_ACCEPTED);

            // write/store modified value object
            shipmentRouteSegment.store();

            // now process the PackageResults elements
            List<? extends Element> packageResultsElements = UtilXml.childElementList(shipmentResultsElement, x.PackageResults);
            Iterator<GenericValue> shipmentPackageRouteSegIter = shipmentPackageRouteSegs.iterator();
            for (Element packageResultsElement : packageResultsElements) {
                String trackingNumber = UtilXml.childElementValue(packageResultsElement, x.TrackingNumber);

                Element packageServiceOptionsChargesElement = UtilXml.firstChildElement(packageResultsElement, x.ServiceOptionsCharges);
                String packageServiceOptionsCurrencyCode = UtilXml.childElementValue(packageServiceOptionsChargesElement, x.CurrencyCode);
                String packageServiceOptionsMonetaryValue = UtilXml.childElementValue(packageServiceOptionsChargesElement, x.MonetaryValue);

                Element packageLabelImageElement = UtilXml.firstChildElement(packageResultsElement, x.LabelImage);
                //Element packageLabelImageFormatElement = UtilXml.firstChildElement(packageResultsElement, "LabelImageFormat");
                // will be EPL or GIF, should always be GIF since that is what we requested
                String packageLabelGraphicImageString = UtilXml.childElementValue(packageLabelImageElement, x.GraphicImage);
                String packageLabelInternationalSignatureGraphicImageString = UtilXml.childElementValue(packageLabelImageElement,
                        x.InternationalSignatureGraphicImage);
                String packageLabelHTMLImageString = UtilXml.childElementValue(packageLabelImageElement, x.HTMLImage);

                if (!shipmentPackageRouteSegIter.hasNext()) {
                    errorList.add(UtilProperties.getMessage(RES_ERROR, x.FacilityShipmentUpsErrorMorePackageResultsWereReturned,
                            UtilMisc.toMap(x.trackingNumber, trackingNumber, x.ServiceOptionsCharges,
                                    packageServiceOptionsMonetaryValue + packageServiceOptionsCurrencyCode), locale));
                    // NOTE: if this happens much we should just create a new package to store all of the info...
                    continue;
                }

                //NOTE: I guess they come back in the same order we sent them, so we'll get the packages in order and off we go...
                GenericValue shipmentPackageRouteSeg = shipmentPackageRouteSegIter.next();
                shipmentPackageRouteSeg.set(x.trackingCode, trackingNumber);
                shipmentPackageRouteSeg.set(x.boxNumber, x.emptyString);
                shipmentPackageRouteSeg.set(x.currencyUomId, packageServiceOptionsCurrencyCode);
                try {
                    shipmentPackageRouteSeg.set(x.packageServiceCost, new BigDecimal(packageServiceOptionsMonetaryValue));
                } catch (NumberFormatException e) {
                    String excErrMsg = x.Error_parsing_the_packageServiceOptionsMonetaryValue + packageServiceOptionsMonetaryValue + x._for_2b9c1020
                            + x.Package_5fb6d258 + shipmentPackageRouteSeg.getString(x.shipmentPackageSeqId) + x.str_89222ecc + e.toString();
                    Debug.logError(e, excErrMsg, MODULE);
                    errorList.add(UtilProperties.getMessage(RES_ERROR, x.FacilityShipmentUpsErrorParsingServiceOptionsMonetaryValue,
                            UtilMisc.toMap(x.serviceOptionsMonetaryValue, serviceOptionsMonetaryValue, x.errorString, e.toString()), locale));
                }
                byte[] labelImageBytes = null;
                if (packageLabelGraphicImageString != null) {
                    labelImageBytes = Base64.getMimeDecoder().decode(packageLabelGraphicImageString.getBytes(StandardCharsets.UTF_8));
                    shipmentPackageRouteSeg.setBytes(x.labelImage, labelImageBytes);
                }
                byte[] labelInternationalSignatureGraphicImageBytes = null;
                if (packageLabelInternationalSignatureGraphicImageString != null) {
                    labelInternationalSignatureGraphicImageBytes =
                            Base64.getMimeDecoder().decode(packageLabelInternationalSignatureGraphicImageString.getBytes(StandardCharsets.UTF_8));
                    shipmentPackageRouteSeg.set(x.labelIntlSignImage, labelInternationalSignatureGraphicImageBytes);
                }
                String packageLabelHTMLImageStringDecoded =
                        Arrays.toString(Base64.getMimeDecoder().decode(packageLabelHTMLImageString.getBytes(StandardCharsets.UTF_8)));
                shipmentPackageRouteSeg.set(x.labelHtml, packageLabelHTMLImageStringDecoded);

                if (shipmentUpsSaveCertificationInfo) {
                    if (labelImageBytes != null) {
                        String outFileName = shipmentUpsSaveCertificationPath + x.label + trackingNumber + x.gif;
                        try (FileOutputStream fileOut = new FileOutputStream(outFileName)) {
                            fileOut.write(labelImageBytes);
                            fileOut.flush();
                        } catch (IOException e) {
                            Debug.logInfo(e,
                                    x.Could_not_save_UPS_LabelImage_GIF_file + packageLabelGraphicImageString + x.to_file
                                            + outFileName, MODULE);
                        }
                    }
                    if (labelInternationalSignatureGraphicImageBytes != null) {
                        String outFileName = shipmentUpsSaveCertificationPath + x.UpsShipmentLabelIntlSignImage + x.label_64c65374 + trackingNumber + x.gif;
                        try (FileOutputStream fileOut = new FileOutputStream(outFileName)) {
                            fileOut.write(labelInternationalSignatureGraphicImageBytes);
                            fileOut.flush();
                        } catch (IOException e) {
                            Debug.logInfo(e,
                                    x.Could_not_save_UPS_IntlSign_LabelImage_GIF_file
                                            + packageLabelInternationalSignatureGraphicImageString + x.str_af0f7c23 + x.to_file_c93a7642 + outFileName, MODULE);
                        }
                    }
                    if (packageLabelHTMLImageStringDecoded != null) {
                        String outFileName = shipmentUpsSaveCertificationPath + x.UpsShipmentLabelHTMLImage + shipmentRouteSegment.getString(
                                x.shipmentId) + x.str_53a0acfa + shipmentRouteSegment.getString(x.shipmentRouteSegmentId) + x.str_53a0acfa
                                + shipmentPackageRouteSeg.getString(x.shipmentPackageSeqId) + x.html_4af55114;
                        try (FileOutputStream fileOut = new FileOutputStream(outFileName)) {
                            fileOut.write(packageLabelHTMLImageStringDecoded.getBytes(StandardCharsets.UTF_8));
                            fileOut.flush();
                        } catch (IOException e) {
                            Debug.logInfo(e, x.Could_not_save_UPS_LabelImage_HTML_file + packageLabelHTMLImageStringDecoded + x.to_file
                                            + outFileName, MODULE);
                        }
                    }
                }

                shipmentPackageRouteSeg.store();
            }

            if (shipmentPackageRouteSegIter.hasNext()) {
                errorList.add(UtilProperties.getMessage(RES_ERROR, x.FacilityShipmentUpsErrorMorePackageOnThisShipment, locale));

                while (shipmentPackageRouteSegIter.hasNext()) {
                    GenericValue shipmentPackageRouteSeg = shipmentPackageRouteSegIter.next();
                    errorList.add(UtilProperties.getMessage(RES_ERROR, x.FacilityShipmentUpsErrorNoPackageResultsWereReturned,
                            UtilMisc.toMap(x.shipmentPackageSeqId, shipmentPackageRouteSeg.getString(x.shipmentPackageSeqId)), locale));
                }
            }

            // save the High Value Report image if it exists
            Element controlLogReceiptElement = UtilXml.firstChildElement(shipmentResultsElement, x.ControlLogReceipt);
            if (controlLogReceiptElement != null) {
                String fileString = UtilXml.childElementValue(controlLogReceiptElement, x.GraphicImage);
                String fileStringDecoded = Arrays.toString(Base64.getMimeDecoder().decode(fileString.getBytes(StandardCharsets.UTF_8)));
                if (fileStringDecoded != null) {
                    shipmentRouteSegment.set(x.upsHighValueReport, fileStringDecoded);
                    shipmentRouteSegment.store();
                    String outFileName =
                            shipmentUpsSaveCertificationPath + x.HighValueReport + shipmentRouteSegment.getString(x.shipmentId)
                                    + x.str_53a0acfa + shipmentRouteSegment.getString(x.shipmentRouteSegmentId) + x.html_4af55114;
                    try (FileOutputStream fileOut = new FileOutputStream(outFileName)) {
                        fileOut.write(fileStringDecoded.getBytes(StandardCharsets.UTF_8));
                        fileOut.flush();
                    } catch (IOException e) {
                        Debug.logInfo(e, x.Could_not_save_UPS_High_Value_Report_data + fileStringDecoded + x.to_file + outFileName,
                                MODULE);
                    }
                }
            }

            // -=-=-=- Okay, now done with that, just return any extra info...
            StringBuilder successString = new StringBuilder(UtilProperties.getMessage(RES_ERROR,
                    x.FacilityShipmentUpsShipmentAcceptSucceeded, locale));
            if (!errorList.isEmpty()) {
                // this shouldn't happen much, but handle it anyway
                successString.append(UtilProperties.getMessage(RES_ERROR,
                        x.FacilityShipmentUpsShipmentAcceptError, locale));
                Iterator<Object> errorListIter = errorList.iterator();
                while (errorListIter.hasNext()) {
                    successString.append(errorListIter.next());
                    if (errorListIter.hasNext()) {
                        successString.append(x.str_d3bc9a37);
                    }
                }
            }
            return ServiceUtil.returnSuccess(successString.toString());
        } else {
            errorList.add(0, UtilProperties.getMessage(RES_ERROR, x.FacilityShipmentUpsShipmentAcceptFailed, locale));
            return ServiceUtil.returnError(errorList);
        }
    }

    public static Map<String, Object> upsVoidShipment(DispatchContext dctx, UpsServicesContext context) {
        Delegator delegator = dctx.getDelegator();
        String shipmentId = (String) context.get(x.shipmentId);
        String shipmentRouteSegmentId = (String) context.get(x.shipmentRouteSegmentId);
        Locale locale = (Locale) context.get(x.locale);

        Map<String, Object> shipmentGatewayConfig = ShipmentServices.getShipmentGatewayConfigFromShipment(delegator, shipmentId, locale);
        String shipmentGatewayConfigId = (String) shipmentGatewayConfig.get(x.shipmentGatewayConfigId);
        String resource = (String) shipmentGatewayConfig.get(x.configProps);
        if (UtilValidate.isEmpty(shipmentGatewayConfigId) && UtilValidate.isEmpty(resource)) {
            return ServiceUtil.returnError(UtilProperties.getMessage(RES_ERROR, x.FacilityShipmentUpsGatewayNotAvailable, locale));
        }
        boolean shipmentUpsSaveCertificationInfo = x._true.equals(getShipmentGatewayConfigValue(delegator, shipmentGatewayConfigId, x.saveCertInfo,
                resource, x.shipment_ups_save_certification_info, x._true));
        String shipmentUpsSaveCertificationPath = FlexibleStringExpander.expandString(getShipmentGatewayConfigValue(delegator,
                shipmentGatewayConfigId, x.saveCertPath, resource, x.shipment_ups_save_certification_path, x.emptyString), context);
        File shipmentUpsSaveCertificationFile = null;
        if (shipmentUpsSaveCertificationInfo) {
            shipmentUpsSaveCertificationFile = new File(shipmentUpsSaveCertificationPath);
            if (!shipmentUpsSaveCertificationFile.exists()) {
                shipmentUpsSaveCertificationFile.mkdirs();
            }
        }

        String voidShipmentResponseString = null;

        try {
            ShipmentRouteSegmentDao shipmentRouteSegmentDao = DaoRegistry.getDao(delegator, x.ShipmentRouteSegment,
                    ShipmentRouteSegmentDao.class);
            GenericValue shipmentRouteSegment = shipmentRouteSegmentDao.findOneByWhere(delegator, x.ShipmentRouteSegment,
                    UtilMisc.toMap(x.shipmentId, shipmentId, x.shipmentRouteSegmentId, shipmentRouteSegmentId), null, null, false);

            if (!x.UPS.equals(shipmentRouteSegment.getString(x.carrierPartyId))) {
                return ServiceUtil.returnError(UtilProperties.getMessage(RES_ERROR, x.FacilityShipmentUpsNotRouteSegmentCarrier, UtilMisc.toMap(
                        x.shipmentRouteSegmentId, shipmentRouteSegmentId, x.shipmentId, shipmentId), locale));
            }

            // add ShipmentRouteSegment carrierServiceStatusId, check before all UPS services
            if (!x.SHRSCS_CONFIRMED.equals(shipmentRouteSegment.getString(x.carrierServiceStatusId))
                    && !x.SHRSCS_ACCEPTED.equals(shipmentRouteSegment.getString(x.carrierServiceStatusId))) {
                return ServiceUtil.returnError(UtilProperties.getMessage(RES_ERROR, x.FacilityShipmentUpsRouteSegmentStatusMustBeConfirmedOrAccepted,
                        UtilMisc.toMap(x.shipmentRouteSegmentId, shipmentRouteSegmentId, x.shipmentId, shipmentId, x.shipmentRouteSegmentStatus,
                                shipmentRouteSegment.getString(x.carrierServiceStatusId)), locale));
            }

            if (UtilValidate.isEmpty(shipmentRouteSegment.getString(x.trackingIdNumber))) {
                return ServiceUtil.returnError(UtilProperties.getMessage(RES_ERROR, x.FacilityShipmentUpsTrackingIdNumberWasNotSet, locale));
            }

            Document voidShipmentRequestDoc = UtilXml.makeEmptyXmlDocument(x.VoidShipmentRequest);
            Element voidShipmentRequestElement = voidShipmentRequestDoc.getDocumentElement();
            voidShipmentRequestElement.setAttribute(x.xml_lang, x.en_US);

            // Top Level Element: Request
            Element requestElement = UtilXml.addChildElement(voidShipmentRequestElement, x.Request, voidShipmentRequestDoc);

            Element transactionReferenceElement = UtilXml.addChildElement(requestElement, x.TransactionReference, voidShipmentRequestDoc);
            UtilXml.addChildElementValue(transactionReferenceElement, x.CustomerContext, x.Void_1, voidShipmentRequestDoc);
            UtilXml.addChildElementValue(transactionReferenceElement, x.XpciVersion, x._1_0001, voidShipmentRequestDoc);

            UtilXml.addChildElementValue(requestElement, x.RequestAction, x.Void, voidShipmentRequestDoc);
            UtilXml.addChildElementValue(requestElement, x.RequestOption, x._1, voidShipmentRequestDoc);

            UtilXml.addChildElementValue(voidShipmentRequestElement, x.ShipmentIdentificationNumber, shipmentRouteSegment.getString(
                    x.trackingIdNumber), voidShipmentRequestDoc);

            String voidShipmentRequestString = null;
            try {
                voidShipmentRequestString = UtilXml.writeXmlDocument(voidShipmentRequestDoc);
            } catch (IOException e) {
                String ioeErrMsg = x.Error_writing_the_VoidShipmentRequest_XML_Document_to_a_String + e.toString();
                Debug.logError(e, ioeErrMsg, MODULE);
                return ServiceUtil.returnError(UtilProperties.getMessage(RES_ERROR, x.FacilityShipmentUpsErrorVoidShipmentRequestXmlToString,
                        UtilMisc.toMap(x.errorString, e.toString()), locale));
            }

            // create AccessRequest XML doc
            Document accessRequestDocument = createAccessRequestDocument(delegator, shipmentGatewayConfigId, resource);
            String accessRequestString = null;
            try {
                accessRequestString = UtilXml.writeXmlDocument(accessRequestDocument);
            } catch (IOException e) {
                String ioeErrMsg = x.Error_writing_the_AccessRequest_XML_Document_to_a_String + e.toString();
                Debug.logError(e, ioeErrMsg, MODULE);
                return ServiceUtil.returnError(UtilProperties.getMessage(RES_ERROR, x.FacilityShipmentUpsErrorAccessRequestXmlToString,
                        UtilMisc.toMap(x.errorString, e.toString()), locale));
            }

            // connect to UPS server, send AccessRequest to auth
            // send ShipmentConfirmRequest String
            // get ShipmentConfirmResponse String back
            StringBuilder xmlString = new StringBuilder();
            xmlString.append(accessRequestString);
            xmlString.append(voidShipmentRequestString);

            if (shipmentUpsSaveCertificationInfo) {
                String outFileName =
                        shipmentUpsSaveCertificationPath + x.UpsVoidShipmentRequest + shipmentId + x.str_53a0acfa + shipmentRouteSegment.getString(
                         x.shipmentRouteSegmentId) + x.xml_657e4752;
                try (FileOutputStream fileOut = new FileOutputStream(outFileName)) {
                    fileOut.write(xmlString.toString().getBytes(StandardCharsets.UTF_8));
                    fileOut.flush();
                } catch (IOException e) {
                    Debug.logInfo(e, x.Could_not_save_UPS_XML_file + xmlString.toString() + x.to_file + outFileName, MODULE);
                }
            }

            try {
                voidShipmentResponseString = sendUpsRequest(x.Void, xmlString.toString(), shipmentGatewayConfigId, resource, delegator, locale);
            } catch (UpsConnectException e) {
                String uceErrMsg = x.Error_sending_UPS_request_for_UPS_Service_Void + e.toString();
                Debug.logError(e, uceErrMsg, MODULE);
                return ServiceUtil.returnError(UtilProperties.getMessage(RES_ERROR, x.FacilityShipmentUpsErrorSendingVoid,
                        UtilMisc.toMap(x.errorString, e.toString()), locale));
            }

            if (shipmentUpsSaveCertificationInfo) {
                String outFileName =
                        shipmentUpsSaveCertificationPath + x.UpsVoidShipmentResponse + shipmentId + x.str_53a0acfa + shipmentRouteSegment.getString(
                         x.shipmentRouteSegmentId) + x.xml_657e4752;
                try (FileOutputStream fileOut = new FileOutputStream(outFileName)) {
                    fileOut.write(voidShipmentResponseString.getBytes(StandardCharsets.UTF_8));
                    fileOut.flush();
                } catch (IOException e) {
                    Debug.logInfo(e, x.Could_not_save_UPS_XML_file + xmlString.toString() + x.to_file + outFileName, MODULE);
                }
            }

            Document voidShipmentResponseDocument = null;
            try {
                voidShipmentResponseDocument = UtilXml.readXmlDocument(voidShipmentResponseString, false);
            } catch (SAXException | IOException | ParserConfigurationException e2) {
                String excErrMsg = x.Error_parsing_the_VoidShipmentResponse + e2.toString();
                Debug.logError(e2, excErrMsg, MODULE);
                return ServiceUtil.returnError(UtilProperties.getMessage(RES_ERROR, x.FacilityShipmentUpsErrorParsingVoidShipmentResponse,
                        UtilMisc.toMap(x.errorString, e2.toString()), locale));
            }

            return handleUpsVoidShipmentResponse(voidShipmentResponseDocument, shipmentRouteSegment, locale);
        } catch (GenericEntityException e) {
            Debug.logError(e, MODULE);
            return ServiceUtil.returnError(UtilProperties.getMessage(RES_ERROR, x.FacilityShipmentUpsErrorDataShipmentVoid,
                    UtilMisc.toMap(x.errorString, e.toString()), locale));
        }
    }

    public static Map<String, Object> handleUpsVoidShipmentResponse(Document voidShipmentResponseDocument, GenericValue shipmentRouteSegment,
                                                                    Locale locale) throws GenericEntityException {
        // process VoidShipmentResponse, update data as needed
        Element voidShipmentResponseElement = voidShipmentResponseDocument.getDocumentElement();

        // handle Response element info
        Element responseElement = UtilXml.firstChildElement(voidShipmentResponseElement, x.Response_6e617e4f);
        String responseStatusCode = UtilXml.childElementValue(responseElement, x.ResponseStatusCode);
        List<Object> errorList = new LinkedList<>();
        UpsServices.handleErrors(responseElement, errorList, locale);

        // handle other response elements
        Element statusElement = UtilXml.firstChildElement(voidShipmentResponseElement, x.Status_bae7d5be);

        Element statusTypeElement = UtilXml.firstChildElement(statusElement, x.StatusType);
        String statusTypeCode = UtilXml.childElementValue(statusTypeElement, x.Code);
        String statusTypeDescription = UtilXml.childElementValue(statusTypeElement, x.Description);

        Element statusCodeElement = UtilXml.firstChildElement(statusElement, x.StatusCode);
        String statusCodeCode = UtilXml.childElementValue(statusCodeElement, x.Code);
        String statusCodeDescription = UtilXml.childElementValue(statusCodeElement, x.Description);

        if (x._1.equals(responseStatusCode)) {
            // set ShipmentRouteSegment carrierServiceStatusId after each UPS service applicable
            shipmentRouteSegment.put(x.carrierServiceStatusId, x.SHRSCS_VOIDED);
            shipmentRouteSegment.store();

            // -=-=-=- Okay, now done with that, just return any extra info...
            StringBuilder successString = new StringBuilder(UtilProperties.getMessage(RES_ERROR, x.FacilityShipmentUpsShipmentVoidSucceeded,
                    UtilMisc.toMap(x.statusTypeCode, statusTypeCode, x.statusTypeDescription, statusTypeDescription,
                            x.statusCodeCode, statusCodeCode, x.statusCodeDescription, statusCodeDescription), locale));
            if (!errorList.isEmpty()) {
                // this shouldn't happen much, but handle it anyway
                successString.append(UtilProperties.getMessage(RES_ERROR, x.FacilityShipmentUpsShipmentVoidError, locale));
                Iterator<Object> errorListIter = errorList.iterator();
                while (errorListIter.hasNext()) {
                    successString.append(errorListIter.next());
                    if (errorListIter.hasNext()) {
                        successString.append(x.str_d3bc9a37);
                    }
                }
            }
            return ServiceUtil.returnSuccess(successString.toString());
        } else {
            errorList.add(0, UtilProperties.getMessage(RES_ERROR, x.FacilityShipmentUpsShipmentVoidFailed,
                    UtilMisc.toMap(x.statusTypeCode, statusTypeCode, x.statusTypeDescription, statusTypeDescription,
                            x.statusCodeCode, statusCodeCode, x.statusCodeDescription, statusCodeDescription), locale));
            return ServiceUtil.returnError(errorList);
        }
    }

    public static Map<String, Object> upsTrackShipment(DispatchContext dctx, UpsServicesContext context) {
        Delegator delegator = dctx.getDelegator();
        String shipmentId = (String) context.get(x.shipmentId);
        String shipmentRouteSegmentId = (String) context.get(x.shipmentRouteSegmentId);
        Locale locale = (Locale) context.get(x.locale);

        Map<String, Object> shipmentGatewayConfig = ShipmentServices.getShipmentGatewayConfigFromShipment(delegator, shipmentId, locale);
        String shipmentGatewayConfigId = (String) shipmentGatewayConfig.get(x.shipmentGatewayConfigId);
        String resource = (String) shipmentGatewayConfig.get(x.configProps);
        if (UtilValidate.isEmpty(shipmentGatewayConfigId) && UtilValidate.isEmpty(resource)) {
            return ServiceUtil.returnError(UtilProperties.getMessage(RES_ERROR, x.FacilityShipmentUpsGatewayNotAvailable, locale));
        }
        boolean shipmentUpsSaveCertificationInfo = x._true.equals(getShipmentGatewayConfigValue(delegator, shipmentGatewayConfigId, x.saveCertInfo,
                resource, x.shipment_ups_save_certification_info, x._true));
        String shipmentUpsSaveCertificationPath = FlexibleStringExpander.expandString(getShipmentGatewayConfigValue(delegator,
                shipmentGatewayConfigId, x.saveCertPath, resource, x.shipment_ups_save_certification_path, x.emptyString), context);
        File shipmentUpsSaveCertificationFile = null;
        if (shipmentUpsSaveCertificationInfo) {
            shipmentUpsSaveCertificationFile = new File(shipmentUpsSaveCertificationPath);
            if (!shipmentUpsSaveCertificationFile.exists()) {
                shipmentUpsSaveCertificationFile.mkdirs();
            }
        }

        String trackResponseString = null;

        try {
            ShipmentRouteSegmentDao shipmentRouteSegmentDao = DaoRegistry.getDao(delegator, x.ShipmentRouteSegment,
                    ShipmentRouteSegmentDao.class);
            GenericValue shipmentRouteSegment = shipmentRouteSegmentDao.findOneByWhere(delegator, x.ShipmentRouteSegment,
                    UtilMisc.toMap(x.shipmentId, shipmentId, x.shipmentRouteSegmentId, shipmentRouteSegmentId), null, null, false);

            if (!x.UPS.equals(shipmentRouteSegment.getString(x.carrierPartyId))) {
                return ServiceUtil.returnError(UtilProperties.getMessage(RES_ERROR, x.FacilityShipmentUpsNotRouteSegmentCarrier, UtilMisc.toMap(
                        x.shipmentRouteSegmentId, shipmentRouteSegmentId, x.shipmentId, shipmentId), locale));
            }

            // add ShipmentRouteSegment carrierServiceStatusId, check before all UPS services
            if (!x.SHRSCS_ACCEPTED.equals(shipmentRouteSegment.getString(x.carrierServiceStatusId))) {
                return ServiceUtil.returnError(UtilProperties.getMessage(RES_ERROR, x.FacilityShipmentUpsRouteSegmentStatusNotAccepted,
                        UtilMisc.toMap(x.shipmentRouteSegmentId, shipmentRouteSegmentId, x.shipmentId, shipmentId, x.shipmentRouteSegmentStatus,
                                shipmentRouteSegment.getString(x.carrierServiceStatusId)), locale));
            }

            List<GenericValue> shipmentPackageRouteSegs = shipmentRouteSegment.getRelated(x.ShipmentPackageRouteSeg, null, UtilMisc.toList(
                    x.shipmentPackageSeqId_39d5d38d), false);
            if (UtilValidate.isEmpty(shipmentPackageRouteSegs)) {
                return ServiceUtil.returnError(UtilProperties.getMessage(RES_ERROR, x.FacilityShipmentUpsPackageRouteSegsNotFound, UtilMisc.toMap(
                        x.shipmentId, shipmentId, x.shipmentRouteSegmentId, shipmentRouteSegmentId), locale));
            }

            if (UtilValidate.isEmpty(shipmentRouteSegment.getString(x.trackingIdNumber))) {
                return ServiceUtil.returnError(UtilProperties.getMessage(RES_ERROR, x.FacilityShipmentUpsTrackingIdNumberWasNotSet, locale));
            }

            Document trackRequestDoc = UtilXml.makeEmptyXmlDocument(x.TrackRequest);
            Element trackRequestElement = trackRequestDoc.getDocumentElement();
            trackRequestElement.setAttribute(x.xml_lang, x.en_US);

            // Top Level Element: Request
            Element requestElement = UtilXml.addChildElement(trackRequestElement, x.Request, trackRequestDoc);

            Element transactionReferenceElement = UtilXml.addChildElement(requestElement, x.TransactionReference, trackRequestDoc);
            UtilXml.addChildElementValue(transactionReferenceElement, x.CustomerContext, x.Track, trackRequestDoc);
            UtilXml.addChildElementValue(transactionReferenceElement, x.XpciVersion, x._1_0001, trackRequestDoc);

            UtilXml.addChildElementValue(requestElement, x.RequestAction, x.Track, trackRequestDoc);

            UtilXml.addChildElementValue(trackRequestElement, x.ShipmentIdentificationNumber, shipmentRouteSegment.getString(x.trackingIdNumber),
                    trackRequestDoc);

            String trackRequestString = null;
            try {
                trackRequestString = UtilXml.writeXmlDocument(trackRequestDoc);
            } catch (IOException e) {
                String ioeErrMsg = x.Error_writing_the_TrackRequest_XML_Document_to_a_String + e.toString();
                Debug.logError(e, ioeErrMsg, MODULE);
                return ServiceUtil.returnError(UtilProperties.getMessage(RES_ERROR, x.FacilityShipmentUpsErrorTrackRequestXmlToString,
                        UtilMisc.toMap(x.errorString, e.toString()), locale));
            }

            // create AccessRequest XML doc
            Document accessRequestDocument = createAccessRequestDocument(delegator, shipmentGatewayConfigId, resource);
            String accessRequestString = null;
            try {
                accessRequestString = UtilXml.writeXmlDocument(accessRequestDocument);
            } catch (IOException e) {
                String ioeErrMsg = x.Error_writing_the_AccessRequest_XML_Document_to_a_String + e.toString();
                Debug.logError(e, ioeErrMsg, MODULE);
                return ServiceUtil.returnError(UtilProperties.getMessage(RES_ERROR, x.FacilityShipmentUpsErrorAccessRequestXmlToString,
                        UtilMisc.toMap(x.errorString, e.toString()), locale));
            }

            // connect to UPS server, send AccessRequest to auth
            // send ShipmentConfirmRequest String
            // get ShipmentConfirmResponse String back
            StringBuilder xmlString = new StringBuilder();
            xmlString.append(accessRequestString);
            xmlString.append(trackRequestString);

            if (shipmentUpsSaveCertificationInfo) {
                String outFileName = shipmentUpsSaveCertificationPath + x.UpsTrackRequest + shipmentId + x.str_53a0acfa + shipmentRouteSegment.getString(
                        x.shipmentRouteSegmentId) + x.xml_657e4752;
                try (FileOutputStream fileOut = new FileOutputStream(outFileName)) {
                    fileOut.write(xmlString.toString().getBytes(StandardCharsets.UTF_8));
                    fileOut.flush();
                } catch (IOException e) {
                    Debug.logInfo(e, x.Could_not_save_UPS_XML_file + xmlString.toString() + x.to_file + outFileName, MODULE);
                }
            }

            try {
                trackResponseString = sendUpsRequest(x.Track, xmlString.toString(), shipmentGatewayConfigId, resource, delegator, locale);
            } catch (UpsConnectException e) {
                String uceErrMsg = x.Error_sending_UPS_request_for_UPS_Service_Track + e.toString();
                Debug.logError(e, uceErrMsg, MODULE);
                return ServiceUtil.returnError(UtilProperties.getMessage(RES_ERROR, x.FacilityShipmentUpsErrorSendingTrack,
                        UtilMisc.toMap(x.errorString, e.toString()), locale));
            }

            if (shipmentUpsSaveCertificationInfo) {
                String outFileName =
                        shipmentUpsSaveCertificationPath + x.UpsTrackResponseString + shipmentId + x.str_53a0acfa + shipmentRouteSegment.getString(
                         x.shipmentRouteSegmentId) + x.xml_657e4752;
                try (FileOutputStream fileOut = new FileOutputStream(outFileName)) {
                    fileOut.write(trackResponseString.getBytes(StandardCharsets.UTF_8));
                    fileOut.flush();
                } catch (IOException e) {
                    Debug.logInfo(e, x.Could_not_save_UPS_XML_file + xmlString.toString() + x.to_file + outFileName, MODULE);
                }
            }

            Document trackResponseDocument = null;
            try {
                trackResponseDocument = UtilXml.readXmlDocument(trackResponseString, false);
            } catch (SAXException | IOException | ParserConfigurationException e2) {
                String excErrMsg = x.Error_parsing_the_TrackResponse + e2.toString();
                Debug.logError(e2, excErrMsg, MODULE);
                return ServiceUtil.returnError(UtilProperties.getMessage(RES_ERROR, x.FacilityShipmentUpsErrorParsingTrackResponse,
                        UtilMisc.toMap(x.errorString, e2.toString()), locale));
            }

            return handleUpsTrackShipmentResponse(trackResponseDocument, shipmentRouteSegment, shipmentPackageRouteSegs, locale);
        } catch (GenericEntityException e) {
            Debug.logError(e, MODULE);
            return ServiceUtil.returnError(UtilProperties.getMessage(RES_ERROR, x.FacilityShipmentUpsErrorDataShipmentTrack,
                    UtilMisc.toMap(x.errorString, e.toString()), locale));
        }
    }

    public static Map<String, Object> handleUpsTrackShipmentResponse(Document trackResponseDocument, GenericValue shipmentRouteSegment,
                                         List<GenericValue> shipmentPackageRouteSegs, Locale locale) throws GenericEntityException {
        // process TrackResponse, update data as needed
        Element trackResponseElement = trackResponseDocument.getDocumentElement();

        // handle Response element info
        Element responseElement = UtilXml.firstChildElement(trackResponseElement, x.Response_6e617e4f);
        String responseStatusCode = UtilXml.childElementValue(responseElement, x.ResponseStatusCode);
        List<Object> errorList = new LinkedList<>();
        UpsServices.handleErrors(responseElement, errorList, locale);

        if (x._1.equals(responseStatusCode)) {
            // TODO: handle other response elements
/*
        <Package>
            <TrackingNumber>1Z12345E1512345676</TrackingNumber>
            <Activity>
                <ActivityLocation>
                    <Address>
                        <City>CLAKVILLE</City>
                        <StateProvinceCode>AK</StateProvinceCode>
                        <PostalCode>99901</PostalCode>
                        <CountryCode>US</CountryCode>
                    </Address>
                    <Code>MG</Code>
                    <Description>MC MAN</Description>
                </ActivityLocation>
                <Status>
                    <StatusType>
                        <Code>D</Code>
                        <Description>DELIVERED</Description>
                    </StatusType>
                    <StatusCode>
                        <Code>FS</Code>
                    </StatusCode>
                </Status>
                <Date>20020930</Date>
                <Time>130900</Time>
            </Activity>
            <PackageWeight>
                <UnitOfMeasurement>
                    <Code>LBS</Code>
                </UnitOfMeasurement>
                <Weight>0.00</Weight>
            </PackageWeight>
        </Package>
 *
 */
            // -=-=-=- Okay, now done with that, just return any extra info...
            StringBuilder successString = new StringBuilder(UtilProperties.getMessage(RES_ERROR, x.FacilityShipmentUpsShipmentTrackSucceeded,
                    locale));
            if (!errorList.isEmpty()) {
                // this shouldn't happen much, but handle it anyway
                successString.append(UtilProperties.getMessage(RES_ERROR, x.FacilityShipmentUpsShipmentTrackError, locale));
                Iterator<Object> errorListIter = errorList.iterator();
                while (errorListIter.hasNext()) {
                    successString.append(errorListIter.next());
                    if (errorListIter.hasNext()) {
                        successString.append(x.str_d3bc9a37);
                    }
                }
            }
            return ServiceUtil.returnSuccess(successString.toString());
        } else {
            errorList.add(0, UtilProperties.getMessage(RES_ERROR, x.FacilityShipmentUpsShipmentTrackFailed, locale));
            return ServiceUtil.returnError(errorList);
        }
    }

    public static Map<String, Object> upsRateInquire(DispatchContext dctx, UpsServicesContext context) {
        Delegator delegator = dctx.getDelegator();
        Locale locale = (Locale) context.get(x.locale);
        // prepare the data
        String shippingContactMechId = (String) context.get(x.shippingContactMechId);
        String shippingOriginContactMechId = (String) context.get(x.shippingOriginContactMechId);
        // obtain the ship-to address
        GenericValue shipToAddress = null;
        if (shippingContactMechId != null) {
            try {
                PostalAddressDao postalAddressDao = DaoRegistry.getDao(delegator, x.PostalAddress, PostalAddressDao.class);
                shipToAddress = postalAddressDao.findOneByWhere(delegator, x.PostalAddress,
                        UtilMisc.toMap(x.contactMechId, shippingContactMechId), null, null, false);
            } catch (GenericEntityException e) {
                Debug.logError(e, MODULE);
            }
        }
        if (shipToAddress == null) {
            return ServiceUtil.returnError(UtilProperties.getMessage(RES_ERROR, x.FacilityShipmentUnableFoundShipToAddresss, locale));
        }

        // obtain the ship from address if provided
        GenericValue shipFromAddress = null;
        if (shippingOriginContactMechId != null) {
            try {
                PostalAddressDao postalAddressDao = DaoRegistry.getDao(delegator, x.PostalAddress, PostalAddressDao.class);
                shipFromAddress = postalAddressDao.findOneByWhere(delegator, x.PostalAddress,
                        UtilMisc.toMap(x.contactMechId, shippingOriginContactMechId), null, null, false);
            } catch (GenericEntityException e) {
                Debug.logError(e, MODULE);
                return ServiceUtil.returnError(UtilProperties.getMessage(RES_ERROR, x.FacilityShipmentUnableFoundShipToAddresssForDropShipping,
                        locale));
            }
        }

        GenericValue destCountryGeo = null;
        try {
            destCountryGeo = shipToAddress.getRelatedOne(x.CountryGeo, false);
        } catch (GenericEntityException e) {
            Debug.logError(e, MODULE);
            return ServiceUtil.returnError(e.getMessage());
        }
        if (UtilValidate.isEmpty(destCountryGeo)) {
            return ServiceUtil.returnError(UtilProperties.getMessage(RES_ERROR, x.FacilityShipmentUpsShipToAddresssNoDestionationCountry, locale));
        }
        Map<String, Object> cxt = UtilMisc.toMap(x.serviceConfigProps, context.get(x.serviceConfigProps), x.upsRateInquireMode, context.get(
                x.upsRateInquireMode),
                x.productStoreId, context.get(x.productStoreId), x.carrierRoleTypeId, context.get(x.carrierRoleTypeId));
        cxt.put(x.carrierPartyId, context.get(x.carrierPartyId));
        cxt.put(x.shipmentMethodTypeId, context.get(x.shipmentMethodTypeId));
        cxt.put(x.shippingPostalCode, shipToAddress.getString(x.postalCode));
        cxt.put(x.shippingCountryCode, destCountryGeo.getString(x.geoCode));
        cxt.put(x.packageWeights, context.get(x.packageWeights));
        cxt.put(x.shippableItemInfo, context.get(x.shippableItemInfo));
        cxt.put(x.shippableTotal, context.get(x.shippableTotal));
        cxt.put(x.shippableQuantity, context.get(x.shippableQuantity));
        cxt.put(x.shippableWeight, context.get(x.shippableWeight));
        cxt.put(x.isResidentialAddress, context.get(x.isResidentialAddress));
        cxt.put(x.shipFromAddress, shipFromAddress);
        cxt.put(x.shipmentGatewayConfigId, context.get(x.shipmentGatewayConfigId));
        try {
            Map<String, Object> serviceResult = dctx.getDispatcher().runSync(x.upsRateEstimateByPostalCode, cxt);
            if (ServiceUtil.isError(serviceResult)) {
                return ServiceUtil.returnError(ServiceUtil.getErrorMessage(serviceResult));
            }
            return serviceResult;
        } catch (GenericServiceException e) {
            Debug.logError(e, MODULE);
            return ServiceUtil.returnError(UtilProperties.getMessage(RES_ERROR, x.FacilityShipmentUpsRateEstimateError,
                    UtilMisc.toMap(x.errorString, e.getMessage()), locale));
        }
    }

    private static void splitEstimatePackages(DispatchContext dctx, Document requestDoc, Element shipmentElement,
                                              List<Map<String, Object>> shippableItemInfo,
                                              BigDecimal maxWeight, BigDecimal minWeight, String totalWeightStr) {
        List<Map<String, BigDecimal>> packages = ShipmentWorker.getPackageSplit(dctx, shippableItemInfo, maxWeight);
        if (UtilValidate.isNotEmpty(packages)) {
            for (Map<String, BigDecimal> packageMap : packages) {
                addPackageElement(dctx, requestDoc, shipmentElement, shippableItemInfo, packageMap, minWeight);
            }
        } else {
            // Add a dummy package
            BigDecimal packageWeight = BigDecimal.ONE;
            try {
                packageWeight = new BigDecimal(totalWeightStr);
            } catch (NumberFormatException e) {
                Debug.logError(e, MODULE);
            }
            Element packageElement = UtilXml.addChildElement(shipmentElement, x.Package_7431e3df, requestDoc);
            Element packagingTypeElement = UtilXml.addChildElement(packageElement, x.PackagingType, requestDoc);
            UtilXml.addChildElementValue(packagingTypeElement, x.Code, x._00_fb965496, requestDoc);
            Element packageWeightElement = UtilXml.addChildElement(packageElement, x.PackageWeight, requestDoc);
            UtilXml.addChildElementValue(packageWeightElement, x.Weight_69c0b815, x.emptyString + packageWeight, requestDoc);
        }
    }

    private static void addPackageElement(DispatchContext dctx, Document requestDoc, Element shipmentElement,
                                          List<Map<String, Object>> shippableItemInfo, Map<String, BigDecimal> packageMap, BigDecimal minWeight) {
        BigDecimal packageWeight = checkForDefaultPackageWeight(ShipmentWorker.calcPackageWeight(dctx, packageMap, shippableItemInfo,
                BigDecimal.ZERO), minWeight);
        Element packageElement = UtilXml.addChildElement(shipmentElement, x.Package_7431e3df, requestDoc);
        Element packagingTypeElement = UtilXml.addChildElement(packageElement, x.PackagingType, requestDoc);
        UtilXml.addChildElementValue(packagingTypeElement, x.Code, x._00_fb965496, requestDoc);
        UtilXml.addChildElementValue(packagingTypeElement, x.Description, x.Unknown_PackagingType, requestDoc);
        UtilXml.addChildElementValue(packageElement, x.Description, x.Package_Description, requestDoc);
        Element packageWeightElement = UtilXml.addChildElement(packageElement, x.PackageWeight, requestDoc);
        UtilXml.addChildElementValue(packageWeightElement, x.Weight_69c0b815, packageWeight.toPlainString(), requestDoc);
        //If product is in shippable Package then it we should have one product per packagemap
        if (packageMap.size() == 1) {
            Iterator<String> i = packageMap.keySet().iterator();
            String productId = i.next();
            Map<String, Object> productInfo = ShipmentWorker.getProductItemInfo(shippableItemInfo, productId);
            if (productInfo.get(x.inShippingBox) != null && x.Y.equalsIgnoreCase((String) productInfo.get(x.inShippingBox))
                    && productInfo.get(x.shippingDepth) != null && productInfo.get(x.shippingWidth) != null
                    && productInfo.get(x.shippingHeight) != null) {
                Element dimensionsElement = UtilXml.addChildElement(packageElement, x.Dimensions, requestDoc);
                UtilXml.addChildElementValue(dimensionsElement, x.Length, productInfo.get(x.shippingDepth).toString(), requestDoc);
                UtilXml.addChildElementValue(dimensionsElement, x.Width, productInfo.get(x.shippingWidth).toString(), requestDoc);
                UtilXml.addChildElementValue(dimensionsElement, x.Height, productInfo.get(x.shippingHeight).toString(), requestDoc);
            }
        }
    }

    private static void addPackageElement(Document requestDoc, Element shipmentElement, BigDecimal packageWeight) {
        Element packageElement = UtilXml.addChildElement(shipmentElement, x.Package_7431e3df, requestDoc);
        Element packagingTypeElement = UtilXml.addChildElement(packageElement, x.PackagingType, requestDoc);
        UtilXml.addChildElementValue(packagingTypeElement, x.Code, x._00_fb965496, requestDoc);
        UtilXml.addChildElementValue(packagingTypeElement, x.Description, x.Unknown_PackagingType, requestDoc);
        UtilXml.addChildElementValue(packageElement, x.Description, x.Package_Description, requestDoc);
        Element packageWeightElement = UtilXml.addChildElement(packageElement, x.PackageWeight, requestDoc);
        UtilXml.addChildElementValue(packageWeightElement, x.Weight_69c0b815, packageWeight.toString(), requestDoc);
    }


    private static BigDecimal checkForDefaultPackageWeight(BigDecimal weight, BigDecimal minWeight) {
        return (weight.compareTo(BigDecimal.ZERO) > 0 && weight.compareTo(minWeight) > 0 ? weight : minWeight);
    }

    public static Map<String, Object> handleUpsRateInquireResponse(Document rateResponseDocument, Locale locale) {
        // process TrackResponse, update data as needed
        Element rateResponseElement = rateResponseDocument.getDocumentElement();

        // handle Response element info
        Element responseElement = UtilXml.firstChildElement(rateResponseElement, x.Response_6e617e4f);
        String responseStatusCode = UtilXml.childElementValue(responseElement, x.ResponseStatusCode);
        List<Object> errorList = new LinkedList<>();
        UpsServices.handleErrors(responseElement, errorList, locale);

        if (x._1.equals(responseStatusCode)) {
            List<? extends Element> rates = UtilXml.childElementList(rateResponseElement, x.RatedShipment);
            Map<String, BigDecimal> rateMap = new HashMap<>();
            BigDecimal firstRate = null;
            if (UtilValidate.isEmpty(rates)) {
                return ServiceUtil.returnError(UtilProperties.getMessage(RES_ERROR,
                        x.FacilityShipmentRateNotAvailable, locale));
            } else {
                for (Element element : rates) {
                    // get service
                    Element service = UtilXml.firstChildElement(element, x.Service);
                    String serviceCode = UtilXml.childElementValue(service, x.Code);

                    // get total
                    Element totalCharges = UtilXml.firstChildElement(element, x.TotalCharges);
                    String totalString = UtilXml.childElementValue(totalCharges, x.MonetaryValue);

                    rateMap.put(serviceCode, new BigDecimal(totalString));
                    if (firstRate == null) {
                        firstRate = rateMap.get(serviceCode);
                    }
                }
            }

            Debug.logInfo(x.UPS_Rate_Map + rateMap, MODULE);

            Map<String, Object> resp = ServiceUtil.returnSuccess();
            resp.put(x.upsRateCodeMap, rateMap);
            resp.put(x.shippingEstimateAmount, firstRate);
            return resp;
        } else {
            errorList.add(ServiceUtil.returnError(UtilProperties.getMessage(RES_ERROR, x.FacilityShipmentUpsErrorStatusCode,
                    UtilMisc.toMap(x.responseStatusCode, responseStatusCode), locale)));
            return ServiceUtil.returnFailure(errorList);
        }
    }

    public static Document createAccessRequestDocument(Delegator delegator, String shipmentGatewayConfigId,
                                                       String serviceConfigProps) {
        Document accessRequestDocument = UtilXml.makeEmptyXmlDocument(x.AccessRequest);
        Element accessRequestElement = accessRequestDocument.getDocumentElement();
        String accessLicenseNumber = getShipmentGatewayConfigValue(delegator, shipmentGatewayConfigId, x.accessLicenseNumber, serviceConfigProps,
                x.shipment_ups_access_license_number, x.emptyString);
        String userId = getShipmentGatewayConfigValue(delegator, shipmentGatewayConfigId, x.accessUserId, serviceConfigProps, x.shipment_ups_access
                + x.user_id, x.emptyString);
        String password = getShipmentGatewayConfigValue(delegator, shipmentGatewayConfigId, x.accessPassword, serviceConfigProps, x.shipment_ups
                + x.access_password, x.emptyString);
        UtilXml.addChildElementValue(accessRequestElement, x.AccessLicenseNumber, accessLicenseNumber, accessRequestDocument);
        UtilXml.addChildElementValue(accessRequestElement, x.UserId, userId, accessRequestDocument);
        UtilXml.addChildElementValue(accessRequestElement, x.Password, password, accessRequestDocument);
        return accessRequestDocument;
    }

    public static void handleErrors(Element responseElement, List<Object> errorList, Locale locale) {
        List<? extends Element> errorElements = UtilXml.childElementList(responseElement, x.Error);
        for (Element errorElement : errorElements) {
            StringBuilder errorMessageBuf = new StringBuilder();

            String errorSeverity = UtilXml.childElementValue(errorElement, x.ErrorSeverity);
            String errorCode = UtilXml.childElementValue(errorElement, x.ErrorCode);
            String errorDescription = UtilXml.childElementValue(errorElement, x.ErrorDescription);
            String minimumRetrySeconds = UtilXml.childElementValue(errorElement, x.MinimumRetrySeconds);

            errorMessageBuf.append(UtilProperties.getMessage(RES_ERROR, x.FacilityShipmentUpsErrorMessage,
                    UtilMisc.toMap(x.errorCode, errorCode, x.errorSeverity, errorSeverity, x.errorDescription, errorDescription), locale));
            if (UtilValidate.isNotEmpty(minimumRetrySeconds)) {
                errorMessageBuf.append(UtilProperties.getMessage(RES_ERROR, x.FacilityShipmentUpsErrorMessageMinimumRetrySeconds,
                        UtilMisc.toMap(x.minimumRetrySeconds, minimumRetrySeconds), locale));
            } else {
                errorMessageBuf.append(x.str_94c67da0);
            }

            List<? extends Element> errorLocationElements = UtilXml.childElementList(errorElement, x.ErrorLocation);
            for (Element errorLocationElement : errorLocationElements) {
                String errorLocationElementName = UtilXml.childElementValue(errorLocationElement, x.ErrorLocationElementName);
                String errorLocationAttributeName = UtilXml.childElementValue(errorLocationElement, x.ErrorLocationAttributeName);

                errorMessageBuf.append(UtilProperties.getMessage(RES_ERROR, x.FacilityShipmentUpsErrorWasAtElement,
                        UtilMisc.toMap(x.errorLocationElementName, errorLocationElementName), locale));

                if (UtilValidate.isNotEmpty(errorLocationAttributeName)) {
                    errorMessageBuf.append(UtilProperties.getMessage(RES_ERROR, x.FacilityShipmentUpsErrorWasAtElementAttribute,
                            UtilMisc.toMap(x.errorLocationAttributeName, errorLocationAttributeName), locale));
                }

                List<? extends Element> errorDigestElements = UtilXml.childElementList(errorLocationElement, x.ErrorDigest);
                for (Element errorDigestElement : errorDigestElements) {
                    errorMessageBuf.append(UtilProperties.getMessage(RES_ERROR, x.FacilityShipmentUpsErrorWasAtElementFullText,
                            UtilMisc.toMap(x.fullText, UtilXml.elementValue(errorDigestElement)), locale));
                }
            }

            errorList.add(errorMessageBuf.toString());
        }
    }

    /**
     * Opens a URL to UPS and makes a request.
     * @param upsService Name of the UPS service to invoke
     * @param xmlString  XML message to send
     * @return XML string response from UPS
     * @throws UpsConnectException
     */
    public static String sendUpsRequest(String upsService, String xmlString, String shipmentGatewayConfigId,
                                        String resource, Delegator delegator, Locale locale) throws UpsConnectException {

        // need a ups service to call
        if (upsService == null) {
            throw new UpsConnectException(UtilProperties.getMessage(RES_ERROR, x.FacilityShipmentUpsServiceNameCannotBeNull, locale));
        }

        // xmlString should contain the auth document at the beginning
        // all documents require an <?xml version="1.0"?> header
        if (xmlString == null) {
            throw new UpsConnectException(UtilProperties.getMessage(RES_ERROR, x.FacilityShipmentUpsXmlMessageCannotBeNull, locale));
        }

        String conStr = getShipmentGatewayConfigValue(delegator, shipmentGatewayConfigId, x.connectUrl, resource, x.shipment_ups_connect_url);
        if (UtilValidate.isEmpty(conStr)) {
            throw new UpsConnectException(UtilProperties.getMessage(RES_ERROR, x.FacilityShipmentUpsIncompleteConnectionURL, locale));
        }

        // prepare the connect string
        conStr = conStr.trim();
        if (!conStr.endsWith(x.str_42099b4a)) {
            conStr = conStr + x.str_42099b4a;
        }
        conStr = conStr + upsService;

        String timeOutStr = getShipmentGatewayConfigValue(delegator, shipmentGatewayConfigId, x.connectTimeout, resource, x.shipment_ups_connect
                + x.timeout_efe578ad, x._60);
        int timeout = 60;
        try {
            timeout = Integer.parseInt(timeOutStr);
        } catch (NumberFormatException e) {
            Debug.logError(e, x.Unable_to_set_timeout_to + timeOutStr + x.using_default + timeout);
        }

        HttpClient http = new HttpClient(conStr);
        http.setTimeout(timeout * 1000);
        http.setAllowUntrusted(true);
        String response = null;
        try {
            response = http.post(xmlString);
        } catch (HttpClientException e) {
            Debug.logError(e, x.Problem_connecting_with_UPS_server + conStr + x.str_4ff447b8, MODULE);
            throw new UpsConnectException(UtilProperties.getMessage(RES_ERROR, x.FacilityShipmentUpsURLConnectionProblem,
                    UtilMisc.toMap(x.exception, e), locale));
        }

        if (response == null) {
            throw new UpsConnectException(UtilProperties.getMessage(RES_ERROR, x.FacilityShipmentUpsReceivedNullResponse, locale));
        }
        if (Debug.verboseOn()) {
            Debug.logVerbose(x.UPS_Response + response, MODULE);
        }

        return response;
    }

    public static Map<String, Object> upsRateInquireByPostalCode(DispatchContext dctx, UpsServicesContext context) {
        Delegator delegator = dctx.getDelegator();
        Locale locale = (Locale) context.get(x.locale);
        // prepare the data
        String serviceConfigProps = (String) context.get(x.serviceConfigProps);
        String shipmentGatewayConfigId = (String) context.get(x.shipmentGatewayConfigId);
        String upsRateInquireMode = (String) context.get(x.upsRateInquireMode);
        String productStoreId = (String) context.get(x.productStoreId);
        String carrierRoleTypeId = (String) context.get(x.carrierRoleTypeId);
        String carrierPartyId = (String) context.get(x.carrierPartyId);
        String shipmentMethodTypeId = (String) context.get(x.shipmentMethodTypeId);
        String shippingPostalCode = (String) context.get(x.shippingPostalCode);
        String shippingCountryCode = (String) context.get(x.shippingCountryCode);
        List<BigDecimal> packageWeights = UtilGenerics.cast(context.get(x.packageWeights));
        List<Map<String, Object>> shippableItemInfo = UtilGenerics.cast(context.get(x.shippableItemInfo));
        String isResidentialAddress = (String) context.get(x.isResidentialAddress);

        // Important: DO NOT returnError here or you could trigger a transaction rollback and break other services.
        if (UtilValidate.isEmpty(shippingPostalCode)) {
            return ServiceUtil.returnFailure(UtilProperties.getMessage(RES_ERROR, x.FacilityShipmentUpsCannotRateEstimatePostalCodeMissing, locale));
        }

        if (serviceConfigProps == null) {
            serviceConfigProps = x.shipment_properties;
        }
        if (upsRateInquireMode == null || !x.Shop.equals(upsRateInquireMode)) {
            // can be either Rate || Shop
            Debug.logWarning(x.No_upsRateInquireMode_set_defaulting_to_Rate, MODULE);
            upsRateInquireMode = x.Rate;
        }

        // grab the pickup type; if none is defined we will assume daily pickup
        String pickupType = getShipmentGatewayConfigValue(delegator, shipmentGatewayConfigId, x.shipperPickupType, serviceConfigProps, x.shipment
                + x.ups_shipper_pickup_type, x._01);

        // if we're drop shipping from a supplier, then the address is given to us
        GenericValue shipFromAddress = (GenericValue) context.get(x.shipFromAddress);
        if (shipFromAddress == null) {

            // locate the ship-from address based on the product store's default facility
            GenericValue productStore = ProductStoreWorker.getProductStore(productStoreId, delegator);
            if (productStore != null && productStore.get(x.inventoryFacilityId) != null) {
                GenericValue facilityContactMech = ContactMechWorker.getFacilityContactMechByPurpose(delegator, productStore.getString(
                        x.inventoryFacilityId), UtilMisc.toList(x.SHIP_ORIG_LOCATION, x.PRIMARY_LOCATION));
                if (facilityContactMech != null) {
                    try {
                        PostalAddressDao postalAddressDao = DaoRegistry.getDao(delegator, x.PostalAddress, PostalAddressDao.class);
                        shipFromAddress = postalAddressDao.findOneByWhere(delegator, x.PostalAddress,
                                UtilMisc.toMap(x.contactMechId, facilityContactMech.getString(x.contactMechId)), null, null, false);
                    } catch (GenericEntityException e) {
                        Debug.logError(e, MODULE);
                    }
                }
            }
        }
        if (shipFromAddress == null) {
            return ServiceUtil.returnError(UtilProperties.getMessage(RES_ERROR, x.FacilityShipmentUnableFoundShipToAddresss, locale));
        }

        // locate the service code
        String serviceCode = null;
        if (!x.Shop.equals(upsRateInquireMode)) {
            // locate the CarrierShipmentMethod record
            GenericValue carrierShipmentMethod = null;
            try {
                CarrierShipmentMethodDao carrierShipmentMethodDao = DaoRegistry.getDao(delegator, x.CarrierShipmentMethod,
                        CarrierShipmentMethodDao.class);
                carrierShipmentMethod = carrierShipmentMethodDao.findOneByWhere(delegator, x.CarrierShipmentMethod,
                        UtilMisc.toMap(x.shipmentMethodTypeId, shipmentMethodTypeId, x.partyId, carrierPartyId, x.roleTypeId,
                                carrierRoleTypeId), null, null, false);
            } catch (GenericEntityException e) {
                Debug.logError(e, MODULE);
            }
            if (carrierShipmentMethod == null) {
                return ServiceUtil.returnError(UtilProperties.getMessage(RES_ERROR, x.FacilityShipmentUpsUnableToLocateShippingMethodRequested,
                        locale));
            }

            // service code is 'carrierServiceCode'
            serviceCode = carrierShipmentMethod.getString(x.carrierServiceCode);

        }

        // prepare the XML Document
        Document rateRequestDoc = UtilXml.makeEmptyXmlDocument(x.RatingServiceSelectionRequest);
        Element rateRequestElement = rateRequestDoc.getDocumentElement();
        rateRequestElement.setAttribute(x.xml_lang, x.en_US);

        // XML request header
        Element requestElement = UtilXml.addChildElement(rateRequestElement, x.Request, rateRequestDoc);
        Element transactionReferenceElement = UtilXml.addChildElement(requestElement, x.TransactionReference, rateRequestDoc);
        UtilXml.addChildElementValue(transactionReferenceElement, x.CustomerContext, x.Rating_and_Service, rateRequestDoc);
        UtilXml.addChildElementValue(transactionReferenceElement, x.XpciVersion, x._1_0001, rateRequestDoc);

        // RequestAction is always Rate, but RequestOption can be Rate to get a single rate or Shop for all shipping methods
        UtilXml.addChildElementValue(requestElement, x.RequestAction, x.Rate, rateRequestDoc);
        UtilXml.addChildElementValue(requestElement, x.RequestOption, upsRateInquireMode, rateRequestDoc);

        // set the pickup type
        Element pickupElement = UtilXml.addChildElement(rateRequestElement, x.PickupType, rateRequestDoc);
        UtilXml.addChildElementValue(pickupElement, x.Code, pickupType, rateRequestDoc);

        // shipment info
        Element shipmentElement = UtilXml.addChildElement(rateRequestElement, x.Shipment, rateRequestDoc);

        // shipper info - (sub of shipment)
        Element shipperElement = UtilXml.addChildElement(shipmentElement, x.Shipper, rateRequestDoc);
        Element shipperAddrElement = UtilXml.addChildElement(shipperElement, x.Address, rateRequestDoc);
        UtilXml.addChildElementValue(shipperAddrElement, x.PostalCode, shipFromAddress.getString(x.postalCode), rateRequestDoc);
        try {
            //If the warehouse you are shipping from its located in a country other than US, you need to supply its country code to UPS
            UtilXml.addChildElementValue(shipperAddrElement, x.CountryCode, shipFromAddress.getRelatedOne(x.CountryGeo, true).getString(x.geoCode),
                    rateRequestDoc);
        } catch (GenericEntityException e) {
            return ServiceUtil.returnError(e.getMessage());
        }

        // ship-to info - (sub of shipment)
        Element shiptoElement = UtilXml.addChildElement(shipmentElement, x.ShipTo, rateRequestDoc);
        Element shiptoAddrElement = UtilXml.addChildElement(shiptoElement, x.Address, rateRequestDoc);
        UtilXml.addChildElementValue(shiptoAddrElement, x.PostalCode, shippingPostalCode, rateRequestDoc);
        if (shippingCountryCode != null && !x.emptyString.equals(shippingCountryCode)) {
            UtilXml.addChildElementValue(shiptoAddrElement, x.CountryCode, shippingCountryCode, rateRequestDoc);
        }

        if (isResidentialAddress != null && x.Y.equals(isResidentialAddress)) {
            UtilXml.addChildElement(shiptoAddrElement, x.ResidentialAddress, rateRequestDoc);
        }
        // requested service (code) - not used when in Shop mode
        if (serviceCode != null) {
            Element serviceElement = UtilXml.addChildElement(shipmentElement, x.Service, rateRequestDoc);
            UtilXml.addChildElementValue(serviceElement, x.Code, serviceCode, rateRequestDoc);
        }

        // package info
        String maxWeightStr = getShipmentGatewayConfigValue(delegator, shipmentGatewayConfigId, x.maxEstimateWeight, serviceConfigProps, x.shipment
                + x.ups_max_estimate_weight, x._99);
        BigDecimal maxWeight;
        try {
            maxWeight = new BigDecimal(maxWeightStr);
        } catch (NumberFormatException e) {
            maxWeight = new BigDecimal(x._99);
        }
        String minWeightStr = getShipmentGatewayConfigValue(delegator, shipmentGatewayConfigId, x.minEstimateWeight, serviceConfigProps, x.shipment
                + x.ups_min_estimate_weight, x._1_09a9fb78);
        BigDecimal minWeight;
        try {
            minWeight = new BigDecimal(minWeightStr);
        } catch (NumberFormatException e) {
            minWeight = new BigDecimal(x._0_1);
        }

        // Passing in a list of package weights overrides the calculation of same via shippableItemInfo
        if (UtilValidate.isEmpty(packageWeights)) {
            String totalWeightStr = getShipmentGatewayConfigValue(delegator, shipmentGatewayConfigId, x.minEstimateWeight, serviceConfigProps,
                    x.shipment_ups_min_estimate_weight, x._1);
            splitEstimatePackages(dctx, rateRequestDoc, shipmentElement, shippableItemInfo, maxWeight, minWeight, totalWeightStr);
        } else {
            for (BigDecimal packageWeight : packageWeights) {
                addPackageElement(rateRequestDoc, shipmentElement, packageWeight);
            }
        }

        // service options
        UtilXml.addChildElement(shipmentElement, x.ShipmentServiceOptions, rateRequestDoc);

        String rateRequestString = null;
        try {
            rateRequestString = UtilXml.writeXmlDocument(rateRequestDoc);
        } catch (IOException e) {
            String ioeErrMsg = x.Error_writing_the_RatingServiceSelectionRequest_XML_Document_to_a_String + e.toString();
            Debug.logError(e, ioeErrMsg, MODULE);
            return ServiceUtil.returnFailure(UtilProperties.getMessage(RES_ERROR,
                    x.FacilityShipmentUpsErrorRatingServiceSelectionRequestXmlToString,
                    UtilMisc.toMap(x.errorString, e.toString()), locale));
        }

        // create AccessRequest XML doc
        Document accessRequestDocument = createAccessRequestDocument(delegator, shipmentGatewayConfigId, serviceConfigProps);
        String accessRequestString = null;
        try {
            accessRequestString = UtilXml.writeXmlDocument(accessRequestDocument);
        } catch (IOException e) {
            String ioeErrMsg = x.Error_writing_the_AccessRequest_XML_Document_to_a_String + e.toString();
            Debug.logError(e, ioeErrMsg, MODULE);
            return ServiceUtil.returnFailure(UtilProperties.getMessage(RES_ERROR, x.FacilityShipmentUpsErrorAccessRequestXmlToString,
                    UtilMisc.toMap(x.errorString, e.toString()), locale));
        }

        // prepare the access/inquire request string
        StringBuilder xmlString = new StringBuilder();
        xmlString.append(accessRequestString);
        xmlString.append(rateRequestString);
        if (Debug.verboseOn()) {
            Debug.logVerbose(xmlString.toString(), MODULE);
        }
        // send the request
        String rateResponseString = null;
        try {
            rateResponseString = sendUpsRequest(x.Rate, xmlString.toString(), shipmentGatewayConfigId, serviceConfigProps, delegator, locale);
        } catch (UpsConnectException e) {
            String uceErrMsg = x.Error_sending_UPS_request_for_UPS_Service_Rate + e.toString();
            Debug.logError(e, uceErrMsg, MODULE);
            return ServiceUtil.returnFailure(UtilProperties.getMessage(RES_ERROR, x.FacilityShipmentUpsErrorSendingRate,
                    UtilMisc.toMap(x.errorString, e.toString()), locale));
        }
        Debug.logVerbose(rateResponseString, MODULE);
        Document rateResponseDocument = null;
        try {
            rateResponseDocument = UtilXml.readXmlDocument(rateResponseString, false);
        } catch (SAXException | IOException | ParserConfigurationException e2) {
            String excErrMsg = x.Error_parsing_the_RatingServiceSelectionResponse + e2.toString();
            Debug.logError(e2, excErrMsg, MODULE);
            return ServiceUtil.returnFailure(UtilProperties.getMessage(RES_ERROR, x.FacilityShipmentUpsErrorParsingRatingServiceSelectionResponse,
                    UtilMisc.toMap(x.errorString, e2.toString()), locale));
        }
        return handleUpsRateInquireResponse(rateResponseDocument, locale);
    }

    public static Map<String, Object> upsAddressValidation(DispatchContext dctx, UpsServicesContext context) {
        Delegator delegator = dctx.getDelegator();
        Locale locale = (Locale) context.get(x.locale);
        String city = (String) context.get(x.city);
        String stateProvinceGeoId = (String) context.get(x.stateProvinceGeoId);
        String postalCode = (String) context.get(x.postalCode);

        String shipmentGatewayConfigId = (String) context.get(x.shipmentGatewayConfigId);
        String resource = (String) context.get(x.serviceConfigProps);

        if (UtilValidate.isEmpty(city) && UtilValidate.isEmpty(postalCode)) {
            return ServiceUtil.returnError(UtilProperties.getMessage(RES_ERROR,
                    x.FacilityShipmentUpsAddressValidationRequireCityOrPostalCode, locale));
        }

        // prepare the XML Document
        Document avRequestDoc = UtilXml.makeEmptyXmlDocument(x.AddressValidationRequest);
        Element avRequestElement = avRequestDoc.getDocumentElement();
        avRequestElement.setAttribute(x.xml_lang, x.en_US);

        // XML request header
        Element requestElement = UtilXml.addChildElement(avRequestElement, x.Request, avRequestDoc);
        Element transactionReferenceElement = UtilXml.addChildElement(requestElement, x.TransactionReference, avRequestDoc);
        UtilXml.addChildElementValue(transactionReferenceElement, x.CustomerContext, x.Rating_and_Service, avRequestDoc);
        UtilXml.addChildElementValue(transactionReferenceElement, x.XpciVersion, x._1_0001, avRequestDoc);
        UtilXml.addChildElementValue(requestElement, x.RequestAction, x.AV, avRequestDoc);

        // Address
        Element addressElement = UtilXml.addChildElement(avRequestElement, x.Address, avRequestDoc);
        if (UtilValidate.isNotEmpty(city)) {
            UtilXml.addChildElementValue(addressElement, x.City, city, avRequestDoc);
        }
        if (UtilValidate.isNotEmpty(stateProvinceGeoId)) {
            UtilXml.addChildElementValue(addressElement, x.StateProvinceCode, stateProvinceGeoId, avRequestDoc);
        }
        if (UtilValidate.isNotEmpty(postalCode)) {
            UtilXml.addChildElementValue(addressElement, x.PostalCode, postalCode, avRequestDoc);
        }

        String avRequestString = null;
        try {
            avRequestString = UtilXml.writeXmlDocument(avRequestDoc);
        } catch (IOException e) {
            String ioeErrMsg = x.Error_writing_the_AddressValidationRequest_XML_Document_to_a_String + e.toString();
            Debug.logError(e, ioeErrMsg, MODULE);
            return ServiceUtil.returnError(UtilProperties.getMessage(RES_ERROR,
                    x.FacilityShipmentUpsErrorAddressValidationRequestXmlToString,
                    UtilMisc.toMap(x.errorString, e.toString()), locale));
        }

        // create AccessRequest XML doc
        Document accessRequestDocument = createAccessRequestDocument(delegator, shipmentGatewayConfigId, resource);

        String accessRequestString = null;
        try {
            accessRequestString = UtilXml.writeXmlDocument(accessRequestDocument);
        } catch (IOException e) {
            String ioeErrMsg = x.Error_writing_the_AccessRequest_XML_Document_to_a_String + e.toString();
            Debug.logError(e, ioeErrMsg, MODULE);
            return ServiceUtil.returnError(UtilProperties.getMessage(RES_ERROR,
                    x.FacilityShipmentUpsErrorShipmentAcceptRequestXmlToString,
                    UtilMisc.toMap(x.errorString, e.toString()), locale));
        }

        // prepare the request string
        StringBuilder xmlString = new StringBuilder();
        xmlString.append(accessRequestString);
        xmlString.append(avRequestString);
        Debug.logInfo(xmlString.toString(), MODULE);

        // send the request
        String avResponseString = null;
        try {
            avResponseString = sendUpsRequest(x.AV, xmlString.toString(), shipmentGatewayConfigId, resource, delegator, locale);
        } catch (UpsConnectException e) {
            String uceErrMsg = x.Error_sending_UPS_request + e.toString();
            Debug.logError(e, uceErrMsg, MODULE);
            return ServiceUtil.returnError(UtilProperties.getMessage(RES_ERROR,
                    x.FacilityShipmentUpsErrorSendingAddressVerification,
                    UtilMisc.toMap(x.errorString, e.toString()), locale));
        }

        Debug.logInfo(avResponseString, MODULE);

        Document avResponseDocument = null;
        try {
            avResponseDocument = UtilXml.readXmlDocument(avResponseString, false);
        } catch (SAXException | IOException | ParserConfigurationException e2) {
            String excErrMsg = x.Error_parsing_the_UPS_response + e2.toString();
            Debug.logError(e2, excErrMsg, MODULE);
            return ServiceUtil.returnError(UtilProperties.getMessage(RES_ERROR,
                    x.FacilityShipmentUpsErrorParsingAddressVerificationResponse,
                    UtilMisc.toMap(x.errorString, e2.toString()), locale));
        }

        return handleUpsAddressValidationResponse(avResponseDocument, locale);

    }

    public static Map<String, Object> handleUpsAddressValidationResponse(Document rateResponseDocument, Locale locale) {
        Element avResponseElement = rateResponseDocument.getDocumentElement();
        Element responseElement = UtilXml.firstChildElement(avResponseElement, x.Response_6e617e4f);
        String responseStatusCode = UtilXml.childElementValue(responseElement, x.ResponseStatusCode);

        List<Object> errorList = new LinkedList<>();
        UpsServices.handleErrors(responseElement, errorList, locale);

        if (x._1.equals(responseStatusCode)) {
            List<Map<String, String>> matches = new LinkedList<>();

            List<? extends Element> avResultList = UtilXml.childElementList(avResponseElement, x.AddressValidationResult);
            // TODO: return error if there are no matches?
            if (UtilValidate.isNotEmpty(avResultList)) {
                for (Element avResultElement : avResultList) {
                    Map<String, String> match = new HashMap<>();

                    match.put(x.Rank, UtilXml.childElementValue(avResultElement, x.Rank));
                    match.put(x.Quality, UtilXml.childElementValue(avResultElement, x.Quality));

                    Element addressElement = UtilXml.firstChildElement(avResultElement, x.Address);
                    match.put(x.City, UtilXml.childElementValue(addressElement, x.City));
                    match.put(x.StateProvinceCode, UtilXml.childElementValue(addressElement, x.StateProvinceCode));

                    match.put(x.PostalCodeLowEnd, UtilXml.childElementValue(avResultElement, x.PostalCodeLowEnd));
                    match.put(x.PostalCodeHighEnd, UtilXml.childElementValue(avResultElement, x.PostalCodeHighEnd));

                    matches.add(match);
                }
            }

            Map<String, Object> result = ServiceUtil.returnSuccess();
            result.put(x.matches, matches);
            return result;
        } else {
            errorList.add(ServiceUtil.returnError(UtilProperties.getMessage(RES_ERROR, x.FacilityShipmentUpsErrorStatusCode,
                    UtilMisc.toMap(x.responseStatusCode, responseStatusCode), locale)));
            return ServiceUtil.returnError(errorList);
        }
    }

    public static Map<String, Object> upsEmailReturnLabel(DispatchContext dctx, UpsServicesContext context) {
        Delegator delegator = dctx.getDelegator();
        LocalDispatcher dispatcher = dctx.getDispatcher();
        String shipmentId = (String) context.get(x.shipmentId);
        String shipmentRouteSegmentId = (String) context.get(x.shipmentRouteSegmentId);
        Locale locale = (Locale) context.get(x.locale);
        GenericValue userLogin = (GenericValue) context.get(x.userLogin);

        Map<String, Object> shipmentGatewayConfig = ShipmentServices.getShipmentGatewayConfigFromShipment(delegator, shipmentId, locale);
        String shipmentGatewayConfigId = (String) shipmentGatewayConfig.get(x.shipmentGatewayConfigId);
        String resource = (String) shipmentGatewayConfig.get(x.configProps);
        if (UtilValidate.isEmpty(shipmentGatewayConfigId) && UtilValidate.isEmpty(resource)) {
            return ServiceUtil.returnError(UtilProperties.getMessage(RES_ERROR, x.FacilityShipmentUpsGatewayNotAvailable, locale));
        }
        boolean shipmentUpsSaveCertificationInfo = x._true.equals(getShipmentGatewayConfigValue(delegator, shipmentGatewayConfigId, x.saveCertInfo,
                resource, x.shipment_ups_save_certification_info, x._true));
        String shipmentUpsSaveCertificationPath = FlexibleStringExpander.expandString(getShipmentGatewayConfigValue(delegator,
                shipmentGatewayConfigId, x.saveCertPath, resource, x.shipment_ups_save_certification_path, x.emptyString), context);
        File shipmentUpsSaveCertificationFile = null;
        if (shipmentUpsSaveCertificationInfo) {
            shipmentUpsSaveCertificationFile = new File(shipmentUpsSaveCertificationPath);
            if (!shipmentUpsSaveCertificationFile.exists()) {
                shipmentUpsSaveCertificationFile.mkdirs();
            }
        }

        //Shipment Confirm request
        String shipmentConfirmResponseString = null;

        try {
            ShipmentDao shipmentDao = DaoRegistry.getDao(delegator, x.Shipment, ShipmentDao.class);
            ShipmentRouteSegmentDao shipmentRouteSegmentDao = DaoRegistry.getDao(delegator, x.ShipmentRouteSegment,
                    ShipmentRouteSegmentDao.class);
            CarrierShipmentMethodDao carrierShipmentMethodDao = DaoRegistry.getDao(delegator, x.CarrierShipmentMethod,
                    CarrierShipmentMethodDao.class);
            GenericValue shipment = shipmentDao.findOneByWhere(delegator, x.Shipment, UtilMisc.toMap(x.shipmentId, shipmentId), null, null,
                    false);
            if (shipment == null) {
                return ServiceUtil.returnError(UtilProperties.getMessage(RES_ERROR, x.ProductShipmentNotFoundId, locale) + x.str_b858cb28 + shipmentId);
            }
            GenericValue shipmentRouteSegment = shipmentRouteSegmentDao.findOneByWhere(delegator, x.ShipmentRouteSegment,
                    UtilMisc.toMap(x.shipmentId, shipmentId, x.shipmentRouteSegmentId, shipmentRouteSegmentId), null, null, false);
            if (shipmentRouteSegment == null) {
                return ServiceUtil.returnError(UtilProperties.getMessage(RES_ERROR, x.ProductShipmentRouteSegmentNotFound,
                        UtilMisc.toMap(x.shipmentId, shipmentId, x.shipmentRouteSegmentId, shipmentRouteSegmentId), locale));
            }

            if (!x.UPS.equals(shipmentRouteSegment.getString(x.carrierPartyId))) {
                return ServiceUtil.returnError(UtilProperties.getMessage(RES_ERROR, x.FacilityShipmentUpsNotRouteSegmentCarrier, UtilMisc.toMap(
                        x.shipmentRouteSegmentId, shipmentRouteSegmentId, x.shipmentId, shipmentId), locale));
            }

            // Get Origin Info
            GenericValue originPostalAddress = shipmentRouteSegment.getRelatedOne(x.OriginPostalAddress, false);
            if (originPostalAddress == null) {
                return ServiceUtil.returnError(UtilProperties.getMessage(RES_ERROR, x.FacilityShipmentUpsRouteSegmentOriginPostalAddressNotFound,
                        UtilMisc.toMap(x.shipmentId, shipmentId, x.shipmentRouteSegmentId, shipmentRouteSegmentId), locale));
            }
            GenericValue originTelecomNumber = shipmentRouteSegment.getRelatedOne(x.OriginTelecomNumber, false);
            if (originTelecomNumber == null) {
                return ServiceUtil.returnError(UtilProperties.getMessage(RES_ERROR, x.FacilityShipmentUpsRouteSegmentOriginTelecomNumberNotFound,
                        UtilMisc.toMap(x.shipmentId, shipmentId, x.shipmentRouteSegmentId, shipmentRouteSegmentId), locale));
            }
            String originPhoneNumber = originTelecomNumber.getString(x.areaCode) + originTelecomNumber.getString(x.contactNumber);
            // don't put on country code if not specified or is the US country code (UPS wants it this way)
            if (UtilValidate.isNotEmpty(originTelecomNumber.getString(x.countryCode))
                    && !x._001.equals(originTelecomNumber.getString(x.countryCode))) {
                originPhoneNumber = originTelecomNumber.getString(x.countryCode) + originPhoneNumber;
            }
            originPhoneNumber = StringUtil.replaceString(originPhoneNumber, x.str_3bc15c8a, x.emptyString);
            originPhoneNumber = StringUtil.replaceString(originPhoneNumber, x.str_b858cb28, x.emptyString);
            // lookup the two letter country code (in the geoCode field)
            GenericValue originCountryGeo = originPostalAddress.getRelatedOne(x.CountryGeo, false);
            if (originCountryGeo == null) {
                return ServiceUtil.returnError(UtilProperties.getMessage(RES_ERROR, x.FacilityShipmentUpsRouteSegmentOriginCountryGeoNotFound,
                        UtilMisc.toMap(x.shipmentId, shipmentId, x.shipmentRouteSegmentId, shipmentRouteSegmentId), locale));
            }

            // Get Dest Info
            GenericValue destPostalAddress = shipmentRouteSegment.getRelatedOne(x.DestPostalAddress, false);
            if (destPostalAddress == null) {
                return ServiceUtil.returnError(UtilProperties.getMessage(RES_ERROR, x.FacilityShipmentUpsRouteSegmentDestPostalAddressNotFound,
                        UtilMisc.toMap(x.shipmentId, shipmentId, x.shipmentRouteSegmentId, shipmentRouteSegmentId), locale));
            }

            GenericValue destTelecomNumber = shipmentRouteSegment.getRelatedOne(x.DestTelecomNumber, false);
            if (destTelecomNumber == null) {
                String missingErrMsg = x.DestTelecomNumber_not_found_for_ShipmentRouteSegment_with_shipmentId + shipmentId + x._and
                        + x.shipmentRouteSegmentId_59538687 + shipmentRouteSegmentId;
                Debug.logError(missingErrMsg, MODULE);
            }
            String destPhoneNumber = null;
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

            // lookup the two letter country code (in the geoCode field)
            GenericValue destCountryGeo = destPostalAddress.getRelatedOne(x.CountryGeo, false);
            if (destCountryGeo == null) {
                return ServiceUtil.returnError(UtilProperties.getMessage(RES_ERROR, x.FacilityShipmentUpsRouteSegmentDestCountryGeoNotFound,
                        UtilMisc.toMap(x.shipmentId, shipmentId, x.shipmentRouteSegmentId, shipmentRouteSegmentId), locale));
            }

            GenericValue carrierShipmentMethod = carrierShipmentMethodDao.findOneByWhere(delegator, x.CarrierShipmentMethod,
                    UtilMisc.toMap(x.partyId, shipmentRouteSegment.get(x.carrierPartyId), x.roleTypeId, x.CARRIER, x.shipmentMethodTypeId,
                            shipmentRouteSegment.get(x.shipmentMethodTypeId)), null, null, false);
            if (carrierShipmentMethod == null) {
                return ServiceUtil.returnError(UtilProperties.getMessage(RES_ERROR, x.FacilityShipmentUpsRouteSegmentCarrierShipmentMethodNotFound,
                        UtilMisc.toMap(x.shipmentId, shipmentId, x.shipmentRouteSegmentId, shipmentRouteSegmentId, x.carrierPartyId,
                                shipmentRouteSegment.get(x.carrierPartyId), x.shipmentMethodTypeId, shipmentRouteSegment.get(x.shipmentMethodTypeId)),
                        locale));
            }

            Map<String, Object> destEmail = dispatcher.runSync(x.getPartyEmail, UtilMisc.toMap(x.partyId, shipment.get(x.partyIdTo), x.userLogin,
                    userLogin));
            if (ServiceUtil.isError(destEmail)) {
                return ServiceUtil.returnError(ServiceUtil.getErrorMessage(destEmail));
            }
            String recipientEmail = null;
            if (UtilValidate.isNotEmpty(destEmail.get(x.emailAddress))) {
                recipientEmail = (String) destEmail.get(x.emailAddress);
            }
            String senderEmail = null;
            Map<String, Object> originEmail = dispatcher.runSync(x.getPartyEmail, UtilMisc.toMap(x.partyId, shipment.get(x.partyIdFrom),
                    x.userLogin, userLogin));
            if (ServiceUtil.isError(originEmail)) {
                return ServiceUtil.returnError(ServiceUtil.getErrorMessage(originEmail));
            }
            if (UtilValidate.isNotEmpty(originEmail.get(x.emailAddress))) {
                senderEmail = (String) originEmail.get(x.emailAddress);
            }

            List<GenericValue> shipmentPackageRouteSegs = shipmentRouteSegment.getRelated(x.ShipmentPackageRouteSeg, null, UtilMisc.toList(
                    x.shipmentPackageSeqId_39d5d38d), false);
            if (UtilValidate.isEmpty(shipmentPackageRouteSegs)) {
                return ServiceUtil.returnError(UtilProperties.getMessage(RES_ERROR, x.FacilityShipmentUpsPackageRouteSegsNotFound,
                        UtilMisc.toMap(x.shipmentId, shipmentId, x.shipmentRouteSegmentId, shipmentRouteSegmentId), locale));
            }

            // Okay, start putting the XML together...
            Document shipmentConfirmRequestDoc = UtilXml.makeEmptyXmlDocument(x.ShipmentConfirmRequest);
            Element shipmentConfirmRequestElement = shipmentConfirmRequestDoc.getDocumentElement();
            shipmentConfirmRequestElement.setAttribute(x.xml_lang, x.en_US);

            // Top Level Element: Request
            Element requestElement = UtilXml.addChildElement(shipmentConfirmRequestElement, x.Request, shipmentConfirmRequestDoc);

            UtilXml.addChildElementValue(requestElement, x.RequestAction, x.ShipConfirm, shipmentConfirmRequestDoc);
            UtilXml.addChildElementValue(requestElement, x.RequestOption, x.nonvalidate, shipmentConfirmRequestDoc);

            // Top Level Element: Shipment
            Element shipmentElement = UtilXml.addChildElement(shipmentConfirmRequestElement, x.Shipment, shipmentConfirmRequestDoc);
            UtilXml.addChildElementValue(shipmentElement, x.Description, x.Goods_for_Shipment + shipment.get(x.shipmentId),
                    shipmentConfirmRequestDoc);

            // Child of Shipment: ReturnService
            Element returnServiceElement = UtilXml.addChildElement(shipmentElement, x.ReturnService, shipmentConfirmRequestDoc);
            UtilXml.addChildElementValue(returnServiceElement, x.Code, String.valueOf(RET_SERVICE_CODE), shipmentConfirmRequestDoc);

            // Child of Shipment: Shipper
            String shipperNumber = getShipmentGatewayConfigValue(delegator, shipmentGatewayConfigId, x.shipperNumber, resource, x.shipment_ups
                    + x.shipper_number, x.emptyString);
            Element shipperElement = UtilXml.addChildElement(shipmentElement, x.Shipper, shipmentConfirmRequestDoc);
            UtilXml.addChildElementValue(shipperElement, x.Name, UtilValidate.isNotEmpty(originPostalAddress.getString(x.toName))
                    ? originPostalAddress.getString(x.toName) : x.emptyString, shipmentConfirmRequestDoc);
            UtilXml.addChildElementValue(shipperElement, x.AttentionName, UtilValidate.isNotEmpty(originPostalAddress.getString(x.attnName))
                    ? originPostalAddress.getString(x.attnName) : x.emptyString, shipmentConfirmRequestDoc);
            UtilXml.addChildElementValue(shipperElement, x.PhoneNumber, originPhoneNumber, shipmentConfirmRequestDoc);
            UtilXml.addChildElementValue(shipperElement, x.ShipperNumber, shipperNumber, shipmentConfirmRequestDoc);

            Element shipperAddressElement = UtilXml.addChildElement(shipperElement, x.Address, shipmentConfirmRequestDoc);
            UtilXml.addChildElementValue(shipperAddressElement, x.AddressLine1, originPostalAddress.getString(x.address1), shipmentConfirmRequestDoc);
            if (UtilValidate.isNotEmpty(originPostalAddress.getString(x.address2))) {
                UtilXml.addChildElementValue(shipperAddressElement, x.AddressLine2, originPostalAddress.getString(x.address2),
                        shipmentConfirmRequestDoc);
            }
            UtilXml.addChildElementValue(shipperAddressElement, x.City, originPostalAddress.getString(x.city), shipmentConfirmRequestDoc);
            UtilXml.addChildElementValue(shipperAddressElement, x.StateProvinceCode, originPostalAddress.getString(x.stateProvinceGeoId),
                    shipmentConfirmRequestDoc);
            UtilXml.addChildElementValue(shipperAddressElement, x.PostalCode, originPostalAddress.getString(x.postalCode), shipmentConfirmRequestDoc);
            UtilXml.addChildElementValue(shipperAddressElement, x.CountryCode, originCountryGeo.getString(x.geoCode), shipmentConfirmRequestDoc);

            // Child of Shipment: ShipTo
            Element shipToElement = UtilXml.addChildElement(shipmentElement, x.ShipTo, shipmentConfirmRequestDoc);
            UtilXml.addChildElementValue(shipToElement, x.CompanyName, UtilValidate.isNotEmpty(destPostalAddress.getString(x.toName))
                    ? destPostalAddress.getString(x.toName) : x.emptyString, shipmentConfirmRequestDoc);
            UtilXml.addChildElementValue(shipToElement, x.AttentionName, UtilValidate.isNotEmpty(destPostalAddress.getString(x.attnName))
                    ? destPostalAddress.getString(x.attnName) : x.emptyString, shipmentConfirmRequestDoc);
            if (UtilValidate.isNotEmpty(destPhoneNumber)) {
                UtilXml.addChildElementValue(shipToElement, x.PhoneNumber, destPhoneNumber, shipmentConfirmRequestDoc);
            }
            Element shipToAddressElement = UtilXml.addChildElement(shipToElement, x.Address, shipmentConfirmRequestDoc);
            UtilXml.addChildElementValue(shipToAddressElement, x.AddressLine1, destPostalAddress.getString(x.address1), shipmentConfirmRequestDoc);
            if (UtilValidate.isNotEmpty(destPostalAddress.getString(x.address2))) {
                UtilXml.addChildElementValue(shipToAddressElement, x.AddressLine2, destPostalAddress.getString(x.address2),
                        shipmentConfirmRequestDoc);
            }
            UtilXml.addChildElementValue(shipToAddressElement, x.City, destPostalAddress.getString(x.city), shipmentConfirmRequestDoc);
            UtilXml.addChildElementValue(shipToAddressElement, x.StateProvinceCode, destPostalAddress.getString(x.stateProvinceGeoId),
                    shipmentConfirmRequestDoc);
            UtilXml.addChildElementValue(shipToAddressElement, x.PostalCode, destPostalAddress.getString(x.postalCode), shipmentConfirmRequestDoc);
            UtilXml.addChildElementValue(shipToAddressElement, x.CountryCode, destCountryGeo.getString(x.geoCode), shipmentConfirmRequestDoc);
            if (UtilValidate.isNotEmpty(shipmentRouteSegment.getString(x.homeDeliveryType))) {
                UtilXml.addChildElement(shipToAddressElement, x.ResidentialAddress, shipmentConfirmRequestDoc);
            }

            // Child of Shipment: ShipFrom
            Element shipFromElement = UtilXml.addChildElement(shipmentElement, x.ShipFrom, shipmentConfirmRequestDoc);
            UtilXml.addChildElementValue(shipFromElement, x.CompanyName, UtilValidate.isNotEmpty(originPostalAddress.getString(x.toName))
                    ? originPostalAddress.getString(x.toName) : x.emptyString, shipmentConfirmRequestDoc);
            UtilXml.addChildElementValue(shipFromElement, x.AttentionName, UtilValidate.isNotEmpty(originPostalAddress.getString(x.attnName))
                    ? originPostalAddress.getString(x.attnName) : x.emptyString, shipmentConfirmRequestDoc);
            UtilXml.addChildElementValue(shipFromElement, x.PhoneNumber, originPhoneNumber, shipmentConfirmRequestDoc);
            Element shipFromAddressElement = UtilXml.addChildElement(shipFromElement, x.Address, shipmentConfirmRequestDoc);
            UtilXml.addChildElementValue(shipFromAddressElement, x.AddressLine1, originPostalAddress.getString(x.address1),
                    shipmentConfirmRequestDoc);
            if (UtilValidate.isNotEmpty(originPostalAddress.getString(x.address2))) {
                UtilXml.addChildElementValue(shipFromAddressElement, x.AddressLine2, originPostalAddress.getString(x.address2),
                        shipmentConfirmRequestDoc);
            }
            UtilXml.addChildElementValue(shipFromAddressElement, x.City, originPostalAddress.getString(x.city), shipmentConfirmRequestDoc);
            UtilXml.addChildElementValue(shipFromAddressElement, x.StateProvinceCode, originPostalAddress.getString(x.stateProvinceGeoId),
                    shipmentConfirmRequestDoc);
            UtilXml.addChildElementValue(shipFromAddressElement, x.PostalCode, originPostalAddress.getString(x.postalCode),
                    shipmentConfirmRequestDoc);
            UtilXml.addChildElementValue(shipFromAddressElement, x.CountryCode, originCountryGeo.getString(x.geoCode), shipmentConfirmRequestDoc);

            // Child of Shipment: PaymentInformation
            Element paymentInformationElement = UtilXml.addChildElement(shipmentElement, x.PaymentInformation, shipmentConfirmRequestDoc);

            String thirdPartyAccountNumber = shipmentRouteSegment.getString(x.thirdPartyAccountNumber);

            if (UtilValidate.isEmpty(thirdPartyAccountNumber)) {
                // Paid by shipper
                Element prepaidElement = UtilXml.addChildElement(paymentInformationElement, x.Prepaid, shipmentConfirmRequestDoc);
                Element billShipperElement = UtilXml.addChildElement(prepaidElement, x.BillShipper, shipmentConfirmRequestDoc);

                // fill in BillShipper AccountNumber element from properties file
                String billShipperAccountNumber = getShipmentGatewayConfigValue(delegator, shipmentGatewayConfigId, x.billShipperAccountNumber,
                        resource, x.shipment_ups_bill_shipper_account_number, x.emptyString);
                UtilXml.addChildElementValue(billShipperElement, x.AccountNumber, billShipperAccountNumber, shipmentConfirmRequestDoc);
            }

            // Child of Shipment: Service
            Element serviceElement = UtilXml.addChildElement(shipmentElement, x.Service, shipmentConfirmRequestDoc);
            UtilXml.addChildElementValue(serviceElement, x.Code, carrierShipmentMethod.getString(x.carrierServiceCode), shipmentConfirmRequestDoc);

            // Child of Shipment: ShipmentServiceOptions
            String defaultReturnLabelMemo = getShipmentGatewayConfigValue(delegator, shipmentGatewayConfigId, x.defaultReturnLabelMemo, resource,
                    x.shipment_ups_default_returnLabel_memo, x.emptyString);
            String defaultReturnLabelSubject = getShipmentGatewayConfigValue(delegator, shipmentGatewayConfigId, x.defaultReturnLabelSubject,
                    resource, x.shipment_ups_default_returnLabel_subject, x.emptyString);
            Element shipmentServiceOptionsElement = UtilXml.addChildElement(shipmentElement, x.ShipmentServiceOptions, shipmentConfirmRequestDoc);
            Element labelDeliveryElement = UtilXml.addChildElement(shipmentServiceOptionsElement, x.LabelDelivery, shipmentConfirmRequestDoc);
            Element emailMessageElement = UtilXml.addChildElement(labelDeliveryElement, x.EMailMessage, shipmentConfirmRequestDoc);
            UtilXml.addChildElementValue(emailMessageElement, x.EMailAddress, recipientEmail, shipmentConfirmRequestDoc);
            UtilXml.addChildElementValue(emailMessageElement, x.FromEMailAddress, senderEmail, shipmentConfirmRequestDoc);
            UtilXml.addChildElementValue(emailMessageElement, x.FromName, UtilValidate.isNotEmpty(originPostalAddress.getString(x.attnName))
                    ? originPostalAddress.getString(x.attnName) : x.emptyString, shipmentConfirmRequestDoc);
            UtilXml.addChildElementValue(emailMessageElement, x.Memo, defaultReturnLabelMemo, shipmentConfirmRequestDoc);
            UtilXml.addChildElementValue(emailMessageElement, x.Subject, defaultReturnLabelSubject, shipmentConfirmRequestDoc);

            // Child of Shipment: Package
            Element packageElement = UtilXml.addChildElement(shipmentElement, x.Package_7431e3df, shipmentConfirmRequestDoc);
            Element packagingTypeElement = UtilXml.addChildElement(packageElement, x.PackagingType, shipmentConfirmRequestDoc);
            UtilXml.addChildElementValue(packagingTypeElement, x.Code, x._02, shipmentConfirmRequestDoc);
            UtilXml.addChildElementValue(packageElement, x.Description, x.Package_Description, shipmentConfirmRequestDoc);
            Element packageWeightElement = UtilXml.addChildElement(packageElement, x.PackageWeight, shipmentConfirmRequestDoc);
            Element packageWeightUnitOfMeasurementElement = UtilXml.addChildElement(packageElement, x.UnitOfMeasurement, shipmentConfirmRequestDoc);
            UtilXml.addChildElementValue(packageWeightUnitOfMeasurementElement, x.Code, x.LBS, shipmentConfirmRequestDoc);
            UtilXml.addChildElementValue(packageWeightElement, x.Weight_69c0b815, EntityUtilProperties.getPropertyValue(x.shipment, x.shipment_default_weight
                    + x.value_f7bb4c48, delegator), shipmentConfirmRequestDoc);

            String shipmentConfirmRequestString = null;
            try {
                shipmentConfirmRequestString = UtilXml.writeXmlDocument(shipmentConfirmRequestDoc);
            } catch (IOException e) {
                String ioeErrMsg = x.Error_writing_the_ShipmentConfirmRequest_XML_Document_to_a_String + e.toString();
                Debug.logError(e, ioeErrMsg, MODULE);
                return ServiceUtil.returnError(UtilProperties.getMessage(RES_ERROR, x.FacilityShipmentUpsErrorShipmentConfirmRequestXmlToString,
                        UtilMisc.toMap(x.errorString, e.toString()), locale));
            }

            // create AccessRequest XML doc
            Document accessRequestDocument = createAccessRequestDocument(delegator, shipmentGatewayConfigId, resource);
            String accessRequestString = null;
            try {
                accessRequestString = UtilXml.writeXmlDocument(accessRequestDocument);
            } catch (IOException e) {
                String ioeErrMsg = x.Error_writing_the_AccessRequest_XML_Document_to_a_String + e.toString();
                Debug.logError(e, ioeErrMsg, MODULE);
                return ServiceUtil.returnError(UtilProperties.getMessage(RES_ERROR, x.FacilityShipmentUpsErrorAccessRequestXmlToString,
                        UtilMisc.toMap(x.errorString, e.toString()), locale));
            }

            // connect to UPS server, send AccessRequest to auth
            // send ShipmentConfirmRequest String
            // get ShipmentConfirmResponse String back
            StringBuilder xmlString = new StringBuilder();
            xmlString.append(accessRequestString);
            xmlString.append(shipmentConfirmRequestString);
            try {
                shipmentConfirmResponseString = sendUpsRequest(x.ShipConfirm, xmlString.toString(), shipmentGatewayConfigId, resource, delegator,
                        locale);
            } catch (UpsConnectException e) {
                String uceErrMsg = x.Error_sending_UPS_request_for_UPS_Service_ShipConfirm + e.toString();
                Debug.logError(e, uceErrMsg, MODULE);
                return ServiceUtil.returnError(UtilProperties.getMessage(RES_ERROR, x.FacilityShipmentUpsErrorSendingShipConfirm,
                        UtilMisc.toMap(x.errorString, e.toString()), locale));
            }

            Document shipmentConfirmResponseDocument = null;
            try {
                shipmentConfirmResponseDocument = UtilXml.readXmlDocument(shipmentConfirmResponseString, false);
            } catch (SAXException | IOException | ParserConfigurationException e2) {
                String excErrMsg = x.Error_parsing_the_ShipmentConfirmResponse + e2.toString();
                Debug.logError(e2, excErrMsg, MODULE);
                return ServiceUtil.returnError(UtilProperties.getMessage(RES_ERROR, x.FacilityShipmentUpsErrorParsingShipmentConfirm,
                        UtilMisc.toMap(x.errorString, e2.toString()), locale));
            }
            Element shipmentConfirmResponseElement = shipmentConfirmResponseDocument.getDocumentElement();
            // handle Response element info
            Element responseElement = UtilXml.firstChildElement(shipmentConfirmResponseElement, x.Response_6e617e4f);
            String responseStatusCode = UtilXml.childElementValue(responseElement, x.ResponseStatusCode);
            List<Object> errorList = new LinkedList<>();
            UpsServices.handleErrors(responseElement, errorList, locale);
            if (!x._1.equals(responseStatusCode)) {
                errorList.add(0, UtilProperties.getMessage(RES_ERROR, x.FacilityShipmentUpsShipmentConfirmFailedForReturnShippingLabel, locale));
                return ServiceUtil.returnError(errorList);
            }

            //Shipment Accept Request follows
            if (!x.UPS.equals(shipmentRouteSegment.getString(x.carrierPartyId))) {
                return ServiceUtil.returnError(UtilProperties.getMessage(RES_ERROR, x.FacilityShipmentUpsNotRouteSegmentCarrier, UtilMisc.toMap(
                        x.shipmentRouteSegmentId, shipmentRouteSegmentId, x.shipmentId, shipmentId), locale));
            }

            if (UtilValidate.isEmpty(shipmentPackageRouteSegs)) {
                return ServiceUtil.returnError(UtilProperties.getMessage(RES_ERROR, x.FacilityShipmentUpsPackageRouteSegsNotFound,
                        UtilMisc.toMap(x.shipmentId, shipmentId, x.shipmentRouteSegmentId, shipmentRouteSegmentId), locale));
            }

            String shipmentDigest = UtilXml.childElementValue(shipmentConfirmResponseElement, x.ShipmentDigest);
            if (UtilValidate.isEmpty(shipmentDigest)) {
                return ServiceUtil.returnError(UtilProperties.getMessage(RES_ERROR,
                        x.FacilityShipmentUpsTrackingDigestWasNotSet, locale));
            }

            Document shipmentAcceptRequestDoc = UtilXml.makeEmptyXmlDocument(x.ShipmentAcceptRequest);
            Element shipmentAcceptRequestElement = shipmentAcceptRequestDoc.getDocumentElement();
            shipmentAcceptRequestElement.setAttribute(x.xml_lang, x.en_US);

            // Top Level Element: Request
            Element acceptRequestElement = UtilXml.addChildElement(shipmentAcceptRequestElement, x.Request, shipmentAcceptRequestDoc);

            Element acceptTransactionReferenceElement = UtilXml.addChildElement(acceptRequestElement, x.TransactionReference,
                    shipmentAcceptRequestDoc);
            UtilXml.addChildElementValue(acceptTransactionReferenceElement, x.CustomerContext, x.ShipAccept_01, shipmentAcceptRequestDoc);
            UtilXml.addChildElementValue(acceptTransactionReferenceElement, x.XpciVersion, x._1_0001, shipmentAcceptRequestDoc);

            UtilXml.addChildElementValue(acceptRequestElement, x.RequestAction, x.ShipAccept, shipmentAcceptRequestDoc);
            UtilXml.addChildElementValue(acceptRequestElement, x.RequestOption, x._01, shipmentAcceptRequestDoc);

            UtilXml.addChildElementValue(shipmentAcceptRequestElement, x.ShipmentDigest, shipmentDigest, shipmentAcceptRequestDoc);

            String shipmentAcceptRequestString = null;
            try {
                shipmentAcceptRequestString = UtilXml.writeXmlDocument(shipmentAcceptRequestDoc);
            } catch (IOException e) {
                String ioeErrMsg = x.Error_writing_the_ShipmentAcceptRequest_XML_Document_to_a_String + e.toString();
                Debug.logError(e, ioeErrMsg, MODULE);
                return ServiceUtil.returnError(UtilProperties.getMessage(RES_ERROR,
                        x.FacilityShipmentUpsErrorShipmentAcceptRequestXmlToString,
                        UtilMisc.toMap(x.errorString, e.toString()), locale));
            }

            // create AccessRequest XML doc
            Document acceptAccessRequestDocument = createAccessRequestDocument(delegator, shipmentGatewayConfigId, resource);
            String acceptAccessRequestString = null;
            try {
                acceptAccessRequestString = UtilXml.writeXmlDocument(acceptAccessRequestDocument);
            } catch (IOException e) {
                String ioeErrMsg = x.Error_writing_the_AccessRequest_XML_Document_to_a_String + e.toString();
                Debug.logError(e, ioeErrMsg, MODULE);
                return ServiceUtil.returnError(UtilProperties.getMessage(RES_ERROR,
                        x.FacilityShipmentUpsErrorAccessRequestXmlToString,
                        UtilMisc.toMap(x.errorString, e.toString()), locale));
            }

            // connect to UPS server, send AccessRequest to auth
            StringBuilder acceptXmlString = new StringBuilder();
            acceptXmlString.append(acceptAccessRequestString);
            acceptXmlString.append(shipmentAcceptRequestString);

            if (shipmentUpsSaveCertificationInfo) {
                String outFileName =
                        shipmentUpsSaveCertificationPath + x.UpsShipmentAcceptRequest + shipmentId + x.str_53a0acfa + shipmentRouteSegment.getString(
                         x.shipmentRouteSegmentId) + x.xml_657e4752;
                try (FileOutputStream fileOut = new FileOutputStream(outFileName)) {
                    fileOut.write(xmlString.toString().getBytes(StandardCharsets.UTF_8));
                    fileOut.flush();
                } catch (IOException e) {
                    Debug.logInfo(e, x.Could_not_save_UPS_XML_file + xmlString.toString() + x.to_file + outFileName, MODULE);
                }
            }
            try {
                sendUpsRequest(x.ShipAccept, acceptXmlString.toString(), shipmentGatewayConfigId, resource, delegator, locale);
            } catch (UpsConnectException e) {
                String uceErrMsg = x.Error_sending_UPS_request_for_UPS_Service_ShipAccept + e.toString();
                Debug.logError(e, uceErrMsg, MODULE);
                return ServiceUtil.returnError(UtilProperties.getMessage(RES_ERROR, x.FacilityShipmentUpsErrorSendingShipAccept,
                        UtilMisc.toMap(x.errorString, e.toString()), locale));
            }
        } catch (GenericEntityException e) {
            Debug.logError(e, MODULE);
            return ServiceUtil.returnError(UtilProperties.getMessage(RES_ERROR, x.FacilityShipmentUpsErrorDataShipmentAccept,
                    UtilMisc.toMap(x.errorString, e.toString()), locale));
        } catch (GenericServiceException e) {
            Debug.logError(e, MODULE);
            return ServiceUtil.returnError(UtilProperties.getMessage(RES_ERROR, x.FacilityShipmentUpsErrorDataShipmentConfirm,
                    UtilMisc.toMap(x.errorString, e.toString()), locale));
        }
        return ServiceUtil.returnSuccess(UtilProperties.getMessage(x.OrderUiLabels, x.OrderReturnLabelEmailSuccessful, locale));
    }

    public static Map<String, Object> upsShipmentAlternateRatesInquiry(DispatchContext dctx, UpsServicesContext context) {
        Delegator delegator = dctx.getDelegator();

        // prepare the data
        String upsRateInquireMode;
        String shipmentId = (String) context.get(x.shipmentId);
        String shipmentRouteSegmentId = (String) context.get(x.shipmentRouteSegmentId);
        Locale locale = (Locale) context.get(x.locale);
        String rateResponseString = null;
        String productStoreId = (String) context.get(x.productStoreId);
        List<Map<String, Object>> shippingRates = new LinkedList<>();
        GenericValue shipmentRouteSegment = null;
        Map<String, Object> shipmentGatewayConfig = ShipmentServices.getShipmentGatewayConfigFromShipment(delegator, shipmentId, locale);
        String shipmentGatewayConfigId = (String) shipmentGatewayConfig.get(x.shipmentGatewayConfigId);
        String resource = (String) shipmentGatewayConfig.get(x.configProps);
        if (UtilValidate.isEmpty(shipmentGatewayConfigId) && UtilValidate.isEmpty(resource)) {
            return ServiceUtil.returnError(UtilProperties.getMessage(RES_ERROR, x.FacilityShipmentUpsGatewayNotAvailable, locale));
        }

        try {
            ShipmentRouteSegmentDao shipmentRouteSegmentDao = DaoRegistry.getDao(delegator, x.ShipmentRouteSegment,
                    ShipmentRouteSegmentDao.class);
            if (shipmentRouteSegmentId != null) {
                shipmentRouteSegment = shipmentRouteSegmentDao.findOneByWhere(delegator, x.ShipmentRouteSegment,
                        UtilMisc.toMap(x.shipmentId, shipmentId, x.shipmentRouteSegmentId, shipmentRouteSegmentId), null, null, false);
            } else {
                shipmentRouteSegment = shipmentRouteSegmentDao.findFirstByWhere(delegator, x.ShipmentRouteSegment,
                        EntityCondition.makeCondition(x.shipmentId, EntityOperator.EQUALS, shipmentId), null, null, false);
            }

            if (shipmentRouteSegment == null) {
                return ServiceUtil.returnError(UtilProperties.getMessage(RES_ERROR, x.ProductShipmentRouteSegmentNotFound,
                        UtilMisc.toMap(x.shipmentId, shipmentId, x.shipmentRouteSegmentId, shipmentRouteSegmentId), locale));
            }
            shipmentRouteSegmentId = shipmentRouteSegment.getString(x.shipmentRouteSegmentId);

            if (!x.UPS.equals(shipmentRouteSegment.getString(x.carrierPartyId))) {
                return ServiceUtil.returnError(UtilProperties.getMessage(RES_ERROR, x.FacilityShipmentUpsNotRouteSegmentCarrier, UtilMisc.toMap(
                        x.shipmentRouteSegmentId, shipmentRouteSegmentId, x.shipmentId, shipmentId), locale));
            }

            // Get Origin Info
            GenericValue originPostalAddress = shipmentRouteSegment.getRelatedOne(x.OriginPostalAddress, false);
            if (originPostalAddress == null) {
                return ServiceUtil.returnError(UtilProperties.getMessage(RES_ERROR, x.FacilityShipmentUpsRouteSegmentOriginPostalAddressNotFound,
                        UtilMisc.toMap(x.shipmentId, shipmentId, x.shipmentRouteSegmentId, shipmentRouteSegmentId), locale));
            }
            GenericValue originTelecomNumber = shipmentRouteSegment.getRelatedOne(x.OriginTelecomNumber, false);
            if (originTelecomNumber == null) {
                return ServiceUtil.returnError(UtilProperties.getMessage(RES_ERROR, x.FacilityShipmentUpsRouteSegmentOriginTelecomNumberNotFound,
                        UtilMisc.toMap(x.shipmentId, shipmentId, x.shipmentRouteSegmentId, shipmentRouteSegmentId), locale));
            }
            String originPhoneNumber = originTelecomNumber.getString(x.areaCode) + originTelecomNumber.getString(x.contactNumber);
            // don't put on country code if not specified or is the US country code (UPS wants it this way)
            if (UtilValidate.isNotEmpty(originTelecomNumber.getString(x.countryCode))
                    && !x._001.equals(originTelecomNumber.getString(x.countryCode))) {
                originPhoneNumber = originTelecomNumber.getString(x.countryCode) + originPhoneNumber;
            }
            originPhoneNumber = StringUtil.replaceString(originPhoneNumber, x.str_3bc15c8a, x.emptyString);
            originPhoneNumber = StringUtil.replaceString(originPhoneNumber, x.str_b858cb28, x.emptyString);
            // lookup the two letter country code (in the geoCode field)
            GenericValue originCountryGeo = originPostalAddress.getRelatedOne(x.CountryGeo, false);
            if (originCountryGeo == null) {
                return ServiceUtil.returnError(UtilProperties.getMessage(RES_ERROR, x.FacilityShipmentUpsRouteSegmentOriginCountryGeoNotFound,
                        UtilMisc.toMap(x.shipmentId, shipmentId, x.shipmentRouteSegmentId, shipmentRouteSegmentId), locale));
            }

            // Get Dest Info
            GenericValue destPostalAddress = shipmentRouteSegment.getRelatedOne(x.DestPostalAddress, false);
            if (destPostalAddress == null) {
                return ServiceUtil.returnError(UtilProperties.getMessage(RES_ERROR, x.FacilityShipmentUpsRouteSegmentDestPostalAddressNotFound,
                        UtilMisc.toMap(x.shipmentId, shipmentId, x.shipmentRouteSegmentId, shipmentRouteSegmentId), locale));
            }

            GenericValue destTelecomNumber = shipmentRouteSegment.getRelatedOne(x.DestTelecomNumber, false);
            if (destTelecomNumber == null) {
                String missingErrMsg = x.DestTelecomNumber_not_found_for_ShipmentRouteSegment_with_shipmentId + shipmentId + x._and
                        + x.shipmentRouteSegmentId_59538687 + shipmentRouteSegmentId;
                Debug.logError(missingErrMsg, MODULE);
                // for now we won't require the dest phone number, but is it required?
            }
            String destPhoneNumber = null;
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

            // lookup the two letter country code (in the geoCode field)
            GenericValue destCountryGeo = destPostalAddress.getRelatedOne(x.CountryGeo, false);
            if (destCountryGeo == null) {
                return ServiceUtil.returnError(UtilProperties.getMessage(RES_ERROR, x.FacilityShipmentUpsRouteSegmentDestCountryGeoNotFound,
                        UtilMisc.toMap(x.shipmentId, shipmentId, x.shipmentRouteSegmentId, shipmentRouteSegmentId), locale));
            }

            // grab the pickup type; if none is defined we will assume daily pickup
            String pickupType = getShipmentGatewayConfigValue(delegator, shipmentGatewayConfigId, x.shipperPickupType, resource, x.shipment_ups
                    + x.shipper_pickup_type, x._01);

            // grab the customer classification; if none is defined we will assume daily pickup
            String customerClassification = getShipmentGatewayConfigValue(delegator, shipmentGatewayConfigId, x.customerClassification, resource,
                    x.shipment_ups_customerclassification, x._01);

            // should be shop to get estimates for all the possible shipping method of UPS
            upsRateInquireMode = x.Shop;

            // prepare the XML Document
            Document rateRequestDoc = UtilXml.makeEmptyXmlDocument(x.RatingServiceSelectionRequest);
            Element rateRequestElement = rateRequestDoc.getDocumentElement();
            rateRequestElement.setAttribute(x.xml_lang, x.en_US);

            // XML request header
            Element requestElement = UtilXml.addChildElement(rateRequestElement, x.Request, rateRequestDoc);
            Element transactionReferenceElement = UtilXml.addChildElement(requestElement, x.TransactionReference, rateRequestDoc);
            UtilXml.addChildElementValue(transactionReferenceElement, x.CustomerContext, x.Rating_and_Service, rateRequestDoc);
            UtilXml.addChildElementValue(transactionReferenceElement, x.XpciVersion, x._1_0001, rateRequestDoc);

            // RequestAction is always Rate, but RequestOption can be Rate to get a single rate or Shop for all shipping methods
            UtilXml.addChildElementValue(requestElement, x.RequestAction, x.Rate, rateRequestDoc);
            UtilXml.addChildElementValue(requestElement, x.RequestOption, upsRateInquireMode, rateRequestDoc);

            // set the pickup type
            Element pickupElement = UtilXml.addChildElement(rateRequestElement, x.PickupType, rateRequestDoc);
            UtilXml.addChildElementValue(pickupElement, x.Code, pickupType, rateRequestDoc);

            Element customerClassificationElement = UtilXml.addChildElement(rateRequestElement, x.CustomerClassification, rateRequestDoc);
            UtilXml.addChildElementValue(customerClassificationElement, x.Code, customerClassification, rateRequestDoc);

            // shipment info
            Element shipmentElement = UtilXml.addChildElement(rateRequestElement, x.Shipment, rateRequestDoc);
            Element shipperElement = UtilXml.addChildElement(shipmentElement, x.Shipper, rateRequestDoc);
            UtilXml.addChildElementValue(shipperElement, x.Name, UtilValidate.isNotEmpty(originPostalAddress.getString(x.toName))
                    ? originPostalAddress.getString(x.toName) : x.emptyString, rateRequestDoc);
            UtilXml.addChildElementValue(shipperElement, x.AttentionName, UtilValidate.isNotEmpty(originPostalAddress.getString(x.attnName))
                    ? originPostalAddress.getString(x.attnName) : x.emptyString, rateRequestDoc);
            UtilXml.addChildElementValue(shipperElement, x.PhoneNumber, originPhoneNumber, rateRequestDoc);
            UtilXml.addChildElementValue(shipperElement, x.ShipperNumber, EntityUtilProperties.getPropertyValue(x.shipment, x.shipment_ups_shipper
                    + x.number, delegator), rateRequestDoc);

            Element shipperAddressElement = UtilXml.addChildElement(shipperElement, x.Address, rateRequestDoc);
            UtilXml.addChildElementValue(shipperAddressElement, x.AddressLine1, originPostalAddress.getString(x.address1), rateRequestDoc);
            if (UtilValidate.isNotEmpty(originPostalAddress.getString(x.address2))) {
                UtilXml.addChildElementValue(shipperAddressElement, x.AddressLine2, originPostalAddress.getString(x.address2), rateRequestDoc);
            }

            UtilXml.addChildElementValue(shipperAddressElement, x.City, originPostalAddress.getString(x.city), rateRequestDoc);
            UtilXml.addChildElementValue(shipperAddressElement, x.StateProvinceCode, originPostalAddress.getString(x.stateProvinceGeoId),
                    rateRequestDoc);
            UtilXml.addChildElementValue(shipperAddressElement, x.PostalCode, originPostalAddress.getString(x.postalCode), rateRequestDoc);
            UtilXml.addChildElementValue(shipperAddressElement, x.CountryCode, originCountryGeo.getString(x.geoCode), rateRequestDoc);

            // Child of Shipment: ShipTo
            Element shipToElement = UtilXml.addChildElement(shipmentElement, x.ShipTo, rateRequestDoc);
            UtilXml.addChildElementValue(shipToElement, x.CompanyName, UtilValidate.isNotEmpty(destPostalAddress.getString(x.toName))
                    ? destPostalAddress.getString(x.toName) : x.emptyString, rateRequestDoc);
            UtilXml.addChildElementValue(shipToElement, x.AttentionName, UtilValidate.isNotEmpty(destPostalAddress.getString(x.attnName))
                    ? destPostalAddress.getString(x.attnName) : x.emptyString, rateRequestDoc);
            if (UtilValidate.isNotEmpty(destPhoneNumber)) {
                UtilXml.addChildElementValue(shipToElement, x.PhoneNumber, destPhoneNumber, rateRequestDoc);
            }
            Element shipToAddressElement = UtilXml.addChildElement(shipToElement, x.Address, rateRequestDoc);
            UtilXml.addChildElementValue(shipToAddressElement, x.AddressLine1, destPostalAddress.getString(x.address1), rateRequestDoc);
            if (UtilValidate.isNotEmpty(destPostalAddress.getString(x.address2))) {
                UtilXml.addChildElementValue(shipToAddressElement, x.AddressLine2, destPostalAddress.getString(x.address2), rateRequestDoc);
            }

            UtilXml.addChildElementValue(shipToAddressElement, x.City, destPostalAddress.getString(x.city), rateRequestDoc);
            UtilXml.addChildElementValue(shipToAddressElement, x.StateProvinceCode, destPostalAddress.getString(x.stateProvinceGeoId),
                    rateRequestDoc);
            UtilXml.addChildElementValue(shipToAddressElement, x.PostalCode, destPostalAddress.getString(x.postalCode), rateRequestDoc);
            UtilXml.addChildElementValue(shipToAddressElement, x.CountryCode, destCountryGeo.getString(x.geoCode), rateRequestDoc);
            if (UtilValidate.isNotEmpty(shipmentRouteSegment.getString(x.homeDeliveryType))) {
                UtilXml.addChildElement(shipToAddressElement, x.ResidentialAddress, rateRequestDoc);
            }

            // Child of Shipment: ShipFrom
            Element shipFromElement = UtilXml.addChildElement(shipmentElement, x.ShipFrom, rateRequestDoc);
            UtilXml.addChildElementValue(shipFromElement, x.CompanyName, UtilValidate.isNotEmpty(originPostalAddress.getString(x.toName))
                    ? originPostalAddress.getString(x.toName) : x.emptyString, rateRequestDoc);
            UtilXml.addChildElementValue(shipFromElement, x.AttentionName, UtilValidate.isNotEmpty(originPostalAddress.getString(x.attnName))
                    ? originPostalAddress.getString(x.attnName) : x.emptyString, rateRequestDoc);
            UtilXml.addChildElementValue(shipFromElement, x.PhoneNumber, originPhoneNumber, rateRequestDoc);
            Element shipFromAddressElement = UtilXml.addChildElement(shipFromElement, x.Address, rateRequestDoc);
            UtilXml.addChildElementValue(shipFromAddressElement, x.AddressLine1, originPostalAddress.getString(x.address1), rateRequestDoc);
            if (UtilValidate.isNotEmpty(originPostalAddress.getString(x.address2))) {
                UtilXml.addChildElementValue(shipFromAddressElement, x.AddressLine2, originPostalAddress.getString(x.address2), rateRequestDoc);
            }
            UtilXml.addChildElementValue(shipFromAddressElement, x.City, originPostalAddress.getString(x.city), rateRequestDoc);
            UtilXml.addChildElementValue(shipFromAddressElement, x.StateProvinceCode, originPostalAddress.getString(x.stateProvinceGeoId),
                    rateRequestDoc);
            UtilXml.addChildElementValue(shipFromAddressElement, x.PostalCode, originPostalAddress.getString(x.postalCode), rateRequestDoc);
            UtilXml.addChildElementValue(shipFromAddressElement, x.CountryCode, originCountryGeo.getString(x.geoCode), rateRequestDoc);

            List<GenericValue> shipmentPackageRouteSegs = shipmentRouteSegment.getRelated(x.ShipmentPackageRouteSeg, null, UtilMisc.toList(
                    x.shipmentPackageSeqId_39d5d38d), false);
            if (UtilValidate.isEmpty(shipmentPackageRouteSegs)) {
                return ServiceUtil.returnError(UtilProperties.getMessage(RES_ERROR, x.FacilityShipmentUpsPackageRouteSegsNotFound,
                        UtilMisc.toMap(x.shipmentId, shipmentId, x.shipmentRouteSegmentId, shipmentRouteSegmentId), locale));
            }
            for (GenericValue shipmentPackageRouteSeg : shipmentPackageRouteSegs) {

                GenericValue shipmentPackage = shipmentPackageRouteSeg.getRelatedOne(x.ShipmentPackage, false);
                GenericValue shipmentBoxType = shipmentPackage.getRelatedOne(x.ShipmentBoxType, false);
                List<GenericValue> carrierShipmentBoxTypes = shipmentPackage.getRelated(x.CarrierShipmentBoxType, UtilMisc.toMap(x.partyId, x.UPS),
                        null, false);
                GenericValue carrierShipmentBoxType = null;
                if (!carrierShipmentBoxTypes.isEmpty()) {
                    carrierShipmentBoxType = carrierShipmentBoxTypes.get(0);
                }

                Element packageElement = UtilXml.addChildElement(shipmentElement, x.Package_7431e3df, rateRequestDoc);
                Element packagingTypeElement = UtilXml.addChildElement(packageElement, x.PackagingType, rateRequestDoc);
                if (carrierShipmentBoxType != null && carrierShipmentBoxType.get(x.packagingTypeCode) != null) {
                    UtilXml.addChildElementValue(packagingTypeElement, x.Code, carrierShipmentBoxType.getString(x.packagingTypeCode), rateRequestDoc);
                } else {
                    // default to "02", plain old Package
                    UtilXml.addChildElementValue(packagingTypeElement, x.Code, x._02, rateRequestDoc);
                }
                if (shipmentBoxType != null) {
                    Element dimensionsElement = UtilXml.addChildElement(packageElement, x.Dimensions, rateRequestDoc);
                    Element unitOfMeasurementElement = UtilXml.addChildElement(dimensionsElement, x.UnitOfMeasurement, rateRequestDoc);
                    GenericValue dimensionUom = shipmentBoxType.getRelatedOne(x.DimensionUom, false);
                    if (dimensionUom != null) {
                        UtilXml.addChildElementValue(unitOfMeasurementElement, x.Code,
                                dimensionUom.getString(x.abbreviation).toUpperCase(Locale.getDefault()), rateRequestDoc);
                    } else {
                        UtilXml.addChildElementValue(unitOfMeasurementElement, x.Code, ModelService.IN_PARAM, rateRequestDoc);
                    }
                    BigDecimal boxLength = shipmentBoxType.getBigDecimal(x.boxLength);
                    BigDecimal boxWidth = shipmentBoxType.getBigDecimal(x.boxWidth);
                    BigDecimal boxHeight = shipmentBoxType.getBigDecimal(x.boxHeight);
                    UtilXml.addChildElementValue(dimensionsElement, x.Length, UtilValidate.isNotEmpty(boxLength) ? x.emptyString + boxLength.intValue() : x.emptyString,
                            rateRequestDoc);
                    UtilXml.addChildElementValue(dimensionsElement, x.Width, UtilValidate.isNotEmpty(boxWidth) ? x.emptyString + boxWidth.intValue() : x.emptyString,
                            rateRequestDoc);
                    UtilXml.addChildElementValue(dimensionsElement, x.Height, UtilValidate.isNotEmpty(boxHeight) ? x.emptyString + boxHeight.intValue() : x.emptyString,
                            rateRequestDoc);
                } else if (UtilValidate.isNotEmpty(shipmentPackage.get(x.boxLength))
                        && UtilValidate.isNotEmpty(shipmentPackage.get(x.boxWidth))
                        && UtilValidate.isNotEmpty(shipmentPackage.get(x.boxHeight))) {
                    Element dimensionsElement = UtilXml.addChildElement(packageElement, x.Dimensions, rateRequestDoc);
                    Element unitOfMeasurementElement = UtilXml.addChildElement(dimensionsElement, x.UnitOfMeasurement, rateRequestDoc);
                    UtilXml.addChildElementValue(unitOfMeasurementElement, x.Code, ModelService.IN_PARAM, rateRequestDoc);
                    BigDecimal length = (BigDecimal) shipmentPackage.get(x.boxLength);
                    BigDecimal width = (BigDecimal) shipmentPackage.get(x.boxWidth);
                    BigDecimal height = (BigDecimal) shipmentPackage.get(x.boxHeight);
                    UtilXml.addChildElementValue(dimensionsElement, x.Length, length.setScale(DECIMALS, ROUNDING).toString(), rateRequestDoc);
                    UtilXml.addChildElementValue(dimensionsElement, x.Width, width.setScale(DECIMALS, ROUNDING).toString(), rateRequestDoc);
                    UtilXml.addChildElementValue(dimensionsElement, x.Height, height.setScale(DECIMALS, ROUNDING).toString(), rateRequestDoc);
                }

                Element packageWeightElement = UtilXml.addChildElement(packageElement, x.PackageWeight, rateRequestDoc);
                Element packageWeightUnitOfMeasurementElement = UtilXml.addChildElement(packageElement, x.UnitOfMeasurement, rateRequestDoc);
                String weightUomUps = OFBIZ_TO_UPS.get(shipmentPackage.get(x.weightUomId));
                if (weightUomUps != null) {
                    UtilXml.addChildElementValue(packageWeightUnitOfMeasurementElement, x.Code, weightUomUps, rateRequestDoc);
                } else {
                    // might as well default to LBS
                    UtilXml.addChildElementValue(packageWeightUnitOfMeasurementElement, x.Code, x.LBS, rateRequestDoc);
                }

                if (shipmentPackage.getString(x.weight) == null) {
                    return ServiceUtil.returnError(UtilProperties.getMessage(RES_ERROR, x.FacilityShipmentUpsWeightValueNotFound,
                            UtilMisc.toMap(x.shipmentId, shipmentId, x.shipmentRouteSegmentId, shipmentRouteSegmentId, x.shipmentPackageSeqId,
                                    shipmentPackage.getString(x.shipmentPackageSeqId)), locale));
                }
                BigDecimal boxWeight = shipmentPackage.getBigDecimal(x.weight);
                UtilXml.addChildElementValue(packageWeightElement, x.Weight_69c0b815, UtilValidate.isNotEmpty(boxWeight) ? x.emptyString + boxWeight.intValue() : x.emptyString,
                        rateRequestDoc);
            }

            // service options
            UtilXml.addChildElement(shipmentElement, x.ShipmentServiceOptions, rateRequestDoc);
            String rateRequestString = null;
            try {
                rateRequestString = UtilXml.writeXmlDocument(rateRequestDoc);
            } catch (IOException e) {
                String ioeErrMsg = x.Error_writing_the_RatingServiceSelectionRequest_XML_Document_to_a_String + e.toString();
                Debug.logError(e, ioeErrMsg, MODULE);
                return ServiceUtil.returnFailure(UtilProperties.getMessage(RES_ERROR,
                        x.FacilityShipmentUpsErrorRatingServiceSelectionRequestXmlToString,
                        UtilMisc.toMap(x.errorString, e.toString()), locale));
            }

            // create AccessRequest XML doc
            Document accessRequestDocument = createAccessRequestDocument(delegator, shipmentGatewayConfigId, resource);
            String accessRequestString = null;
            try {
                accessRequestString = UtilXml.writeXmlDocument(accessRequestDocument);
            } catch (IOException e) {
                String ioeErrMsg = x.Error_writing_the_AccessRequest_XML_Document_to_a_String + e.toString();
                Debug.logError(e, ioeErrMsg, MODULE);
                return ServiceUtil.returnFailure(UtilProperties.getMessage(RES_ERROR,
                        x.FacilityShipmentUpsErrorAccessRequestXmlToString,
                        UtilMisc.toMap(x.errorString, e.toString()), locale));
            }

            // prepare the access/inquire request string
            StringBuilder xmlString = new StringBuilder();
            xmlString.append(accessRequestString);
            xmlString.append(rateRequestString);
            if (Debug.verboseOn()) {
                Debug.logVerbose(xmlString.toString(), MODULE);
            }
            // send the request
            try {
                rateResponseString = sendUpsRequest(x.Rate, xmlString.toString(), shipmentGatewayConfigId, resource, delegator, locale);
            } catch (UpsConnectException e) {
                String uceErrMsg = x.Error_sending_UPS_request_for_UPS_Service_Rate + e.toString();
                Debug.logError(e, uceErrMsg, MODULE);
                return ServiceUtil.returnFailure(UtilProperties.getMessage(RES_ERROR, x.FacilityShipmentUpsErrorSendingRate,
                        UtilMisc.toMap(x.errorString, e.toString()), locale));
            }
            Debug.logVerbose(rateResponseString, MODULE);
            Document rateResponseDocument = null;
            try {
                rateResponseDocument = UtilXml.readXmlDocument(rateResponseString, false);
            } catch (SAXException | IOException | ParserConfigurationException e2) {
                String excErrMsg = x.Error_parsing_the_RatingServiceSelectionResponse + e2.toString();
                Debug.logError(e2, excErrMsg, MODULE);
                return ServiceUtil.returnFailure(UtilProperties.getMessage(RES_ERROR, x.FacilityShipmentUpsErrorParsingRatingServiceSelectionResponse,
                        UtilMisc.toMap(x.errorString, e2.toString()), locale));
            }
            Map<String, Object> upsResponse = handleUpsAlternateRatesInquireResponse(rateResponseDocument, locale);
            Map<String, BigDecimal> upsRateCodeMap = UtilGenerics.cast(upsResponse.get(x.upsRateCodeMap));
            GenericValue carrierShipmentMethod = null;
            // Filtering out rates of shipping methods which are not configured in ProductStoreShipmentMeth entity.
            try {
                ProductStoreShipmentMethDao productStoreShipmentMethDao = DaoRegistry.getDao(delegator, x.ProductStoreShipmentMeth,
                        ProductStoreShipmentMethDao.class);
                CarrierShipmentMethodDao carrierShipmentMethodDao = DaoRegistry.getDao(delegator, x.CarrierShipmentMethod,
                        CarrierShipmentMethodDao.class);
                List<GenericValue> productStoreShipmentMethods = productStoreShipmentMethDao.findListByWhere(delegator,
                        x.ProductStoreShipmentMethView, UtilMisc.toMap(x.productStoreId, productStoreId), null, null, false);
                for (GenericValue productStoreShipmentMethod : productStoreShipmentMethods) {
                    if (x.UPS.equals(productStoreShipmentMethod.get(x.partyId))) {
                        Map<String, Object> thisUpsRateCodeMap = new HashMap<>();
                        carrierShipmentMethod = carrierShipmentMethodDao.findOneByWhere(delegator, x.CarrierShipmentMethod,
                                UtilMisc.toMap(x.shipmentMethodTypeId, productStoreShipmentMethod.getString(x.shipmentMethodTypeId), x.partyId,
                                        productStoreShipmentMethod.getString(x.partyId), x.roleTypeId,
                                        productStoreShipmentMethod.getString(x.roleTypeId)), null, null, false);
                        String serviceCode = carrierShipmentMethod.getString(x.carrierServiceCode);
                        for (String thisServiceCode : upsRateCodeMap.keySet()) {
                            if (serviceCode.equals(thisServiceCode)) {
                                BigDecimal newRate = upsRateCodeMap.get(serviceCode);
                                thisUpsRateCodeMap.put(serviceCode, newRate);
                                shippingRates.add(thisUpsRateCodeMap);
                            }
                        }
                    }
                }
            } catch (GenericEntityException e) {
                Debug.logError(e, MODULE);
            }
            return UtilMisc.toMap(x.shippingRates, shippingRates,
                    ModelService.RESPONSE_MESSAGE, ModelService.RESPOND_SUCCESS);
        } catch (GenericEntityException e) {
            Debug.logError(e, MODULE);
            return ServiceUtil.returnError(UtilProperties.getMessage(RES_ERROR, x.FacilityShipmentUpsErrorDataShipmentAlternateRate,
                    UtilMisc.toMap(x.errorString, e.toString()), locale));
        }
    }

    public static Map<String, Object> handleUpsAlternateRatesInquireResponse(Document rateResponseDocument, Locale locale) {
        Element rateResponseElement = rateResponseDocument.getDocumentElement();

        // handle Response element info
        Element responseElement = UtilXml.firstChildElement(rateResponseElement, x.Response_6e617e4f);
        String responseStatusCode = UtilXml.childElementValue(responseElement, x.ResponseStatusCode);
        List<Object> errorList = new LinkedList<>();
        UpsServices.handleErrors(responseElement, errorList, locale);
        String totalRates = null;

        if (x._1.equals(responseStatusCode)) {
            List<? extends Element> rates = UtilXml.childElementList(rateResponseElement, x.RatedShipment);
            Map<String, BigDecimal> rateMap = new HashMap<>();
            if (UtilValidate.isEmpty(rates)) {
                return ServiceUtil.returnError(UtilProperties.getMessage(RES_ERROR, x.FacilityShipmentUpsNoRateAvailable, locale));
            } else {
                for (Element element : rates) {
                    // get service
                    Element service = UtilXml.firstChildElement(element, x.Service);
                    String serviceCode = UtilXml.childElementValue(service, x.Code);

                    // get negotiated rates
                    Element negotiatedRates = UtilXml.firstChildElement(element, x.NegotiatedRates);
                    if (negotiatedRates != null) {
                        Element netSummaryCharges = UtilXml.firstChildElement(negotiatedRates, x.NetSummaryCharges);
                        Element grandTotal = UtilXml.firstChildElement(netSummaryCharges, x.GrandTotal);
                        totalRates = UtilXml.childElementValue(grandTotal, x.MonetaryValue);
                    } else {
                        // get total rates
                        Element totalCharges = UtilXml.firstChildElement(element, x.TotalCharges);
                        totalRates = UtilXml.childElementValue(totalCharges, x.MonetaryValue);
                    }
                    rateMap.put(serviceCode, new BigDecimal(totalRates));
                }
            }
            Debug.logInfo(x.UPS_Rate_Map + rateMap, MODULE);
            Map<String, Object> resp = ServiceUtil.returnSuccess();
            resp.put(x.upsRateCodeMap, rateMap);
            return resp;
        } else {
            errorList.add(ServiceUtil.returnError(UtilProperties.getMessage(RES_ERROR, x.FacilityShipmentUpsErrorStatusCode,
                    UtilMisc.toMap(x.responseStatusCode, responseStatusCode), locale)));
            return ServiceUtil.returnFailure(errorList);
        }
    }

    private static String getShipmentGatewayConfigValue(Delegator delegator, String shipmentGatewayConfigId,
                                                        String shipmentGatewayConfigParameterName,
                                                        String resource, String parameterName) {
        String returnValue = x.emptyString;
        if (UtilValidate.isNotEmpty(shipmentGatewayConfigId)) {
            try {
                ShipmentGatewayUpsDao shipmentGatewayUpsDao = DaoRegistry.getDao(delegator, x.ShipmentGatewayUps,
                        ShipmentGatewayUpsDao.class);
                GenericValue ups = shipmentGatewayUpsDao.findOneByWhere(delegator, x.ShipmentGatewayUps,
                        UtilMisc.toMap(x.shipmentGatewayConfigId, shipmentGatewayConfigId), null, null, false);
                if (UtilValidate.isNotEmpty(ups)) {
                    Object upsField = ups.get(shipmentGatewayConfigParameterName);
                    if (upsField != null) {
                        returnValue = upsField.toString().trim();
                    }
                }
            } catch (GenericEntityException e) {
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

    private static String getShipmentGatewayConfigValue(Delegator delegator, String shipmentGatewayConfigId,
                                                        String shipmentGatewayConfigParameterName,
                                                        String resource, String parameterName, String defaultValue) {
        String returnValue = getShipmentGatewayConfigValue(delegator, shipmentGatewayConfigId, shipmentGatewayConfigParameterName, resource,
                parameterName);
        if (UtilValidate.isEmpty(returnValue)) {
            returnValue = defaultValue;
        }
        return returnValue;
    }
}

@SuppressWarnings(x.serial)
class UpsConnectException extends GeneralException {
    UpsConnectException() {
        super();
    }

    UpsConnectException(String msg) {
        super(msg);
    }

    UpsConnectException(Throwable t) {
        super(t);
    }

    UpsConnectException(String msg, Throwable t) {
        super(msg, t);
    }
}


/*
 * UPS Code Reference

UPS Service IDs
ShipConfirm
ShipAccept
Void
Track
Rate

Package Type Code
00 Unknown
01 UPS Letter
02 Package
03 UPS Tube
04 UPS Pak
21 UPS Express Box
24 UPS 25KG Box
25 UPS 10KG Box

Pickup Types
01 Daily Pickup
03 Customer Counter
06 One Time Pickup
07 On Call Air Pickup
19 Letter Center
20 Air Service Center

UPS Service Codes
US Origin
01 UPS Next Day Air
02 UPS 2nd Day Air
03 UPS Ground
07 UPS Worldwide Express
08 UPS Worldwide Expedited
11 UPS Standard
12 UPS 3-Day Select
13 UPS Next Day Air Saver
14 UPS Next Day Air Early AM
54 UPS Worldwide Express Plus
59 UPS 2nd Day Air AM
64 N/A
65 UPS Express Saver

Reference Number Codes
AJ Acct. Rec. Customer Acct.
AT Appropriation Number
BM Bill of Lading Number
9V COD Number
ON Dealer Order Number
DP Department Number
EI Employer's ID Number
3Q FDA Product Code
TJ Federal Taxpayer ID Number
IK Invoice Number
MK Manifest Key Number
MJ Model Number
PM Part Number
PC Production Code
PO Purchase Order No.
RQ Purchase Request No.
RZ Return Authorization No.
SA Salesperson No.
SE Serial No.
SY Social Security No.
ST Store No.
TN Transaction Ref. No.

Error Codes
First note that in the ref guide there are about 21 pages of error codes
Here are some overalls:
1 Success (no error)
01xxxx XML Error
02xxxx Architecture Error
15xxxx Tracking Specific Error

 */


/*
 * Sample XML documents:
 *
<?xml version="1.0"?>
<AccessRequest xml:lang="en-US">
   <AccessLicenseNumber>TEST262223144CAT</AccessLicenseNumber>
   <UserId>REG111111</UserId>
   <Password>REG111111</Password>
</AccessRequest>

=======================================
Shipment Confirm Request/Response
=======================================

<?xml version="1.0"?>
<ShipmentConfirmRequest xml:lang="en-US">
    <Request>
        <TransactionReference>
            <CustomerContext>Ship Confirm / nonvalidate</CustomerContext>
            <XpciVersion>1.0001</XpciVersion>
        </TransactionReference>
        <RequestAction>ShipConfirm</RequestAction>
        <RequestOption>nonvalidate</RequestOption>
    </Request>
    <LabelSpecification>
        <LabelPrintMethod>
            <Code>GIF</Code>
        </LabelPrintMethod>
        <HTTPUserAgent>Mozilla/5.0</HTTPUserAgent>
        <LabelImageFormat>
            <Code>GIF</Code>
        </LabelImageFormat>
    </LabelSpecification>
    <Shipment>
        <Description>DescriptionofGoodsTest</Description>
        <Shipper>
            <Name>ShipperName</Name>
            <AttentionName>ShipperName</AttentionName>
            <PhoneNumber>2226267227</PhoneNumber>
            <ShipperNumber>12345E</ShipperNumber>
            <Address>
                <AddressLine1>123 ShipperStreet</AddressLine1>
                <AddressLine2>123 ShipperStreet</AddressLine2>
                <AddressLine3>123 ShipperStreet</AddressLine3>
                <City>ShipperCity</City>
                <StateProvinceCode>foo</StateProvinceCode>
                <PostalCode>03570</PostalCode>
                <CountryCode>DE</CountryCode>
            </Address>
        </Shipper>
        <ShipTo>
            <CompanyName>ShipToCompanyName</CompanyName>
            <AttentionName>ShipToAttnName</AttentionName>
            <PhoneNumber>3336367336</PhoneNumber>
            <Address>
                <AddressLine1>123 ShipToStreet</AddressLine1>
                <PostalCode>DT09</PostalCode>
                <City>Trent</City>
                <CountryCode>GB</CountryCode>
            </Address>
        </ShipTo>
        <ShipFrom>
            <CompanyName>ShipFromCompanyName</CompanyName>
            <AttentionName>ShipFromAttnName</AttentionName>
            <PhoneNumber>7525565064</PhoneNumber>
            <Address>
                <AddressLine1>123 ShipFromStreet</AddressLine1>
                <City>Berlin</City>
                <PostalCode>03570</PostalCode>
                <CountryCode>DE</CountryCode>
            </Address>
        </ShipFrom>
        <PaymentInformation>
            <Prepaid>
                <BillShipper>
                    <AccountNumber>12345E</AccountNumber>
                </BillShipper>
            </Prepaid>
        </PaymentInformation>
        <Service>
            <Code>07</Code>
        </Service>
        <Package>
            <PackagingType>
                <Code>02</Code>
            </PackagingType>
            <Dimensions>
                <UnitOfMeasurement>
                    <Code>CM</Code>
                </UnitOfMeasurement>
                <Length>60</Length>
                <Width>7</Width>
                <Height>5</Height>
            </Dimensions>
            <PackageWeight>
                <UnitOfMeasurement>
                    <Code>KGS</Code>
                </UnitOfMeasurement>
                <Weight>3.0</Weight>
            </PackageWeight>
            <ReferenceNumber>
                <Code>MK</Code>
                <Value>00001</Value>
            </ReferenceNumber>
        </Package>
    </Shipment>
</ShipmentConfirmRequest>

=======================================

<?xml version="1.0"?>
<ShipmentConfirmResponse>
    <Response>
        <TransactionReference>
            <CustomerContext>ShipConfirmUS</CustomerContext>
            <XpciVersion>1.0001</XpciVersion>
        </TransactionReference>
        <ResponseStatusCode>1</ResponseStatusCode>
        <ResponseStatusDescription>Success</ResponseStatusDescription>
    </Response>
    <ShipmentCharges>
        <TransportationCharges>
            <CurrencyCode>USD</CurrencyCode>
            <MonetaryValue>31.38</MonetaryValue>
        </TransportationCharges>
        <ServiceOptionsCharges>
            <CurrencyCode>USD</CurrencyCode>
            <MonetaryValue>7.75</MonetaryValue>
        </ServiceOptionsCharges>
        <TotalCharges>
            <CurrencyCode>USD</CurrencyCode>
            <MonetaryValue>39.13</MonetaryValue>
        </TotalCharges>
    </ShipmentCharges>
    <BillingWeight>
        <UnitOfMeasurement>
            <Code>LBS</Code>
        </UnitOfMeasurement>
        <Weight>4.0</Weight>
    </BillingWeight>
    <ShipmentIdentificationNumber>1Z12345E1512345676</ShipmentIdentificationNumber>
    <ShipmentDigest>INSERT SHIPPING DIGEST HERE</ShipmentDigest>
</ShipmentConfirmResponse>

=======================================
Shipment Accept Request/Response
=======================================

<?xml version="1.0"?>
<ShipmentAcceptRequest>
    <Request>
        <TransactionReference>
            <CustomerContext>TR01</CustomerContext>
            <XpciVersion>1.0001</XpciVersion>
        </TransactionReference>
        <RequestAction>ShipAccept</RequestAction>
        <RequestOption>01</RequestOption>
    </Request>
    <ShipmentDigest>INSERT SHIPPING DIGEST HERE</ShipmentDigest>
</ShipmentAcceptRequest>

=======================================

<?xml version="1.0"?>
<ShipmentAcceptResponse>
    <Response>
        <TransactionReference>
            <CustomerContext>TR01</CustomerContext>
            <XpciVersion>1.0001</XpciVersion>
        </TransactionReference>
        <ResponseStatusCode>1</ResponseStatusCode>
        <ResponseStatusDescription>Success</ResponseStatusDescription>
    </Response>
    <ShipmentResults>
        <ShipmentCharges>
            <TransportationCharges>
                <CurrencyCode>USD</CurrencyCode>
                <MonetaryValue>31.38</MonetaryValue>
            </TransportationCharges>
            <ServiceOptionsCharges>
                <CurrencyCode>USD</CurrencyCode>
                <MonetaryValue>7.75</MonetaryValue>
            </ServiceOptionsCharges>
            <TotalCharges>
                <CurrencyCode>USD</CurrencyCode>
                <MonetaryValue>39.13</MonetaryValue>
            </TotalCharges>
        </ShipmentCharges>
        <BillingWeight>
            <UnitOfMeasurement>
                <Code>LBS</Code>
            </UnitOfMeasurement>
            <Weight>4.0</Weight>
        </BillingWeight>
        <ShipmentIdentificationNumber>1Z12345E1512345676</ShipmentIdentificationNumber>
        <PackageResults>
            <TrackingNumber>1Z12345E1512345676</TrackingNumber>
            <ServiceOptionsCharges>
                <CurrencyCode>USD</CurrencyCode>
                <MonetaryValue>0.00</MonetaryValue>
            </ServiceOptionsCharges>
            <LabelImage>
                <LabelImageFormat>
                    <Code>epl</Code>
                </LabelImageFormat>
                <GraphicImage>INSERT GRAPHIC IMAGE HERE</GraphicImage>
            </LabelImage>
        </PackageResults>
        <PackageResults>
            <TrackingNumber>1Z12345E1512345686</TrackingNumber>
            <ServiceOptionsCharges>
                <CurrencyCode>USD</CurrencyCode>
                <MonetaryValue>7.75</MonetaryValue>
            </ServiceOptionsCharges>
            <LabelImage>
                <LabelImageFormat>
                    <Code>epl</Code>
                </LabelImageFormat>
                <GraphicImage>INSERT GRAPHIC IMAGE HERE</GraphicImage>
            </LabelImage>
        </PackageResults>
    </ShipmentResults>
</ShipmentAcceptResponse>

=======================================
Void Shipment Request/Response
=======================================

<?xml version="1.0"?>
<VoidShipmentRequest>
    <Request>
        <TransactionReference>
            <CustomerContext>Void</CustomerContext>
            <XpciVersion>1.0001</XpciVersion>
        </TransactionReference>
        <RequestAction>Void</RequestAction>
        <RequestOption>1</RequestOption>
    </Request>
    <ShipmentIdentificationNumber>1Z12345E1512345676</ShipmentIdentificationNumber>
</VoidShipmentRequest>

=======================================

<?xml version="1.0"?>
<VoidShipmentResponse>
    <Response>
        <TransactionReference>
            <XpciVersion>1.0001</XpciVersion>
        </TransactionReference>
        <ResponseStatusCode>1</ResponseStatusCode>
        <ResponseStatusDescription>Success</ResponseStatusDescription>
    </Response>
    <Status>
        <StatusType>
            <Code>1</Code>
            <Description>Success</Description>
        </StatusType>
        <StatusCode>
            <Code>1</Code>
            <Description>Success</Description>
        </StatusCode>
    </Status>
</VoidShipmentResponse>

=======================================
Track Shipment Request/Response
=======================================

<?xml version="1.0"?>
<TrackRequest xml:lang="en-US">
    <Request>
        <TransactionReference>
            <CustomerContext>sample</CustomerContext>
            <XpciVersion>1.0001</XpciVersion>
        </TransactionReference>
        <RequestAction>Track</RequestAction>
    </Request>
    <TrackingNumber>1Z12345E1512345676</TrackingNumber>
</TrackRequest>

=======================================

<?xml version="1.0" encoding="UTF-8"?>
<TrackResponse>
    <Response>
        <TransactionReference>
            <CustomerContext>sample</CustomerContext>
            <XpciVersion>1.0001</XpciVersion>
        </TransactionReference>
        <ResponseStatusCode>1</ResponseStatusCode>
        <ResponseStatusDescription>Success</ResponseStatusDescription>
    </Response>
    <Shipment>
        <Shipper>
            <ShipperNumber>12345E</ShipperNumber>
        </Shipper>
        <Service>
            <Code>15</Code>
            <Description>NDA EAM/EXP EAM</Description>
        </Service>
        <ShipmentIdentificationNumber>1Z12345E1512345676</ShipmentIdentificationNumber>
        <Package>
            <TrackingNumber>1Z12345E1512345676</TrackingNumber>
            <Activity>
                <ActivityLocation>
                    <Address>
                        <City>CLAKVILLE</City>
                        <StateProvinceCode>AK</StateProvinceCode>
                        <PostalCode>99901</PostalCode>
                        <CountryCode>US</CountryCode>
                    </Address>
                    <Code>MG</Code>
                    <Description>MC MAN</Description>
                </ActivityLocation>
                <Status>
                    <StatusType>
                        <Code>D</Code>
                        <Description>DELIVERED</Description>
                    </StatusType>
                    <StatusCode>
                        <Code>FS</Code>
                    </StatusCode>
                </Status>
                <Date>20020930</Date>
                <Time>130900</Time>
            </Activity>
            <PackageWeight>
                <UnitOfMeasurement>
                    <Code>LBS</Code>
                </UnitOfMeasurement>
                <Weight>0.00</Weight>
            </PackageWeight>
        </Package>
    </Shipment>
</TrackResponse>

=======================================
Rates & Service Request/Response
=======================================

<?xml version="1.0"?>
<RatingServiceSelectionRequest xml:lang="en-US">
  <Request>
    <TransactionReference>
      <CustomerContext>Bare Bones Rate Request</CustomerContext>
      <XpciVersion>1.0</XpciVersion>
    </TransactionReference>
    <RequestAction>Rate</RequestAction>
    <RequestOption>Rate</RequestOption>
  </Request>
  <PickupType>
    <Code>01</Code>
  </PickupType>
  <Shipment>
    <Shipper>
        <Address>
            <PostalCode>44129</PostalCode>
            <CountryCode>US</CountryCode>
        </Address>
    </Shipper>
    <ShipTo>
        <Address>
            <PostalCode>44129</PostalCode>
            <CountryCode>US</CountryCode>
        </Address>
    </ShipTo>
    <ShipFrom>
        <Address>
            <PostalCode>32779</PostalCode>
            <CountryCode>US</CountryCode>
        </Address>
    </ShipFrom>
    <Service>
        <Code>01</Code>
    </Service>
    <Package>
        <PackagingType>
            <Code>02</Code>
        </PackagingType>
        <Dimensions>
            <UnitOfMeasurement>
                <Code>IN</Code>
            </UnitOfMeasurement>
            <Length>20</Length>
            <Width>20</Width>
            <Height>20</Height>
        </Dimensions>
        <PackageWeight>
            <UnitOfMeasurement>
                <Code>LBS</Code>
            </UnitOfMeasurement>
            <Weight>23</Weight>
        </PackageWeight>
    </Package>
  </Shipment>
</RatingServiceSelectionRequest>

=======================================

<?xml version="1.0" encoding="UTF-8"?>
<RatingServiceSelectionResponse>
    <Response>
        <TransactionReference>
            <CustomerContext>Bare Bones Rate Request</CustomerContext>
            <XpciVersion>1.0</XpciVersion>
        </TransactionReference>
        <ResponseStatusCode>1</ResponseStatusCode>
        <ResponseStatusDescription>Success</ResponseStatusDescription>
    </Response>
    <RatedShipment>
        <Service>
            <Code>01</Code>
        </Service>
        <BillingWeight>
            <UnitOfMeasurement>
                <Code>LBS</Code>
            </UnitOfMeasurement>
            <Weight>42.0</Weight>
        </BillingWeight>
        <TransportationCharges>
            <CurrencyCode>USD</CurrencyCode>
            <MonetaryValue>108.61</MonetaryValue>
        </TransportationCharges>
        <ServiceOptionsCharges>
            <CurrencyCode>USD</CurrencyCode>
            <MonetaryValue>0.00</MonetaryValue>
        </ServiceOptionsCharges>
        <TotalCharges>
            <CurrencyCode>USD</CurrencyCode>
            <MonetaryValue>108.61</MonetaryValue>
        </TotalCharges>
        <GuaranteedDaysToDelivery>1</GuaranteedDaysToDelivery>
        <ScheduledDeliveryTime>10:30 A.M.</ScheduledDeliveryTime>
        <RatedPackage>
            <TransportationCharges>
                <CurrencyCode>USD</CurrencyCode>
                <MonetaryValue>108.61</MonetaryValue>
            </TransportationCharges>
            <ServiceOptionsCharges>
                <CurrencyCode>USD</CurrencyCode>
                <MonetaryValue>0.00</MonetaryValue>
            </ServiceOptionsCharges>
            <TotalCharges>
                <CurrencyCode>USD</CurrencyCode>
                <MonetaryValue>108.61</MonetaryValue>
            </TotalCharges>
            <Weight>23.0</Weight>
            <BillingWeight>
                <UnitOfMeasurement>
                    <Code>LBS</Code>
                </UnitOfMeasurement>
                <Weight>42.0</Weight>
            </BillingWeight>
        </RatedPackage>
    </RatedShipment>
</RatingServiceSelectionResponse>

=======================================
Address Validation Request/Response
=======================================

<AddressValidationRequest xml:lang="en-US">
    <Request>
        <TransactionReference>
            <CustomerContext>Maryam Dennis-Customer Data</CustomerContext>
            <XpciVersion>1.0001</XpciVersion>
        </TransactionReference>
        <RequestAction>AV</RequestAction>
    </Request>
    <Address>
        <City>MIAMI</City>
        <StateProvinceCode>FL</StateProvinceCode>
    </Address>
</AddressValidationRequest>

=======================================

<AddressValidationResponse>
    <Response>
        <TransactionReference>
            <XpciVersion>1.0001</XpciVersion>
        </TransactionReference>
        <ResponseStatusCode>1</ResponseStatusCode>
        <ResponseStatusDescription>Success</ResponseStatusDescription>
    </Response>
    <AddressValidationResult>
        <Rank>1</Rank>
        <Quality>1.0</Quality>
        <Address>
            <City>TIMONIUM</City>
            <StateProvinceCode>MD</StateProvinceCode>
        </Address>
        <PostalCodeLowEnd>21093</PostalCodeLowEnd>
        <PostalCodeHighEnd>21094</PostalCodeHighEnd>
    </AddressValidationResult>
</AddressValidationResponse>

=======================================

<AddressValidationResponse>
    <Response>
        <TransactionReference>
            <XpciVersion>1.0001</XpciVersion>
        </TransactionReference>
        <ResponseStatusCode>1</ResponseStatusCode>
        <ResponseStatusDescription>Success</ResponseStatusDescription>
    </Response>
    <AddressValidationResult>
        <Rank>1</Rank>
        <Quality>0.9975000023841858</Quality>
        <Address>
            <City>TIMONIUM</City>
            <StateProvinceCode>MD</StateProvinceCode>
        </Address>
        <PostalCodeLowEnd>21093</PostalCodeLowEnd>
        <PostalCodeHighEnd>21094</PostalCodeHighEnd>
    </AddressValidationResult>
    <AddressValidationResult>
        <Rank>2</Rank>
        <Quality>0.8299999833106995</Quality>
        <Address>
            <City>LUTHERVILLE TIMONIUM</City>
            <StateProvinceCode>MD</StateProvinceCode>
        </Address>
        <PostalCodeLowEnd>21093</PostalCodeLowEnd>
        <PostalCodeHighEnd>21094</PostalCodeHighEnd>
    </AddressValidationResult>
    <AddressValidationResult>
        <Rank>3</Rank>
        <Quality>0.8299999833106995</Quality>
        <Address>
            <City>LUTHERVILLE</City>
            <StateProvinceCode>MD</StateProvinceCode>
        </Address>
        <PostalCodeLowEnd>21093</PostalCodeLowEnd>
        <PostalCodeHighEnd>21094</PostalCodeHighEnd>
    </AddressValidationResult>
</AddressValidationResponse>

 */
