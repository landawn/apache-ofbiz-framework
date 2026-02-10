/*******************************************************************************
 * Licensed partyIdTo the Apache Software Foundation (ASF) under one
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

package org.apache.ofbiz.party.party;

import java.sql.Timestamp;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.apache.ofbiz.base.util.Debug;
import org.apache.ofbiz.base.util.UtilDateTime;
import org.apache.ofbiz.base.util.UtilMisc;
import org.apache.ofbiz.base.util.UtilProperties;
import org.apache.ofbiz.base.util.UtilValidate;
import org.apache.ofbiz.entity.Delegator;
import org.apache.ofbiz.entity.GenericValue;
import org.apache.ofbiz.persistence.dao.DaoRegistry;
import org.apache.ofbiz.persistence.dao.PartyRelationshipDao;
import org.apache.ofbiz.persistence.dao.PartyRoleDao;
import org.apache.ofbiz.persistence.entity.PartyRelationshipEntity;
import org.apache.ofbiz.persistence.entity.PartyRoleEntity;
import org.apache.ofbiz.service.DispatchContext;
import org.apache.ofbiz.service.GenericServiceException;
import org.apache.ofbiz.service.LocalDispatcher;
import org.apache.ofbiz.service.ModelService;
import org.apache.ofbiz.service.ServiceUtil;
import com.landawn.abacus.query.Filters;
import com.landawn.abacus.util.Beans;



import org.apache.ofbiz.persistence.entity.x;
import org.apache.ofbiz.model.ServiceContext;
import org.apache.ofbiz.model.PartyRelationshipServicesContext;
/**
 * Services for Party Relationship maintenance
 */
public class PartyRelationshipServices {

    private static final String MODULE = PartyRelationshipServices.class.getName();
    private static final String RES_ERROR = x.PartyErrorUiLabels;

    /** Creates and updates a PartyRelationship creating related PartyRoles if needed.
     *  A side of the relationship is checked to maintain history
     *@param ctx The DispatchContext that this service is operating in
     *@param context Map containing the input parameters
     *@return Map with the result of the service, the output parameters
     */
    public static Map<String, Object> createUpdatePartyRelationshipAndRoles(DispatchContext ctx, PartyRelationshipServicesContext context) {
        Map<String, Object> result = new HashMap<>();
        Delegator delegator = ctx.getDelegator();
        LocalDispatcher dispatcher = ctx.getDispatcher();
        Locale locale = (Locale) context.get(x.locale);

        try {
            List<GenericValue> partyRelationShipList = PartyRelationshipHelper.getActivePartyRelationships(delegator, context);
            if (UtilValidate.isEmpty(partyRelationShipList)) { // If already exists and active nothing to do: keep the current one
                String partyId = (String) context.get(x.partyId);
                String partyIdFrom = (String) context.get(x.partyIdFrom);
                String partyIdTo = (String) context.get(x.partyIdTo);
                String roleTypeIdFrom = (String) context.get(x.roleTypeIdFrom);
                String roleTypeIdTo = (String) context.get(x.roleTypeIdTo);
                String partyRelationshipTypeId = (String) context.get(x.partyRelationshipTypeId);
                PartyRoleDao partyRoleDao = DaoRegistry.getDao(delegator, x.PartyRole, PartyRoleDao.class);
                PartyRelationshipDao partyRelationshipDao = DaoRegistry.getDao(delegator, x.PartyRelationship, PartyRelationshipDao.class);
                Timestamp nowTimestamp = UtilDateTime.nowTimestamp();

                // Before creating the partyRelationShip, create the partyRoles if they don't exist
                GenericValue partyToRole = null;
                PartyRoleEntity partyToRoleEntity = partyRoleDao.list(Filters.and(Filters.eq(x.partyId, partyIdTo), Filters.eq(x.roleTypeId, roleTypeIdTo)))
                        .stream().findFirst().orElse(null);
                if (partyToRoleEntity != null) {
                    partyToRole = delegator.makeValue(x.PartyRole, Beans.beanToMap(partyToRoleEntity));
                }
                if (partyToRole == null) {
                    partyToRole = delegator.makeValue(x.PartyRole, UtilMisc.toMap(x.partyId, partyIdTo, x.roleTypeId, roleTypeIdTo));
                    partyToRole.create();
                }

                GenericValue partyFromRole = null;
                PartyRoleEntity partyFromRoleEntity = partyRoleDao.list(Filters.and(Filters.eq(x.partyId, partyIdFrom), Filters.eq(x.roleTypeId, roleTypeIdFrom)))
                        .stream().findFirst().orElse(null);
                if (partyFromRoleEntity != null) {
                    partyFromRole = delegator.makeValue(x.PartyRole, Beans.beanToMap(partyFromRoleEntity));
                }
                if (partyFromRole == null) {
                    partyFromRole = delegator.makeValue(x.PartyRole, UtilMisc.toMap(x.partyId, partyIdFrom, x.roleTypeId, roleTypeIdFrom));
                    partyFromRole.create();
                }

                // Check if there is already a partyRelationship of that type with another party from the side indicated
                String sideChecked = partyIdFrom.equals(partyId) ? x.partyIdFrom : x.partyIdTo;
                // We consider the last one (in time) as sole active (we try to maintain a unique relationship and keep changes history)
                PartyRelationshipEntity oldPartyRelationShipEntity = partyRelationshipDao
                        .list(Filters.and(Filters.eq(sideChecked, partyId), Filters.eq(x.roleTypeIdFrom, roleTypeIdFrom), Filters.eq(x.roleTypeIdTo, roleTypeIdTo),
                                Filters.eq(x.partyRelationshipTypeId, partyRelationshipTypeId)))
                        .stream()
                        .filter(relationShip -> (relationShip.getFromDate() == null || !relationShip.getFromDate().after(nowTimestamp))
                                && (relationShip.getThruDate() == null || relationShip.getThruDate().after(nowTimestamp)))
                        .findFirst()
                        .orElse(null);
                GenericValue oldPartyRelationShip = oldPartyRelationShipEntity == null ? null
                        : delegator.makeValue(x.PartyRelationship, Beans.beanToMap(oldPartyRelationShipEntity));
                if (oldPartyRelationShip != null) {
                    oldPartyRelationShip.setFields(UtilMisc.toMap(x.thruDate, nowTimestamp)); // Current becomes inactive
                    oldPartyRelationShip.store();
                }
                try {
                    Map<String, Object> resultMap = dispatcher.runSync(x.createPartyRelationship, context); // Create new one
                    if (ServiceUtil.isError(resultMap)) {
                        return ServiceUtil.returnError(ServiceUtil.getErrorMessage(resultMap));
                    }
                } catch (GenericServiceException e) {
                    Debug.logWarning(e.getMessage(), MODULE);
                    return ServiceUtil.returnError(UtilProperties.getMessage(RES_ERROR,
                            x.partyrelationshipservices_could_not_create_party_role_write,
                            UtilMisc.toMap(x.errorString, e.getMessage()), locale));
                }
            }
        } catch (Exception e) {
            Debug.logWarning(e.getMessage(), MODULE);
            return ServiceUtil.returnError(UtilProperties.getMessage(RES_ERROR,
                    x.partyrelationshipservices_could_not_create_party_role_write,
                    UtilMisc.toMap(x.errorString, e.getMessage()), locale));
        }
        result.put(ModelService.RESPONSE_MESSAGE, ModelService.RESPOND_SUCCESS);
        return result;
    }
}
