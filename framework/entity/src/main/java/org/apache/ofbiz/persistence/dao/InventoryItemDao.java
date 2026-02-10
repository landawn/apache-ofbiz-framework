package org.apache.ofbiz.persistence.dao;

import com.landawn.abacus.jdbc.dao.CrudDao;
import com.landawn.abacus.query.SQLBuilder;
import java.util.List;
import org.apache.ofbiz.entity.Delegator;
import org.apache.ofbiz.entity.GenericEntityException;
import org.apache.ofbiz.entity.GenericValue;
import org.apache.ofbiz.entity.condition.EntityCondition;
import org.apache.ofbiz.entity.model.DynamicViewEntity;
import org.apache.ofbiz.entity.util.EntityListIterator;
import org.apache.ofbiz.persistence.entity.InventoryItemEntity;

public interface InventoryItemDao extends CrudDao<InventoryItemEntity, String, SQLBuilder.PSC, InventoryItemDao> , DelegatorQueryDao{
    default List<GenericValue> listShipmentAndItem(Delegator delegator, EntityCondition condition, List<String> orderBy)
            throws GenericEntityException {
        return delegator.findList("ShipmentAndItem", condition, null, orderBy, null, false);
    }

    default EntityListIterator queryIterator(Delegator delegator, DynamicViewEntity dynamicViewEntity, EntityCondition condition)
            throws GenericEntityException {
        return delegator.findListIteratorByCondition(dynamicViewEntity, condition, null, null, null, null);
    }
}
