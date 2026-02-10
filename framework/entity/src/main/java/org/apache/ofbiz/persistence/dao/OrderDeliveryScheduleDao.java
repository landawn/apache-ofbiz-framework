package org.apache.ofbiz.persistence.dao;

import com.landawn.abacus.jdbc.dao.CrudDao;
import com.landawn.abacus.query.SQLBuilder;
import org.apache.ofbiz.persistence.entity.OrderDeliveryScheduleEntity;

public interface OrderDeliveryScheduleDao extends CrudDao<OrderDeliveryScheduleEntity, OrderDeliveryScheduleEntity, SQLBuilder.PSC, OrderDeliveryScheduleDao> , DelegatorQueryDao{
}
