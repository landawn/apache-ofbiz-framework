package org.apache.ofbiz.persistence.dao;

import com.landawn.abacus.jdbc.dao.CrudDao;
import com.landawn.abacus.query.SqlBuilder;
import org.apache.ofbiz.persistence.entity.SubscriptionActivityEntity;

public interface SubscriptionActivityDao extends CrudDao<SubscriptionActivityEntity, String, SqlBuilder.PSC, SubscriptionActivityDao> {
}
