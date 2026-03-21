package org.apache.ofbiz.persistence.dao;

import com.landawn.abacus.jdbc.dao.CrudDao;
import com.landawn.abacus.query.SqlBuilder;
import java.util.List;
import org.apache.ofbiz.entity.Delegator;
import org.apache.ofbiz.entity.GenericEntityException;
import org.apache.ofbiz.entity.GenericValue;
import org.apache.ofbiz.entity.condition.EntityCondition;
import org.apache.ofbiz.entity.util.EntityUtil;
import org.apache.ofbiz.persistence.entity.TaxAuthorityRateProductEntity;

public interface TaxAuthorityRateProductDao extends CrudDao<TaxAuthorityRateProductEntity, String, SqlBuilder.PSC, TaxAuthorityRateProductDao> {
    default List<GenericValue> listByCondition(Delegator delegator, EntityCondition condition, List<String> orderBy, boolean filterByDate)
            throws GenericEntityException {
        List<GenericValue> values = delegator.findList("TaxAuthorityRateProduct", condition, null, orderBy, null, false);
        return filterByDate ? EntityUtil.filterByDate(values) : values;
    }
}
