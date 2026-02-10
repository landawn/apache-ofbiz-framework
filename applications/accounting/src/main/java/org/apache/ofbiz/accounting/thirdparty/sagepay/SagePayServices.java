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

package org.apache.ofbiz.accounting.thirdparty.sagepay;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import org.apache.http.HttpHost;
import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.ofbiz.base.util.Debug;
import org.apache.ofbiz.base.util.UtilMisc;
import org.apache.ofbiz.base.util.UtilProperties;
import org.apache.ofbiz.base.util.UtilValidate;
import org.apache.ofbiz.entity.Delegator;
import org.apache.ofbiz.entity.GenericEntityException;
import org.apache.ofbiz.entity.GenericValue;
import org.apache.ofbiz.persistence.dao.DaoRegistry;
import org.apache.ofbiz.persistence.dao.PaymentGatewaySagePayDao;
import org.apache.ofbiz.persistence.entity.PaymentGatewaySagePayEntity;
import org.apache.ofbiz.service.DispatchContext;
import org.apache.ofbiz.service.ModelService;
import org.apache.ofbiz.service.ServiceUtil;
import com.landawn.abacus.util.Beans;


import org.apache.ofbiz.persistence.entity.x;
import org.apache.ofbiz.model.ServiceContext;
import org.apache.ofbiz.model.SagePayServicesContext;
public class SagePayServices {
    private static final String MODULE = SagePayServices.class.getName();
    private static final String RESOURCE = x.AccountingUiLabels;

    private static Map<String, String> buildSagePayProperties(SagePayServicesContext context, Delegator delegator) {

        Map<String, String> sagePayConfig = new HashMap<>();

        String paymentGatewayConfigId = (String) context.get(x.paymentGatewayConfigId);

        if (UtilValidate.isNotEmpty(paymentGatewayConfigId)) {
            try {
                PaymentGatewaySagePayDao paymentGatewaySagePayDao = DaoRegistry.getDao(delegator, x.PaymentGatewaySagePay, PaymentGatewaySagePayDao.class);
                PaymentGatewaySagePayEntity sagePay = paymentGatewaySagePayDao.get(paymentGatewayConfigId).orElse(null);
                if (sagePay != null) {
                    for (Entry<String, Object> set : Beans.beanToMap(sagePay).entrySet()) {
                        if (set.getValue() == null) {
                            sagePayConfig.put(set.getKey(), null);
                        } else {
                            sagePayConfig.put(set.getKey(), set.getValue().toString());
                        }
                    }
                }
            } catch (Exception e) {
                Debug.logError(e, MODULE);
            }
        }

        Debug.logInfo(x.SagePay_Configuration + sagePayConfig.toString(), MODULE);
        return sagePayConfig;
    }

