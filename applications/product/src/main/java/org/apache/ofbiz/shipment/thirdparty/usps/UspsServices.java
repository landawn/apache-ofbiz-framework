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

package org.apache.ofbiz.shipment.thirdparty.usps;

import java.io.ByteArrayOutputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.text.DecimalFormat;
import java.util.Base64;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;
import java.util.Locale;
import java.util.Map;

import javax.xml.transform.TransformerException;

import org.apache.commons.lang.StringUtils;
import org.apache.ofbiz.base.util.Debug;
import org.apache.ofbiz.base.util.GeneralException;
import org.apache.ofbiz.base.util.HttpClient;
import org.apache.ofbiz.base.util.HttpClientException;
import org.apache.ofbiz.base.util.StringUtil;
import org.apache.ofbiz.base.util.UtilGenerics;
import org.apache.ofbiz.base.util.UtilMisc;
import org.apache.ofbiz.base.util.UtilProperties;
import org.apache.ofbiz.base.util.UtilValidate;
import org.apache.ofbiz.base.util.UtilXml;
import org.apache.ofbiz.common.uom.UomWorker;
import org.apache.ofbiz.entity.Delegator;
import org.apache.ofbiz.entity.GenericEntityException;
import org.apache.ofbiz.entity.GenericValue;
import org.apache.ofbiz.entity.util.EntityUtil;
import org.apache.ofbiz.entity.util.EntityUtilProperties;
import org.apache.ofbiz.party.contact.ContactMechWorker;
import org.apache.ofbiz.persistence.dao.CarrierShipmentMethodDao;
import org.apache.ofbiz.persistence.dao.DaoRegistry;
import org.apache.ofbiz.persistence.dao.PostalAddressDao;
import org.apache.ofbiz.persistence.dao.ShipmentDao;
import org.apache.ofbiz.persistence.dao.ShipmentGatewayUspsDao;
import org.apache.ofbiz.persistence.dao.ShipmentRouteSegmentDao;
import org.apache.ofbiz.persistence.dao.UomConversionDao;
import org.apache.ofbiz.persistence.entity.CarrierShipmentMethodEntity;
import org.apache.ofbiz.persistence.entity.PostalAddressEntity;
import org.apache.ofbiz.persistence.entity.ShipmentEntity;
import org.apache.ofbiz.persistence.entity.ShipmentGatewayUspsEntity;
import org.apache.ofbiz.persistence.entity.ShipmentRouteSegmentEntity;
import org.apache.ofbiz.persistence.entity.UomConversionEntity;
import org.apache.ofbiz.product.product.ProductWorker;
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
import com.landawn.abacus.query.Filters;
import com.landawn.abacus.util.Beans;


import org.apache.ofbiz.persistence.entity.x;
import org.apache.ofbiz.model.ServiceContext;
import org.apache.ofbiz.model.UspsServicesContext;
/**
 * USPS Webtools API Services
 */
public class UspsServices {

    private static final String MODULE = UspsServices.class.getName();
    private static final String RES_ERROR = x.ProductUiLabels;

    private static List<String> domesticCountries = new LinkedList<>();
    // Countries treated as domestic for rate enquiries
    static {
        domesticCountries.add(x.USA);
        domesticCountries.add(x.ASM);
        domesticCountries.add(x.GU);
        domesticCountries = Collections.unmodifiableList(domesticCountries);
    }

    public static Map<String, Object> uspsRateInquire(DispatchContext dctx, UspsServicesContext context) {
        Delegator delegator = dctx.getDelegator();
        String shipmentGatewayConfigId = (String) context.get(x.shipmentGatewayConfigId);
        String resource = (String) context.get(x.configProps);
        Locale locale = (Locale) context.get(x.locale);
        // check for 0 weight
        BigDecimal shippableWeight = (BigDecimal) context.get(x.shippableWeight);
        if (shippableWeight.compareTo(BigDecimal.ZERO) == 0) {
            // TODO: should we return an error, or $0.00 ?
            return ServiceUtil.returnFailure(UtilProperties.getMessage(RES_ERROR,
                    x.FacilityShipmentUspsShippableWeightMustGreaterThanZero, locale));
        }

        // get the origination ZIP
        String originationZip = null;
        GenericValue productStore = ProductStoreWorker.getProductStore(((String) context.get(x.productStoreId)), delegator);
        if (productStore != null && productStore.get(x.inventoryFacilityId) != null) {
            GenericValue facilityContactMech = ContactMechWorker.getFacilityContactMechByPurpose(delegator, productStore
                    .getString(x.inventoryFacilityId), UtilMisc.toList(x.SHIP_ORIG_LOCATION, x.PRIMARY_LOCATION));
            if (facilityContactMech != null) {
                try {
                    GenericValue shipFromAddress = getPostalAddressValue(delegator, facilityContactMech.getString(x.contactMechId));
                    if (shipFromAddress != null) {
                        originationZip = shipFromAddress.getString(x.postalCode);
                    }
                } catch (GenericEntityException e) {
                    Debug.logError(e, MODULE);
                }
            }
        }
        if (UtilValidate.isEmpty(originationZip)) {
            return ServiceUtil.returnError(UtilProperties.getMessage(RES_ERROR,
                    x.FacilityShipmentUspsUnableDetermineOriginationZip, locale));
        }

        // get the destination ZIP
        String destinationZip = null;
        String shippingContactMechId = (String) context.get(x.shippingContactMechId);
        if (UtilValidate.isNotEmpty(shippingContactMechId)) {
            try {
                GenericValue shipToAddress = getPostalAddressValue(delegator, shippingContactMechId);
                if (shipToAddress != null) {
                    if (!domesticCountries.contains(shipToAddress.getString(x.countryGeoId))) {
                        return ServiceUtil.returnError(UtilProperties.getMessage(RES_ERROR,
                                x.FacilityShipmentUspsRateInquiryOnlyInUsDestinations, locale));
                    }
                    destinationZip = shipToAddress.getString(x.postalCode);
                }
            } catch (GenericEntityException e) {
                Debug.logError(e, MODULE);
            }
        }
        if (UtilValidate.isEmpty(destinationZip)) {
            return ServiceUtil.returnError(UtilProperties.getMessage(RES_ERROR,
                    x.FacilityShipmentUspsUnableDetermineDestinationZip, locale));
        }

        // get the service code
        String serviceCode = null;
        try {
            GenericValue carrierShipmentMethod = getCarrierShipmentMethodValue(delegator, context.get(x.shipmentMethodTypeId),
                    context.get(x.carrierPartyId), context.get(x.carrierRoleTypeId));
            if (carrierShipmentMethod != null) {
                serviceCode = carrierShipmentMethod.getString(x.carrierServiceCode).toUpperCase(Locale.getDefault());
            }
        } catch (GenericEntityException e) {
            Debug.logError(e, MODULE);
        }
        if (UtilValidate.isEmpty(serviceCode)) {
            return ServiceUtil.returnError(UtilProperties.getMessage(RES_ERROR,
                    x.FacilityShipmentUspsUnableDetermineServiceCode, locale));
        }

        // create the request document
        Document requestDocument = createUspsRequestDocument(x.RateV2Request, true, delegator, shipmentGatewayConfigId, resource);

        // TODO: 70 lb max is valid for Express, Priority and Parcel only - handle other methods
        BigDecimal maxWeight;
        String maxWeightStr = getShipmentGatewayConfigValue(delegator, shipmentGatewayConfigId, x.maxEstimateWeight,
                resource, x.shipment_usps_max_estimate_weight, x._70);
        try {
            maxWeight = new BigDecimal(maxWeightStr);
        } catch (NumberFormatException e) {
            Debug.logWarning(x.Error_parsing_max_estimate_weight_string + maxWeightStr + x.using_default_instead, MODULE);
            maxWeight = new BigDecimal(x._70);
        }

        List<Map<String, Object>> shippableItemInfo = UtilGenerics.cast(context.get(x.shippableItemInfo));
        List<Map<String, BigDecimal>> packages = ShipmentWorker.getPackageSplit(dctx, shippableItemInfo, maxWeight);
        boolean isOnePackage = packages.size() == 1; // use shippableWeight if there's only one package
        // TODO: Up to 25 packages can be included per request - handle more than 25
        for (ListIterator<Map<String, BigDecimal>> li = packages.listIterator(); li.hasNext();) {
            Map<String, BigDecimal> packageMap = li.next();

            BigDecimal packageWeight = isOnePackage ? shippableWeight : ShipmentWorker.calcPackageWeight(dctx, packageMap,
                    shippableItemInfo, BigDecimal.ZERO);
            if (packageWeight.compareTo(BigDecimal.ZERO) == 0) {
                continue;
            }

            Element packageElement = UtilXml.addChildElement(requestDocument.getDocumentElement(), x.Package_7431e3df, requestDocument);
            packageElement.setAttribute(x.ID, String.valueOf(li.nextIndex() - 1)); // use zero-based index (see examples)

            UtilXml.addChildElementValue(packageElement, x.Service, serviceCode, requestDocument);
            UtilXml.addChildElementValue(packageElement, x.ZipOrigination, StringUtils.substring(originationZip, 0, 5), requestDocument);
            UtilXml.addChildElementValue(packageElement, x.ZipDestination, StringUtils.substring(destinationZip, 0, 5), requestDocument);

            BigDecimal weightPounds = packageWeight.setScale(0, RoundingMode.FLOOR);
            // for Parcel post, the weight must be at least 1 lb
            if (x.PARCEL.equals(serviceCode.toUpperCase(Locale.getDefault())) && (weightPounds.compareTo(BigDecimal.ONE) < 0)) {
                weightPounds = BigDecimal.ONE;
                packageWeight = BigDecimal.ZERO;
            }
            // (packageWeight % 1) * 16 (Rounded up to 0 dp)
            BigDecimal weightOunces = packageWeight.remainder(BigDecimal.ONE).multiply(new BigDecimal(x._16)).setScale(0, RoundingMode.CEILING);

            UtilXml.addChildElementValue(packageElement, x.Pounds, weightPounds.toPlainString(), requestDocument);
            UtilXml.addChildElementValue(packageElement, x.Ounces, weightOunces.toPlainString(), requestDocument);

            // TODO: handle other container types, package sizes, and machinable packages
            // IMPORTANT: Express or Priority Mail will fail if you supply a Container tag: you will get a message like
            // Invalid container type. Valid container types for Priority Mail are Flat Rate Envelope and Flat Rate Box.
            /* This is an official response from the United States Postal Service:
            The <Container> tag is used to specify the flat rate mailing options, or the type of large or oversized package being mailed.
            If you are wanting to get regular Express Mail rates, leave the <Container> tag empty, or do not include it in the request at all.
             */
            if (x.Parcel.equalsIgnoreCase(serviceCode)) {
                UtilXml.addChildElementValue(packageElement, x.Container, x._None, requestDocument);
            }
            UtilXml.addChildElementValue(packageElement, x.Size, x.REGULAR, requestDocument);
            UtilXml.addChildElementValue(packageElement, x.Machinable, x._false, requestDocument);
        }

        // send the request
        Document responseDocument = null;
        try {
            responseDocument = sendUspsRequest(x.RateV2, requestDocument, delegator, shipmentGatewayConfigId, resource, locale);
        } catch (UspsRequestException e) {
            Debug.logInfo(e, MODULE);
            return ServiceUtil.returnError(UtilProperties.getMessage(RES_ERROR,
                    x.FacilityShipmentUspsRateDomesticSendingError, UtilMisc.toMap(x.errorString, e.getMessage()), locale));
        }

        if (responseDocument == null) {
            return ServiceUtil.returnError(UtilProperties.getMessage(RES_ERROR,
                    x.FacilityShipmentRateNotAvailable, locale));
        }

        List<? extends Element> rates = UtilXml.childElementList(responseDocument.getDocumentElement(), x.Package_7431e3df);
        if (UtilValidate.isEmpty(rates)) {
            return ServiceUtil.returnError(UtilProperties.getMessage(RES_ERROR,
                    x.FacilityShipmentRateNotAvailable, locale));
        }

        BigDecimal estimateAmount = BigDecimal.ZERO;
        for (Element packageElement: rates) {
            try {
                Element postageElement = UtilXml.firstChildElement(packageElement, x.Postage);
                BigDecimal packageAmount = new BigDecimal(UtilXml.childElementValue(postageElement, x.Rate));
                estimateAmount = estimateAmount.add(packageAmount);
            } catch (NumberFormatException e) {
                Debug.logInfo(e, MODULE);
            }
        }

        Map<String, Object> result = ServiceUtil.returnSuccess();
        result.put(x.shippingEstimateAmount, estimateAmount);
        return result;
    }

