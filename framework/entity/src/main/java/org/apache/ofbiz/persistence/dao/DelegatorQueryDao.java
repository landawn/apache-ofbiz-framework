package org.apache.ofbiz.persistence.dao;

import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.ofbiz.entity.Delegator;
import org.apache.ofbiz.entity.GenericEntityException;
import org.apache.ofbiz.entity.GenericValue;
import org.apache.ofbiz.entity.condition.EntityCondition;
import org.apache.ofbiz.entity.model.DynamicViewEntity;
import org.apache.ofbiz.entity.util.EntityFindOptions;
import org.apache.ofbiz.entity.util.EntityListIterator;
import org.apache.ofbiz.entity.util.EntityUtil;

public interface DelegatorQueryDao {

    private static Set<String> asSet(Collection<String> fieldsToSelect) {
        if (fieldsToSelect == null) {
            return null;
        }
        if (fieldsToSelect instanceof Set) {
            return (Set<String>) fieldsToSelect;
        }
        return new LinkedHashSet<>(fieldsToSelect);
    }

    default GenericValue findOne(Delegator delegator, String entityName, Map<String, ? extends Object> fields, boolean useCache)
            throws GenericEntityException {
        return delegator.findOne(entityName, fields, useCache);
    }

    default List<GenericValue> findByAnd(Delegator delegator, String entityName, Map<String, ? extends Object> fields, List<String> orderBy,
            boolean useCache) throws GenericEntityException {
        return delegator.findByAnd(entityName, fields, orderBy, useCache);
    }

    default List<GenericValue> findByCondition(Delegator delegator, String entityName, EntityCondition whereCondition,
            Collection<String> fieldsToSelect, List<String> orderBy, EntityFindOptions findOptions, boolean useCache)
            throws GenericEntityException {
        return delegator.findList(entityName, whereCondition, asSet(fieldsToSelect), orderBy, findOptions, useCache);
    }

    default GenericValue findFirstByCondition(Delegator delegator, String entityName, EntityCondition whereCondition,
            Collection<String> fieldsToSelect, List<String> orderBy, boolean useCache) throws GenericEntityException {
        EntityFindOptions findOptions = new EntityFindOptions();
        findOptions.setMaxRows(1);
        List<GenericValue> values = delegator.findList(entityName, whereCondition, asSet(fieldsToSelect), orderBy, findOptions, useCache);
        return EntityUtil.getFirst(values);
    }

    default GenericValue findOneByCondition(Delegator delegator, String entityName, EntityCondition whereCondition,
            Collection<String> fieldsToSelect, List<String> orderBy, boolean useCache) throws GenericEntityException {
        EntityFindOptions findOptions = new EntityFindOptions();
        findOptions.setMaxRows(2);
        List<GenericValue> values = delegator.findList(entityName, whereCondition, asSet(fieldsToSelect), orderBy, findOptions, useCache);
        return EntityUtil.getOnly(values);
    }

    default EntityListIterator findIteratorByCondition(Delegator delegator, String entityName, EntityCondition whereCondition,
            Collection<String> fieldsToSelect, List<String> orderBy, EntityFindOptions findOptions)
            throws GenericEntityException {
        return delegator.find(entityName, whereCondition, null, asSet(fieldsToSelect), orderBy, findOptions);
    }

    default EntityListIterator findIteratorByCondition(Delegator delegator, DynamicViewEntity dynamicViewEntity, EntityCondition whereCondition,
            EntityCondition havingCondition, Collection<String> fieldsToSelect, List<String> orderBy, EntityFindOptions findOptions)
            throws GenericEntityException {
        return delegator.findListIteratorByCondition(dynamicViewEntity, whereCondition, havingCondition, asSet(fieldsToSelect), orderBy, findOptions);
    }

    default long countByCondition(Delegator delegator, String entityName, EntityCondition whereCondition, Collection<String> fieldsToSelect,
            EntityFindOptions findOptions) throws GenericEntityException {
        return delegator.findCountByCondition(entityName, whereCondition, asSet(fieldsToSelect), null, findOptions);
    }

