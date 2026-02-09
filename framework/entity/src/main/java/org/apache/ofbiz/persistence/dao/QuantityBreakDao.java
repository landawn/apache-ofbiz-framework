package org.apache.ofbiz.persistence.dao;

import com.landawn.abacus.jdbc.dao.CrudDao;
import com.landawn.abacus.query.SQLBuilder;
import org.apache.ofbiz.persistence.entity.QuantityBreakEntity;

public interface QuantityBreakDao extends CrudDao<QuantityBreakEntity, String, SQLBuilder.PSC, QuantityBreakDao> {
}