    /*
     * USPS International Service Codes
     * 1 - Express Mail International
     * 2 - Priority Mail International
     * 4 - Global Express Guaranteed (Document and Non-document)
     * 5 - Global Express Guaranteed Document used
     * 6 - Global Express Guaranteed Non-Document Rectangular shape
     * 7 - Global Express Guaranteed Non-Document Non-Rectangular
     * 8 - Priority Mail Flat Rate Envelope
     * 9 - Priority Mail Flat Rate Box
     * 10 - Express Mail International Flat Rate Envelope
     * 11 - Priority Mail Large Flat Rate Box
     * 12 - Global Express Guaranteed Envelope
     * 13 - First Class Mail International Letters
     * 14 - First Class Mail International Flats
     * 15 - First Class Mail International Parcels
     * 16 - Priority Mail Small Flat Rate Box
     * 21 - PostCards
     */
    public static Map<String, Object> uspsInternationalRateInquire(DispatchContext dctx, UspsServicesContext context) {
        Delegator delegator = dctx.getDelegator();
        String shipmentGatewayConfigId = (String) context.get(x.shipmentGatewayConfigId);
        String resource = (String) context.get(x.configProps);
        Locale locale = (Locale) context.get(x.locale);

        // check for 0 weight
        BigDecimal shippableWeight = (BigDecimal) context.get(x.shippableWeight);
        if (shippableWeight.compareTo(BigDecimal.ZERO) == 0) {
            return ServiceUtil.returnError(UtilProperties.getMessage(RES_ERROR,
                    x.FacilityShipmentUspsShippableWeightMustGreaterThanZero, locale));
        }

        // get the destination country
        String destinationCountry = null;
        String shippingContactMechId = (String) context.get(x.shippingContactMechId);
        if (UtilValidate.isNotEmpty(shippingContactMechId)) {
            try {
                GenericValue shipToAddress = getPostalAddressValue(delegator, shippingContactMechId);
                if (domesticCountries.contains(shipToAddress.get(x.countryGeoId))) {
                    return ServiceUtil.returnError(UtilProperties.getMessage(RES_ERROR,
                            x.FacilityShipmentUspsRateInternationCannotBeUsedForUsDestinations, locale));
                }
                if (UtilValidate.isNotEmpty(shipToAddress.getString(x.countryGeoId))) {
                    GenericValue countryGeo = shipToAddress.getRelatedOne(x.CountryGeo, false);
                    // TODO: Test against all country geoNames against what USPS expects
                    destinationCountry = countryGeo.getString(x.geoName);
                }
            } catch (GenericEntityException e) {
                Debug.logError(e, MODULE);
            }
        }
        if (UtilValidate.isEmpty(destinationCountry)) {
            return ServiceUtil.returnError(UtilProperties.getMessage(RES_ERROR,
                    x.FacilityShipmentUspsUnableDetermineDestinationCountry, locale));
        }

        // get the service code
        String serviceCode = null;
        try {
            GenericValue carrierShipmentMethod = getCarrierShipmentMethodValue(delegator, context.get(x.shipmentMethodTypeId),
                    context.get(x.carrierPartyId), context.get(x.carrierRoleTypeId));
            if (carrierShipmentMethod != null) {
                serviceCode = carrierShipmentMethod.getString(x.carrierServiceCode);
            }
        } catch (GenericEntityException e) {
            Debug.logError(e, MODULE);
        }
        if (UtilValidate.isEmpty(serviceCode)) {
            return ServiceUtil.returnError(UtilProperties.getMessage(RES_ERROR,
                    x.FacilityShipmentUspsUnableDetermineServiceCode, locale));
        }

        BigDecimal maxWeight;
        String maxWeightStr = getShipmentGatewayConfigValue(delegator, shipmentGatewayConfigId, x.maxEstimateWeight,
                resource, x.shipment_usps_max_estimate_weight, x._70);
        try {
            maxWeight = new BigDecimal(maxWeightStr);
        } catch (NumberFormatException e) {
            Debug.logWarning(x.Error_parsing_max_estimate_weight_string + maxWeightStr + x.using_default_instead, MODULE);
            maxWeight = new BigDecimal(x._70);
        }

        List<Map<String, Object>> shippableItemInfo = UtilGenerics.cast(context.get(x.shippableItemInfo));
        List<Map<String, BigDecimal>> packages = ShipmentWorker.getPackageSplit(dctx, shippableItemInfo, maxWeight);
        boolean isOnePackage = packages.size() == 1; // use shippableWeight if there's only one package

        // create the request document
        Document requestDocument = createUspsRequestDocument(x.IntlRateRequest, false, delegator, shipmentGatewayConfigId, resource);

        // TODO: Up to 25 packages can be included per request - handle more than 25
        for (ListIterator<Map<String, BigDecimal>> li = packages.listIterator(); li.hasNext();) {
            Map<String, BigDecimal> packageMap = li.next();

            Element packageElement = UtilXml.addChildElement(requestDocument.getDocumentElement(), x.Package_7431e3df, requestDocument);
            packageElement.setAttribute(x.ID, String.valueOf(li.nextIndex() - 1)); // use zero-based index (see examples)

            BigDecimal packageWeight = isOnePackage ? shippableWeight : ShipmentWorker.calcPackageWeight(dctx, packageMap,
                    shippableItemInfo, BigDecimal.ZERO);
            if (packageWeight.compareTo(BigDecimal.ZERO) == 0) {
                continue;
            }
            Integer[] weightPoundsOunces = convertPoundsToPoundsOunces(packageWeight);
            // for Parcel post, the weight must be at least 1 lb
            if (x.PARCEL.equals(serviceCode.toUpperCase(Locale.getDefault())) && (weightPoundsOunces[0] < 1)) {
                weightPoundsOunces[0] = 1;
                weightPoundsOunces[1] = 0;
            }
            UtilXml.addChildElementValue(packageElement, x.Pounds, weightPoundsOunces[0].toString(), requestDocument);
            UtilXml.addChildElementValue(packageElement, x.Ounces, weightPoundsOunces[1].toString(), requestDocument);

            UtilXml.addChildElementValue(packageElement, x.Machinable, x._False, requestDocument);
            UtilXml.addChildElementValue(packageElement, x.MailType, x.Package_7431e3df, requestDocument);

            // TODO: Add package value so that an insurance fee can be returned

            UtilXml.addChildElementValue(packageElement, x.Country, destinationCountry, requestDocument);
        }

        // send the request
        Document responseDocument = null;
        try {
            responseDocument = sendUspsRequest(x.IntlRate, requestDocument, delegator, shipmentGatewayConfigId, resource, locale);
        } catch (UspsRequestException e) {
            Debug.logInfo(e, MODULE);
            return ServiceUtil.returnError(UtilProperties.getMessage(RES_ERROR,
                    x.FacilityShipmentUspsRateInternationalSendingError, UtilMisc.toMap(x.errorString, e.getMessage()), locale));
        }

        if (responseDocument == null) {
            return ServiceUtil.returnError(UtilProperties.getMessage(RES_ERROR,
                    x.FacilityShipmentRateNotAvailable, locale));
        }

        List<? extends Element> packageElements = UtilXml.childElementList(responseDocument.getDocumentElement(), x.Package_7431e3df);
        if (UtilValidate.isEmpty(packageElements)) {
            return ServiceUtil.returnError(UtilProperties.getMessage(RES_ERROR,
                    x.FacilityShipmentRateNotAvailable, locale));
        }

        BigDecimal estimateAmount = BigDecimal.ZERO;
        for (Element packageElement: packageElements) {
            Element errorElement = UtilXml.firstChildElement(packageElement, x.Error);
            if (errorElement != null) {
                String errorDescription = UtilXml.childElementValue(errorElement, x.Description);
                Debug.logInfo(x.USPS_International_Rate_Calculation_returned_a_package_error + errorDescription, MODULE);
                return ServiceUtil.returnError(UtilProperties.getMessage(RES_ERROR,
                        x.FacilityShipmentRateNotAvailable, locale));
            }
            List<? extends Element> serviceElements = UtilXml.childElementList(packageElement, x.Service);
            for (Element serviceElement : serviceElements) {
                String respServiceCode = serviceElement.getAttribute(x.ID);
                if (!serviceCode.equalsIgnoreCase(respServiceCode)) {
                    continue;
                }
                try {
                    BigDecimal packageAmount = new BigDecimal(UtilXml.childElementValue(serviceElement, x.Postage));
                    estimateAmount = estimateAmount.add(packageAmount);
                } catch (NumberFormatException e) {
                    Debug.logInfo(x.USPS_International_Rate_Calculation_returned_an_unparsable_postage_amount
                            + UtilXml.childElementValue(serviceElement, x.Postage), MODULE);
                    return ServiceUtil.returnError(UtilProperties.getMessage(RES_ERROR,
                            x.FacilityShipmentRateNotAvailable, locale));
                }
            }
        }

        Map<String, Object> result = ServiceUtil.returnSuccess();
        result.put(x.shippingEstimateAmount, estimateAmount);
        return result;
    }

    // lifted from UpsServices with no changes - 2004.09.06 JFE
    /*

    Track/Confirm Samples: (API=TrackV2)

    Request:
    <TrackRequest USERID="xxxxxxxx" PASSWORD="xxxxxxxx">
        <TrackID ID="EJ958083578US"></TrackID>
    </TrackRequest>

    Response:
    <TrackResponse>
        <TrackInfo ID="EJ958083578US">
            <TrackSummary>Your item was delivered at 8:10 am on June 1 in Wilmington DE 19801.</TrackSummary>
            <TrackDetail>May 30 11:07 am NOTICE LEFT WILMINGTON DE 19801.</TrackDetail>
            <TrackDetail>May 30 10:08 am ARRIVAL AT UNIT WILMINGTON DE 19850.</TrackDetail>
            <TrackDetail>May 29 9:55 am ACCEPT OR PICKUP EDGEWATER NJ 07020.</TrackDetail>
        </TrackInfo>
    </TrackResponse>

    */

    public static Map<String, Object> uspsTrackConfirm(DispatchContext dctx, UspsServicesContext context) {
        Delegator delegator = dctx.getDelegator();
        String shipmentGatewayConfigId = (String) context.get(x.shipmentGatewayConfigId);
        String resource = (String) context.get(x.configProps);
        Locale locale = (Locale) context.get(x.locale);

        Document requestDocument = createUspsRequestDocument(x.TrackRequest, true, delegator, shipmentGatewayConfigId, resource);

        Element trackingElement = UtilXml.addChildElement(requestDocument.getDocumentElement(), x.TrackID, requestDocument);
        trackingElement.setAttribute(x.ID, (String) context.get(x.trackingId));

        Document responseDocument = null;
        try {
            responseDocument = sendUspsRequest(x.TrackV2, requestDocument, delegator, shipmentGatewayConfigId, resource, locale);
        } catch (UspsRequestException e) {
            Debug.logInfo(e, MODULE);
            return ServiceUtil.returnError(UtilProperties.getMessage(RES_ERROR,
                    x.FacilityShipmentUspsTrackingSendingError, UtilMisc.toMap(x.errorString, e.getMessage()), locale));
        }

        Element trackInfoElement = UtilXml.firstChildElement(responseDocument.getDocumentElement(), x.TrackInfo);
        if (trackInfoElement == null) {
            return ServiceUtil.returnError(UtilProperties.getMessage(RES_ERROR,
                    x.FacilityShipmentUspsTrackingIncompleteResponse, locale));
        }

        Map<String, Object> result = ServiceUtil.returnSuccess();

        result.put(x.trackingSummary, UtilXml.childElementValue(trackInfoElement, x.TrackSummary));

        List<? extends Element> detailElementList = UtilXml.childElementList(trackInfoElement, x.TrackDetail);
        if (UtilValidate.isNotEmpty(detailElementList)) {
            List<String> trackingDetailList = new LinkedList<>();
            for (Element detailElement: detailElementList) {
                trackingDetailList.add(UtilXml.elementValue(detailElement));
            }
            result.put(x.trackingDetailList, trackingDetailList);
        }

        return result;
    }

    /*

    Address Standardization Samples: (API=Verify)

    Request:
    <AddressValidateRequest USERID="xxxxxxx" PASSWORD="xxxxxxx">
        <Address ID="0">
            <Address1></Address1>
            <Address2>6406 Ivy Lane</Address2>
            <City>Greenbelt</City>
            <State>MD</State>
            <Zip5></Zip5>
            <Zip4></Zip4>
        </Address>
    </AddressValidateRequest>

    Response:
    <AddressValidateResponse>
        <Address ID="0">
            <Address2>6406 IVY LN</Address2>
            <City>GREENBELT</City>
            <State>MD</State>
            <Zip5>20770</Zip5>
            <Zip4>1440</Zip4>
        </Address>
    </AddressValidateResponse>

    Note:
        The service parameters address1 and addess2 follow the OFBiz naming convention,
        and are converted to USPS conventions internally
        (OFBiz address1 = USPS address2, OFBiz address2 = USPS address1)

    */