    public static Map<String, Object> paymentAuthentication(DispatchContext ctx, SagePayServicesContext context) {
        Debug.logInfo(x.SagePay_Entered_paymentAuthentication, MODULE);
        Debug.logInfo(x.SagePay_paymentAuthentication_context + context, MODULE);

        Delegator delegator = ctx.getDelegator();
        Map<String, Object> resultMap = new HashMap<>();

        Map<String, String> props = buildSagePayProperties(context, delegator);

        String vendorTxCode = (String) context.get(x.vendorTxCode);
        String cardHolder = (String) context.get(x.cardHolder);
        String cardNumber = (String) context.get(x.cardNumber);
        String expiryDate = (String) context.get(x.expiryDate);
        String cardType = (String) context.get(x.cardType);
        String cv2 = (String) context.get(x.cv2);
        String amount = (String) context.get(x.amount);
        String currency = (String) context.get(x.currency);
        String description = (String) context.get(x.description);

        String billingSurname = (String) context.get(x.billingSurname);
        String billingFirstnames = (String) context.get(x.billingFirstnames);
        String billingAddress = (String) context.get(x.billingAddress);
        String billingAddress2 = (String) context.get(x.billingAddress2);
        String billingCity = (String) context.get(x.billingCity);
        String billingPostCode = (String) context.get(x.billingPostCode);
        String billingCountry = (String) context.get(x.billingCountry);
        String billingState = (String) context.get(x.billingState);
        String billingPhone = (String) context.get(x.billingPhone);

        Boolean isBillingSameAsDelivery = (Boolean) context.get(x.isBillingSameAsDelivery);

        String deliverySurname = (String) context.get(x.deliverySurname);
        String deliveryFirstnames = (String) context.get(x.deliveryFirstnames);
        String deliveryAddress = (String) context.get(x.deliveryAddress);
        String deliveryAddress2 = (String) context.get(x.deliveryAddress2);
        String deliveryCity = (String) context.get(x.deliveryCity);
        String deliveryPostCode = (String) context.get(x.deliveryPostCode);
        String deliveryCountry = (String) context.get(x.deliveryCountry);
        String deliveryState = (String) context.get(x.deliveryState);
        String deliveryPhone = (String) context.get(x.deliveryPhone);

        String startDate = (String) context.get(x.startDate);
        String issueNumber = (String) context.get(x.issueNumber);
        String basket = (String) context.get(x.basket);
        String clientIPAddress = (String) context.get(x.clientIPAddress);
        Locale locale = (Locale) context.get(x.locale);

        HttpHost host = SagePayUtil.getHost(props);

        //start - authentication parameters
        Map<String, String> parameters = new HashMap<>();

        String vpsProtocol = props.get(x.protocolVersion);
        String vendor = props.get(x.vendor);
        String txType = props.get(x.authenticationTransType);
        //start - required parameters
        StringBuilder errorRequiredParameters = new StringBuilder();
        if (vpsProtocol == null) {
            errorRequiredParameters.append(x.Required_transaction_parameter_protocolVersion_is_missing);
        }
        if (vendor == null) {
            errorRequiredParameters.append(x.Required_transaction_parameter_vendor_is_missing);
        }
        if (txType == null) {
            errorRequiredParameters.append(x.Required_transaction_parameter_authenticationsTransType_is_missing);
        }
        if (errorRequiredParameters.length() > 0) {
            return ServiceUtil.returnError(UtilProperties.getMessage(RESOURCE, x.AccountingSagePayPaymentAuthorisationException,
                    UtilMisc.toMap(x.errorString, errorRequiredParameters), locale));
        }
        parameters.put(x.VPSProtocol, vpsProtocol);
        parameters.put(x.TxType, txType);
        parameters.put(x.Vendor, vendor);

        if (vendorTxCode != null) {
            parameters.put(x.VendorTxCode, vendorTxCode);
        }
        if (amount != null) {
            parameters.put(x.Amount, amount);
        }
        if (currency != null) {
            parameters.put(x.Currency, currency);
        } //GBP/USD
        if (description != null) {
            parameters.put(x.Description, description);
        }
        if (cardHolder != null) {
            parameters.put(x.CardHolder, cardHolder);
        }
        if (cardNumber != null) {
            parameters.put(x.CardNumber, cardNumber);
        }
        if (cardType != null) {
            parameters.put(x.CardType, cardType);
        }
        if (expiryDate != null) {
            parameters.put(x.ExpiryDate, expiryDate);
        }

        //start - billing details
        if (billingSurname != null) {
            parameters.put(x.BillingSurname, billingSurname);
        }
        if (billingFirstnames != null) {
            parameters.put(x.BillingFirstnames, billingFirstnames);
        }
        if (billingAddress != null) {
            parameters.put(x.BillingAddress, billingAddress);
        }
        if (billingAddress2 != null) {
            parameters.put(x.BillingAddress2, billingAddress2);
        }
        if (billingCity != null) {
            parameters.put(x.BillingCity, billingCity);
        }
        if (billingPostCode != null) {
            parameters.put(x.BillingPostCode, billingPostCode);
        }
        if (billingCountry != null) {
            parameters.put(x.BillingCountry, billingCountry);
        }
        if (billingState != null) {
            parameters.put(x.BillingState, billingState);
        }
        if (billingPhone != null) {
            parameters.put(x.BillingPhone, billingPhone);
        }
        //end - billing details

        //start - delivery details
        if (isBillingSameAsDelivery != null && isBillingSameAsDelivery) {
            if (billingSurname != null) {
                parameters.put(x.DeliverySurname, billingSurname);
            }
            if (billingFirstnames != null) {
                parameters.put(x.DeliveryFirstnames, billingFirstnames);
            }
            if (billingAddress != null) {
                parameters.put(x.DeliveryAddress, billingAddress);
            }
            if (billingAddress2 != null) {
                parameters.put(x.DeliveryAddress2, billingAddress2);
            }
            if (billingCity != null) {
                parameters.put(x.DeliveryCity, billingCity);
            }
            if (billingPostCode != null) {
                parameters.put(x.DeliveryPostCode, billingPostCode);
            }
            if (billingCountry != null) {
                parameters.put(x.DeliveryCountry, billingCountry);
            }
            if (billingState != null) {
                parameters.put(x.DeliveryState, billingState);
            }
            if (billingPhone != null) {
                parameters.put(x.DeliveryPhone, billingPhone);
            }
        } else {
            if (deliverySurname != null) {
                parameters.put(x.DeliverySurname, deliverySurname);
            }
            if (deliveryFirstnames != null) {
                parameters.put(x.DeliveryFirstnames, deliveryFirstnames);
            }
            if (deliveryAddress != null) {
                parameters.put(x.DeliveryAddress, deliveryAddress);
            }
            if (deliveryAddress2 != null) {
                parameters.put(x.DeliveryAddress2, deliveryAddress2);
            }
            if (deliveryCity != null) {
                parameters.put(x.DeliveryCity, deliveryCity);
            }
            if (deliveryPostCode != null) {
                parameters.put(x.DeliveryPostCode, deliveryPostCode);
            }
            if (deliveryCountry != null) {
                parameters.put(x.DeliveryCountry, deliveryCountry);
            }
            if (deliveryState != null) {
                parameters.put(x.DeliveryState, deliveryState);
            }
            if (deliveryPhone != null) {
                parameters.put(x.DeliveryPhone, deliveryPhone);
            }
        }
        //end - delivery details
        //end - required parameters

        //start - optional parameters
        if (cv2 != null) {
            parameters.put(x.CV2, cv2);
        }
        if (startDate != null) {
            parameters.put(x.StartDate, startDate);
        }
        if (issueNumber != null) {
            parameters.put(x.IssueNumber, issueNumber);
        }
        if (basket != null) {
            parameters.put(x.Basket, basket);
        }
        if (clientIPAddress != null) {
            parameters.put(x.ClientIPAddress, clientIPAddress);
        }
        //end - optional parameters
        //end - authentication parameters

        try (CloseableHttpClient httpClient = SagePayUtil.getHttpClient()) {

            String successMessage = null;
            HttpPost httpPost = SagePayUtil.getHttpPost(props.get(x.authenticationUrl), parameters);
            HttpResponse response = httpClient.execute(host, httpPost);
            Map<String, String> responseData = SagePayUtil.getResponseData(response);

            String status = responseData.get(x.Status_bae7d5be);
            String statusDetail = responseData.get(x.StatusDetail);

            resultMap.put(x.status, status);
            resultMap.put(x.statusDetail, statusDetail);

            //returning the below details back to the calling code, as it not returned back by the payment gateway
            resultMap.put(x.vendorTxCode, vendorTxCode);
            resultMap.put(x.amount, amount);
            resultMap.put(x.transactionType, txType);

            //start - transaction authorized
            if (x.OK.equals(status)) {
                resultMap.put(x.vpsTxId, responseData.get(x.VPSTxId));
                resultMap.put(x.securityKey, responseData.get(x.SecurityKey));
                resultMap.put(x.txAuthNo, responseData.get(x.TxAuthNo));
                resultMap.put(x.avsCv2, responseData.get(x.AVSCV2));
                resultMap.put(x.addressResult, responseData.get(x.AddressResult));
                resultMap.put(x.postCodeResult, responseData.get(x.PostCodeResult));
                resultMap.put(x.cv2Result, responseData.get(x.CV2Result));
                successMessage = x.Payment_authorized;
            }
            //end - transaction authorized

            if (x.NOTAUTHED.equals(status)) {
                resultMap.put(x.vpsTxId, responseData.get(x.VPSTxId));
                resultMap.put(x.securityKey, responseData.get(x.SecurityKey));
                resultMap.put(x.avsCv2, responseData.get(x.AVSCV2));
                resultMap.put(x.addressResult, responseData.get(x.AddressResult));
                resultMap.put(x.postCodeResult, responseData.get(x.PostCodeResult));
                resultMap.put(x.cv2Result, responseData.get(x.CV2Result));
                successMessage = x.Payment_not_authorized_8d42a994;
            }

            if (x.MALFORMED.equals(status)) {
                //request not formed properly or parameters missing
                resultMap.put(x.vpsTxId, responseData.get(x.VPSTxId));
                resultMap.put(x.securityKey, responseData.get(x.SecurityKey));
                resultMap.put(x.avsCv2, responseData.get(x.AVSCV2));
                resultMap.put(x.addressResult, responseData.get(x.AddressResult));
                resultMap.put(x.postCodeResult, responseData.get(x.PostCodeResult));
                resultMap.put(x.cv2Result, responseData.get(x.CV2Result));
            }

            if (x.INVALID.equals(status)) {
                //invalid information in request
                resultMap.put(x.vpsTxId, responseData.get(x.VPSTxId));
                resultMap.put(x.securityKey, responseData.get(x.SecurityKey));
                resultMap.put(x.avsCv2, responseData.get(x.AVSCV2));
                resultMap.put(x.addressResult, responseData.get(x.AddressResult));
                resultMap.put(x.postCodeResult, responseData.get(x.PostCodeResult));
                resultMap.put(x.cv2Result, responseData.get(x.CV2Result));
            }

            if (x.REJECTED.equals(status)) {
                //invalid information in request
                resultMap.put(x.vpsTxId, responseData.get(x.VPSTxId));
                resultMap.put(x.securityKey, responseData.get(x.SecurityKey));
                resultMap.put(x.avsCv2, responseData.get(x.AVSCV2));
                resultMap.put(x.addressResult, responseData.get(x.AddressResult));
                resultMap.put(x.postCodeResult, responseData.get(x.PostCodeResult));
                resultMap.put(x.cv2Result, responseData.get(x.CV2Result));
            }

            resultMap.put(ModelService.RESPONSE_MESSAGE, ModelService.RESPOND_SUCCESS);
            resultMap.put(ModelService.SUCCESS_MESSAGE, successMessage);

        } catch (UnsupportedEncodingException uee) {
            //exception in encoding parameters in httpPost
            Debug.logError(uee, x.Error_occurred_in_encoding_parameters_for_HttpPost + uee.getMessage() + x.str_e7064f0b, MODULE);
            resultMap = ServiceUtil.returnError(UtilProperties.getMessage(RESOURCE, x.AccountingSagePayErrorEncodingParameters,
                    UtilMisc.toMap(x.errorString, uee.getMessage()), locale));
        } catch (ClientProtocolException cpe) {
            //from httpClient execute
            Debug.logError(cpe, x.Error_occurred_in_HttpClient_execute + cpe.getMessage() + x.str_e7064f0b, MODULE);
            resultMap = ServiceUtil.returnError(UtilProperties.getMessage(RESOURCE, x.AccountingSagePayErrorHttpClientExecute,
                    UtilMisc.toMap(x.errorString, cpe.getMessage()), locale));
        } catch (IOException ioe) {
            //from httpClient execute or getResponsedata
            Debug.logError(ioe, x.Error_occurred_in_HttpClient_execute_or_getting_response + ioe.getMessage() + x.str_e7064f0b, MODULE);
            resultMap = ServiceUtil.returnError(UtilProperties.getMessage(RESOURCE, x.AccountingSagePayErrorHttpClientExecuteOrGettingResponse,
                    UtilMisc.toMap(x.errorString, ioe.getMessage()), locale));
        }
        return resultMap;
    }