    @SuppressWarnings("unchecked")
    private static EntityCondition asCondition(Object whereClause) {
        if (whereClause == null) {
            return null;
        }
        if (whereClause instanceof EntityCondition) {
            return (EntityCondition) whereClause;
        }
        if (whereClause instanceof Map) {
            return EntityCondition.makeCondition((Map<String, ? extends Object>) whereClause);
        }
        if (whereClause instanceof Collection<?>) {
            List<EntityCondition> conditions = new java.util.ArrayList<>();
            for (Object entry : (Collection<?>) whereClause) {
                EntityCondition condition = asCondition(entry);
                if (condition != null) {
                    conditions.add(condition);
                }
            }
            if (conditions.isEmpty()) {
                return null;
            }
            if (conditions.size() == 1) {
                return conditions.get(0);
            }
            return EntityCondition.makeCondition(conditions);
        }
        if (whereClause instanceof Object[]) {
            List<EntityCondition> conditions = new java.util.ArrayList<>();
            for (Object entry : (Object[]) whereClause) {
                EntityCondition condition = asCondition(entry);
                if (condition != null) {
                    conditions.add(condition);
                }
            }
            if (conditions.isEmpty()) {
                return null;
            }
            if (conditions.size() == 1) {
                return conditions.get(0);
            }
            return EntityCondition.makeCondition(conditions);
        }
        throw new IllegalArgumentException("Unsupported whereClause type: " + whereClause.getClass().getName());
    }

    private static EntityCondition withFilterByDate(EntityCondition whereCondition, boolean filterByDate) {
        if (!filterByDate) {
            return whereCondition;
        }
        EntityCondition filterByDateExpr = EntityUtil.getFilterByDateExpr();
        if (whereCondition == null) {
            return filterByDateExpr;
        }
        return EntityCondition.makeCondition(whereCondition, filterByDateExpr);
    }

    default GenericValue findOneByWhere(Delegator delegator, String entityName, Object whereClause, Collection<String> fieldsToSelect,
            List<String> orderBy, boolean useCache) throws GenericEntityException {
        return findOneByCondition(delegator, entityName, asCondition(whereClause), fieldsToSelect, orderBy, useCache);
    }

    default List<GenericValue> findListByWhere(Delegator delegator, String entityName, Object whereClause,
            Collection<String> fieldsToSelect, List<String> orderBy, boolean useCache) throws GenericEntityException {
        return findByCondition(delegator, entityName, asCondition(whereClause), fieldsToSelect, orderBy, null, useCache);
    }

    default List<GenericValue> findListByWhere(Delegator delegator, String entityName, Object whereClause,
            Collection<String> fieldsToSelect, List<String> orderBy, boolean useCache, boolean filterByDate)
            throws GenericEntityException {
        EntityCondition condition = withFilterByDate(asCondition(whereClause), filterByDate);
        return findByCondition(delegator, entityName, condition, fieldsToSelect, orderBy, null, useCache);
    }

    default GenericValue findFirstByWhere(Delegator delegator, String entityName, Object whereClause,
            Collection<String> fieldsToSelect, List<String> orderBy, boolean useCache) throws GenericEntityException {
        return findFirstByCondition(delegator, entityName, asCondition(whereClause), fieldsToSelect, orderBy, useCache);
    }

    default EntityListIterator findIteratorByWhere(Delegator delegator, String entityName, Object whereClause,
            Collection<String> fieldsToSelect, List<String> orderBy, EntityFindOptions findOptions) throws GenericEntityException {
        return findIteratorByCondition(delegator, entityName, asCondition(whereClause), fieldsToSelect, orderBy, findOptions);
    }

    default long countByWhere(Delegator delegator, String entityName, Object whereClause, Collection<String> fieldsToSelect,
            EntityFindOptions findOptions) throws GenericEntityException {
        return countByCondition(delegator, entityName, asCondition(whereClause), fieldsToSelect, findOptions);
    }
}
