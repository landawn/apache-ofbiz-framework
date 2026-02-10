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

import static org.apache.ofbiz.base.util.UtilGenerics.checkCollection;
import static org.apache.ofbiz.base.util.UtilGenerics.checkMap;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TimeZone;
import java.util.stream.Collectors;

import org.apache.ofbiz.base.util.Debug;
import org.apache.ofbiz.base.util.ObjectType;
import org.apache.ofbiz.base.util.StringUtil;
import org.apache.ofbiz.base.util.UtilDateTime;
import org.apache.ofbiz.base.util.UtilGenerics;
import org.apache.ofbiz.base.util.UtilHttp;
import org.apache.ofbiz.base.util.UtilMisc;
import org.apache.ofbiz.base.util.UtilProperties;
import org.apache.ofbiz.base.util.UtilValidate;
import org.apache.ofbiz.entity.Delegator;
import org.apache.ofbiz.entity.GenericEntity;
import org.apache.ofbiz.entity.GenericEntityException;
import org.apache.ofbiz.entity.GenericValue;
import org.apache.ofbiz.entity.condition.EntityComparisonOperator;
import org.apache.ofbiz.entity.condition.EntityCondition;
import org.apache.ofbiz.entity.condition.EntityConditionList;
import org.apache.ofbiz.entity.condition.EntityFunction;
import org.apache.ofbiz.entity.condition.EntityJoinOperator;
import org.apache.ofbiz.entity.condition.EntityOperator;
import org.apache.ofbiz.entity.model.DynamicViewEntity;
import org.apache.ofbiz.entity.model.ModelEntity;
import org.apache.ofbiz.entity.model.ModelField;
import org.apache.ofbiz.entity.model.ModelReader;
import org.apache.ofbiz.entity.model.ModelViewEntity;
import org.apache.ofbiz.entity.util.EntityFindOptions;
import org.apache.ofbiz.entity.util.EntityListIterator;
import org.apache.ofbiz.entity.util.EntityUtil;
import org.apache.ofbiz.entity.util.EntityUtilProperties;
import org.apache.ofbiz.service.DispatchContext;
import org.apache.ofbiz.service.GenericServiceException;
import org.apache.ofbiz.service.LocalDispatcher;
import org.apache.ofbiz.service.ServiceUtil;


import org.apache.ofbiz.persistence.entity.x;
import org.apache.ofbiz.model.ServiceContext;
import org.apache.ofbiz.model.FindServicesContext;
/**
 * FindServices Class
 */
public class FindServices {

    private static final String MODULE = FindServices.class.getName();
    private static final String RESOURCE = x.CommonUiLabels;
    public static final Map<String, EntityComparisonOperator<?, ?>> ENTITY_OPERATORS;
    public static final List<String> PERFORMFIND_SEARCH_SUFFIXES = List.of(x.ic, x.op, x.grp, x.value_0fab5fa9);

    static {
        ENTITY_OPERATORS = new LinkedHashMap<>();
        ENTITY_OPERATORS.put(x.between, EntityOperator.BETWEEN);
        ENTITY_OPERATORS.put(x.equals, EntityOperator.EQUALS);
        ENTITY_OPERATORS.put(x.greaterThan, EntityOperator.GREATER_THAN);
        ENTITY_OPERATORS.put(x.greaterThanEqualTo, EntityOperator.GREATER_THAN_EQUAL_TO);
        ENTITY_OPERATORS.put(x._in, EntityOperator.IN);
        ENTITY_OPERATORS.put(x.not_in, EntityOperator.NOT_IN);
        ENTITY_OPERATORS.put(x.lessThan, EntityOperator.LESS_THAN);
        ENTITY_OPERATORS.put(x.lessThanEqualTo, EntityOperator.LESS_THAN_EQUAL_TO);
        ENTITY_OPERATORS.put(x.like, EntityOperator.LIKE);
        ENTITY_OPERATORS.put(x.notLike, EntityOperator.NOT_LIKE);
        ENTITY_OPERATORS.put(x._not, EntityOperator.NOT);
        ENTITY_OPERATORS.put(x.notEqual, EntityOperator.NOT_EQUAL);
    }

    public FindServices() { }

