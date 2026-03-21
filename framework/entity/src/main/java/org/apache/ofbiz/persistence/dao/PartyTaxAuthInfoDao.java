package org.apache.ofbiz.persistence.dao;

import com.landawn.abacus.jdbc.dao.CrudDao;
import com.landawn.abacus.query.SqlBuilder;
import java.util.List;
import org.apache.ofbiz.entity.Delegator;
import org.apache.ofbiz.entity.GenericEntityException;
import org.apache.ofbiz.entity.GenericValue;
import org.apache.ofbiz.entity.condition.EntityCondition;
import org.apache.ofbiz.entity.util.EntityUtil;
import org.apache.ofbiz.persistence.entity.PartyTaxAuthInfoEntity;

public interface PartyTaxAuthInfoDao extends CrudDao<PartyTaxAuthInfoEntity, PartyTaxAuthInfoEntity, SqlBuilder.PSC, PartyTaxAuthInfoDao> {
    default GenericValue queryFirstByCondition(Delegator delegator, EntityCondition condition, List<String> orderBy)
            throws GenericEntityException {
        List<GenericValue> values = delegator.findList("PartyTaxAuthInfo", condition, null, orderBy, null, false);
        return EntityUtil.getFirst(values);
    }
}
