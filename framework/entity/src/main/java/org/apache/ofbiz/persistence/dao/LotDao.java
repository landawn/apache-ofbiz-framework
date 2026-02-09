package org.apache.ofbiz.persistence.dao;

import com.landawn.abacus.jdbc.dao.CrudDao;
import com.landawn.abacus.query.SQLBuilder;
import org.apache.ofbiz.persistence.entity.LotEntity;

public interface LotDao extends CrudDao<LotEntity, String, SQLBuilder.PSC, LotDao> {
}