    public static Map<String, Object> paymentAuthorisation(DispatchContext ctx, SagePayServicesContext context) {
        Debug.logInfo(x.SagePay_Entered_paymentAuthorisation, MODULE);
        Debug.logInfo(x.SagePay_paymentAuthorisation_context + context, MODULE);

        Delegator delegator = ctx.getDelegator();
        Map<String, Object> resultMap = new HashMap<>();

        Map<String, String> props = buildSagePayProperties(context, delegator);

        String vendorTxCode = (String) context.get(x.vendorTxCode);
        String vpsTxId = (String) context.get(x.vpsTxId);
        String securityKey = (String) context.get(x.securityKey);
        String txAuthNo = (String) context.get(x.txAuthNo);
        String amount = (String) context.get(x.amount);
        Locale locale = (Locale) context.get(x.locale);

        HttpHost host = SagePayUtil.getHost(props);

        //start - authorization parameters
        Map<String, String> parameters = new HashMap<>();

        String vpsProtocol = props.get(x.protocolVersion);
        String vendor = props.get(x.vendor);
        String txType = props.get(x.authoriseTransType);

        parameters.put(x.VPSProtocol, vpsProtocol);
        parameters.put(x.TxType, txType);
        parameters.put(x.Vendor, vendor);
        parameters.put(x.VendorTxCode, vendorTxCode);
        parameters.put(x.VPSTxId, vpsTxId);
        parameters.put(x.SecurityKey, securityKey);
        parameters.put(x.TxAuthNo, txAuthNo);
        parameters.put(x.ReleaseAmount, amount);

        Debug.logInfo(x.authorization_parameters + parameters, MODULE);
        //end - authorization parameters

        try (CloseableHttpClient httpClient = SagePayUtil.getHttpClient()) {
            String successMessage = null;
            HttpPost httpPost = SagePayUtil.getHttpPost(props.get(x.authoriseUrl), parameters);
            HttpResponse response = httpClient.execute(host, httpPost);

            Map<String, String> responseData = SagePayUtil.getResponseData(response);
            String status = responseData.get(x.Status_bae7d5be);
            String statusDetail = responseData.get(x.StatusDetail);

            resultMap.put(x.status, status);
            resultMap.put(x.statusDetail, statusDetail);

            //start - payment refunded
            if (x.OK.equals(status)) {
                successMessage = UtilProperties.getMessage(RESOURCE, x.AccountingSagePayPaymentReleased, locale);
            }
            //end - payment refunded

            //start - refund request not formed properly or parameters missing
            if (x.MALFORMED.equals(status)) {
                successMessage = UtilProperties.getMessage(RESOURCE, x.AccountingSagePayPaymentReleaseRequestMalformed, locale);
            }
            //end - refund request not formed properly or parameters missing

            //start - invalid information passed in parameters
            if (x.INVALID.equals(status)) {
                successMessage = UtilProperties.getMessage(RESOURCE, x.AccountingSagePayPaymentInvalidInformationPassed, locale);
            }
            //end - invalid information passed in parameters

            //start - problem at Sagepay
            if (x.ERROR.equals(status)) {
                successMessage = UtilProperties.getMessage(RESOURCE, x.AccountingSagePayPaymentError, locale);
            }
            //end - problem at Sagepay

            resultMap.put(ModelService.RESPONSE_MESSAGE, ModelService.RESPOND_SUCCESS);
            resultMap.put(ModelService.SUCCESS_MESSAGE, successMessage);

        } catch (UnsupportedEncodingException uee) {
            //exception in encoding parameters in httpPost
            Debug.logError(uee, x.Error_occurred_in_encoding_parameters_for_HttpPost + uee.getMessage() + x.str_e7064f0b, MODULE);
            resultMap = ServiceUtil.returnError(UtilProperties.getMessage(RESOURCE, x.AccountingSagePayErrorEncodingParameters,
                    UtilMisc.toMap(x.errorString, uee.getMessage()), locale));
        } catch (ClientProtocolException cpe) {
            //from httpClient execute
            Debug.logError(cpe, x.Error_occurred_in_HttpClient_execute + cpe.getMessage() + x.str_e7064f0b, MODULE);
            resultMap = ServiceUtil.returnError(UtilProperties.getMessage(RESOURCE, x.AccountingSagePayErrorHttpClientExecute,
                    UtilMisc.toMap(x.errorString, cpe.getMessage()), locale));
        } catch (IOException ioe) {
            //from httpClient execute or getResponsedata
            Debug.logError(ioe, x.Error_occurred_in_HttpClient_execute_or_getting_response + ioe.getMessage() + x.str_e7064f0b, MODULE);
            resultMap = ServiceUtil.returnError(UtilProperties.getMessage(RESOURCE, x.AccountingSagePayErrorHttpClientExecuteOrGettingResponse,
                    UtilMisc.toMap(x.errorString, ioe.getMessage()), locale));
        }
        return resultMap;
    }

