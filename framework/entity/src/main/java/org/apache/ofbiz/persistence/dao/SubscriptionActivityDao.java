package org.apache.ofbiz.persistence.dao;

import com.landawn.abacus.jdbc.dao.CrudDao;
import com.landawn.abacus.query.SQLBuilder;
import org.apache.ofbiz.persistence.entity.SubscriptionActivityEntity;

public interface SubscriptionActivityDao extends CrudDao<SubscriptionActivityEntity, String, SQLBuilder.PSC, SubscriptionActivityDao> {
}