    /**
     * prepareField, analyse inputFields to created normalizedFields a map with field name and operator.
     * This is use to the generic method that expects entity data affixed with special suffixes
     * to indicate their purpose in formulating an SQL query statement.
     * @param inputFields     Input parameters run thru UtilHttp.getParameterMap
     * @return a map with field name and operator
     */
    public static Map<String, Map<String, Map<String, Object>>> prepareField(Map<String, ?> inputFields, Map<String, Object>
            queryStringMap, Map<String, List<Object[]>> origValueMap) {
        // Strip the "_suffix" off of the parameter name and
        // build a three-level map of values keyed by fieldRoot name,
        //    fld0 or fld1, and, then, "op" or "value"
        // ie. id
        //  - fld0
        //      - op:like
        //      - value:abc
        //  - fld1 (if there is a range)
        //      - op:lessThan
        //      - value:55 (note: these two "flds" wouldn't really go together)
        // Also note that op/fld can be in any order. (eg. id_fld1_equals or id_equals_fld1)
        // Note that "normalizedFields" will contain values other than those
        // Contained in the associated entity.
        // Those extra fields will be ignored in the second half of this method.
        Map<String, Map<String, Map<String, Object>>> normalizedFields = new LinkedHashMap<>();
        for (Entry<String, ?> entry : inputFields.entrySet()) { // The name as it appears in the HTML form
            String fieldNameRaw = entry.getKey();
            String fieldNameRoot = null; // The entity field name. Everything to the left of the first "_" if
                                                                 //  it exists, or the whole word, if not.
            String fieldPair = null; // "fld0" or "fld1" - begin/end of range or just fld0 if no range.
            Object fieldValue = null; // If it is a "value" field, it will be the value to be used in the query.
                                                        // If it is an "op" field, it will be "equals", "greaterThan", etc.
            int iPos = -1;
            int iPos2 = -1;
            Map<String, Map<String, Object>> subMap = null;
            Map<String, Object> subMap2 = null;
            String fieldMode = null;

            fieldValue = entry.getValue();
            if (ObjectType.isEmpty(fieldValue)) {
                continue;
            }

            queryStringMap.put(fieldNameRaw, fieldValue);
            iPos = fieldNameRaw.indexOf('_'); // Look for suffix

            // This is a hack to skip fields from "multi" forms
            // These would have the form "fieldName_o_1"
            if (iPos >= 0) {
                String suffix = fieldNameRaw.substring(iPos + 1);
                iPos2 = suffix.indexOf('_');
                if (iPos2 == 1) {
                    continue;
                }
            }

            // If no suffix, assume no range (default to fld0) and operations of equals
            // If no field op is present, it will assume "equals".
            if (iPos < 0) {
                fieldNameRoot = fieldNameRaw;
                fieldPair = x.fld0;
                fieldMode = x.value;
            } else { // Must have at least "fld0/1" or "equals, greaterThan, etc."
                // Some bogus fields will slip in, like "ENTITY_NAME", but they will be ignored

                fieldNameRoot = fieldNameRaw.substring(0, iPos);
                String suffix = fieldNameRaw.substring(iPos + 1);
                iPos2 = suffix.indexOf('_');
                if (iPos2 < 0) {
                    if (suffix.startsWith(x.fld)) {
                        // If only one token and it starts with "fld"
                        //  assume it is a value field, not an op
                        fieldPair = suffix;
                        fieldMode = x.value;
                    } else {
                        // if it does not start with fld,
                        // assume it is an op or the 'ignore case' (ic) field
                        fieldPair = x.fld0;
                        fieldMode = suffix;
                    }
                } else {
                    String tkn0 = suffix.substring(0, iPos2);
                    String tkn1 = suffix.substring(iPos2 + 1);
                    // If suffix has two parts, let them be in any order
                    // One will be "fld0/1" and the other will be the op (eg. equals, greaterThan_
                    if (tkn0.startsWith(x.fld)) {
                        fieldPair = tkn0;
                        fieldMode = tkn1;
                    } else {
                        fieldPair = tkn1;
                        fieldMode = tkn0;
                    }
                }
            }
            subMap = normalizedFields.get(fieldNameRoot);
            if (subMap == null) {
                subMap = new LinkedHashMap<>();
                normalizedFields.put(fieldNameRoot, subMap);
            }
            subMap2 = subMap.get(fieldPair);
            if (subMap2 == null) {
                subMap2 = new LinkedHashMap<>();
                subMap.put(fieldPair, subMap2);
            }
            subMap2.put(fieldMode, fieldValue);

            List<Object[]> origList = origValueMap.get(fieldNameRoot);
            if (origList == null) {
                origList = new LinkedList<>();
                origValueMap.put(fieldNameRoot, origList);
            }
            Object[] origValues = {fieldNameRaw, fieldValue};
            origList.add(origValues);
        }
        return normalizedFields;
    }

