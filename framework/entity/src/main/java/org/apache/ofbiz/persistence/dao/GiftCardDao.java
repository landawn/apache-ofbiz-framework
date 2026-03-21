package org.apache.ofbiz.persistence.dao;

import com.landawn.abacus.jdbc.dao.CrudDao;
import com.landawn.abacus.query.SqlBuilder;
import org.apache.ofbiz.persistence.entity.GiftCardEntity;

public interface GiftCardDao extends CrudDao<GiftCardEntity, String, SqlBuilder.PSC, GiftCardDao> {
}
