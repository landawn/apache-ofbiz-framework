package org.apache.ofbiz.persistence.dao;

import com.landawn.abacus.jdbc.dao.CrudDao;
import com.landawn.abacus.query.SqlBuilder;
import org.apache.ofbiz.persistence.entity.SubscriptionEntity;

public interface SubscriptionDao extends CrudDao<SubscriptionEntity, String, SqlBuilder.PSC, SubscriptionDao>, DelegatorQueryDao {
}