    /**
     * Parses input parameters and returns an <code>EntityCondition</code> list.
     * @param parameters
     * @param fieldList
     * @param queryStringMap
     * @param delegator
     * @param context
     * @return returns an EntityCondition list
     */
    public static List<EntityCondition> createConditionList(Map<String, ?> parameters, List<ModelField> fieldList, Map<String, Object> queryStringMap,
                                                            Delegator delegator, FindServicesContext context, String groupConditionOperator) {
        Set<String> processed = new LinkedHashSet<>();
        Set<String> keys = new LinkedHashSet<>();
        Map<String, ModelField> fieldMap = new LinkedHashMap<>();
        /*
         * When inputFields contains several xxxx_grp, yyyy_grp ... values,
         * Corresponding conditions will group depending on {@param groupConditionOperator}
         * Default behaviour is grouping by an {@link EntityOperator.AND} then all added to final
         * condition grouped by an {@link EntityOperator.OR}
         * "OR_AND" behaviour is grouping by an {@link EntityOperator.OR} then all added to final
         * condition grouped by an {@link EntityOperator.AND}
         */
        EntityJoinOperator operatorInsideGroup = x.OR_AND.equals(groupConditionOperator) ? EntityOperator.OR : EntityOperator.AND;
        EntityJoinOperator operatorBetweenGroups = x.OR_AND.equals(groupConditionOperator) ? EntityOperator.AND : EntityOperator.OR;
        Map<String, List<EntityCondition>> savedGroups = new LinkedHashMap<>();
        for (ModelField modelField : fieldList) {
            fieldMap.put(modelField.getName(), modelField);
        }
        List<EntityCondition> result = new LinkedList<>();
        for (Map.Entry<String, ? extends Object> entry : parameters.entrySet()) {
            String parameterName = entry.getKey();
            if (processed.contains(parameterName)) {
                continue;
            }
            keys.clear();
            String fieldName = extractFieldNameIfSuffix(parameterName, PERFORMFIND_SEARCH_SUFFIXES);
            String currentGroup = (String) getValueFromParametersWithSuffix(fieldName, parameters, x.grp, keys);
            boolean ignoreCase = x.Y.equals(getValueFromParametersWithSuffix(fieldName, parameters, x.ic, keys));
            String operation = (String) getValueFromParametersWithSuffix(fieldName, parameters, x.op, keys);
            Object fieldValue = getValueFromParametersWithSuffix(fieldName, parameters, x.value_0fab5fa9, keys);

            if (fieldName.endsWith(x.fld0_f0a27274) || fieldName.endsWith(x.fld1)) {
                if (parameters.containsKey(fieldName)) {
                    keys.add(fieldName);
                }
                fieldName = fieldName.substring(0, fieldName.length() - 5);
            }
            if (parameters.containsKey(fieldName)) {
                keys.add(fieldName);
            }
            processed.addAll(keys);
            ModelField modelField = fieldMap.get(fieldName);
            if (modelField == null) {
                continue;
            }
            if (fieldValue == null) {
                fieldValue = parameters.get(fieldName);
            }
            if (ObjectType.isEmpty(fieldValue) && !x.empty.equals(operation)) {
                continue;
            }

            EntityCondition cond = createSingleCondition(modelField, operation, fieldValue, ignoreCase, delegator, context);
            if (UtilValidate.isEmpty(currentGroup)) {
                result.add(cond);
            } else {
                savedGroups.computeIfAbsent(currentGroup, k -> new ArrayList<>())
                        .add(cond);
            }
            keys.forEach(mapKey -> queryStringMap.put(mapKey, parameters.get(mapKey)));
        }
        List<EntityCondition> orConditions = savedGroups.keySet().stream()
                .map(groupName -> EntityCondition.makeCondition(savedGroups.get(groupName), operatorInsideGroup))
                .collect(Collectors.toList());

        if (UtilValidate.isNotEmpty(orConditions)) {
            result.add(EntityCondition.makeCondition(orConditions, operatorBetweenGroups));
        }

        return result;
    }

    private static Object getValueFromParametersWithSuffix(String fieldName, Map<String, ?> parameters, String suffix, Set<String> keys) {
        String key = fieldName.concat(suffix);
        if (parameters.containsKey(key)) {
            keys.add(key);
            return parameters.get(key);
        }
        return null;
    }

    private static String extractFieldNameIfSuffix(String parameterName, List<String> suffixes) {
        return suffixes.stream()
                .filter(parameterName::endsWith)
                .map(suffix -> parameterName.substring(0, parameterName.length() - suffix.length()))
                .findFirst().orElse(parameterName);
    }

