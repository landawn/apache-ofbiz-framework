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
package org.apache.ofbiz.common;

import java.io.IOException;
import java.util.Locale;
import java.util.Map;

import javax.xml.parsers.ParserConfigurationException;

import org.apache.ofbiz.base.util.Debug;
import org.apache.ofbiz.base.util.HttpClient;
import org.apache.ofbiz.base.util.HttpClientException;
import org.apache.ofbiz.base.util.UtilMisc;
import org.apache.ofbiz.base.util.UtilProperties;
import org.apache.ofbiz.base.util.UtilXml;
import org.apache.ofbiz.service.DispatchContext;
import org.apache.ofbiz.service.ServiceUtil;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.xml.sax.SAXException;


import org.apache.ofbiz.persistence.entity.x;
import org.apache.ofbiz.model.ServiceContext;
import org.apache.ofbiz.model.CdyneServicesContext;
/**
 * OFBIZ CDyne Services: for info see http://www.cdyne.com/developers/overview.aspx
 */
public class CdyneServices {

    private static final String MODULE = CdyneServices.class.getName();
    private static final String RESOURCE = x.CommonUiLabels;
    public static final String LICENSE_KEY = UtilProperties.getPropertyValue(x.cdyne, x.LicenseKey, x._0);

    /**
     * CDyne ReturnCityState Service
     *@param dctx The DispatchContext that this service is operating in
     *@param context Map containing the input parameters
     *@return Map with the result of the service, the output parameters
     */
    public static Map<String, Object> cdyneReturnCityState(DispatchContext dctx, CdyneServicesContext context) {
        String zipcode = (String) context.get(x.zipcode);
        Locale locale = (Locale) context.get(x.locale);
        String serviceUrl = x.http_ws_cdyne_com_psaddress_addresslookup_asmx_ReturnCityState_zipcode + zipcode + x.LicenseKey_d293f43f + LICENSE_KEY;
        try {
            String httpResponse = HttpClient.getUrlContent(serviceUrl);

            Document addressDocument = UtilXml.readXmlDocument(httpResponse);
            Element addressRootElement = addressDocument.getDocumentElement();

            Map<String, Object> response = ServiceUtil.returnSuccess();
            populateCdyneAddress(addressRootElement, response);

            if (x._true.equals(response.get(x.ServiceError))) {
                return ServiceUtil.returnError(UtilProperties.getMessage(RESOURCE, x.CommonCDyneServiceError,
                        UtilMisc.toMap(x.zipcode, zipcode), locale));
            }
            if (x._true.equals(response.get(x.AddressError))) {
                return ServiceUtil.returnError(UtilProperties.getMessage(RESOURCE, x.CommonCDyneAddressError,
                        UtilMisc.toMap(x.zipcode, zipcode), locale));
            }

            return response;
        } catch (HttpClientException e) {
            Debug.logError(e, x.Error_calling_CDyne_service_at_URL + serviceUrl + x.str_89222ecc + e.toString(), MODULE);
            return ServiceUtil.returnError(UtilProperties.getMessage(RESOURCE, x.CommonCDyneCallingError,
                    UtilMisc.toMap(x.serviceUrl, serviceUrl, x.errorString, e.toString()), locale));
        } catch (SAXException | ParserConfigurationException | IOException e) {
            Debug.logError(e, x.Error_parsing_XML_result_from_CDyne_service_at_URL + serviceUrl + x.str_89222ecc + e.toString(), MODULE);
            return ServiceUtil.returnError(UtilProperties.getMessage(RESOURCE, x.CommonCDyneParsingError,
                    UtilMisc.toMap(x.serviceUrl, serviceUrl, x.errorString, e.toString()), locale));
        }
    }