    public static Map<String, Object> paymentRelease(DispatchContext ctx, SagePayServicesContext context) {
        Debug.logInfo(x.SagePay_Entered_paymentRelease, MODULE);
        Debug.logInfo(x.SagePay_paymentRelease_context + context, MODULE);

        Delegator delegator = ctx.getDelegator();
        Map<String, Object> resultMap = new HashMap<>();

        Map<String, String> props = buildSagePayProperties(context, delegator);

        String vendorTxCode = (String) context.get(x.vendorTxCode);
        String vpsTxId = (String) context.get(x.vpsTxId);
        String securityKey = (String) context.get(x.securityKey);
        String txAuthNo = (String) context.get(x.txAuthNo);
        Locale locale = (Locale) context.get(x.locale);

        HttpHost host = SagePayUtil.getHost(props);

        //start - release parameters
        Map<String, String> parameters = new HashMap<>();

        String vpsProtocol = props.get(x.protocolVersion);
        String vendor = props.get(x.vendor);
        String txType = props.get(x.releaseTransType);

        parameters.put(x.VPSProtocol, vpsProtocol);
        parameters.put(x.TxType, txType);
        parameters.put(x.Vendor, vendor);
        parameters.put(x.VendorTxCode, vendorTxCode);
        parameters.put(x.VPSTxId, vpsTxId);
        parameters.put(x.SecurityKey, securityKey);
        parameters.put(x.TxAuthNo, txAuthNo);
        //end - release parameters

        try (CloseableHttpClient httpClient = SagePayUtil.getHttpClient()) {
            String successMessage = null;
            HttpPost httpPost = SagePayUtil.getHttpPost(props.get(x.releaseUrl), parameters);
            HttpResponse response = httpClient.execute(host, httpPost);

            Map<String, String> responseData = SagePayUtil.getResponseData(response);

            String status = responseData.get(x.Status_bae7d5be);
            String statusDetail = responseData.get(x.StatusDetail);

            resultMap.put(x.status, status);
            resultMap.put(x.statusDetail, statusDetail);

            //start - payment released
            if (x.OK.equals(status)) {
                successMessage = UtilProperties.getMessage(RESOURCE, x.AccountingSagePayPaymentReleased, locale);
            }
            //end - payment released

            //start - release request not formed properly or parameters missing
            if (x.MALFORMED.equals(status)) {
                successMessage = UtilProperties.getMessage(RESOURCE, x.AccountingSagePayPaymentReleaseRequestMalformed, locale);
            }
            //end - release request not formed properly or parameters missing

            //start - invalid information passed in parameters
            if (x.INVALID.equals(status)) {
                successMessage = UtilProperties.getMessage(RESOURCE, x.AccountingSagePayPaymentInvalidInformationPassed, locale);
            }
            //end - invalid information passed in parameters

            //start - problem at Sagepay
            if (x.ERROR.equals(status)) {
                successMessage = UtilProperties.getMessage(RESOURCE, x.AccountingSagePayPaymentError, locale);
            }
            //end - problem at Sagepay

            resultMap.put(ModelService.RESPONSE_MESSAGE, ModelService.RESPOND_SUCCESS);
            resultMap.put(ModelService.SUCCESS_MESSAGE, successMessage);

        } catch (UnsupportedEncodingException uee) {
            //exception in encoding parameters in httpPost
            Debug.logError(uee, x.Error_occurred_in_encoding_parameters_for_HttpPost + uee.getMessage() + x.str_e7064f0b, MODULE);
            resultMap = ServiceUtil.returnError(UtilProperties.getMessage(RESOURCE, x.AccountingSagePayErrorEncodingParameters,
                    UtilMisc.toMap(x.errorString, uee.getMessage()), locale));
        } catch (ClientProtocolException cpe) {
            //from httpClient execute
            Debug.logError(cpe, x.Error_occurred_in_HttpClient_execute + cpe.getMessage() + x.str_e7064f0b, MODULE);
            resultMap = ServiceUtil.returnError(UtilProperties.getMessage(RESOURCE, x.AccountingSagePayErrorHttpClientExecute,
                    UtilMisc.toMap(x.errorString, cpe.getMessage()), locale));
        } catch (IOException ioe) {
            //from httpClient execute or getResponsedata
            Debug.logError(ioe, x.Error_occurred_in_HttpClient_execute_or_getting_response + ioe.getMessage() + x.str_e7064f0b, MODULE);
            resultMap = ServiceUtil.returnError(UtilProperties.getMessage(RESOURCE, x.AccountingSagePayErrorHttpClientExecuteOrGettingResponse,
                    UtilMisc.toMap(x.errorString, ioe.getMessage()), locale));
        }
        return resultMap;
    }

