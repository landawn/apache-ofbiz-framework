package org.apache.ofbiz.persistence.dao;

import com.landawn.abacus.jdbc.dao.CrudDao;
import com.landawn.abacus.query.SqlBuilder;
import org.apache.ofbiz.persistence.entity.TelecomNumberEntity;

public interface TelecomNumberDao extends CrudDao<TelecomNumberEntity, String, SqlBuilder.PSC, TelecomNumberDao> {
}
