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
package org.apache.ofbiz.entityext;

import java.util.Map;

import org.apache.ofbiz.base.util.Debug;
import org.apache.ofbiz.base.util.UtilValidate;
import org.apache.ofbiz.entity.GenericEntityException;
import org.apache.ofbiz.entity.GenericValue;
import org.apache.ofbiz.service.DispatchContext;
import org.apache.ofbiz.service.ServiceUtil;


import org.apache.ofbiz.persistence.entity.x;
import org.apache.ofbiz.model.ServiceContext;
import org.apache.ofbiz.model.EntityWatchServicesContext;
public class EntityWatchServices {

    private static final String MODULE = EntityWatchServices.class.getName();

    /**
     * This service is meant to be called through an Entity ECA (EECA) to watch an entity
     * @param dctx the dispatch context
     * @param context the context
     * @return the result of the service execution
     */
    public static Map<String, Object> watchEntity(DispatchContext dctx, EntityWatchServicesContext context) {
        GenericValue newValue = (GenericValue) context.get(x.newValue);
        String fieldName = (String) context.get(x.fieldName);

        if (newValue == null) {
            return ServiceUtil.returnSuccess();
        }

        GenericValue currentValue = null;
        try {
            currentValue = dctx.getDelegator().findOne(newValue.getEntityName(), newValue.getPrimaryKey(), false);
        } catch (GenericEntityException e) {
            String errMsg = x.Error_finding_currentValue_for_primary_key + newValue.getPrimaryKey() + x.str_89222ecc + e.toString();
            Debug.logError(e, errMsg, MODULE);
        }

        if (currentValue != null) {
            if (UtilValidate.isNotEmpty(fieldName)) {
                // just watch the field
                Object currentFieldValue = currentValue.get(fieldName);
                Object newFieldValue = newValue.get(fieldName);
                boolean changed = false;
                if (currentFieldValue != null) {
                    if (!currentFieldValue.equals(newFieldValue)) {
                        changed = true;
                    }
                } else {
                    if (newFieldValue != null) {
                        changed = true;
                    }
                }

                if (changed) {
                    String errMsg = x.Watching_entity + currentValue.getEntityName() + x.field + fieldName + x.value_changed_from
                            + currentFieldValue + x.to_23757279 + newFieldValue + x.for_pk + newValue.getPrimaryKey() + x.str_4ff447b8;
                    Debug.logInfo(new Exception(errMsg), errMsg, MODULE);
                }
            } else {
                // watch the whole entity
                if (!currentValue.equals(newValue)) {
                    String errMsg = x.Watching_entity + currentValue.getEntityName() + x.values_changed_from + currentValue + x.to_23757279
                            + newValue + x.for_pk + newValue.getPrimaryKey() + x.str_4ff447b8;
                    Debug.logInfo(new Exception(errMsg), errMsg, MODULE);
                }
            }
        } else {
            if (UtilValidate.isNotEmpty(fieldName)) {
                // just watch the field
                Object newFieldValue = newValue.get(fieldName);
                String errMsg = x.Watching_entity + newValue.getEntityName() + x.field + fieldName + x.value_changed_from_null_to
                        + newFieldValue + x.for_pk + newValue.getPrimaryKey() + x.str_4ff447b8;
                Debug.logInfo(new Exception(errMsg), errMsg, MODULE);
            } else {
                // watch the whole entity
                String errMsg = x.Watching_entity + newValue.getEntityName() + x.values_changed_from_null_to + newValue + x.for_pk
                        + newValue.getPrimaryKey() + x.str_4ff447b8;
                Debug.logInfo(new Exception(errMsg), errMsg, MODULE);
            }
        }

        return ServiceUtil.returnSuccess();
    }
}