    /**
     * Creates a single <code>EntityCondition</code> based on a set of parameters.
     * @param modelField
     * @param operation
     * @param fieldValue
     * @param ignoreCase
     * @param delegator
     * @param context
     * @return return an EntityCondition
     */
    public static EntityCondition createSingleCondition(ModelField modelField, String operation, Object fieldValue, boolean ignoreCase,
                                                        Delegator delegator, FindServicesContext context) {
        EntityCondition cond = null;
        String fieldName = modelField.getName();
        Locale locale = (Locale) context.get(x.locale);
        TimeZone timeZone = (TimeZone) context.get(x.timeZone);
        EntityComparisonOperator<?, ?> fieldOp = null;
        if (operation != null) {
            if (x.contains.equals(operation)) {
                fieldOp = EntityOperator.LIKE;
                fieldValue = x.str_4345cb1f + fieldValue + x.str_4345cb1f;
            } else if (x.not_contains.equals(operation) || x.notContains.equals(operation)) {
                fieldOp = EntityOperator.NOT_LIKE;
                fieldValue = x.str_4345cb1f + fieldValue + x.str_4345cb1f;
            } else if (x.empty.equals(operation)) {
                return EntityCondition.makeCondition(fieldName, EntityOperator.EQUALS, null);
            } else if (x.like.equals(operation)) {
                fieldOp = EntityOperator.LIKE;
                fieldValue = fieldValue + x.str_4345cb1f;
            } else if (x.not_like.equals(operation) || x.notLike.equals(operation)) {
                fieldOp = EntityOperator.NOT_LIKE;
                fieldValue = fieldValue + x.str_4345cb1f;
            } else if (x.opLessThan.equals(operation)) {
                fieldOp = EntityOperator.LESS_THAN;
            } else if (x.upToDay.equals(operation)) {
                fieldOp = EntityOperator.LESS_THAN;
            } else if (x.upThruDay.equals(operation)) {
                fieldOp = EntityOperator.LESS_THAN_EQUAL_TO;
            } else if (x.greaterThanFromDayStart.equals(operation)) {
                String timeStampString = (String) fieldValue;
                Object startValue = modelField.getModelEntity().convertFieldValue(modelField, dayStart(timeStampString, 0,
                        timeZone, locale), delegator, context);
                return EntityCondition.makeCondition(fieldName, EntityOperator.GREATER_THAN_EQUAL_TO, startValue);
            } else if (x.sameDay.equals(operation)) {
                String timeStampString = (String) fieldValue;
                Object startValue = modelField.getModelEntity().convertFieldValue(modelField, dayStart(timeStampString, 0,
                        timeZone, locale), delegator, context);
                EntityCondition startCond = EntityCondition.makeCondition(fieldName, EntityOperator.GREATER_THAN_EQUAL_TO, startValue);
                Object endValue = modelField.getModelEntity().convertFieldValue(modelField, dayStart(timeStampString, 1,
                        timeZone, locale), delegator, context);
                EntityCondition endCond = EntityCondition.makeCondition(fieldName, EntityOperator.LESS_THAN, endValue);
                return EntityCondition.makeCondition(startCond, endCond);
            } else {
                fieldOp = ENTITY_OPERATORS.get(operation);
            }
        } else {
            List<Object> fieldList = (fieldValue instanceof List) ? UtilGenerics.cast(fieldValue) : null;
            if (UtilValidate.isNotEmpty(fieldList)) {
                fieldOp = EntityOperator.IN;
            } else {
                fieldOp = EntityOperator.EQUALS;
            }
        }
        Object fieldObject = fieldValue;
        if ((fieldOp != EntityOperator.IN && fieldOp != EntityOperator.NOT_IN) || !(fieldValue instanceof Collection<?>)) {
            fieldObject = modelField.getModelEntity().convertFieldValue(modelField, fieldValue, delegator, context);
        }
        if (ignoreCase && fieldObject instanceof String) {
            cond = EntityCondition.makeCondition(EntityFunction.upperField(fieldName), fieldOp, EntityFunction.upper(((String)
                    fieldValue).toUpperCase(Locale.getDefault())));
        } else {
            if (fieldObject.equals(GenericEntity.NULL_FIELD.toString())) {
                fieldObject = null;
            }
            cond = EntityCondition.makeCondition(fieldName, fieldOp, fieldObject);
        }
        if (EntityOperator.NOT_EQUAL.equals(fieldOp) && fieldObject != null) {
            cond = EntityCondition.makeCondition(UtilMisc.toList(cond, EntityCondition.makeCondition(fieldName, null)), EntityOperator.OR);
        }
        return cond;
    }