    public static Map<String, Object> uspsAddressValidation(DispatchContext dctx, UspsServicesContext context) {
        Delegator delegator = dctx.getDelegator();
        String shipmentGatewayConfigId = (String) context.get(x.shipmentGatewayConfigId);
        String resource = (String) context.get(x.configProps);
        String state = (String) context.get(x.state);
        String city = (String) context.get(x.city);
        String zip5 = (String) context.get(x.zip5);
        Locale locale = (Locale) context.get(x.locale);
        if ((UtilValidate.isEmpty(state) && UtilValidate.isEmpty(city) && UtilValidate.isEmpty(zip5)) // No state, city or zip5
                || (UtilValidate.isEmpty(zip5) && (UtilValidate.isEmpty(state) || UtilValidate.isEmpty(city)))) {
            // Both state and city are required if no zip5
            Debug.logError(x.USPS_address_validation_requires_either_zip5_or_city_and_state, MODULE);
            return ServiceUtil.returnError(UtilProperties.getMessage(RES_ERROR,
                    x.FacilityShipmentUspsAddressValidationStateAndCityOrZipRqd, locale));
        }

        Document requestDocument = createUspsRequestDocument(x.AddressValidateRequest, true, delegator, shipmentGatewayConfigId, resource);

        Element addressElement = UtilXml.addChildElement(requestDocument.getDocumentElement(), x.Address, requestDocument);
        addressElement.setAttribute(x.ID, x._0);

        // 38 chars max
        UtilXml.addChildElementValue(addressElement, x.FirmName, (String) context.get(x.firmName), requestDocument);
        // 38 chars max
        UtilXml.addChildElementValue(addressElement, x.Address1, (String) context.get(x.address2), requestDocument);
        // 38 chars max
        UtilXml.addChildElementValue(addressElement, x.Address2, (String) context.get(x.address1), requestDocument);
        // 15 chars max
        UtilXml.addChildElementValue(addressElement, x.City, (String) context.get(x.city), requestDocument);

        UtilXml.addChildElementValue(addressElement, x.State, (String) context.get(x.state), requestDocument);
        UtilXml.addChildElementValue(addressElement, x.Zip5, (String) context.get(x.zip5), requestDocument);
        UtilXml.addChildElementValue(addressElement, x.Zip4, (String) context.get(x.zip4), requestDocument);

        Document responseDocument = null;
        try {
            responseDocument = sendUspsRequest(x.Verify, requestDocument, delegator, shipmentGatewayConfigId, resource, locale);
        } catch (UspsRequestException e) {
            Debug.logInfo(e, MODULE);
            return ServiceUtil.returnError(UtilProperties.getMessage(RES_ERROR,
                    x.FacilityShipmentUspsAddressValidationSendingError, UtilMisc.toMap(x.errorString, e.getMessage()), locale));
        }

        Element respAddressElement = UtilXml.firstChildElement(responseDocument.getDocumentElement(), x.Address);
        if (respAddressElement == null) {
            return ServiceUtil.returnError(UtilProperties.getMessage(RES_ERROR,
                    x.FacilityShipmentUspsAddressValidationIncompleteResponse, locale));
        }

        Element respErrorElement = UtilXml.firstChildElement(respAddressElement, x.Error);
        if (respErrorElement != null) {
            return ServiceUtil.returnError(UtilProperties.getMessage(RES_ERROR,
                    x.FacilityShipmentUspsAddressValidationIncompleteResponse,
                    UtilMisc.toMap(x.errorString, UtilXml.childElementValue(respErrorElement, x.Description)), locale));
        }

        Map<String, Object> result = ServiceUtil.returnSuccess();

        // Note: a FirmName element is not returned if empty
        String firmName = UtilXml.childElementValue(respAddressElement, x.FirmName);
        if (UtilValidate.isNotEmpty(firmName)) {
            result.put(x.firmName, firmName);
        }

        // Note: an Address1 element is not returned if empty
        String address1 = UtilXml.childElementValue(respAddressElement, x.Address1);
        if (UtilValidate.isNotEmpty(address1)) {
            result.put(x.address2, address1);
        }

        result.put(x.address1, UtilXml.childElementValue(respAddressElement, x.Address2));
        result.put(x.city, UtilXml.childElementValue(respAddressElement, x.City));
        result.put(x.state, UtilXml.childElementValue(respAddressElement, x.State));
        result.put(x.zip5, UtilXml.childElementValue(respAddressElement, x.Zip5));
        result.put(x.zip4, UtilXml.childElementValue(respAddressElement, x.Zip4));
        Element returnTextElement = UtilXml.firstChildElement(respAddressElement, x.ReturnText);
        if (returnTextElement != null) {
            result.put(x.returnText, UtilXml.elementValue(returnTextElement));
        }
        return result;
    }

    /*

    City/State Lookup Samples: (API=CityStateLookup)

    Request:
    <CityStateLookupRequest USERID="xxxxxxx" PASSWORD="xxxxxxx">
        <ZipCode ID="0">
            <Zip5>90210</Zip5>
        </ZipCode>
    </CityStateLookupRequest>

    Response:
    <CityStateLookupResponse>
        <ZipCode ID="0">
            <Zip5>90210</Zip5>
            <City>BEVERLY HILLS</City>
            <State>CA</State>
        </ZipCode>
    </CityStateLookupResponse>

    */

    public static Map<String, Object> uspsCityStateLookup(DispatchContext dctx, UspsServicesContext context) {
        Delegator delegator = dctx.getDelegator();
        String shipmentGatewayConfigId = (String) context.get(x.shipmentGatewayConfigId);
        String resource = (String) context.get(x.configProps);
        Locale locale = (Locale) context.get(x.locale);

        Document requestDocument = createUspsRequestDocument(x.CityStateLookupRequest, true, delegator, shipmentGatewayConfigId, resource);

        Element zipCodeElement = UtilXml.addChildElement(requestDocument.getDocumentElement(), x.ZipCode, requestDocument);
        zipCodeElement.setAttribute(x.ID, x._0);

        String zipCode = ((String) context.get(x.zip5)).trim(); // trim leading/trailing spaces

        // only the first 5 digits are used, the rest are ignored
        UtilXml.addChildElementValue(zipCodeElement, x.Zip5, zipCode, requestDocument);

        Document responseDocument = null;
        try {
            responseDocument = sendUspsRequest(x.CityStateLookup, requestDocument, delegator, shipmentGatewayConfigId, resource, locale);
        } catch (UspsRequestException e) {
            Debug.logInfo(e, MODULE);
            return ServiceUtil.returnError(UtilProperties.getMessage(RES_ERROR,
                    x.FacilityShipmentUspsCityStateLookupSendingError,
                    UtilMisc.toMap(x.errorString, e.getMessage()), locale));
        }

        Element respAddressElement = UtilXml.firstChildElement(responseDocument.getDocumentElement(), x.ZipCode);
        if (respAddressElement == null) {
            return ServiceUtil.returnError(UtilProperties.getMessage(RES_ERROR,
                    x.FacilityShipmentUspsCityStateLookupIncompleteResponse, locale));
        }

        Element respErrorElement = UtilXml.firstChildElement(respAddressElement, x.Error);
        if (respErrorElement != null) {
            return ServiceUtil.returnFailure(UtilProperties.getMessage(RES_ERROR,
                    x.FacilityShipmentUspsCityStateLookupResponseError,
                    UtilMisc.toMap(x.errorString, UtilXml.childElementValue(respErrorElement, x.Description)), locale));
        }

        Map<String, Object> result = ServiceUtil.returnSuccess();

        String city = UtilXml.childElementValue(respAddressElement, x.City);
        if (UtilValidate.isEmpty(city)) {
            return ServiceUtil.returnFailure(UtilProperties.getMessage(RES_ERROR,
                    x.FacilityShipmentUspsCityStateLookupIncompleteCityElement, locale));
        }
        result.put(x.city, city);

        String state = UtilXml.childElementValue(respAddressElement, x.State);
        if (UtilValidate.isEmpty(state)) {
            return ServiceUtil.returnFailure(UtilProperties.getMessage(RES_ERROR,
                    x.FacilityShipmentUspsCityStateLookupIncompleteStateElement, locale));
        }
        result.put(x.state, state);

        return result;
    }

    /*

    Service Standards Samples:

    Priority Mail: (API=PriorityMail)

        Request:
        <PriorityMailRequest USERID="xxxxxxx" PASSWORD="xxxxxxx">
            <OriginZip>4</OriginZip>
            <DestinationZip>4</DestinationZip>
        </PriorityMailRequest>

        Response:
        <PriorityMailResponse>
            <OriginZip>4</OriginZip>
            <DestinationZip>4</DestinationZip>
            <Days>1</Days>
        </PriorityMailResponse>

    Package Services: (API=StandardB)

        Request:
        <StandardBRequest USERID="xxxxxxx" PASSWORD="xxxxxxx">
            <OriginZip>4</OriginZip>
            <DestinationZip>4</DestinationZip>
        </StandardBRequest>

        Response:
        <StandardBResponse>
            <OriginZip>4</OriginZip>
            <DestinationZip>4</DestinationZip>
            <Days>2</Days>
        </StandardBResponse>

    Note:
        When submitting ZIP codes, only the first 3 digits are used.
        If a 1- or 2-digit ZIP code is entered, leading zeros are implied.
        If a 4- or 5-digit ZIP code is entered, the last digits  will be ignored.

    */

    public static Map<String, Object> uspsPriorityMailStandard(DispatchContext dctx, UspsServicesContext context) {
        UspsServicesContext subContext = new UspsServicesContext(UtilMisc.makeMapWritable(context));
        subContext.put(x.serviceType, x.PriorityMail);
        return uspsServiceStandards(dctx, subContext);
    }

    public static Map<String, Object> uspsPackageServicesStandard(DispatchContext dctx, UspsServicesContext context) {
        UspsServicesContext subContext = new UspsServicesContext(UtilMisc.makeMapWritable(context));
        subContext.put(x.serviceType, x.StandardB);
        return uspsServiceStandards(dctx, subContext);
    }

    private static Map<String, Object> uspsServiceStandards(DispatchContext dctx, UspsServicesContext context) {
        Delegator delegator = dctx.getDelegator();
        String shipmentGatewayConfigId = (String) context.get(x.shipmentGatewayConfigId);
        String resource = (String) context.get(x.configProps);
        String type = (String) context.get(x.serviceType);
        Locale locale = (Locale) context.get(x.locale);
        if (!type.matches(x.PriorityMail_StandardB)) {
            return ServiceUtil.returnError(UtilProperties.getMessage(RES_ERROR,
                    x.FacilityShipmentUspsUnsupporteServiceType,
                    UtilMisc.toMap(x.serviceType, type), locale));
        }

        Document requestDocument = createUspsRequestDocument(type + x.Request, true, delegator, shipmentGatewayConfigId, resource);

        UtilXml.addChildElementValue(requestDocument.getDocumentElement(), x.OriginZip,
                (String) context.get(x.originZip), requestDocument);
        UtilXml.addChildElementValue(requestDocument.getDocumentElement(), x.DestinationZip,
                (String) context.get(x.destinationZip), requestDocument);

        Document responseDocument = null;
        try {
            responseDocument = sendUspsRequest(type, requestDocument, delegator, shipmentGatewayConfigId, resource, locale);
        } catch (UspsRequestException e) {
            Debug.logInfo(e, MODULE);
            return ServiceUtil.returnError(UtilProperties.getMessage(RES_ERROR,
                    x.FacilityShipmentUspsServiceStandardSendingError,
                    UtilMisc.toMap(x.serviceType, type, x.errorString, e.getMessage()), locale));
        }

        Map<String, Object> result = ServiceUtil.returnSuccess();

        String days = UtilXml.childElementValue(responseDocument.getDocumentElement(), x.Days);
        if (UtilValidate.isEmpty(days)) {
            return ServiceUtil.returnError(UtilProperties.getMessage(RES_ERROR,
                    x.FacilityShipmentUspsServiceStandardResponseIncompleteDaysElement,
                    UtilMisc.toMap(x.serviceType, type), locale));
        }
        result.put(x.days, days);

        return result;
    }



