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
package org.apache.ofbiz.order.thirdparty.zipsales;

import java.math.BigDecimal;
import java.net.URL;
import java.sql.Timestamp;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.apache.ofbiz.base.util.Debug;
import org.apache.ofbiz.base.util.GeneralException;
import org.apache.ofbiz.base.util.StringUtil;
import org.apache.ofbiz.base.util.UtilGenerics;
import org.apache.ofbiz.base.util.UtilMisc;
import org.apache.ofbiz.base.util.UtilProperties;
import org.apache.ofbiz.base.util.UtilURL;
import org.apache.ofbiz.base.util.UtilValidate;
import org.apache.ofbiz.datafile.DataFile;
import org.apache.ofbiz.datafile.DataFileException;
import org.apache.ofbiz.datafile.Record;
import org.apache.ofbiz.datafile.RecordIterator;
import org.apache.ofbiz.entity.Delegator;
import org.apache.ofbiz.entity.GenericEntityException;
import org.apache.ofbiz.entity.GenericValue;
import org.apache.ofbiz.entity.util.EntityUtil;
import org.apache.ofbiz.entity.util.EntityUtilProperties;
import org.apache.ofbiz.persistence.dao.DaoRegistry;
import org.apache.ofbiz.persistence.dao.ZipSalesRuleLookupDao;
import org.apache.ofbiz.persistence.dao.ZipSalesTaxLookupDao;
import org.apache.ofbiz.persistence.entity.ZipSalesRuleLookupEntity;
import org.apache.ofbiz.persistence.entity.ZipSalesTaxLookupEntity;
import org.apache.ofbiz.security.Security;
import org.apache.ofbiz.service.DispatchContext;
import org.apache.ofbiz.service.ServiceUtil;
import com.landawn.abacus.query.Filters;
import com.landawn.abacus.util.Beans;


import org.apache.ofbiz.persistence.entity.x;
import org.apache.ofbiz.model.ServiceContext;
import org.apache.ofbiz.model.ZipSalesServicesContext;
/**
 * Zip-Sales Database Services
 */
public class ZipSalesServices {

    private static final String MODULE = ZipSalesServices.class.getName();
    private static final String RES_ERROR = x.OrderErrorUiLabels;
    private static final String DATA_FILE = x.org_apache_ofbiz_order_thirdparty_zipsales_ZipSalesTaxTables_xml;
    private static final String FLAT_TABLE = x.FlatTaxTable;
    private static final String RULE_TABLE = x.FreightRuleTable;

    // date formatting
    private static final String DATE_PATTERN = x.yyyyMMdd;

