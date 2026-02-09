package org.apache.ofbiz.persistence.dao;

import com.landawn.abacus.jdbc.dao.CrudDao;
import com.landawn.abacus.query.SQLBuilder;
import org.apache.ofbiz.persistence.entity.OrderHeaderWorkEffortEntity;

public interface OrderHeaderWorkEffortDao extends CrudDao<OrderHeaderWorkEffortEntity, OrderHeaderWorkEffortEntity, SQLBuilder.PSC, OrderHeaderWorkEffortDao> {
}
