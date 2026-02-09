package org.apache.ofbiz.persistence.dao;

import com.landawn.abacus.jdbc.dao.CrudDao;
import com.landawn.abacus.query.SQLBuilder;
import org.apache.ofbiz.persistence.entity.SubscriptionEntity;

public interface SubscriptionDao extends CrudDao<SubscriptionEntity, String, SQLBuilder.PSC, SubscriptionDao> {
}
