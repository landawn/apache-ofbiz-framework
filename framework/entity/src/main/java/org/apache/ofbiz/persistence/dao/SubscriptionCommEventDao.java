package org.apache.ofbiz.persistence.dao;

import com.landawn.abacus.jdbc.dao.CrudDao;
import com.landawn.abacus.query.SQLBuilder;
import org.apache.ofbiz.persistence.entity.SubscriptionCommEventEntity;

public interface SubscriptionCommEventDao extends CrudDao<SubscriptionCommEventEntity, SubscriptionCommEventEntity, SQLBuilder.PSC, SubscriptionCommEventDao> {
}
