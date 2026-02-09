package org.apache.ofbiz.persistence.dao;

import com.landawn.abacus.jdbc.dao.CrudDao;
import com.landawn.abacus.query.SQLBuilder;
import org.apache.ofbiz.persistence.entity.SubscriptionAttributeEntity;

public interface SubscriptionAttributeDao extends CrudDao<SubscriptionAttributeEntity, SubscriptionAttributeEntity, SQLBuilder.PSC, SubscriptionAttributeDao> {
}
