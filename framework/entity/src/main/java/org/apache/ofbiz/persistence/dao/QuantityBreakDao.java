package org.apache.ofbiz.persistence.dao;

import com.landawn.abacus.jdbc.dao.CrudDao;
import com.landawn.abacus.query.SqlBuilder;
import org.apache.ofbiz.persistence.entity.QuantityBreakEntity;

public interface QuantityBreakDao extends CrudDao<QuantityBreakEntity, String, SqlBuilder.PSC, QuantityBreakDao> {
}