    public static Map<String, Object> paymentVoid(DispatchContext ctx, SagePayServicesContext context) {
        Debug.logInfo(x.SagePay_Entered_paymentVoid, MODULE);
        Debug.logInfo(x.SagePay_paymentVoid_context + context, MODULE);

        Delegator delegator = ctx.getDelegator();
        Map<String, Object> resultMap = new HashMap<>();

        Map<String, String> props = buildSagePayProperties(context, delegator);

        String vendorTxCode = (String) context.get(x.vendorTxCode);
        String vpsTxId = (String) context.get(x.vpsTxId);
        String securityKey = (String) context.get(x.securityKey);
        String txAuthNo = (String) context.get(x.txAuthNo);
        Locale locale = (Locale) context.get(x.locale);

        HttpHost host = SagePayUtil.getHost(props);

        //start - void parameters
        Map<String, String> parameters = new HashMap<>();

        String vpsProtocol = props.get(x.protocolVersion);
        String vendor = props.get(x.vendor);

        parameters.put(x.VPSProtocol, vpsProtocol);
        parameters.put(x.TxType, x.VOID);
        parameters.put(x.Vendor, vendor);
        parameters.put(x.VendorTxCode, vendorTxCode);
        parameters.put(x.VPSTxId, vpsTxId);
        parameters.put(x.SecurityKey, securityKey);
        parameters.put(x.TxAuthNo, txAuthNo);
        //end - void parameters

        try (CloseableHttpClient httpClient = SagePayUtil.getHttpClient()) {
            String successMessage = null;
            HttpPost httpPost = SagePayUtil.getHttpPost(props.get(x.voidUrl), parameters);
            HttpResponse response = httpClient.execute(host, httpPost);
            Map<String, String> responseData = SagePayUtil.getResponseData(response);

            String status = responseData.get(x.Status_bae7d5be);
            String statusDetail = responseData.get(x.StatusDetail);

            resultMap.put(x.status, status);
            resultMap.put(x.statusDetail, statusDetail);

            //start - payment void
            if (x.OK.equals(status)) {
                successMessage = UtilProperties.getMessage(RESOURCE, x.AccountingSagePayPaymentVoided, locale);
            }
            //end - payment void

            //start - void request not formed properly or parameters missing
            if (x.MALFORMED.equals(status)) {
                successMessage = UtilProperties.getMessage(RESOURCE, x.AccountingSagePayPaymentVoidRequestMalformed, locale);
            }
            //end - void request not formed properly or parameters missing

            //start - invalid information passed in parameters
            if (x.INVALID.equals(status)) {
                successMessage = UtilProperties.getMessage(RESOURCE, x.AccountingSagePayPaymentInvalidInformationPassed, locale);
            }
            //end - invalid information passed in parameters

            //start - problem at Sagepay
            if (x.ERROR.equals(status)) {
                successMessage = UtilProperties.getMessage(RESOURCE, x.AccountingSagePayPaymentError, locale);
            }
            //end - problem at Sagepay

            resultMap.put(ModelService.RESPONSE_MESSAGE, ModelService.RESPOND_SUCCESS);
            resultMap.put(ModelService.SUCCESS_MESSAGE, successMessage);
        } catch (UnsupportedEncodingException uee) {
            //exception in encoding parameters in httpPost
            Debug.logError(uee, x.Error_occurred_in_encoding_parameters_for_HttpPost + uee.getMessage() + x.str_e7064f0b, MODULE);
            resultMap = ServiceUtil.returnError(UtilProperties.getMessage(RESOURCE, x.AccountingSagePayErrorEncodingParameters,
                    UtilMisc.toMap(x.errorString, uee.getMessage()), locale));
        } catch (ClientProtocolException cpe) {
            //from httpClient execute
            Debug.logError(cpe, x.Error_occurred_in_HttpClient_execute + cpe.getMessage() + x.str_e7064f0b, MODULE);
            resultMap = ServiceUtil.returnError(UtilProperties.getMessage(RESOURCE, x.AccountingSagePayErrorHttpClientExecute,
                    UtilMisc.toMap(x.errorString, cpe.getMessage()), locale));
        } catch (IOException ioe) {
            //from httpClient execute or getResponsedata
            Debug.logError(ioe, x.Error_occurred_in_HttpClient_execute_or_getting_response + ioe.getMessage() + x.str_e7064f0b, MODULE);
            resultMap = ServiceUtil.returnError(UtilProperties.getMessage(RESOURCE, x.AccountingSagePayErrorHttpClientExecuteOrGettingResponse,
                    UtilMisc.toMap(x.errorString, ioe.getMessage()), locale));
        }
        return resultMap;
    }

