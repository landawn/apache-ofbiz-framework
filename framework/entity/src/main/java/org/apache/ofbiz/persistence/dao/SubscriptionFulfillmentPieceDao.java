package org.apache.ofbiz.persistence.dao;

import com.landawn.abacus.jdbc.dao.CrudDao;
import com.landawn.abacus.query.SqlBuilder;
import org.apache.ofbiz.persistence.entity.SubscriptionFulfillmentPieceEntity;

public interface SubscriptionFulfillmentPieceDao extends CrudDao<SubscriptionFulfillmentPieceEntity, SubscriptionFulfillmentPieceEntity, SqlBuilder.PSC, SubscriptionFulfillmentPieceDao> {
}