    // import table service
    public static Map<String, Object> importFlatTable(DispatchContext dctx, ZipSalesServicesContext context) {
        Delegator delegator = dctx.getDelegator();
        Security security = dctx.getSecurity();
        GenericValue userLogin = (GenericValue) context.get(x.userLogin);
        String taxFileLocation = (String) context.get(x.taxFileLocation);
        String ruleFileLocation = (String) context.get(x.ruleFileLocation);
        Locale locale = (Locale) context.get(x.locale);

        // do security check
        if (!security.hasPermission(x.SERVICE_INVOKE_ANY, userLogin)) {
            return ServiceUtil.returnError(UtilProperties.getMessage(RES_ERROR, x.OrderYouDoNotHavePermissionToLoadTaxTables, locale));
        }

        // get a now stamp (we'll use 2000-01-01)
        Timestamp now = parseDate(x._20000101, null);

        // load the data file
        DataFile tdf = null;
        try {
            tdf = DataFile.makeDataFile(UtilURL.fromResource(DATA_FILE), FLAT_TABLE);
        } catch (DataFileException e) {
            Debug.logError(e, MODULE);
            return ServiceUtil.returnError(UtilProperties.getMessage(RES_ERROR, x.OrderUnableToReadZipSalesDataFile, locale));
        }

        // locate the file to be imported
        URL tUrl = UtilURL.fromResource(taxFileLocation);
        if (tUrl == null) {
            return ServiceUtil.returnError(UtilProperties.getMessage(RES_ERROR, x.OrderUnableToLocateTaxFileAtLocation, UtilMisc.toMap(
                    x.taxFileLocation, taxFileLocation), locale));
        }

        RecordIterator tri = null;
        try {
            tri = tdf.makeRecordIterator(tUrl);
        } catch (DataFileException e) {
            Debug.logError(e, MODULE);
            return ServiceUtil.returnError(UtilProperties.getMessage(RES_ERROR, x.OrderProblemGettingTheRecordIterator, locale));
        }
        if (tri != null) {
            while (tri.hasNext()) {
                Record entry = null;
                try {
                    entry = tri.next();
                } catch (DataFileException e) {
                    Debug.logError(e, MODULE);
                }
                GenericValue newValue = delegator.makeValue(x.ZipSalesTaxLookup);
                // PK fields
                newValue.set(x.zipCode, entry.getString(x.zipCode).trim());
                newValue.set(x.stateCode, entry.get(x.stateCode) != null ? entry.getString(x.stateCode).trim() : x.NA);
                newValue.set(x.city, entry.get(x.city) != null ? entry.getString(x.city).trim() : x.NA);
                newValue.set(x.county, entry.get(x.county) != null ? entry.getString(x.county).trim() : x.NA);
                newValue.set(x.fromDate, parseDate(entry.getString(x.effectiveDate), now));

                // non-PK fields
                newValue.set(x.countyFips, entry.get(x.countyFips));
                newValue.set(x.countyDefault, entry.get(x.countyDefault));
                newValue.set(x.generalDefault, entry.get(x.generalDefault));
                newValue.set(x.insideCity, entry.get(x.insideCity));
                newValue.set(x.geoCode, entry.get(x.geoCode));
                newValue.set(x.stateSalesTax, entry.get(x.stateSalesTax));
                newValue.set(x.citySalesTax, entry.get(x.citySalesTax));
                newValue.set(x.cityLocalSalesTax, entry.get(x.cityLocalSalesTax));
                newValue.set(x.countySalesTax, entry.get(x.countySalesTax));
                newValue.set(x.countyLocalSalesTax, entry.get(x.countyLocalSalesTax));
                newValue.set(x.comboSalesTax, entry.get(x.comboSalesTax));
                newValue.set(x.stateUseTax, entry.get(x.stateUseTax));
                newValue.set(x.cityUseTax, entry.get(x.cityUseTax));
                newValue.set(x.cityLocalUseTax, entry.get(x.cityLocalUseTax));
                newValue.set(x.countyUseTax, entry.get(x.countyUseTax));
                newValue.set(x.countyLocalUseTax, entry.get(x.countyLocalUseTax));
                newValue.set(x.comboUseTax, entry.get(x.comboUseTax));

                try {
                    delegator.createOrStore(newValue);
                } catch (GenericEntityException e) {
                    Debug.logError(e, MODULE);
                    return ServiceUtil.returnError(UtilProperties.getMessage(RES_ERROR, x.OrderErrorWritingRecordsToTheDatabase, locale));
                }

                // console log
                Debug.logInfo(newValue.get(x.zipCode) + x.str_42099b4a + newValue.get(x.stateCode) + x.str_42099b4a + newValue.get(x.city) + x.str_42099b4a
                        + newValue.get(x.county) + x.str_42099b4a + newValue.get(x.fromDate), MODULE);
            }
        }

        // load the data file
        DataFile rdf = null;
        try {
            rdf = DataFile.makeDataFile(UtilURL.fromResource(DATA_FILE), RULE_TABLE);
        } catch (DataFileException e) {
            Debug.logError(e, MODULE);
            return ServiceUtil.returnError(UtilProperties.getMessage(RES_ERROR, x.OrderUnableToReadZipSalesDataFile, locale));
        }

        // locate the file to be imported
        URL rUrl = UtilURL.fromResource(ruleFileLocation);
        if (rUrl == null) {
            return ServiceUtil.returnError(UtilProperties.getMessage(RES_ERROR, x.OrderUnableToLocateRuleFileFromLocation, UtilMisc.toMap(
                    x.ruleFileLocation, ruleFileLocation), locale));
        }

        RecordIterator rri = null;
        try {
            rri = rdf.makeRecordIterator(rUrl);
        } catch (DataFileException e) {
            Debug.logError(e, MODULE);
            return ServiceUtil.returnError(UtilProperties.getMessage(RES_ERROR, x.OrderProblemGettingTheRecordIterator, locale));
        }
        if (rri != null) {
            while (rri.hasNext()) {
                Record entry = null;
                try {
                    entry = rri.next();
                } catch (DataFileException e) {
                    Debug.logError(e, MODULE);
                }
                if (UtilValidate.isNotEmpty(entry.getString(x.stateCode))) {
                    GenericValue newValue = delegator.makeValue(x.ZipSalesRuleLookup);
                    // PK fields
                    newValue.set(x.stateCode, entry.get(x.stateCode) != null ? entry.getString(x.stateCode).trim() : x.NA);
                    newValue.set(x.city, entry.get(x.city) != null ? entry.getString(x.city).trim() : x.NA);
                    newValue.set(x.county, entry.get(x.county) != null ? entry.getString(x.county).trim() : x.NA);
                    newValue.set(x.fromDate, parseDate(entry.getString(x.effectiveDate), now));

                    // non-PK fields
                    newValue.set(x.idCode, entry.get(x.idCode) != null ? entry.getString(x.idCode).trim() : null);
                    newValue.set(x.taxable, entry.get(x.taxable) != null ? entry.getString(x.taxable).trim() : null);
                    newValue.set(x.shipCond, entry.get(x.shipCond) != null ? entry.getString(x.shipCond).trim() : null);

                    try {
                        // using storeAll as an easy way to create/update
                        delegator.storeAll(UtilMisc.toList(newValue));
                    } catch (GenericEntityException e) {
                        Debug.logError(e, MODULE);
                        return ServiceUtil.returnError(UtilProperties.getMessage(RES_ERROR, x.OrderErrorWritingRecordsToTheDatabase, locale));
                    }

                    // console log
                    Debug.logInfo(newValue.get(x.stateCode) + x.str_42099b4a + newValue.get(x.city) + x.str_42099b4a + newValue.get(x.county) + x.str_42099b4a + newValue.get(
                            x.fromDate), MODULE);
                }
            }
        }

        return ServiceUtil.returnSuccess();
    }