    /**
     * createCondition, comparing the normalizedFields with the list of keys, .
     * This is use to the generic method that expects entity data affixed with special suffixes
     * to indicate their purpose in formulating an SQL query statement.
     * @param modelEntity the model entity object
     * @param normalizedFields list of field the user have populated
     * @return a arrayList usable to create an entityCondition
     */
    public static List<EntityCondition> createCondition(ModelEntity modelEntity, Map<String, Map<String, Map<String, Object>>> normalizedFields,
            Map<String, Object> queryStringMap, Map<String, List<Object[]>> origValueMap, Delegator delegator, FindServicesContext context) {
        Map<String, Map<String, Object>> subMap = null;
        Map<String, Object> subMap2 = null;
        Object fieldValue = null; // If it is a "value" field, it will be the value to be used in the query.
                                  // If it is an "op" field, it will be "equals", "greaterThan", etc.
        EntityCondition cond = null;
        List<EntityCondition> tmpList = new LinkedList<>();
        String opString = null;
        boolean ignoreCase = false;
        List<ModelField> fields = modelEntity.getFieldsUnmodifiable();
        for (ModelField modelField: fields) {
            String fieldName = modelField.getName();
            subMap = normalizedFields.get(fieldName);
            if (subMap == null) {
                continue;
            }
            subMap2 = subMap.get(x.fld0);
            fieldValue = subMap2.get(x.value);
            opString = (String) subMap2.get(x.op_824f601c);
            // null fieldValue is OK if operator is "empty"
            if (fieldValue == null && !x.empty.equals(opString)) {
                continue;
            }
            ignoreCase = x.Y.equals(subMap2.get(x.ic_8c3c21f4));
            cond = createSingleCondition(modelField, opString, fieldValue, ignoreCase, delegator, context);
            tmpList.add(cond);
            subMap2 = subMap.get(x.fld1_92f6323b);
            if (subMap2 == null) {
                continue;
            }
            fieldValue = subMap2.get(x.value);
            opString = (String) subMap2.get(x.op_824f601c);
            if (fieldValue == null && !x.empty.equals(opString)) {
                continue;
            }
            ignoreCase = x.Y.equals(subMap2.get(x.ic_8c3c21f4));
            cond = createSingleCondition(modelField, opString, fieldValue, ignoreCase, delegator, context);
            tmpList.add(cond);
            // add to queryStringMap
            List<Object[]> origList = origValueMap.get(fieldName);
            if (UtilValidate.isNotEmpty(origList)) {
                for (Object[] arr: origList) {
                    queryStringMap.put((String) arr[0], arr[1]);
                }
            }
        }
        return tmpList;
    }

    /**
     *  same as performFind but now returning a list instead of an iterator
     *  Extra parameters viewIndex: startPage of the partial list (0 = first page)
     *                              viewSize: the length of the page (number of records)
     *  Extra output parameter: listSize: size of the totallist
     *                                         list : the list itself.
     * @param dctx
     * @param context
     * @return Map
     */
    public static Map<String, Object> performFindList(DispatchContext dctx, FindServicesContext context) {
        Integer viewSize = (Integer) context.get(x.viewSize);
        if (viewSize == null) {
            viewSize = 20;       // default
        }
        context.put(x.viewSize, viewSize);
        Integer viewIndex = (Integer) context.get(x.viewIndex);
        if (viewIndex == null) {
            viewIndex = 0;  // default
        }
        context.put(x.viewIndex, viewIndex);

        Map<String, Object> result = performFind(dctx, context);

        int start = viewIndex * viewSize;
        List<GenericValue> list = null;
        Integer listSize = 0;
        try (EntityListIterator it = (EntityListIterator) result.get(x.listIt)) {
            list = it.getPartialList(start + 1, viewSize); // list starts at '1'
            listSize = it.getResultsSizeAfterPartialList();
        } catch (ClassCastException | NullPointerException | GenericEntityException e) {
            Debug.logInfo(x.Problem_getting_partial_list + e, MODULE);
        }

        result.put(x.listSize, listSize);
        result.put(x.list_38b62be4, list);
        result.remove(x.listIt);
        return result;
    }