    /*

    Domestic Rate Calculator Samples: (API=Rate)

    Request:
    <RateRequest USERID="xxxxxx" PASSWORD="xxxxxxx">
        <Package ID="0">
            <Service>Priority</Service>
            <ZipOrigination>20770</ZipOrigination>
            <ZipDestination>09021</ZipDestination>
            <Pounds>5</Pounds>
            <Ounces>1</Ounces>
            <Container>None</Container>
            <Size>Regular</Size>
            <Machinable>False</Machinable>
        </Package>
    </RateRequest>

    Response:
    <RateResponse>
        <Package ID="0">
            <Service>Priority</Service>
            <ZipOrigination>20770</ZipOrigination>
            <ZipDestination>09021</ZipDestination>
            <Pounds>5</Pounds>
            <Ounces>1</Ounces>
            <Container>None</Container>
            <Size>REGULAR</Size>
            <Machinable>FALSE</Machinable>
            <Zone>3</Zone>
            <Postage>7.90</Postage>
            <RestrictionCodes>B-B1-C-D-U</RestrictionCodes>
            <RestrictionDescription>
            B. Form 2976-A is required for all mail weighing 16 ounces or more, with exceptions noted below.
            In addition, mailers must properly complete required customs documentation when mailing any potentially
            dutiable mail addressed to an APO or FPO regardless of weight. B1. Form 2976 or 2976-A is required.
            Articles are liable for customs duty and/or purchase tax unless they are bona fide gifts intended for
            use by military personnel or their dependents. C. Cigarettes and other tobacco products are prohibited.
            D. Coffee is prohibited. U. Parcels must weigh less than 16 ounces when addressed to Box R.
            </RestrictionDescription>
        </Package>
    </RateResponse>

    */

    public static Map<String, Object> uspsDomesticRate(DispatchContext dctx, UspsServicesContext context) {
        Delegator delegator = dctx.getDelegator();
        String shipmentGatewayConfigId = (String) context.get(x.shipmentGatewayConfigId);
        String resource = (String) context.get(x.configProps);
        Locale locale = (Locale) context.get(x.locale);

        Document requestDocument = createUspsRequestDocument(x.RateRequest, true, delegator, shipmentGatewayConfigId, resource);

        Element packageElement = UtilXml.addChildElement(requestDocument.getDocumentElement(), x.Package_7431e3df, requestDocument);
        packageElement.setAttribute(x.ID, x._0);

        UtilXml.addChildElementValue(packageElement, x.Service, (String) context.get(x.service), requestDocument);
        UtilXml.addChildElementValue(packageElement, x.ZipOrigination, (String) context.get(x.originZip), requestDocument);
        UtilXml.addChildElementValue(packageElement, x.ZipDestination, (String) context.get(x.destinationZip), requestDocument);
        UtilXml.addChildElementValue(packageElement, x.Pounds, (String) context.get(x.pounds), requestDocument);
        UtilXml.addChildElementValue(packageElement, x.Ounces, (String) context.get(x.ounces), requestDocument);

        String container = (String) context.get(x.container);
        if (UtilValidate.isEmpty(container)) {
            container = x._None;
        }
        UtilXml.addChildElementValue(packageElement, x.Container, container, requestDocument);

        String size = (String) context.get(x.size);
        if (UtilValidate.isEmpty(size)) {
            size = x.Regular;
        }
        UtilXml.addChildElementValue(packageElement, x.Size, size, requestDocument);

        String machinable = (String) context.get(x.machinable);
        if (UtilValidate.isEmpty(machinable)) {
            machinable = x._False;
        }
        UtilXml.addChildElementValue(packageElement, x.Machinable, machinable, requestDocument);

        Document responseDocument = null;
        try {
            responseDocument = sendUspsRequest(x.Rate, requestDocument, delegator, shipmentGatewayConfigId, resource, locale);
        } catch (UspsRequestException e) {
            Debug.logInfo(e, MODULE);
            return ServiceUtil.returnError(UtilProperties.getMessage(RES_ERROR,
                    x.FacilityShipmentUspsRateDomesticSendingError,
                    UtilMisc.toMap(x.errorString, e.getMessage()), locale));
        }

        Element respPackageElement = UtilXml.firstChildElement(responseDocument.getDocumentElement(), x.Package_7431e3df);
        if (respPackageElement == null) {
            return ServiceUtil.returnError(UtilProperties.getMessage(RES_ERROR,
                    x.FacilityShipmentUspsRateDomesticResponseIncompleteElementPackage, locale));
        }

        Element respErrorElement = UtilXml.firstChildElement(respPackageElement, x.Error);
        if (respErrorElement != null) {
            return ServiceUtil.returnError(UtilProperties.getMessage(RES_ERROR,
                    x.FacilityShipmentUspsRateDomesticResponseError,
                    UtilMisc.toMap(x.errorString, UtilXml.childElementValue(respErrorElement, x.Description)), locale));
        }

        Map<String, Object> result = ServiceUtil.returnSuccess();

        String zone = UtilXml.childElementValue(respPackageElement, x.Zone);
        if (UtilValidate.isEmpty(zone)) {
            return ServiceUtil.returnError(UtilProperties.getMessage(RES_ERROR,
                    x.FacilityShipmentUspsRateDomesticResponseIncompleteElementZone, locale));
        }
        result.put(x.zone, zone);

        String postage = UtilXml.childElementValue(respPackageElement, x.Postage);
        if (UtilValidate.isEmpty(postage)) {
            return ServiceUtil.returnError(UtilProperties.getMessage(RES_ERROR,
                    x.FacilityShipmentUspsRateDomesticResponseIncompleteElementPostage, locale));
        }
        result.put(x.postage, postage);

        String restrictionCodes = UtilXml.childElementValue(respPackageElement, x.RestrictionCodes);
        if (UtilValidate.isNotEmpty(restrictionCodes)) {
            result.put(x.restrictionCodes, restrictionCodes);
        }

        String restrictionDesc = UtilXml.childElementValue(respPackageElement, x.RestrictionDescription);
        if (UtilValidate.isNotEmpty(restrictionCodes)) {
            result.put(x.restrictionDesc, restrictionDesc);
        }

        return result;
    }

    // Warning: I don't think the following 2 services were completed or fully tested - 2004.09.06 JFE

    /* --- ShipmentRouteSegment services --------------------------------------------------------------------------- */