    // tax calc service
    public static Map<String, Object> flatTaxCalc(DispatchContext dctx, ZipSalesServicesContext context) {
        Delegator delegator = dctx.getDelegator();
        List<GenericValue> itemProductList = UtilGenerics.cast(context.get(x.itemProductList));
        List<BigDecimal> itemAmountList = UtilGenerics.cast(context.get(x.itemAmountList));
        List<BigDecimal> itemShippingList = UtilGenerics.cast(context.get(x.itemShippingList));
        BigDecimal orderShippingAmount = (BigDecimal) context.get(x.orderShippingAmount);
        GenericValue shippingAddress = (GenericValue) context.get(x.shippingAddress);

        // flatTaxCalc only uses the Zip + City from the address
        String stateProvince = shippingAddress.getString(x.stateProvinceGeoId);
        String postalCode = shippingAddress.getString(x.postalCode);
        String city = shippingAddress.getString(x.city);

        // setup the return lists.
        List<GenericValue> orderAdjustments = new LinkedList<>();
        List<List<GenericValue>> itemAdjustments = new LinkedList<>();

        // check for a valid state/province geo
        String validStates = EntityUtilProperties.getPropertyValue(x.zipsales, x.zipsales_valid_states, delegator);
        if (UtilValidate.isNotEmpty(validStates)) {
            List<String> stateSplit = StringUtil.split(validStates, x.str_3eb41622);
            if (!stateSplit.contains(stateProvince)) {
                Map<String, Object> result = ServiceUtil.returnSuccess();
                result.put(x.orderAdjustments, orderAdjustments);
                result.put(x.itemAdjustments, itemAdjustments);
                return result;
            }
        }

        try {
            // loop through and get per item tax rates
            for (int i = 0; i < itemProductList.size(); i++) {
                GenericValue product = itemProductList.get(i);
                BigDecimal itemAmount = itemAmountList.get(i);
                BigDecimal shippingAmount = itemShippingList.get(i);
                itemAdjustments.add(getItemTaxList(delegator, product, postalCode, city, itemAmount, shippingAmount, false));
            }
            if (orderShippingAmount.compareTo(BigDecimal.ZERO) > 0) {
                List<GenericValue> taxList = getItemTaxList(delegator, null, postalCode, city, BigDecimal.ZERO, orderShippingAmount, false);
                orderAdjustments.addAll(taxList);
            }
        } catch (GeneralException e) {
            return ServiceUtil.returnError(e.getMessage());
        }

        Map<String, Object> result = ServiceUtil.returnSuccess();
        result.put(x.orderAdjustments, orderAdjustments);
        result.put(x.itemAdjustments, itemAdjustments);
        return result;
    }