    /**
     * performFind
     * This is a generic method that expects entity data affixed with special suffixes
     * to indicate their purpose in formulating an SQL query statement.
     */
    public static Map<String, Object> performFind(DispatchContext dctx, FindServicesContext context) {
        String entityName = (String) context.get(x.entityName);
        DynamicViewEntity dynamicViewEntity = (DynamicViewEntity) context.get(x.dynamicViewEntity);
        String orderBy = (String) context.get(x.orderBy);
        String groupConditionOperator = (String) context.get(x.groupConditionOperator);
        Map<String, ?> inputFields = checkMap(context.get(x.inputFields), String.class, Object.class); // Input
        String noConditionFind = (String) context.get(x.noConditionFind);
        String distinct = (String) context.get(x.distinct);
        List<String> fieldList = UtilGenerics.cast(context.get(x.fieldList));
        GenericValue userLogin = (GenericValue) context.get(x.userLogin);
        Locale locale = (Locale) context.get(x.locale);
        Delegator delegator = dctx.getDelegator();
        if (UtilValidate.isEmpty(noConditionFind)) {
            // try finding in inputFields Map
            noConditionFind = (String) inputFields.get(x.noConditionFind);
        }
        if (UtilValidate.isEmpty(noConditionFind)) {
            // Use configured default
            noConditionFind = EntityUtilProperties.getPropertyValue(x.widget, x.widget_defaultNoConditionFind, delegator);
        }
        String filterByDate = (String) context.get(x.filterByDate);
        if (UtilValidate.isEmpty(filterByDate)) {
            // try finding in inputFields Map
            filterByDate = (String) inputFields.get(x.filterByDate);
        }
        Timestamp filterByDateValue = (Timestamp) context.get(x.filterByDateValue);
        String fromDateName = (String) context.get(x.fromDateName);
        if (UtilValidate.isEmpty(fromDateName)) {
            // try finding in inputFields Map
            fromDateName = (String) inputFields.get(x.fromDateName);
        }
        String thruDateName = (String) context.get(x.thruDateName);
        if (UtilValidate.isEmpty(thruDateName)) {
            // try finding in inputFields Map
            thruDateName = (String) inputFields.get(x.thruDateName);
        }

        Integer viewSize = (Integer) context.get(x.viewSize);
        Integer viewIndex = (Integer) context.get(x.viewIndex);
        Integer maxRows = null;
        if (viewSize != null && viewIndex != null) {
            maxRows = viewSize * (viewIndex + 1);
        }

        LocalDispatcher dispatcher = dctx.getDispatcher();

        Map<String, Object> prepareResult = null;
        try {
            prepareResult = dispatcher.runSync(x.prepareFind, UtilMisc.toMap(x.entityName, entityName, x.orderBy, orderBy,
                                               x.dynamicViewEntity, dynamicViewEntity, x.groupConditionOperator, groupConditionOperator,
                                               x.inputFields, inputFields, x.filterByDate, filterByDate, x.noConditionFind, noConditionFind,
                                               x.filterByDateValue, filterByDateValue, x.userLogin, userLogin, x.fromDateName, fromDateName,
                    x.thruDateName, thruDateName,
                                               x.locale, context.get(x.locale), x.timeZone, context.get(x.timeZone)));
        } catch (GenericServiceException gse) {
            return ServiceUtil.returnError(UtilProperties.getMessage(RESOURCE, x.CommonFindErrorPreparingConditions,
                    UtilMisc.toMap(x.errorString, gse.getMessage()), locale));
        }
        EntityConditionList<EntityCondition> exprList = UtilGenerics.cast(prepareResult.get(x.entityConditionList));
        List<String> orderByList = checkCollection(prepareResult.get(x.orderByList), String.class);

        Map<String, Object> executeResult = null;
        try {
            executeResult = dispatcher.runSync(x.executeFind, UtilMisc.toMap(x.entityName, entityName, x.orderByList, orderByList,
                                                                             x.dynamicViewEntity, dynamicViewEntity,
                                                                             x.fieldList, fieldList, x.entityConditionList, exprList,
                                                                             x.noConditionFind, noConditionFind, x.distinct, distinct,
                                                                             x.locale, context.get(x.locale), x.timeZone, context.get(x.timeZone),
                                                                             x.maxRows, maxRows));
        } catch (GenericServiceException gse) {
            return ServiceUtil.returnError(UtilProperties.getMessage(RESOURCE, x.CommonFindErrorRetrieveIterator,
                    UtilMisc.toMap(x.errorString, gse.getMessage()), locale));
        }

        if (executeResult.get(x.listIt) == null) {
            if (Debug.verboseOn()) {
                Debug.logVerbose(x.No_list_iterator_found_for_query_string + prepareResult.get(x.queryString) + x.str_4ff447b8, MODULE);
            }
        }

        Map<String, Object> results = ServiceUtil.returnSuccess();
        results.put(x.listIt, executeResult.get(x.listIt));
        results.put(x.listSize, executeResult.get(x.listSize));
        results.put(x.queryString, prepareResult.get(x.queryString));
        results.put(x.queryStringMap, prepareResult.get(x.queryStringMap));
        return results;
    }