    public static Map<String, Object> uspsUpdateShipmentRateInfo(DispatchContext dctx, UspsServicesContext context) {
        Delegator delegator = dctx.getDelegator();
        LocalDispatcher dispatcher = dctx.getDispatcher();
        String shipmentId = (String) context.get(x.shipmentId);
        String shipmentRouteSegmentId = (String) context.get(x.shipmentRouteSegmentId);
        Locale locale = (Locale) context.get(x.locale);

        Map<String, Object> shipmentGatewayConfig = ShipmentServices.getShipmentGatewayConfigFromShipment(delegator, shipmentId, locale);
        String shipmentGatewayConfigId = (String) shipmentGatewayConfig.get(x.shipmentGatewayConfigId);
        String resource = (String) shipmentGatewayConfig.get(x.configProps);
        if (UtilValidate.isEmpty(shipmentGatewayConfigId) && UtilValidate.isEmpty(resource)) {
            return ServiceUtil.returnError(UtilProperties.getMessage(RES_ERROR,
                    x.FacilityShipmentUspsGatewayNotAvailable, locale));
        }

        try {
            GenericValue shipmentRouteSegment = getShipmentRouteSegmentValue(delegator, shipmentId, shipmentRouteSegmentId);
            if (shipmentRouteSegment == null) {
                return ServiceUtil.returnError(UtilProperties.getMessage(RES_ERROR,
                        x.ProductShipmentRouteSegmentNotFound,
                        UtilMisc.toMap(x.shipmentId, shipmentId, x.shipmentRouteSegmentId, shipmentRouteSegmentId), locale));
            }

            // ensure the carrier is USPS
            if (!x.USPS.equals(shipmentRouteSegment.getString(x.carrierPartyId))) {
                return ServiceUtil.returnError(UtilProperties.getMessage(RES_ERROR,
                        x.FacilityShipmentUspsNotRouteSegmentCarrier,
                        UtilMisc.toMap(x.shipmentRouteSegmentId, shipmentRouteSegmentId, x.shipmentId, shipmentId), locale));
            }

            // get the origin address
            GenericValue originAddress = shipmentRouteSegment.getRelatedOne(x.OriginPostalAddress, false);
            if (originAddress == null) {
                return ServiceUtil.returnError(UtilProperties.getMessage(RES_ERROR,
                        x.FacilityShipmentRouteSegmentOriginPostalAddressNotFound,
                        UtilMisc.toMap(x.shipmentId, shipmentId, x.shipmentRouteSegmentId, shipmentRouteSegmentId), locale));
            }
            if (!x.USA.equals(originAddress.getString(x.countryGeoId))) {
                return ServiceUtil.returnError(UtilProperties.getMessage(RES_ERROR,
                        x.FacilityShipmentUspsRouteSegmentOriginCountryGeoNotInUsa,
                        UtilMisc.toMap(x.shipmentId, shipmentId, x.shipmentRouteSegmentId, shipmentRouteSegmentId), locale));
            }
            String originZip = originAddress.getString(x.postalCode);
            if (UtilValidate.isEmpty(originZip)) {
                return ServiceUtil.returnError(UtilProperties.getMessage(RES_ERROR,
                        x.FacilityShipmentUspsRouteSegmentOriginZipCodeMissing,
                        UtilMisc.toMap(x.contactMechId, originAddress.getString(x.contactMechId)), locale));
            }

            // get the destination address
            GenericValue destinationAddress = shipmentRouteSegment.getRelatedOne(x.DestPostalAddress, false);
            if (destinationAddress == null) {
                return ServiceUtil.returnError(UtilProperties.getMessage(RES_ERROR,
                        x.FacilityShipmentRouteSegmentDestPostalAddressNotFound,
                        UtilMisc.toMap(x.shipmentId, shipmentId, x.shipmentRouteSegmentId, shipmentRouteSegmentId), locale));
            }
            if (!x.USA.equals(destinationAddress.getString(x.countryGeoId))) {
                return ServiceUtil.returnError(UtilProperties.getMessage(RES_ERROR,
                        x.FacilityShipmentUspsRouteSegmentOriginCountryGeoNotInUsa,
                        UtilMisc.toMap(x.shipmentId, shipmentId, x.shipmentRouteSegmentId, shipmentRouteSegmentId), locale));
            }
            String destinationZip = destinationAddress.getString(x.postalCode);
            if (UtilValidate.isEmpty(destinationZip)) {
                return ServiceUtil.returnError(UtilProperties.getMessage(RES_ERROR,
                        x.FacilityShipmentUspsRouteSegmentDestinationZipCodeMissing,
                        UtilMisc.toMap(x.contactMechId, destinationAddress.getString(x.contactMechId)), locale));
            }

            // get the service type from the CarrierShipmentMethod
            String shipmentMethodTypeId = shipmentRouteSegment.getString(x.shipmentMethodTypeId);
            String partyId = shipmentRouteSegment.getString(x.carrierPartyId);

            GenericValue carrierShipmentMethod = getCarrierShipmentMethodValue(delegator, shipmentMethodTypeId, partyId, x.CARRIER);
            if (carrierShipmentMethod == null) {
                return ServiceUtil.returnError(UtilProperties.getMessage(RES_ERROR,
                        x.FacilityShipmentUspsNoCarrierShipmentMethod,
                        UtilMisc.toMap(x.carrierPartyId, partyId, x.shipmentMethodTypeId, shipmentMethodTypeId), locale));
            }
            String serviceType = carrierShipmentMethod.getString(x.carrierServiceCode);
            if (UtilValidate.isEmpty(serviceType)) {
                return ServiceUtil.returnError(UtilProperties.getMessage(RES_ERROR,
                        x.FacilityShipmentUspsNoCarrierServiceCodeFound,
                        UtilMisc.toMap(x.carrierPartyId, partyId, x.shipmentMethodTypeId, shipmentMethodTypeId), locale));
            }

            // get the packages for this shipment route segment
            List<GenericValue> shipmentPackageRouteSegList = shipmentRouteSegment.getRelated(x.ShipmentPackageRouteSeg,
                    null, UtilMisc.toList(x.shipmentPackageSeqId_39d5d38d), false);
            if (UtilValidate.isEmpty(shipmentPackageRouteSegList)) {
                return ServiceUtil.returnError(UtilProperties.getMessage(RES_ERROR,
                        x.FacilityShipmentPackageRouteSegsNotFound,
                        UtilMisc.toMap(x.shipmentId, shipmentId, x.shipmentRouteSegmentId, shipmentRouteSegmentId), locale));
            }

            BigDecimal actualTransportCost = BigDecimal.ZERO;

            String carrierDeliveryZone = null;
            String carrierRestrictionCodes = null;
            String carrierRestrictionDesc = null;

            // send a new request for each package
            for (Iterator<GenericValue> i = shipmentPackageRouteSegList.iterator(); i.hasNext();) {

                GenericValue shipmentPackageRouteSeg = i.next();
                Document requestDocument = createUspsRequestDocument(x.RateRequest, true, delegator, shipmentGatewayConfigId, resource);

                Element packageElement = UtilXml.addChildElement(requestDocument.getDocumentElement(), x.Package_7431e3df, requestDocument);
                packageElement.setAttribute(x.ID, x._0);

                UtilXml.addChildElementValue(packageElement, x.Service, serviceType, requestDocument);
                UtilXml.addChildElementValue(packageElement, x.ZipOrigination, originZip, requestDocument);
                UtilXml.addChildElementValue(packageElement, x.ZipDestination, destinationZip, requestDocument);

                GenericValue shipmentPackage = null;
                shipmentPackage = shipmentPackageRouteSeg.getRelatedOne(x.ShipmentPackage, false);

                // weight elements - Pounds, Ounces
                String weightStr = shipmentPackage.getString(x.weight);
                if (UtilValidate.isEmpty(weightStr)) {
                    return ServiceUtil.returnError(UtilProperties.getMessage(RES_ERROR,
                            x.FacilityShipmentUspsWeightNotFound,
                            UtilMisc.toMap(x.shipmentId, shipmentPackage.getString(x.shipmentId),
                                    x.shipmentPackageSeqId, shipmentPackage.getString(x.shipmentPackageSeqId)), locale));
                }

                BigDecimal weight = BigDecimal.ZERO;
                try {
                    weight = new BigDecimal(weightStr);
                } catch (NumberFormatException nfe) {
                    Debug.logError(nfe, MODULE); // TODO: handle exception
                }

                String weightUomId = shipmentPackage.getString(x.weightUomId);
                if (UtilValidate.isEmpty(weightUomId)) {
                    weightUomId = x.WT_lb; // assume weight is in pounds
                }
                if (!x.WT_lb.equals(weightUomId)) {
                    // attempt a conversion to pounds
                    Map<String, Object> result;
                    try {
                        result = dispatcher.runSync(x.convertUom, UtilMisc.<String, Object>toMap(x.uomId, weightUomId,
                                x.uomIdTo, x.WT_lb, x.originalValue, weight));
                        if (ServiceUtil.isError(result)) {
                            return ServiceUtil.returnError(ServiceUtil.getErrorMessage(result));
                        }
                    } catch (GenericServiceException ex) {
                        return ServiceUtil.returnError(UtilProperties.getMessage(RES_ERROR,
                                x.FacilityShipmentUspsWeightConversionError,
                                UtilMisc.toMap(x.errorString, ex.getMessage()), locale));
                    }

                    if (result.get(ModelService.RESPONSE_MESSAGE).equals(ModelService.RESPOND_SUCCESS) && result.get(x.convertedValue) != null) {
                        weight = weight.multiply((BigDecimal) result.get(x.convertedValue));
                    } else {
                        return ServiceUtil.returnError(UtilProperties.getMessage(RES_ERROR,
                                x.FacilityShipmentUspsWeightUnsupported,
                                UtilMisc.toMap(x.weightUomId, weightUomId, x.shipmentId, shipmentPackage.getString(x.shipmentId),
                                        x.shipmentPackageSeqId, shipmentPackage.getString(x.shipmentPackageSeqId),
                                        x.weightUom, x.WT_lb), locale));
                    }

                }

                BigDecimal weightPounds = weight.setScale(0, RoundingMode.FLOOR);
                BigDecimal weightOunces = weight.multiply(new BigDecimal(x._16)).remainder(new BigDecimal(x._16)).setScale(0, RoundingMode.CEILING);

                DecimalFormat df = new DecimalFormat(x.str_d08f88df);
                UtilXml.addChildElementValue(packageElement, x.Pounds, df.format(weightPounds), requestDocument);
                UtilXml.addChildElementValue(packageElement, x.Ounces, df.format(weightOunces), requestDocument);

                // Container element
                GenericValue carrierShipmentBoxType = null;
                List<GenericValue> carrierShipmentBoxTypes = null;
                carrierShipmentBoxTypes = shipmentPackage.getRelated(x.CarrierShipmentBoxType, UtilMisc.toMap(x.partyId, x.USPS), null, false);

                if (!carrierShipmentBoxTypes.isEmpty()) {
                    carrierShipmentBoxType = carrierShipmentBoxTypes.get(0);
                }

                if (carrierShipmentBoxType != null
                        && UtilValidate.isNotEmpty(carrierShipmentBoxType.getString(x.packagingTypeCode))) {
                    UtilXml.addChildElementValue(packageElement, x.Container,
                            carrierShipmentBoxType.getString(x.packagingTypeCode), requestDocument);
                } else {
                    // default to "None", for customers using their own package
                    UtilXml.addChildElementValue(packageElement, x.Container, x._None, requestDocument);
                }

                // Size element
                if (carrierShipmentBoxType != null && UtilValidate.isNotEmpty(x.oversizeCode)) {
                    UtilXml.addChildElementValue(packageElement, x.Size,
                            carrierShipmentBoxType.getString(x.oversizeCode), requestDocument);
                } else {
                    // default to "Regular", length + girth measurement <= 84 inches
                    UtilXml.addChildElementValue(packageElement, x.Size, x.Regular, requestDocument);
                }

                // Although only applicable for Parcel Post, this tag is required for all requests
                UtilXml.addChildElementValue(packageElement, x.Machinable, x._False, requestDocument);

                Document responseDocument = null;
                try {
                    responseDocument = sendUspsRequest(x.Rate, requestDocument, delegator, shipmentGatewayConfigId, resource, locale);
                } catch (UspsRequestException e) {
                    Debug.logInfo(e, MODULE);
                    return ServiceUtil.returnError(UtilProperties.getMessage(RES_ERROR,
                            x.FacilityShipmentUspsRateDomesticSendingError,
                            UtilMisc.toMap(x.errorString, e.getMessage()), locale));
                }

                Element respPackageElement = UtilXml.firstChildElement(responseDocument.getDocumentElement(), x.Package_7431e3df);
                if (respPackageElement == null) {
                    return ServiceUtil.returnError(UtilProperties.getMessage(RES_ERROR,
                            x.FacilityShipmentUspsRateDomesticResponseIncompleteElementPackage, locale));
                }

                Element respErrorElement = UtilXml.firstChildElement(respPackageElement, x.Error);
                if (respErrorElement != null) {
                    return ServiceUtil.returnError(UtilProperties.getMessage(RES_ERROR,
                            x.FacilityShipmentUspsRateDomesticResponseError,
                            UtilMisc.toMap(x.errorString, UtilXml.childElementValue(respErrorElement, x.Description)), locale));
                }

                // update the ShipmentPackageRouteSeg
                String postageString = UtilXml.childElementValue(respPackageElement, x.Postage);
                if (UtilValidate.isEmpty(postageString)) {
                    return ServiceUtil.returnError(UtilProperties.getMessage(RES_ERROR,
                            x.FacilityShipmentUspsRateDomesticResponseIncompleteElementPostage, locale));
                }

                BigDecimal postage = BigDecimal.ZERO;
                try {
                    postage = new BigDecimal(postageString);
                } catch (NumberFormatException nfe) {
                    Debug.logError(nfe, MODULE); // TODO: handle exception
                }
                actualTransportCost = actualTransportCost.add(postage);

                shipmentPackageRouteSeg.setString(x.packageTransportCost, postageString);
                shipmentPackageRouteSeg.store();

                // if this is the last package, get the zone and APO/FPO restrictions for the ShipmentRouteSegment
                if (!i.hasNext()) {
                    carrierDeliveryZone = UtilXml.childElementValue(respPackageElement, x.Zone);
                    carrierRestrictionCodes = UtilXml.childElementValue(respPackageElement, x.RestrictionCodes);
                    carrierRestrictionDesc = UtilXml.childElementValue(respPackageElement, x.RestrictionDescription);
                }
            }

            // update the ShipmentRouteSegment
            shipmentRouteSegment.set(x.carrierDeliveryZone, carrierDeliveryZone);
            shipmentRouteSegment.set(x.carrierRestrictionCodes, carrierRestrictionCodes);
            shipmentRouteSegment.set(x.carrierRestrictionDesc, carrierRestrictionDesc);
            shipmentRouteSegment.setString(x.actualTransportCost, String.valueOf(actualTransportCost));
            shipmentRouteSegment.store();

        } catch (GenericEntityException gee) {
            Debug.logInfo(gee, MODULE);
            return ServiceUtil.returnError(UtilProperties.getMessage(RES_ERROR,
                    x.FacilityShipmentUspsRateDomesticReadingError,
                    UtilMisc.toMap(x.errorString, gee.getMessage()), locale));
        }

        return ServiceUtil.returnSuccess();
    }

    /*

    Delivery Confirmation Samples:

    <DeliveryConfirmationV2.0Request USERID="xxxxxxx" PASSWORD="xxxxxxxx">
        <Option>3</Option>
        <ImageParameters></ImageParameters>
        <FromName>John Smith</FromName>
        <FromFirm>ABC Corp.</FromFirm>
        <FromAddress1>Ste  4</FromAddress1>
        <FromAddress2>6406  Ivy Lane</FromAddress2>
        <FromCity>Greenbelt</FromCity>
        <FromState>MD</FromState>
        <FromZip5>20770</FromZip5>
        <FromZip4>4354</FromZip4>
        <ToName>Jane Smith</ToName>
        <ToFirm>XYZ Corp.</ToFirm>
        <ToAddress1>Apt 303</ToAddress1>
        <ToAddress2>4411 Romlon Street</ToAddress2>
        <ToCity>Beltsville</ToCity>
        <ToState>MD</ToState>
        <ToZip5>20705</ToZip5>
        <ToZip4>5656</ToZip4>
        <WeightInOunces>22</WeightInOunces>
        <ServiceType>Parcel Post</ServiceType>
        <ImageType>TIF</ImageType>
    </DeliveryConfirmationV2.0Request>

    <DeliveryConfirmationV2.0Response>
        <DeliveryConfirmationNumber>02805213907052510758</DeliveryConfirmationNumber>
        <DeliveryConfirmationLabel>(Base64 encoded data)</DeliveryConfirmationLabel>
    </DeliveryConfirmationV2.0Response>

    */

