package org.apache.ofbiz.persistence.dao;

import com.landawn.abacus.jdbc.dao.CrudDao;
import com.landawn.abacus.query.SQLBuilder;
import org.apache.ofbiz.persistence.entity.OrderNotificationEntity;

public interface OrderNotificationDao extends CrudDao<OrderNotificationEntity, String, SQLBuilder.PSC, OrderNotificationDao> {
}
