package org.apache.ofbiz.persistence.dao;

import com.landawn.abacus.jdbc.dao.CrudDao;
import com.landawn.abacus.query.SQLBuilder;
import org.apache.ofbiz.persistence.entity.UserLoginEntity;

public interface UserLoginDao extends CrudDao<UserLoginEntity, String, SQLBuilder.PSC, UserLoginDao>, DelegatorQueryDao {
    private static java.util.Set<String> asSet(java.util.Collection<String> fieldsToSelect) {
        if (fieldsToSelect == null) {
            return null;
        }
        if (fieldsToSelect instanceof java.util.Set) {
            return (java.util.Set<String>) fieldsToSelect;
        }
        return new java.util.LinkedHashSet<>(fieldsToSelect);
    }

    default org.apache.ofbiz.entity.GenericValue findOne(org.apache.ofbiz.entity.Delegator delegator, String entityName,
            java.util.Map<String, ? extends Object> fields, boolean useCache) throws org.apache.ofbiz.entity.GenericEntityException {
        return delegator.findOne(entityName, fields, useCache);
    }

    default java.util.List<org.apache.ofbiz.entity.GenericValue> findByAnd(org.apache.ofbiz.entity.Delegator delegator, String entityName,
            java.util.Map<String, ? extends Object> fields, java.util.List<String> orderBy, boolean useCache)
            throws org.apache.ofbiz.entity.GenericEntityException {
        return delegator.findByAnd(entityName, fields, orderBy, useCache);
    }

    default java.util.List<org.apache.ofbiz.entity.GenericValue> findByCondition(org.apache.ofbiz.entity.Delegator delegator, String entityName,
            org.apache.ofbiz.entity.condition.EntityCondition whereCondition, java.util.Collection<String> fieldsToSelect,
            java.util.List<String> orderBy, org.apache.ofbiz.entity.util.EntityFindOptions findOptions, boolean useCache)
            throws org.apache.ofbiz.entity.GenericEntityException {
        return delegator.findList(entityName, whereCondition, asSet(fieldsToSelect), orderBy, findOptions, useCache);
    }

    default org.apache.ofbiz.entity.GenericValue findFirstByCondition(org.apache.ofbiz.entity.Delegator delegator, String entityName,
            org.apache.ofbiz.entity.condition.EntityCondition whereCondition, java.util.Collection<String> fieldsToSelect,
            java.util.List<String> orderBy, boolean useCache) throws org.apache.ofbiz.entity.GenericEntityException {
        org.apache.ofbiz.entity.util.EntityFindOptions findOptions = new org.apache.ofbiz.entity.util.EntityFindOptions();
        findOptions.setMaxRows(1);
        java.util.List<org.apache.ofbiz.entity.GenericValue> values = delegator.findList(entityName, whereCondition, asSet(fieldsToSelect), orderBy,
                findOptions, useCache);
        return org.apache.ofbiz.entity.util.EntityUtil.getFirst(values);
    }

    default org.apache.ofbiz.entity.util.EntityListIterator findIteratorByCondition(org.apache.ofbiz.entity.Delegator delegator, String entityName,
            org.apache.ofbiz.entity.condition.EntityCondition whereCondition, java.util.Collection<String> fieldsToSelect,
            java.util.List<String> orderBy, org.apache.ofbiz.entity.util.EntityFindOptions findOptions)
            throws org.apache.ofbiz.entity.GenericEntityException {
        return delegator.find(entityName, whereCondition, null, asSet(fieldsToSelect), orderBy, findOptions);
    }
}
