package org.apache.ofbiz.persistence.dao;

import com.landawn.abacus.jdbc.dao.CrudDao;
import com.landawn.abacus.query.SQLBuilder;
import org.apache.ofbiz.persistence.entity.SubscriptionTypeEntity;

public interface SubscriptionTypeDao extends CrudDao<SubscriptionTypeEntity, String, SQLBuilder.PSC, SubscriptionTypeDao> {
}