    public static Map<String, Object> uspsDeliveryConfirmation(DispatchContext dctx, UspsServicesContext context) {
        Delegator delegator = dctx.getDelegator();
        String shipmentId = (String) context.get(x.shipmentId);
        String shipmentRouteSegmentId = (String) context.get(x.shipmentRouteSegmentId);
        Locale locale = (Locale) context.get(x.locale);

        Map<String, Object> shipmentGatewayConfig = ShipmentServices.getShipmentGatewayConfigFromShipment(delegator, shipmentId, locale);
        String shipmentGatewayConfigId = (String) shipmentGatewayConfig.get(x.shipmentGatewayConfigId);
        String resource = (String) shipmentGatewayConfig.get(x.configProps);
        if (UtilValidate.isEmpty(shipmentGatewayConfigId) && UtilValidate.isEmpty(resource)) {
            return ServiceUtil.returnError(UtilProperties.getMessage(RES_ERROR,
                    x.FacilityShipmentUspsGatewayNotAvailable, locale));
        }

        try {
            GenericValue shipment = getShipmentValue(delegator, shipmentId);
            if (shipment == null) {
                return ServiceUtil.returnError(UtilProperties.getMessage(RES_ERROR,
                        x.ProductShipmentNotFoundId, locale) + shipmentId);
            }

            GenericValue shipmentRouteSegment = getShipmentRouteSegmentValue(delegator, shipmentId, shipmentRouteSegmentId);
            if (shipmentRouteSegment == null) {
                return ServiceUtil.returnError(UtilProperties.getMessage(RES_ERROR,
                        x.ProductShipmentRouteSegmentNotFound,
                        UtilMisc.toMap(x.shipmentId, shipmentId, x.shipmentRouteSegmentId, shipmentRouteSegmentId), locale));
            }

            // ensure the carrier is USPS
            if (!x.USPS.equals(shipmentRouteSegment.getString(x.carrierPartyId))) {
                return ServiceUtil.returnError(UtilProperties.getMessage(RES_ERROR,
                        x.FacilityShipmentUspsNotRouteSegmentCarrier,
                        UtilMisc.toMap(x.shipmentRouteSegmentId, shipmentRouteSegmentId, x.shipmentId, shipmentId), locale));
            }

            // get the origin address
            GenericValue originAddress = shipmentRouteSegment.getRelatedOne(x.OriginPostalAddress, false);
            if (originAddress == null) {
                return ServiceUtil.returnError(UtilProperties.getMessage(RES_ERROR,
                        x.FacilityShipmentRouteSegmentOriginPostalAddressNotFound,
                        UtilMisc.toMap(x.shipmentId, shipmentId, x.shipmentRouteSegmentId, shipmentRouteSegmentId), locale));
            }
            if (!x.USA.equals(originAddress.getString(x.countryGeoId))) {
                return ServiceUtil.returnError(UtilProperties.getMessage(RES_ERROR,
                        x.FacilityShipmentUspsRouteSegmentOriginCountryGeoNotInUsa,
                        UtilMisc.toMap(x.shipmentId, shipmentId, x.shipmentRouteSegmentId, shipmentRouteSegmentId), locale));
            }

            // get the destination address
            GenericValue destinationAddress = shipmentRouteSegment.getRelatedOne(x.DestPostalAddress, false);
            if (destinationAddress == null) {
                return ServiceUtil.returnError(UtilProperties.getMessage(RES_ERROR,
                        x.FacilityShipmentRouteSegmentDestPostalAddressNotFound,
                        UtilMisc.toMap(x.shipmentId, shipmentId, x.shipmentRouteSegmentId, shipmentRouteSegmentId), locale));
            }
            if (!x.USA.equals(destinationAddress.getString(x.countryGeoId))) {
                return ServiceUtil.returnError(UtilProperties.getMessage(RES_ERROR,
                        x.FacilityShipmentUspsRouteSegmentOriginCountryGeoNotInUsa,
                        UtilMisc.toMap(x.shipmentId, shipmentId, x.shipmentRouteSegmentId, shipmentRouteSegmentId), locale));
            }

            // get the service type from the CarrierShipmentMethod
            String shipmentMethodTypeId = shipmentRouteSegment.getString(x.shipmentMethodTypeId);
            String partyId = shipmentRouteSegment.getString(x.carrierPartyId);

            GenericValue carrierShipmentMethod = getCarrierShipmentMethodValue(delegator, shipmentMethodTypeId, partyId, x.CARRIER);
            if (carrierShipmentMethod == null) {
                return ServiceUtil.returnError(UtilProperties.getMessage(RES_ERROR,
                        x.FacilityShipmentUspsNoCarrierShipmentMethod,
                        UtilMisc.toMap(x.carrierPartyId, partyId, x.shipmentMethodTypeId, shipmentMethodTypeId), locale));
            }
            String serviceType = carrierShipmentMethod.getString(x.carrierServiceCode);
            if (UtilValidate.isEmpty(serviceType)) {
                return ServiceUtil.returnError(UtilProperties.getMessage(RES_ERROR,
                        x.FacilityShipmentUspsUnableDetermineServiceCode, locale));
            }

            // get the packages for this shipment route segment
            List<GenericValue> shipmentPackageRouteSegList = shipmentRouteSegment.getRelated(x.ShipmentPackageRouteSeg, null,
                    UtilMisc.toList(x.shipmentPackageSeqId_39d5d38d), false);
            if (UtilValidate.isEmpty(shipmentPackageRouteSegList)) {
                return ServiceUtil.returnError(UtilProperties.getMessage(RES_ERROR,
                        x.FacilityShipmentPackageRouteSegsNotFound,
                        UtilMisc.toMap(x.shipmentId, shipmentId, x.shipmentRouteSegmentId, shipmentRouteSegmentId), locale));
            }

            for (GenericValue shipmentPackageRouteSeg: shipmentPackageRouteSegList) {
                Document requestDocument = createUspsRequestDocument(x.DeliveryConfirmationV2_0Request, true, delegator,
                        shipmentGatewayConfigId, resource);
                Element requestElement = requestDocument.getDocumentElement();

                UtilXml.addChildElementValue(requestElement, x.Option, x._3, requestDocument);
                UtilXml.addChildElement(requestElement, x.ImageParameters, requestDocument);

                // From address
                if (UtilValidate.isNotEmpty(originAddress.getString(x.attnName))) {
                    UtilXml.addChildElementValue(requestElement, x.FromName, originAddress.getString(x.attnName), requestDocument);
                    UtilXml.addChildElementValue(requestElement, x.FromFirm, originAddress.getString(x.toName), requestDocument);
                } else {
                    UtilXml.addChildElementValue(requestElement, x.FromName, originAddress.getString(x.toName), requestDocument);
                }
                // The following 2 assignments are not typos - USPS address1 = OFBiz address2, USPS address2 = OFBiz address1
                UtilXml.addChildElementValue(requestElement, x.FromAddress1, originAddress.getString(x.address2), requestDocument);
                UtilXml.addChildElementValue(requestElement, x.FromAddress2, originAddress.getString(x.address1), requestDocument);
                UtilXml.addChildElementValue(requestElement, x.FromCity, originAddress.getString(x.city), requestDocument);
                UtilXml.addChildElementValue(requestElement, x.FromState, originAddress.getString(x.stateProvinceGeoId), requestDocument);
                UtilXml.addChildElementValue(requestElement, x.FromZip5, originAddress.getString(x.postalCode), requestDocument);
                UtilXml.addChildElement(requestElement, x.FromZip4, requestDocument);

                // To address
                if (UtilValidate.isNotEmpty(destinationAddress.getString(x.attnName))) {
                    UtilXml.addChildElementValue(requestElement, x.ToName, destinationAddress.getString(x.attnName), requestDocument);
                    UtilXml.addChildElementValue(requestElement, x.ToFirm, destinationAddress.getString(x.toName), requestDocument);
                } else {
                    UtilXml.addChildElementValue(requestElement, x.ToName, destinationAddress.getString(x.toName), requestDocument);
                }
                // The following 2 assignments are not typos - USPS address1 = OFBiz address2, USPS address2 = OFBiz address1
                UtilXml.addChildElementValue(requestElement, x.ToAddress1, destinationAddress.getString(x.address2), requestDocument);
                UtilXml.addChildElementValue(requestElement, x.ToAddress2, destinationAddress.getString(x.address1), requestDocument);
                UtilXml.addChildElementValue(requestElement, x.ToCity, destinationAddress.getString(x.city), requestDocument);
                UtilXml.addChildElementValue(requestElement, x.ToState, destinationAddress.getString(x.stateProvinceGeoId), requestDocument);
                UtilXml.addChildElementValue(requestElement, x.ToZip5, destinationAddress.getString(x.postalCode), requestDocument);
                UtilXml.addChildElement(requestElement, x.ToZip4, requestDocument);

                GenericValue shipmentPackage = shipmentPackageRouteSeg.getRelatedOne(x.ShipmentPackage, false);

                // WeightInOunces
                String weightStr = shipmentPackage.getString(x.weight);
                if (UtilValidate.isEmpty(weightStr)) {
                    return ServiceUtil.returnError(UtilProperties.getMessage(RES_ERROR,
                            x.FacilityShipmentUspsWeightNotFound,
                            UtilMisc.toMap(x.shipmentId, shipmentPackage.getString(x.shipmentId),
                                    x.shipmentPackageSeqId, shipmentPackage.getString(x.shipmentPackageSeqId)), locale));
                }

                BigDecimal weight = BigDecimal.ZERO;
                try {
                    weight = new BigDecimal(weightStr);
                } catch (NumberFormatException nfe) {
                    Debug.logError(nfe, MODULE); // TODO: handle exception
                }

                String weightUomId = shipmentPackage.getString(x.weightUomId);
                if (UtilValidate.isEmpty(weightUomId)) {
                    // assume weight is in pounds for consistency (this assumption is made in uspsDomesticRate also)
                    weightUomId = x.WT_lb;
                }
                if (!x.WT_oz.equals(weightUomId)) {
                    // attempt a conversion to pounds
                    GenericValue uomConversion = getUomConversionValue(delegator, weightUomId, x.WT_oz);
                    if (uomConversion == null || UtilValidate.isEmpty(uomConversion.getString(x.conversionFactor))) {
                        return ServiceUtil.returnError(UtilProperties.getMessage(RES_ERROR,
                                x.FacilityShipmentUspsWeightUnsupported,
                                UtilMisc.toMap(x.weightUomId, weightUomId, x.shipmentId, shipmentPackage.getString(x.shipmentId),
                                        x.shipmentPackageSeqId, shipmentPackage.getString(x.shipmentPackageSeqId),
                                        x.weightUom, x.WT_oz), locale));
                    }
                    weight = weight.multiply(uomConversion.getBigDecimal(x.conversionFactor));
                }

                DecimalFormat df = new DecimalFormat(x.str_d08f88df);
                UtilXml.addChildElementValue(requestElement, x.WeightInOunces, df.format(weight.setScale(0, RoundingMode.CEILING)), requestDocument);

                UtilXml.addChildElementValue(requestElement, x.ServiceType, serviceType, requestDocument);
                UtilXml.addChildElementValue(requestElement, x.ImageType, x.TIF, requestDocument);
                UtilXml.addChildElementValue(requestElement, x.AddressServiceRequested, x._True, requestDocument);

                Document responseDocument = null;
                try {
                    responseDocument = sendUspsRequest(x.DeliveryConfirmationV2, requestDocument, delegator, shipmentGatewayConfigId,
                            resource, locale);
                } catch (UspsRequestException e) {
                    Debug.logInfo(e, MODULE);
                    return ServiceUtil.returnError(UtilProperties.getMessage(RES_ERROR,
                            x.FacilityShipmentUspsDeliveryConfirmationSendingError,
                            UtilMisc.toMap(x.errorString, e.getMessage()), locale));
                }
                Element responseElement = responseDocument.getDocumentElement();

                Element respErrorElement = UtilXml.firstChildElement(responseElement, x.Error);
                if (respErrorElement != null) {
                    return ServiceUtil.returnError(UtilProperties.getMessage(RES_ERROR,
                            x.FacilityShipmentUspsDeliveryConfirmationResponseError,
                            UtilMisc.toMap(x.shipmentId, shipmentPackage.getString(x.shipmentId),
                                    x.shipmentPackageSeqId, shipmentPackage.getString(x.shipmentPackageSeqId),
                                    x.errorString, UtilXml.childElementValue(respErrorElement, x.Description)), locale));
                }

                String labelImageString = UtilXml.childElementValue(responseElement, x.DeliveryConfirmationLabel);
                if (UtilValidate.isEmpty(labelImageString)) {
                    return ServiceUtil.returnError(UtilProperties.getMessage(RES_ERROR,
                            x.FacilityShipmentUspsDeliveryConfirmationResponseIncompleteElementDeliveryConfirmationLabel, locale));
                }
                shipmentPackageRouteSeg.setBytes(x.labelImage, Base64.getMimeDecoder().decode(labelImageString.getBytes(StandardCharsets.UTF_8)));
                String trackingCode = UtilXml.childElementValue(responseElement, x.DeliveryConfirmationNumber);
                if (UtilValidate.isEmpty(trackingCode)) {
                    return ServiceUtil.returnError(UtilProperties.getMessage(RES_ERROR,
                            x.FacilityShipmentUspsDeliveryConfirmationResponsenIncompleteElementDeliveryConfirmationNumber, locale));
                }
                shipmentPackageRouteSeg.set(x.trackingCode, trackingCode);
                shipmentPackageRouteSeg.store();
            }

        } catch (GenericEntityException gee) {
            Debug.logInfo(gee, MODULE);
            return ServiceUtil.returnError(UtilProperties.getMessage(RES_ERROR,
                    x.FacilityShipmentUspsDeliveryConfirmationReadingError,
                    UtilMisc.toMap(x.errorString, gee.getMessage()), locale));
        }

        return ServiceUtil.returnSuccess();
    }

    /* ------------------------------------------------------------------------------------------------------------- */

    // testing utility service - remove this
    public static Map<String, Object> uspsDumpShipmentLabelImages(DispatchContext dctx, UspsServicesContext context) {
        Delegator delegator = dctx.getDelegator();

        try {
            String shipmentId = (String) context.get(x.shipmentId);
            String shipmentRouteSegmentId = (String) context.get(x.shipmentRouteSegmentId);

            GenericValue shipmentRouteSegment = getShipmentRouteSegmentValue(delegator, shipmentId, shipmentRouteSegmentId);

            List<GenericValue> shipmentPackageRouteSegList = shipmentRouteSegment.getRelated(x.ShipmentPackageRouteSeg,
                    null, UtilMisc.toList(x.shipmentPackageSeqId_39d5d38d), false);

            for (GenericValue shipmentPackageRouteSeg: shipmentPackageRouteSegList) {
                byte[] labelImageBytes = shipmentPackageRouteSeg.getBytes(x.labelImage);

                String outFileName = x.UspsLabelImage + shipmentRouteSegment.getString(x.shipmentId) + x.str_53a0acfa
                        + shipmentRouteSegment.getString(x.shipmentRouteSegmentId) + x.str_53a0acfa
                        + shipmentPackageRouteSeg.getString(x.shipmentPackageSeqId) + x.gif;

                try (FileOutputStream fileOut = new FileOutputStream(outFileName)) {
                    fileOut.write(labelImageBytes);
                    fileOut.flush();
                } catch (IOException e) {
                    Debug.logInfo(e, MODULE);
                    return ServiceUtil.returnError(e.getMessage());
                }
            }
        } catch (GenericEntityException e) {
            Debug.logInfo(e, MODULE);
            return ServiceUtil.returnError(e.getMessage());
        }
        return ServiceUtil.returnSuccess();
    }