    public static Map<String, Object> paymentRefund(DispatchContext ctx, SagePayServicesContext context) {
        Debug.logInfo(x.SagePay_Entered_paymentRefund, MODULE);
        Debug.logInfo(x.SagePay_paymentRefund_context + context, MODULE);

        Delegator delegator = ctx.getDelegator();
        Map<String, Object> resultMap = new HashMap<>();

        Map<String, String> props = buildSagePayProperties(context, delegator);

        String vendorTxCode = (String) context.get(x.vendorTxCode);
        String amount = (String) context.get(x.amount);
        String currency = (String) context.get(x.currency);
        String description = (String) context.get(x.description);

        String relatedVPSTxId = (String) context.get(x.relatedVPSTxId);
        String relatedVendorTxCode = (String) context.get(x.relatedVendorTxCode);
        String relatedSecurityKey = (String) context.get(x.relatedSecurityKey);
        String relatedTxAuthNo = (String) context.get(x.relatedTxAuthNo);
        Locale locale = (Locale) context.get(x.locale);

        HttpHost host = SagePayUtil.getHost(props);

        //start - refund parameters
        Map<String, String> parameters = new HashMap<>();

        String vpsProtocol = props.get(x.protocolVersion);
        String vendor = props.get(x.vendor);

        parameters.put(x.VPSProtocol, vpsProtocol);
        parameters.put(x.TxType, x.REFUND);
        parameters.put(x.Vendor, vendor);
        parameters.put(x.VendorTxCode, vendorTxCode);
        parameters.put(x.Amount, amount);
        parameters.put(x.Currency, currency);
        parameters.put(x.Description, description);
        parameters.put(x.RelatedVPSTxId, relatedVPSTxId);
        parameters.put(x.RelatedVendorTxCode, relatedVendorTxCode);
        parameters.put(x.RelatedSecurityKey, relatedSecurityKey);
        parameters.put(x.RelatedTxAuthNo, relatedTxAuthNo);
        //end - refund parameters

        try (CloseableHttpClient httpClient = SagePayUtil.getHttpClient()) {
            String successMessage = null;
            HttpPost httpPost = SagePayUtil.getHttpPost(props.get(x.refundUrl), parameters);
            HttpResponse response = httpClient.execute(host, httpPost);
            Map<String, String> responseData = SagePayUtil.getResponseData(response);

            Debug.logInfo(x.response_data + responseData, MODULE);

            String status = responseData.get(x.Status_bae7d5be);
            String statusDetail = responseData.get(x.StatusDetail);

            resultMap.put(x.status, status);
            resultMap.put(x.statusDetail, statusDetail);

            //start - payment refunded
            if (x.OK.equals(status)) {
                resultMap.put(x.vpsTxId, responseData.get(x.VPSTxId));
                resultMap.put(x.txAuthNo, responseData.get(x.TxAuthNo));
                successMessage = UtilProperties.getMessage(RESOURCE, x.AccountingSagePayPaymentRefunded, locale);
            }
            //end - payment refunded

            //start - refund not authorized by the acquiring bank
            if (x.NOTAUTHED.equals(status)) {
                successMessage = UtilProperties.getMessage(RESOURCE, x.AccountingSagePayPaymentRefundNotAuthorized, locale);
            }
            //end - refund not authorized by the acquiring bank

            //start - refund request not formed properly or parameters missing
            if (x.MALFORMED.equals(status)) {
                successMessage = UtilProperties.getMessage(RESOURCE, x.AccountingSagePayPaymentRefundRequestMalformed, locale);
            }
            //end - refund request not formed properly or parameters missing

            //start - invalid information passed in parameters
            if (x.INVALID.equals(status)) {
                successMessage = UtilProperties.getMessage(RESOURCE, x.AccountingSagePayPaymentInvalidInformationPassed, locale);
            }
            //end - invalid information passed in parameters

            //start - problem at Sagepay
            if (x.ERROR.equals(status)) {
                successMessage = UtilProperties.getMessage(RESOURCE, x.AccountingSagePayPaymentError, locale);
            }
            //end - problem at Sagepay

            resultMap.put(ModelService.RESPONSE_MESSAGE, ModelService.RESPOND_SUCCESS);
            resultMap.put(ModelService.SUCCESS_MESSAGE, successMessage);

        } catch (UnsupportedEncodingException uee) {
            //exception in encoding parameters in httpPost
            Debug.logError(uee, x.Error_occurred_in_encoding_parameters_for_HttpPost + uee.getMessage() + x.str_e7064f0b, MODULE);
            resultMap = ServiceUtil.returnError(UtilProperties.getMessage(RESOURCE, x.AccountingSagePayErrorEncodingParameters,
                    UtilMisc.toMap(x.errorString, uee.getMessage()), locale));
        } catch (ClientProtocolException cpe) {
            //from httpClient execute
            Debug.logError(cpe, x.Error_occurred_in_HttpClient_execute + cpe.getMessage() + x.str_e7064f0b, MODULE);
            resultMap = ServiceUtil.returnError(UtilProperties.getMessage(RESOURCE, x.AccountingSagePayErrorHttpClientExecute,
                    UtilMisc.toMap(x.errorString, cpe.getMessage()), locale));
        } catch (IOException ioe) {
            //from httpClient execute or getResponsedata
            Debug.logError(ioe, x.Error_occurred_in_HttpClient_execute_or_getting_response + ioe.getMessage() + x.str_e7064f0b, MODULE);
            resultMap = ServiceUtil.returnError(UtilProperties.getMessage(RESOURCE, x.AccountingSagePayErrorHttpClientExecuteOrGettingResponse,
                    UtilMisc.toMap(x.errorString, ioe.getMessage()), locale));
        }

        return resultMap;
    }
}

