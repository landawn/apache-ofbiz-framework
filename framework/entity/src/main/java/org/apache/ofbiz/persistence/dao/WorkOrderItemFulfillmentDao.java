package org.apache.ofbiz.persistence.dao;

import com.landawn.abacus.jdbc.dao.CrudDao;
import com.landawn.abacus.query.SqlBuilder;
import org.apache.ofbiz.persistence.entity.WorkOrderItemFulfillmentEntity;

public interface WorkOrderItemFulfillmentDao extends CrudDao<WorkOrderItemFulfillmentEntity, WorkOrderItemFulfillmentEntity, SqlBuilder.PSC, WorkOrderItemFulfillmentDao> {
}