    public static Map<String, Object> uspsPriorityMailInternationalLabel(DispatchContext dctx, UspsServicesContext context) {
        Delegator delegator = dctx.getDelegator();
        LocalDispatcher dispatcher = dctx.getDispatcher();
        String shipmentGatewayConfigId = (String) context.get(x.shipmentGatewayConfigId);
        String resource = (String) context.get(x.configProps);
        GenericValue shipmentRouteSegment = (GenericValue) context.get(x.shipmentRouteSegment);
        Locale locale = (Locale) context.get(x.locale);

        // Start the document
        Document requestDocument;
        boolean certify = false;
        String test = getShipmentGatewayConfigValue(delegator, shipmentGatewayConfigId, x.test, resource, x.shipment_usps_test);
        if (!x.Y.equalsIgnoreCase(test)) {
            requestDocument = createUspsRequestDocument(x.PriorityMailIntlRequest, false, delegator, shipmentGatewayConfigId, resource);
        } else {
            requestDocument = createUspsRequestDocument(x.PriorityMailIntlCertifyRequest, false, delegator, shipmentGatewayConfigId, resource);
            certify = true;
        }
        Element rootElement = requestDocument.getDocumentElement();

        // Retrieve from/to address and package details
        GenericValue originAddress = null;
        GenericValue originTelecomNumber = null;
        GenericValue destinationAddress = null;
        GenericValue destinationProvince = null;
        GenericValue destinationCountry = null;
        GenericValue destinationTelecomNumber = null;
        List<GenericValue> shipmentPackageRouteSegs = null;
        try {
            originAddress = shipmentRouteSegment.getRelatedOne(x.OriginPostalAddress, false);
            originTelecomNumber = shipmentRouteSegment.getRelatedOne(x.OriginTelecomNumber, false);
            destinationAddress = shipmentRouteSegment.getRelatedOne(x.DestPostalAddress, false);
            if (destinationAddress != null) {
                destinationProvince = destinationAddress.getRelatedOne(x.StateProvinceGeo, false);
                destinationCountry = destinationAddress.getRelatedOne(x.CountryGeo, false);
            }
            destinationTelecomNumber = shipmentRouteSegment.getRelatedOne(x.DestTelecomNumber, false);
            shipmentPackageRouteSegs = shipmentRouteSegment.getRelated(x.ShipmentPackageRouteSeg, null, null, false);
        } catch (GenericEntityException e) {
            Debug.logError(e, MODULE);
        }
        if (originAddress == null || originTelecomNumber == null) {
            return ServiceUtil.returnError(UtilProperties.getMessage(RES_ERROR,
                    x.FacilityShipmentUspsPriorityMailLabelOriginAddressMissing, locale));
        }

        // Origin Info
        // USPS wants a separate first name and last, best we can do is split the string on the white space, if that doesn't work then
        // default to putting the attnName in both fields
        String fromAttnName = originAddress.getString(x.attnName);
        String fromFirstName = StringUtils.defaultIfEmpty(StringUtils.substringBefore(fromAttnName, x.str_b858cb28), fromAttnName);
        String fromLastName = StringUtils.defaultIfEmpty(StringUtils.substringAfter(fromAttnName, x.str_b858cb28), fromAttnName);
        UtilXml.addChildElementValue(rootElement, x.FromFirstName, fromFirstName, requestDocument);
        UtilXml.addChildElementValue(rootElement, x.FromLastName, fromLastName, requestDocument);
        UtilXml.addChildElementValue(rootElement, x.FromFirm, originAddress.getString(x.toName), requestDocument);
        // The following 2 assignments are not typos - USPS address1 = OFBiz address2, USPS address2 = OFBiz address1
        UtilXml.addChildElementValue(rootElement, x.FromAddress1, originAddress.getString(x.address2), requestDocument);
        UtilXml.addChildElementValue(rootElement, x.FromAddress2, originAddress.getString(x.address1), requestDocument);
        UtilXml.addChildElementValue(rootElement, x.FromCity, originAddress.getString(x.city), requestDocument);
        UtilXml.addChildElementValue(rootElement, x.FromState, originAddress.getString(x.stateProvinceGeoId), requestDocument);
        UtilXml.addChildElementValue(rootElement, x.FromZip5, originAddress.getString(x.postalCode), requestDocument);
        // USPS expects a phone number consisting of area code + contact number as a single numeric string
        String fromPhoneNumber = originTelecomNumber.getString(x.areaCode) + originTelecomNumber.getString(x.contactNumber);
        fromPhoneNumber = StringUtil.removeNonNumeric(fromPhoneNumber);
        UtilXml.addChildElementValue(rootElement, x.FromPhone, fromPhoneNumber, requestDocument);

        // Destination Info
        UtilXml.addChildElementValue(rootElement, x.ToName, destinationAddress.getString(x.attnName), requestDocument);
        UtilXml.addChildElementValue(rootElement, x.ToFirm, destinationAddress.getString(x.toName), requestDocument);
        UtilXml.addChildElementValue(rootElement, x.ToAddress1, destinationAddress.getString(x.address1), requestDocument);
        UtilXml.addChildElementValue(rootElement, x.ToAddress2, destinationAddress.getString(x.address2), requestDocument);
        UtilXml.addChildElementValue(rootElement, x.ToCity, destinationAddress.getString(x.city), requestDocument);
        UtilXml.addChildElementValue(rootElement, x.ToProvince, destinationProvince.getString(x.geoName), requestDocument);
        // TODO: Test these country names, I think we're going to need to maintain a list of USPS names
        UtilXml.addChildElementValue(rootElement, x.ToCountry, destinationCountry.getString(x.geoName), requestDocument);
        UtilXml.addChildElementValue(rootElement, x.ToPostalCode, destinationAddress.getString(x.postalCode), requestDocument);
        // TODO: Figure out how to answer this question accurately
        UtilXml.addChildElementValue(rootElement, x.ToPOBoxFlag, x.N, requestDocument);
        String toPhoneNumber = destinationTelecomNumber.getString(x.countryCode) + destinationTelecomNumber.getString(x.areaCode)
                + destinationTelecomNumber.getString(x.contactNumber);
        UtilXml.addChildElementValue(rootElement, x.ToPhone, toPhoneNumber, requestDocument);
        UtilXml.addChildElementValue(rootElement, x.NonDeliveryOption, x.RETURN, requestDocument);

        for (GenericValue shipmentPackageRouteSeg : shipmentPackageRouteSegs) {
            Document packageDocument = (Document) requestDocument.cloneNode(true);
            // This is our reference and can be whatever we want.  For lack of a better alternative we'll use
            // shipmentId:shipmentPackageSeqId:shipmentRouteSegmentId
            String fromCustomsReference;
            fromCustomsReference = StringUtils.join(
                    UtilMisc.toList(
                            shipmentRouteSegment.get(x.shipmentId),
                            shipmentPackageRouteSeg.get(x.shipmentPackageSeqId),
                            shipmentRouteSegment.get(x.shipmentRouteSegementId)), ':');
            UtilXml.addChildElementValue(rootElement, x.FromCustomsReference, fromCustomsReference, packageDocument);
            // Determine the container type for this package
            String container = x.VARIABLE;
            String packageTypeCode = null;
            GenericValue shipmentPackage = null;
            List<GenericValue> shipmentPackageContents = null;
            try {
                shipmentPackage = shipmentPackageRouteSeg.getRelatedOne(x.ShipmentPackage, false);
                shipmentPackageContents = shipmentPackage.getRelated(x.ShipmentPackageContent, null, null, false);
                GenericValue shipmentBoxType = shipmentPackage.getRelatedOne(x.ShipmentBoxType, false);
                if (shipmentBoxType != null) {
                    GenericValue carrierShipmentBoxType = EntityUtil.getFirst(shipmentBoxType.getRelated(x.CarrierShipmentBoxType,
                            UtilMisc.toMap(x.partyId, x.USPS), null, false));
                    if (carrierShipmentBoxType != null) {
                        packageTypeCode = carrierShipmentBoxType.getString(x.packageTypeCode);
                        // Supported type codes
                        List<String> supportedPackageTypeCodes = UtilMisc.toList(
                                x.LGFLATRATEBOX,
                                x.SMFLATRATEBOX,
                                x.FLATRATEBOX,
                                x.MDFLATRATEBOX,
                                x.FLATRATEENV);
                        if (supportedPackageTypeCodes.contains(packageTypeCode)) {
                            container = packageTypeCode;
                        }
                    }
                }
            } catch (GenericEntityException e) {
                Debug.logError(e, MODULE);
            }
            UtilXml.addChildElementValue(rootElement, x.Container, container, packageDocument);
            // According to the docs sending an empty postage tag will cause the postage to be calculated
            UtilXml.addChildElementValue(rootElement, x.Postage, x.emptyString, packageDocument);

            BigDecimal packageWeight = shipmentPackage.getBigDecimal(x.weight);
            String weightUomId = shipmentPackage.getString(x.weightUomId);
            BigDecimal packageWeightPounds = UomWorker.convertUom(packageWeight, weightUomId, x.WT_lb, dispatcher);
            Integer[] packagePoundsOunces = convertPoundsToPoundsOunces(packageWeightPounds);
            UtilXml.addChildElementValue(rootElement, x.GrossPounds, packagePoundsOunces[0].toString(), packageDocument);
            UtilXml.addChildElementValue(rootElement, x.GrossOunces, packagePoundsOunces[1].toString(), packageDocument);

            UtilXml.addChildElementValue(rootElement, x.ContentType, x.MERCHANDISE, packageDocument);
            UtilXml.addChildElementValue(rootElement, x.Agreement, x.N, packageDocument);
            UtilXml.addChildElementValue(rootElement, x.ImageType, x.PDF, packageDocument);
            // TODO: Try the different layouts
            UtilXml.addChildElementValue(rootElement, x.ImageType, x.ALLINONEFILE, packageDocument);
            UtilXml.addChildElementValue(rootElement, x.CustomerRefNo, fromCustomsReference, packageDocument);

            // Add the shipping contents
            Element shippingContents = UtilXml.addChildElement(rootElement, x.ShippingContents, packageDocument);
            for (GenericValue shipmentPackageContent : shipmentPackageContents) {
                Element itemDetail = UtilXml.addChildElement(shippingContents, x.ItemDetail, packageDocument);
                GenericValue product = null;
                GenericValue originGeo = null;
                try {
                    GenericValue shipmentItem = shipmentPackageContent.getRelatedOne(x.ShipmentItem, false);
                    product = shipmentItem.getRelatedOne(x.Product, false);
                    originGeo = product.getRelatedOne(x.OriginGeo, false);
                } catch (GenericEntityException e) {
                    Debug.logInfo(e, MODULE);
                }

                if (product != null) {
                    UtilXml.addChildElementValue(itemDetail, x.Description, product.getString(x.productName), packageDocument);
                    UtilXml.addChildElementValue(itemDetail, x.Quantity_44f6af69, shipmentPackageContent.getBigDecimal(x.quantity)
                            .setScale(0, RoundingMode.CEILING).toPlainString(), packageDocument);
                    String packageContentValue = ShipmentWorker.getShipmentPackageContentValue(shipmentPackageContent)
                            .setScale(2, RoundingMode.HALF_UP).toPlainString();
                    UtilXml.addChildElementValue(itemDetail, x.Value, packageContentValue, packageDocument);
                    BigDecimal productWeight = ProductWorker.getProductWeight(product, x.WT_lbs, delegator, dispatcher);
                    Integer[] productPoundsOunces = convertPoundsToPoundsOunces(productWeight);
                    UtilXml.addChildElementValue(itemDetail, x.NetPounds, productPoundsOunces[0].toString(), packageDocument);
                    UtilXml.addChildElementValue(itemDetail, x.NetOunces, productPoundsOunces[1].toString(), packageDocument);
                    UtilXml.addChildElementValue(itemDetail, x.HSTariffNumber, x.emptyString, packageDocument);
                    if (originGeo != null) {
                        UtilXml.addChildElementValue(itemDetail, x.CountryOfOrigin, originGeo.getString(x.geoName), packageDocument);
                    }
                }
            }

            // Send the request
            Document responseDocument = null;
            String api = certify ? x.PriorityMailIntlCertify : x.PriorityMailIntl;
            try {
                responseDocument = sendUspsRequest(api, requestDocument, delegator, shipmentGatewayConfigId, resource, locale);
            } catch (UspsRequestException e) {
                Debug.logInfo(e, MODULE);
                return ServiceUtil.returnError(UtilProperties.getMessage(RES_ERROR,
                        x.FacilityShipmentUspsPriorityMailLabelSendingError,
                        UtilMisc.toMap(x.errorString, e.getMessage()), locale));
            }
            Element responseElement = responseDocument.getDocumentElement();

            // TODO: No mention of error returns in the docs

            String labelImageString = UtilXml.childElementValue(responseElement, x.LabelImage);
            if (UtilValidate.isEmpty(labelImageString)) {
                return ServiceUtil.returnError(UtilProperties.getMessage(RES_ERROR,
                        x.FacilityShipmentUspsPriorityMailLabelResponseIncompleteElementLabelImage, locale));
            }
            shipmentPackageRouteSeg.setBytes(x.labelImage, Base64.getMimeDecoder().decode(labelImageString.getBytes(StandardCharsets.UTF_8)));
            String trackingCode = UtilXml.childElementValue(responseElement, x.BarcodeNumber);
            if (UtilValidate.isEmpty(trackingCode)) {
                return ServiceUtil.returnError(UtilProperties.getMessage(RES_ERROR,
                        x.FacilityShipmentUspsPriorityMailLabelResponseIncompleteElementBarcodeNumber, locale));
            }
            shipmentPackageRouteSeg.set(x.trackingCode, trackingCode);
            try {
                shipmentPackageRouteSeg.store();
            } catch (GenericEntityException e) {
                Debug.logError(e, MODULE);
            }

        }
        return ServiceUtil.returnSuccess();
    }