    private static void populateCdyneAddress(Element addressRootElement, Map<String, Object> targetContext) {
        targetContext.put(x.ServiceError, UtilXml.childElementValue(addressRootElement, x.ServiceError));
        targetContext.put(x.AddressError, UtilXml.childElementValue(addressRootElement, x.AddressError));
        targetContext.put(x.AddressFoundBeMoreSpecific, UtilXml.childElementValue(addressRootElement, x.AddressFoundBeMoreSpecific));
        targetContext.put(x.NeededCorrection, UtilXml.childElementValue(addressRootElement, x.NeededCorrection));

        targetContext.put(x.DeliveryAddress, UtilXml.childElementValue(addressRootElement, x.DeliveryAddress));
        targetContext.put(x.City, UtilXml.childElementValue(addressRootElement, x.City));
        targetContext.put(x.StateAbbrev, UtilXml.childElementValue(addressRootElement, x.StateAbbrev));
        targetContext.put(x.ZipCode, UtilXml.childElementValue(addressRootElement, x.ZipCode));
        targetContext.put(x.County, UtilXml.childElementValue(addressRootElement, x.County));
        targetContext.put(x.CountyNum, UtilXml.childElementValue(addressRootElement, x.CountyNum));
        targetContext.put(x.PreferredCityName, UtilXml.childElementValue(addressRootElement, x.PreferredCityName));

        targetContext.put(x.DeliveryPoint, UtilXml.childElementValue(addressRootElement, x.DeliveryPoint));
        targetContext.put(x.CheckDigit, UtilXml.childElementValue(addressRootElement, x.CheckDigit));

        targetContext.put(x.CSKey, UtilXml.childElementValue(addressRootElement, x.CSKey));
        targetContext.put(x.FIPS, UtilXml.childElementValue(addressRootElement, x.FIPS));

        targetContext.put(x.FromLongitude, UtilXml.childElementValue(addressRootElement, x.FromLongitude));
        targetContext.put(x.FromLatitude, UtilXml.childElementValue(addressRootElement, x.FromLatitude));
        targetContext.put(x.ToLongitude, UtilXml.childElementValue(addressRootElement, x.ToLongitude));
        targetContext.put(x.ToLatitude, UtilXml.childElementValue(addressRootElement, x.ToLatitude));
        targetContext.put(x.AvgLongitude, UtilXml.childElementValue(addressRootElement, x.AvgLongitude));
        targetContext.put(x.AvgLatitude, UtilXml.childElementValue(addressRootElement, x.AvgLatitude));

        targetContext.put(x.CMSA, UtilXml.childElementValue(addressRootElement, x.CMSA));
        targetContext.put(x.PMSA, UtilXml.childElementValue(addressRootElement, x.PMSA));
        targetContext.put(x.MSA, UtilXml.childElementValue(addressRootElement, x.MSA));
        targetContext.put(x.MA, UtilXml.childElementValue(addressRootElement, x.MA));

        targetContext.put(x.TimeZone, UtilXml.childElementValue(addressRootElement, x.TimeZone));
        targetContext.put(x.hasDaylightSavings, UtilXml.childElementValue(addressRootElement, x.hasDaylightSavings));
        targetContext.put(x.AreaCode, UtilXml.childElementValue(addressRootElement, x.AreaCode));
        targetContext.put(x.LLCertainty, UtilXml.childElementValue(addressRootElement, x.LLCertainty));

        targetContext.put(x.CensusBlockNum, UtilXml.childElementValue(addressRootElement, x.CensusBlockNum));
        targetContext.put(x.CensusTractNum, UtilXml.childElementValue(addressRootElement, x.CensusTractNum));

        /*
        Example URL: http://ws.cdyne.com/psaddress/addresslookup.asmx/ReturnCityState?zipcode=93940&LicenseKey=0
        NOTE: 0 is a test LicenseKey

        Example Response:
        <?xml version="1.0" encoding="utf-8"?>
        <Address xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" xmlns:xsd="http://www.w3.org/2001/XMLSchema" xmlns="http://ws.cdyne.com/">
            <ServiceError>false</ServiceError>
            <AddressError>false</AddressError>
            <AddressFoundBeMoreSpecific>false</AddressFoundBeMoreSpecific>
            <NeededCorrection>true</NeededCorrection>

            <DeliveryAddress>**UNKNOWN**</DeliveryAddress>
            <City>DEL REY OAKS</City>
            <StateAbbrev>CA</StateAbbrev>
            <ZipCode>93940</ZipCode>
            <County>MONTEREY</County>
            <CountyNum>0</CountyNum>
            <PreferredCityName>MONTEREY</PreferredCityName>

            <DeliveryPoint>99</DeliveryPoint>
            <CheckDigit>0</CheckDigit>

            <CSKey>Z20854</CSKey>
            <FIPS>06053</FIPS>

            <FromLongitude>-121.919965</FromLongitude>
            <FromLatitude>36.362864</FromLatitude>
            <ToLongitude>-121.647022</ToLongitude>
            <ToLatitude>36.652645</ToLatitude>
            <AvgLongitude>-121.7834935</AvgLongitude>
            <AvgLatitude>36.5077545</AvgLatitude>

            <CMSA>7120</CMSA>
            <PMSA />
            <MSA>7120</MSA>
            <MA>712</MA>

            <TimeZone>PST</TimeZone>
            <hasDaylightSavings>true</hasDaylightSavings>
            <AreaCode>831</AreaCode>
            <LLCertainty>90</LLCertainty>

            <CensusBlockNum>9003</CensusBlockNum>
            <CensusTractNum>0134.00</CensusTractNum>
        </Address>
        */
    }
}
