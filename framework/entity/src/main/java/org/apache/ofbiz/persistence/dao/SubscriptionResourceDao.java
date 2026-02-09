package org.apache.ofbiz.persistence.dao;

import com.landawn.abacus.jdbc.dao.CrudDao;
import com.landawn.abacus.query.SQLBuilder;
import org.apache.ofbiz.persistence.entity.SubscriptionResourceEntity;

public interface SubscriptionResourceDao extends CrudDao<SubscriptionResourceEntity, String, SQLBuilder.PSC, SubscriptionResourceDao> {
}