    private static Document createUspsRequestDocument(String rootElement, boolean passwordRequired, Delegator delegator,
                                                      String shipmentGatewayConfigId, String resource) {
        Document requestDocument = UtilXml.makeEmptyXmlDocument(rootElement);
        Element requestElement = requestDocument.getDocumentElement();
        requestElement.setAttribute(x.USERID, getShipmentGatewayConfigValue(delegator, shipmentGatewayConfigId,
                x.accessUserId, resource, x.shipment_usps_access_userid, x.emptyString));
        if (passwordRequired) {
            requestElement.setAttribute(x.PASSWORD, getShipmentGatewayConfigValue(delegator, shipmentGatewayConfigId,
                    x.accessPassword, resource, x.shipment_usps_access_password, x.emptyString));
        }
        return requestDocument;
    }

    private static Document sendUspsRequest(String requestType, Document requestDocument, Delegator delegator,
            String shipmentGatewayConfigId, String resource, Locale locale) throws UspsRequestException {
        String conUrl = null;
        List<String> labelRequestTypes = UtilMisc.toList(x.PriorityMailIntl, x.PriorityMailIntlCertify);
        if (labelRequestTypes.contains(requestType)) {
            conUrl = getShipmentGatewayConfigValue(delegator, shipmentGatewayConfigId, x.connectUrlLabels, resource,
                    x.shipment_usps_connect_url_labels);
        } else {
            conUrl = getShipmentGatewayConfigValue(delegator, shipmentGatewayConfigId, x.connectUrl, resource, x.shipment_usps_connect_url);
        }
        if (UtilValidate.isEmpty(conUrl)) {
            throw new UspsRequestException(UtilProperties.getMessage(RES_ERROR,
                    x.FacilityShipmentUspsConnectUrlIncomplete, locale));
        }

        ByteArrayOutputStream os = new ByteArrayOutputStream();

        try {
            UtilXml.writeXmlDocument(requestDocument, os, x.UTF_8, true, false, 0);
        } catch (TransformerException e) {
            throw new UspsRequestException(
                    UtilProperties.getMessage(RES_ERROR,
                            x.FacilityShipmentUspsSerializingError,
                            UtilMisc.toMap(x.errorString, e.getMessage()), locale));
        }

        String xmlString = new String(os.toByteArray(), StandardCharsets.UTF_8);

        Debug.logInfo(x.USPS_XML_request_string + xmlString, MODULE);

        String timeOutStr = getShipmentGatewayConfigValue(delegator, shipmentGatewayConfigId, x.connectTimeout,
                resource, x.shipment_usps_connect_timeout, x._60);
        int timeout = 60;
        try {
            timeout = Integer.parseInt(timeOutStr);
        } catch (NumberFormatException e) {
            Debug.logError(e, x.Unable_to_set_timeout_to + timeOutStr + x.using_default + timeout);
        }

        HttpClient http = new HttpClient(conUrl);
        http.setTimeout(timeout * 1000);
        http.setParameter(x.API, requestType);
        http.setParameter(x.XML, xmlString);

        String responseString = null;
        try {
            responseString = http.get();
        } catch (HttpClientException e) {
            throw new UspsRequestException(UtilProperties.getMessage(RES_ERROR,
                    x.FacilityShipmentUspsConnectionProblem,
                    UtilMisc.toMap(x.errorString, e), locale));
        }

        Debug.logInfo(x.USPS_response + responseString, MODULE);

        if (UtilValidate.isEmpty(responseString)) {
            return null;
        }

        Document responseDocument = null;
        try {
            responseDocument = UtilXml.readXmlDocument(responseString, false);
        } catch (Exception e) {
            throw new UspsRequestException(UtilProperties.getMessage(RES_ERROR, x.FacilityShipmentUspsResponseError,
                    UtilMisc.toMap(x.errorString, e.getMessage()), locale));
        }

        // If a top-level error document is returned, throw exception
        // Other request-level errors should be handled by the caller
        Element responseElement = responseDocument.getDocumentElement();
        if (x.Error.equals(responseElement.getNodeName())) {
            throw new UspsRequestException(UtilXml.childElementValue(responseElement, x.Description));
        }

        return responseDocument;
    }

    /*
     * Converts decimal pounds to pounds and ounces as an Integer array, ounces are rounded up to the nearest whole number
     */
    public static Integer[] convertPoundsToPoundsOunces(BigDecimal decimalPounds) {
        if (decimalPounds == null) return null;
        Integer[] poundsOunces = new Integer[2];
        poundsOunces[0] = Integer.valueOf(decimalPounds.setScale(0, RoundingMode.FLOOR).toPlainString());
        // (weight % 1) * 16 rounded up to nearest whole number
        poundsOunces[1] = Integer.valueOf(decimalPounds.remainder(BigDecimal.ONE).multiply(new BigDecimal(x._16))
                .setScale(0, RoundingMode.CEILING).toPlainString());
        return poundsOunces;
    }

    private static String getShipmentGatewayConfigValue(Delegator delegator, String shipmentGatewayConfigId, String
            shipmentGatewayConfigParameterName, String resource, String parameterName) {
        String returnValue = x.emptyString;
        if (UtilValidate.isNotEmpty(shipmentGatewayConfigId)) {
            try {
                GenericValue usps = getShipmentGatewayUspsValue(delegator, shipmentGatewayConfigId);
                if (usps != null) {
                    Object uspsField = usps.get(shipmentGatewayConfigParameterName);
                    if (uspsField != null) {
                        returnValue = uspsField.toString().trim();
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

    private static String getShipmentGatewayConfigValue(Delegator delegator, String shipmentGatewayConfigId, String
            shipmentGatewayConfigParameterName, String resource, String parameterName, String defaultValue) {
        String returnValue = getShipmentGatewayConfigValue(delegator, shipmentGatewayConfigId, shipmentGatewayConfigParameterName,
                resource, parameterName);
        if (UtilValidate.isEmpty(returnValue)) {
            returnValue = defaultValue;
        }
        return returnValue;
    }

    private static GenericValue getPostalAddressValue(Delegator delegator, String contactMechId) throws GenericEntityException {
        PostalAddressDao postalAddressDao = DaoRegistry.getDao(delegator, x.PostalAddress, PostalAddressDao.class);
        try {
            PostalAddressEntity postalAddressEntity = postalAddressDao.get(contactMechId).orElse(null);
            return postalAddressEntity == null ? null : delegator.makeValue(x.PostalAddress, Beans.beanToMap(postalAddressEntity));
        } catch (java.sql.SQLException e) {
            throw new GenericEntityException(e);
        }
    }

    private static GenericValue getCarrierShipmentMethodValue(Delegator delegator, Object shipmentMethodTypeId, Object partyId, Object roleTypeId)
            throws GenericEntityException {
        CarrierShipmentMethodDao carrierShipmentMethodDao = DaoRegistry.getDao(delegator, x.CarrierShipmentMethod, CarrierShipmentMethodDao.class);
        try {
            CarrierShipmentMethodEntity carrierShipmentMethodEntity = carrierShipmentMethodDao.list(Filters.and(
                    Filters.eq(x.shipmentMethodTypeId, shipmentMethodTypeId),
                    Filters.eq(x.partyId, partyId),
                    Filters.eq(x.roleTypeId, roleTypeId))).stream().findFirst().orElse(null);
            return carrierShipmentMethodEntity == null ? null
                    : delegator.makeValue(x.CarrierShipmentMethod, Beans.beanToMap(carrierShipmentMethodEntity));
        } catch (java.sql.SQLException e) {
            throw new GenericEntityException(e);
        }
    }

    private static GenericValue getShipmentRouteSegmentValue(Delegator delegator, String shipmentId, String shipmentRouteSegmentId)
            throws GenericEntityException {
        ShipmentRouteSegmentDao shipmentRouteSegmentDao = DaoRegistry.getDao(delegator, x.ShipmentRouteSegment, ShipmentRouteSegmentDao.class);
        try {
            ShipmentRouteSegmentEntity shipmentRouteSegmentEntity = shipmentRouteSegmentDao.list(Filters.and(
                    Filters.eq(x.shipmentId, shipmentId),
                    Filters.eq(x.shipmentRouteSegmentId, shipmentRouteSegmentId))).stream().findFirst().orElse(null);
            return shipmentRouteSegmentEntity == null ? null
                    : delegator.makeValue(x.ShipmentRouteSegment, Beans.beanToMap(shipmentRouteSegmentEntity));
        } catch (java.sql.SQLException e) {
            throw new GenericEntityException(e);
        }
    }

    private static GenericValue getShipmentValue(Delegator delegator, String shipmentId) throws GenericEntityException {
        ShipmentDao shipmentDao = DaoRegistry.getDao(delegator, x.Shipment, ShipmentDao.class);
        try {
            ShipmentEntity shipmentEntity = shipmentDao.get(shipmentId).orElse(null);
            return shipmentEntity == null ? null : delegator.makeValue(x.Shipment, Beans.beanToMap(shipmentEntity));
        } catch (java.sql.SQLException e) {
            throw new GenericEntityException(e);
        }
    }

    private static GenericValue getUomConversionValue(Delegator delegator, String uomId, String uomIdTo) throws GenericEntityException {
        UomConversionDao uomConversionDao = DaoRegistry.getDao(delegator, x.UomConversion, UomConversionDao.class);
        try {
            UomConversionEntity uomConversionEntity = uomConversionDao.list(Filters.and(
                    Filters.eq(x.uomId, uomId),
                    Filters.eq(x.uomIdTo, uomIdTo))).stream().findFirst().orElse(null);
            return uomConversionEntity == null ? null : delegator.makeValue(x.UomConversion, Beans.beanToMap(uomConversionEntity));
        } catch (java.sql.SQLException e) {
            throw new GenericEntityException(e);
        }
    }

    private static GenericValue getShipmentGatewayUspsValue(Delegator delegator, String shipmentGatewayConfigId) throws GenericEntityException {
        ShipmentGatewayUspsDao shipmentGatewayUspsDao = DaoRegistry.getDao(delegator, x.ShipmentGatewayUsps, ShipmentGatewayUspsDao.class);
        try {
            ShipmentGatewayUspsEntity shipmentGatewayUspsEntity = shipmentGatewayUspsDao.get(shipmentGatewayConfigId).orElse(null);
            return shipmentGatewayUspsEntity == null ? null
                    : delegator.makeValue(x.ShipmentGatewayUsps, Beans.beanToMap(shipmentGatewayUspsEntity));
        } catch (java.sql.SQLException e) {
            throw new GenericEntityException(e);
        }
    }
}

@SuppressWarnings(x.serial)
class UspsRequestException extends GeneralException {
    UspsRequestException() {
        super();
    }

    UspsRequestException(String msg) {
        super(msg);
    }

    UspsRequestException(Throwable t) {
        super(t);
    }

    UspsRequestException(String msg, Throwable t) {
        super(msg, t);
    }
}
