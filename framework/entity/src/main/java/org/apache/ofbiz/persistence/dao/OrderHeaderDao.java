package org.apache.ofbiz.persistence.dao;

import com.landawn.abacus.jdbc.dao.CrudDao;
import com.landawn.abacus.query.SQLBuilder;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import org.apache.ofbiz.base.util.UtilDateTime;
import org.apache.ofbiz.base.util.collections.PagedList;
import org.apache.ofbiz.entity.Delegator;
import org.apache.ofbiz.entity.GenericEntityException;
import org.apache.ofbiz.entity.GenericValue;
import org.apache.ofbiz.entity.condition.EntityCondition;
import org.apache.ofbiz.entity.model.DynamicViewEntity;
import org.apache.ofbiz.entity.util.EntityFindOptions;
import org.apache.ofbiz.entity.util.EntityListIterator;
import org.apache.ofbiz.entity.util.EntityUtil;
import org.apache.ofbiz.persistence.entity.OrderHeaderEntity;

public interface OrderHeaderDao extends CrudDao<OrderHeaderEntity, String, SQLBuilder.PSC, OrderHeaderDao>, DelegatorQueryDao {
    default PagedList<GenericValue> queryPagedList(Delegator delegator, DynamicViewEntity dynamicViewEntity,
            EntityCondition whereCondition, Collection<String> fieldsToSelect, List<String> orderBy, int viewIndex, int viewSize)
            throws GenericEntityException {
        EntityFindOptions findOptions = new EntityFindOptions(true, EntityFindOptions.TYPE_SCROLL_INSENSITIVE,
                EntityFindOptions.CONCUR_READ_ONLY, true);

        try (EntityListIterator iterator = delegator.findListIteratorByCondition(dynamicViewEntity, whereCondition, null, fieldsToSelect,
                orderBy, findOptions)) {
            return EntityUtil.getPagedList(iterator, viewIndex, viewSize);
        }
    }

    default List<GenericValue> listOrderAndPartyContactMech(Delegator delegator, String orderId) throws GenericEntityException {
        List<GenericValue> values = delegator.findByAnd("OrderAndPartyContactMech", java.util.Collections.singletonMap("orderId", orderId), null,
                false);
        values = EntityUtil.filterByDate(values, UtilDateTime.nowTimestamp(), "contactFromDate", "contactThruDate", true);

        List<GenericValue> distinctValues = new LinkedList<>();
        Set<String> seen = new HashSet<>();
        for (GenericValue value : values) {
            String key = value.getString("orderId") + "|" + value.getString("contactMechId") + "|" + value.getString("contactMechPurposeTypeId");
            if (seen.add(key)) {
                distinctValues.add(value);
            }
        }
        return distinctValues;
    }
}