    /**
     * prepareFind
     * This is a generic method that expects entity data affixed with special suffixes
     * to indicate their purpose in formulating an SQL query statement.
     */
    public static Map<String, Object> prepareFind(DispatchContext dctx, FindServicesContext context) {
        String entityName = (String) context.get(x.entityName);
        DynamicViewEntity dynamicViewEntity = (DynamicViewEntity) context.get(x.dynamicViewEntity);
        Delegator delegator = dctx.getDelegator();
        String orderBy = (String) context.get(x.orderBy);
        String groupConditionOperator = (String) context.get(x.groupConditionOperator);
        Map<String, ?> inputFields = checkMap(context.get(x.inputFields), String.class, Object.class); // Input
        String noConditionFind = (String) context.get(x.noConditionFind);
        if (UtilValidate.isEmpty(noConditionFind)) {
            // try finding in inputFields Map
            noConditionFind = (String) inputFields.get(x.noConditionFind);
        }
        if (UtilValidate.isEmpty(noConditionFind)) {
            // Use configured default
            noConditionFind = EntityUtilProperties.getPropertyValue(x.widget, x.widget_defaultNoConditionFind, delegator);
        }
        String filterByDate = (String) context.get(x.filterByDate);
        if (UtilValidate.isEmpty(filterByDate)) {
            // try finding in inputFields Map
            filterByDate = (String) inputFields.get(x.filterByDate);
        }
        Timestamp filterByDateValue = (Timestamp) context.get(x.filterByDateValue);
        String fromDateName = (String) context.get(x.fromDateName);
        String thruDateName = (String) context.get(x.thruDateName);

        Map<String, Object> queryStringMap = new LinkedHashMap<>();
        ModelEntity modelEntity;
        if (dynamicViewEntity != null) {
            try {
                modelEntity = new ModelViewEntity(dynamicViewEntity, ModelReader.getModelReader(delegator.getDelegatorName()));
            } catch (GenericEntityException e) {
                return ServiceUtil.returnError(e.toString());
            }
        } else {
            modelEntity = delegator.getModelEntity(entityName);
        }
        List<EntityCondition> tmpList = createConditionList(inputFields, modelEntity.getFieldsUnmodifiable(),
                queryStringMap, delegator, context, groupConditionOperator);

        /* the filter by date condition should only be added when there are other conditions or when
         * the user has specified a noConditionFind.  Otherwise, specifying filterByDate will become
         * its own condition.
         */
        if (!tmpList.isEmpty() || x.Y.equals(noConditionFind)) {
            if (x.Y.equals(filterByDate)) {
                queryStringMap.put(x.filterByDate, filterByDate);
                if (UtilValidate.isEmpty(fromDateName)) {
                    fromDateName = x.fromDate;
                } else {
                    queryStringMap.put(x.fromDateName, fromDateName);
                }
                if (UtilValidate.isEmpty(thruDateName)) {
                    thruDateName = x.thruDate;
                } else {
                    queryStringMap.put(x.thruDateName, thruDateName);
                }
                if (UtilValidate.isEmpty(filterByDateValue)) {
                    EntityCondition filterByDateCondition = EntityUtil.getFilterByDateExpr(fromDateName, thruDateName);
                    tmpList.add(filterByDateCondition);
                } else {
                    queryStringMap.put(x.filterByDateValue, filterByDateValue);
                    EntityCondition filterByDateCondition = EntityUtil.getFilterByDateExpr(filterByDateValue, fromDateName, thruDateName);
                    tmpList.add(filterByDateCondition);
                }
            }
        }

        EntityConditionList<EntityCondition> exprList = null;
        if (!tmpList.isEmpty()) {
            exprList = EntityCondition.makeCondition(tmpList);
        }

        List<String> orderByList = null;
        if (UtilValidate.isNotEmpty(orderBy)) {
            orderByList = StringUtil.split(orderBy, x.str_3eb41622);
        }

        Map<String, Object> results = ServiceUtil.returnSuccess();
        queryStringMap.put(x.noConditionFind, noConditionFind);
        String queryString = UtilHttp.urlEncodeArgs(queryStringMap);
        results.put(x.queryString, queryString);
        results.put(x.queryStringMap, queryStringMap);
        results.put(x.orderByList, orderByList);
        results.put(x.entityConditionList, exprList);
        return results;
    }

    /**
     * executeFind
     * This is a generic method that returns an EntityListIterator.
     */
    public static Map<String, Object> executeFind(DispatchContext dctx, FindServicesContext context) {
        String entityName = (String) context.get(x.entityName);
        DynamicViewEntity dynamicViewEntity = (DynamicViewEntity) context.get(x.dynamicViewEntity);
        EntityConditionList<EntityCondition> entityConditionList = UtilGenerics.cast(context.get(x.entityConditionList));
        List<String> orderByList = checkCollection(context.get(x.orderByList), String.class);
        boolean noConditionFind = x.Y.equals(context.get(x.noConditionFind));
        boolean distinct = x.Y.equals(context.get(x.distinct));
        List<String> fieldList = UtilGenerics.cast(context.get(x.fieldList));
        Locale locale = (Locale) context.get(x.locale);
        Set<String> fieldSet = null;
        if (fieldList != null) {
            fieldSet = new LinkedHashSet<>(fieldList);
        }
        Integer maxRows = (Integer) context.get(x.maxRows);
        maxRows = maxRows != null ? maxRows : -1;
        Delegator delegator = dctx.getDelegator();
        // Retrieve entities  - an iterator over all the values
        EntityListIterator listIt = null;
        int listSize = 0;
        try {
            if (noConditionFind || (entityConditionList != null && entityConditionList.getConditionListSize() > 0)) {
                EntityFindOptions findOptions = new EntityFindOptions(true, EntityFindOptions.TYPE_SCROLL_INSENSITIVE,
                        EntityFindOptions.CONCUR_READ_ONLY, distinct);
                findOptions.setMaxRows(maxRows);

                if (dynamicViewEntity != null) {
                    listIt = delegator.findListIteratorByCondition(dynamicViewEntity, entityConditionList, null, fieldSet, orderByList, findOptions);
                } else {
                    listIt = delegator.find(entityName, entityConditionList, null, fieldSet, orderByList, findOptions);
                }
                listSize = listIt.getResultsSizeAfterPartialList();
            }
        } catch (GenericEntityException e) {
            return ServiceUtil.returnError(UtilProperties.getMessage(RESOURCE, x.CommonFindErrorRunning,
                    UtilMisc.toMap(x.entityName, (dynamicViewEntity != null ? dynamicViewEntity.getEntityName() : entityName),
                                   x.errorString, e.getMessage()), locale));
        }

        Map<String, Object> results = ServiceUtil.returnSuccess();
        results.put(x.listIt, listIt);
        results.put(x.listSize, listSize);
        return results;
    }

