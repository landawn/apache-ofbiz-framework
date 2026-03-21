package org.apache.ofbiz.persistence.dao;

import com.landawn.abacus.jdbc.dao.CrudDao;
import com.landawn.abacus.query.SqlBuilder;
import org.apache.ofbiz.persistence.entity.OrderDeliveryScheduleEntity;

public interface OrderDeliveryScheduleDao extends CrudDao<OrderDeliveryScheduleEntity, OrderDeliveryScheduleEntity, SqlBuilder.PSC, OrderDeliveryScheduleDao> , DelegatorQueryDao{
}
