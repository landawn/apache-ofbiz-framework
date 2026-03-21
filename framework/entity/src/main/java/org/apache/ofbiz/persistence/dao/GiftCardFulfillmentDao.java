package org.apache.ofbiz.persistence.dao;

import com.landawn.abacus.jdbc.dao.CrudDao;
import com.landawn.abacus.query.SqlBuilder;
import org.apache.ofbiz.persistence.entity.GiftCardFulfillmentEntity;

public interface GiftCardFulfillmentDao extends CrudDao<GiftCardFulfillmentEntity, String, SqlBuilder.PSC, GiftCardFulfillmentDao> {
}
