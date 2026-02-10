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
package org.apache.ofbiz.product.migrate;

import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.apache.ofbiz.base.util.Debug;
import org.apache.ofbiz.base.util.UtilValidate;
import org.apache.ofbiz.entity.Delegator;
import org.apache.ofbiz.entity.GenericValue;
import org.apache.ofbiz.persistence.dao.ContactMechDao;
import org.apache.ofbiz.persistence.dao.DaoRegistry;
import org.apache.ofbiz.persistence.dao.ProductPromoCodeEmailDao;
import org.apache.ofbiz.persistence.entity.ContactMechEntity;
import org.apache.ofbiz.persistence.entity.ProductPromoCodeEmailEntity;
import org.apache.ofbiz.service.DispatchContext;
import org.apache.ofbiz.service.ServiceUtil;
import com.landawn.abacus.query.Filters;


import org.apache.ofbiz.persistence.entity.x;
import org.apache.ofbiz.model.ServiceContext;
import org.apache.ofbiz.model.MigrationServicesContext;
public class MigrationServices {
    private static final String MODULE = MigrationServices.class.getName();

    public static Map<String, Object> migrateProductPromoCodeEmail(DispatchContext dctx, MigrationServicesContext context) {
        Delegator delegator = dctx.getDelegator();
        List<Object> errors = new LinkedList<>();
        ProductPromoCodeEmailDao productPromoCodeEmailDao = DaoRegistry.getDao(delegator, x.OldProductPromoCodeEmail, ProductPromoCodeEmailDao.class);
        ContactMechDao contactMechDao = DaoRegistry.getDao(delegator, x.ContactMech, ContactMechDao.class);

        try {
            for (ProductPromoCodeEmailEntity productPromoCodeEmail : productPromoCodeEmailDao.list(Filters.alwaysTrue())) {
                String contactMechId;

                String emailAddress = productPromoCodeEmail.getEmailAddress();
                if (!UtilValidate.isEmail(emailAddress)) {
                    Debug.logError(emailAddress + x.is_not_a_valid_email_address, MODULE);
                    errors.add(emailAddress + x.is_not_a_valid_email_address_e6982769);
                    continue;
                }

                long contactMechs = contactMechDao.count(Filters.eq(x.infoString, emailAddress));
                if (contactMechs > 1) {
                    errors.add(emailAddress + x.Too_many_contactMechIds_found);
                    continue;
                }

                ContactMechEntity contactMechEntity = contactMechDao.list(Filters.eq(x.infoString, emailAddress)).stream().findFirst().orElse(null);
                if (contactMechEntity == null) {
                    //If no contactMech found create new
                    GenericValue newContactMech = delegator.makeValue(x.ContactMech);
                    contactMechId = delegator.getNextSeqId(x.ContactMech);
                    newContactMech.set(x.contactMechId, contactMechId);
                    newContactMech.set(x.contactMechTypeId, x.EMAIL_ADDRESS);
                    newContactMech.set(x.infoString, emailAddress);
                    delegator.create(newContactMech);
                } else {
                    contactMechId = contactMechEntity.getContactMechId();
                }

                GenericValue prodPromoCodeContMech = delegator.makeValue(x.ProdPromoCodeContactMech);
                prodPromoCodeContMech.set(x.productPromoCodeId, productPromoCodeEmail.getProductPromoCodeId());
                prodPromoCodeContMech.set(x.contactMechId, contactMechId);
                //createOrStore to avoid duplicate data for same email.
                delegator.createOrStore(prodPromoCodeContMech);
            }

        } catch (Exception e) {
            Debug.logError(e, MODULE);
        }
        return ServiceUtil.returnSuccess(x.Data_has_been_migrated_with_following_errors + errors);
    }
}