    private static String dayStart(String timeStampString, int daysLater, TimeZone timeZone, Locale locale) {
        String retValue = null;
        Timestamp ts = null;
        Timestamp startTs = null;
        try {
            ts = Timestamp.valueOf(timeStampString);
        } catch (IllegalArgumentException e) {
            timeStampString += x._00_00_00_000_3d6041cd;
            try {
                ts = Timestamp.valueOf(timeStampString);
            } catch (IllegalArgumentException e2) {
                return retValue;
            }
        }
        startTs = UtilDateTime.getDayStart(ts, daysLater, timeZone, locale);
        retValue = startTs.toString();
        return retValue;
    }

    public static Map<String, Object> buildReducedQueryString(Map<String, ?> inputFields, String entityName, Delegator delegator) {
        // Strip the "_suffix" off of the parameter name and
        // build a three-level map of values keyed by fieldRoot name,
        //    fld0 or fld1, and, then, "op" or "value"
        // ie. id
        //  - fld0
        //      - op:like
        //      - value:abc
        //  - fld1 (if there is a range)
        //      - op:lessThan
        //      - value:55 (note: these two "flds" wouldn't really go together)
        // Also note that op/fld can be in any order. (eg. id_fld1_equals or id_equals_fld1)
        // Note that "normalizedFields" will contain values other than those
        // Contained in the associated entity.
        // Those extra fields will be ignored in the second half of this method.
        ModelEntity modelEntity = delegator.getModelEntity(entityName);
        Map<String, Object> normalizedFields = new LinkedHashMap<>();
        //StringBuffer queryStringBuf = new StringBuffer();
        for (Entry<String, ?> entry : inputFields.entrySet()) { // The name as it appears in the HTML form
            String fieldNameRaw = entry.getKey();
            String fieldNameRoot = null; // The entity field name. Everything to the left of the first "_" if
                                                                 //  it exists, or the whole word, if not.
            Object fieldValue = null; // If it is a "value" field, it will be the value to be used in the query.
                                                        // If it is an "op" field, it will be "equals", "greaterThan", etc.
            int iPos = -1;
            int iPos2 = -1;

            fieldValue = entry.getValue();
            if (ObjectType.isEmpty(fieldValue)) {
                continue;
            }

            iPos = fieldNameRaw.indexOf('_'); // Look for suffix

            // This is a hack to skip fields from "multi" forms
            // These would have the form "fieldName_o_1"
            if (iPos >= 0) {
                String suffix = fieldNameRaw.substring(iPos + 1);
                iPos2 = suffix.indexOf('_');
                if (iPos2 == 1) {
                    continue;
                }
            }

            // If no suffix, assume no range (default to fld0) and operations of equals
            // If no field op is present, it will assume "equals".
            if (iPos < 0) {
                fieldNameRoot = fieldNameRaw;
            } else { // Must have at least "fld0/1" or "equals, greaterThan, etc."
                // Some bogus fields will slip in, like "ENTITY_NAME", but they will be ignored

                fieldNameRoot = fieldNameRaw.substring(0, iPos);
            }
            if (modelEntity.isField(fieldNameRoot)) {
                normalizedFields.put(fieldNameRaw, fieldValue);
            }
        }
        return normalizedFields;
    }
    /**
     * Returns the first generic item of the service 'performFind'
     * Same parameters as performFind service but returns a single GenericValue
     * @param dctx
     * @param context
     * @return returns the first item
     */
    public static Map<String, Object> performFindItem(DispatchContext dctx, FindServicesContext context) {
        context.put(x.viewSize, 1);
        context.put(x.viewIndex, 0);
        Map<String, Object> result = org.apache.ofbiz.common.FindServices.performFind(dctx, context);

        List<GenericValue> list = null;
        GenericValue item = null;
        try (EntityListIterator it = (EntityListIterator) result.get(x.listIt)) {
            list = it.getPartialList(1, 1); // list starts at '1'
            if (UtilValidate.isNotEmpty(list)) {
                item = list.get(0);
            }
        } catch (ClassCastException | NullPointerException | GenericEntityException e) {
            Debug.logInfo(x.Problem_getting_list_Item + e, MODULE);
        }

        if (UtilValidate.isNotEmpty(item)) {
            result.put(x.item_3a7d9767, item);
        }
        result.remove(x.listIt);

        if (result.containsKey(x.listSize)) {
            result.remove(x.listSize);
        }
        return result;
    }
}