    private static List<GenericValue> getItemTaxList(Delegator delegator, GenericValue item, String zipCode, String city, BigDecimal itemAmount,
                                                     BigDecimal shippingAmount, boolean isUseTax) throws GeneralException {
        List<GenericValue> adjustments = new LinkedList<>();

        // check the item for tax status
        if (item != null && item.get(x.taxable) != null && x.N.equals(item.getString(x.taxable))) {
            // item not taxable
            return adjustments;
        }

        // lookup the records
        List<GenericValue> zipLookup = new LinkedList<>();
        try {
            ZipSalesTaxLookupDao zipSalesTaxLookupDao = DaoRegistry.getDao(delegator, x.ZipSalesTaxLookup, ZipSalesTaxLookupDao.class);
            for (ZipSalesTaxLookupEntity zipSalesTaxLookupEntity : zipSalesTaxLookupDao.list(Filters.eq(x.zipCode, zipCode))) {
                zipLookup.add(delegator.makeValue(x.ZipSalesTaxLookup, Beans.beanToMap(zipSalesTaxLookupEntity)));
            }
            zipLookup = EntityUtil.orderBy(zipLookup, UtilMisc.toList(x.fromDate_f5440273));
        } catch (Exception e) {
            throw new GeneralException(e);
        }
        if (UtilValidate.isEmpty(zipLookup)) {
            throw new GeneralException(x.The_zip_code_entered_is_not_valid);
        }

        // the filtered list
        // TODO: taxLookup is always null, so filter by County will never be executed
        List<GenericValue> taxLookup = null;

        // only do filtering if there are more then one zip code found
        if (zipLookup != null && zipLookup.size() > 1) {
            // first filter by city
            List<GenericValue> cityLookup = EntityUtil.filterByAnd(zipLookup, UtilMisc.toMap(x.city, city.toUpperCase()));
            if (UtilValidate.isNotEmpty(cityLookup)) {
                if (cityLookup.size() > 1) {
                    // filter by county
                    List<GenericValue> countyLookup = EntityUtil.filterByAnd(taxLookup, UtilMisc.toMap(x.countyDefault, x.Y));
                    if (UtilValidate.isNotEmpty(countyLookup)) {
                        // use the county default
                        taxLookup = countyLookup;
                    } else {
                        // no county default; just use the first city
                        taxLookup = cityLookup;
                    }
                } else {
                    // just one city found; use that one
                    taxLookup = cityLookup;
                }
            } else {
                // no city found; lookup default city
                List<GenericValue> defaultLookup = EntityUtil.filterByAnd(zipLookup, UtilMisc.toMap(x.generalDefault, x.Y));
                if (UtilValidate.isNotEmpty(defaultLookup)) {
                    // use the default city lookup
                    taxLookup = defaultLookup;
                } else {
                    // no default found; just use the first from the zip lookup
                    taxLookup = zipLookup;
                }
            }
        } else {
            // zero or 1 zip code found; use it
            taxLookup = zipLookup;
        }

        // get the first one
        GenericValue taxEntry = null;
        if (UtilValidate.isNotEmpty(taxLookup)) {
            taxEntry = taxLookup.iterator().next();
        }

        if (taxEntry == null) {
            Debug.logWarning(x.No_tax_entry_found_for + zipCode + x.str_0d0c4ddd + city + x.str_fc02e199 + itemAmount, MODULE);
            return adjustments;
        }

        String fieldName = x.comboSalesTax;
        if (isUseTax) {
            fieldName = x.comboUseTax;
        }

        BigDecimal comboTaxRate = taxEntry.getBigDecimal(fieldName);
        if (comboTaxRate == null) {
            Debug.logWarning(x.No_Combo_Tax_Rate_In_Field + fieldName + x.str_8dc29a72 + zipCode + x.str_0d0c4ddd + city + x.str_fc02e199 + itemAmount, MODULE);
            return adjustments;
        }

        // get state code
        String stateCode = taxEntry.getString(x.stateCode);

        // check if shipping is exempt
        boolean taxShipping = true;

        // look up the rules
        List<GenericValue> ruleLookup = null;
        try {
            ruleLookup = new LinkedList<>();
            ZipSalesRuleLookupDao zipSalesRuleLookupDao = DaoRegistry.getDao(delegator, x.ZipSalesRuleLookup, ZipSalesRuleLookupDao.class);
            for (ZipSalesRuleLookupEntity zipSalesRuleLookupEntity : zipSalesRuleLookupDao.list(Filters.eq(x.stateCode, stateCode))) {
                ruleLookup.add(delegator.makeValue(x.ZipSalesRuleLookup, Beans.beanToMap(zipSalesRuleLookupEntity)));
            }
            ruleLookup = EntityUtil.orderBy(ruleLookup, UtilMisc.toList(x.fromDate_f5440273));
        } catch (Exception e) {
            Debug.logError(e, MODULE);
        }

        // filter out city
        if (ruleLookup != null && ruleLookup.size() > 1) {
            ruleLookup = EntityUtil.filterByAnd(ruleLookup, UtilMisc.toMap(x.city, city.toUpperCase()));
        }

        // no county captured; so filter by date
        if (ruleLookup != null && ruleLookup.size() > 1) {
            ruleLookup = EntityUtil.filterByDate(ruleLookup);
        }

        if (ruleLookup != null) {
            for (GenericValue rule : ruleLookup) {
                if (!taxShipping) {
                    // if we found an rule which passes no need to contine (all rules are ||)
                    break;
                }
                String idCode = rule.getString(x.idCode);
                String taxable = rule.getString(x.taxable);
                String condition = rule.getString(x.shipCond);
                if (x.T.equals(taxable)) {
                    // this record is taxable
                    continue;
                } else {
                    // except if conditions are met
                    boolean qualify = false;
                    if (UtilValidate.isNotEmpty(condition)) {
                        char[] conditions = condition.toCharArray();
                        for (int i = 0; i < conditions.length; i++) {
                            switch (conditions[i]) {
                            case 'A':
                                // SHIPPING CHARGE SEPARATELY STATED ON INVOICE
                                qualify = true; // OFBiz does this by default
                                break;
                            case 'B':
                                // SHIPPING CHARGE SEPARATED ON INVOICE FROM HANDLING OR SIMILAR CHARGES
                                qualify = false; // we do not support this currently
                                break;
                            case 'C':
                                // ITEM NOT SOLD FOR GUARANTEED SHIPPED PRICE
                                qualify = false; // we don't support this currently
                                break;
                            case 'D':
                                // SHIPPING CHARGE IS COST ONLY
                                qualify = false; // we assume a handling charge is included
                                break;
                            case 'E':
                                // SHIPPED DIRECTLY TO PURCHASER
                                qualify = true; // this is true, unless gifts do not count?
                                break;
                            case 'F':
                                // SHIPPED VIA COMMON CARRIER
                                qualify = true; // best guess default
                                break;
                            case 'G':
                                // SHIPPED VIA CONTRACT CARRIER
                                qualify = false; // best guess default
                                break;
                            case 'H':
                                // SHIPPED VIA VENDOR EQUIPMENT
                                qualify = false; // best guess default
                                break;
                            case 'I':
                                // SHIPPED F.O.B. ORIGIN
                                qualify = false; // no clue
                                break;
                            case 'J':
                                // SHIPPED F.O.B. DESTINATION
                                qualify = false; // no clue
                                break;
                            case 'K':
                                // F.O.B. IS PURCHASERS OPTION
                                qualify = false; // no clue
                                break;
                            case 'L':
                                // SHIPPING ORIGINATES OR TERMINATES IN DIFFERENT STATES
                                qualify = true; // not determined at order time, no way to know
                                break;
                            case 'M':
                                // PROOF OF VENDOR ACTING AS SHIPPING AGENT FOR PURCHASER
                                qualify = false; // no clue
                                break;
                            case 'N':
                                // SHIPPED FROM VENDOR LOCATION
                                qualify = true; // sure why not
                                break;
                            case 'O':
                                // SHIPPING IS BY PURCHASER OPTION
                                qualify = false; // most online stores require shipping
                                break;
                            case 'P':
                                // CREDIT ALLOWED FOR SHIPPING CHARGE PAID BY PURCHASER TO CARRIER
                                qualify = false; // best guess default
                                break;
                            default:
                                break;
                            }
                        }
                    }

                    if (qualify) {
                        if (isUseTax) {
                            if (idCode.indexOf('U') > 0) {
                                taxShipping = false;
                            }
                        } else {
                            if (idCode.indexOf('S') > 0) {
                                taxShipping = false;
                            }
                        }
                    }
                }
            }
        }

        BigDecimal taxableAmount = itemAmount;
        if (taxShipping) {
            //Debug.logInfo("Taxing shipping", MODULE);
            taxableAmount = taxableAmount.add(shippingAmount);
        } else {
            Debug.logInfo(x.Shipping_is_not_taxable, MODULE);
        }

        // calc tax amount
        BigDecimal taxRate = comboTaxRate;
        BigDecimal taxCalc = taxableAmount.multiply(taxRate);

        adjustments.add(delegator.makeValue(x.OrderAdjustment, UtilMisc.toMap(x.amount, taxCalc, x.orderAdjustmentTypeId, x.SALES_TAX, x.comments,
                taxRate, x.description, x.Sales_Tax + stateCode + x.str_e7064f0b)));

        return adjustments;
    }

    // formatting methods
    private static Timestamp parseDate(String dateString, Timestamp useWhenNull) {
        Timestamp ts = null;
        SimpleDateFormat dateFormat = new SimpleDateFormat(DATE_PATTERN);
        if (dateString != null) {
            try {
                ts = new Timestamp(dateFormat.parse(dateString).getTime());
            } catch (ParseException e) {
                Debug.logError(e, MODULE);
            }
        }

        if (ts != null) {
            return ts;
        } else {
            return useWhenNull;
        }
    }
}
