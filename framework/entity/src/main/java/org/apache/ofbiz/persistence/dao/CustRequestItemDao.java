package org.apache.ofbiz.persistence.dao;

import com.landawn.abacus.jdbc.dao.CrudDao;
import com.landawn.abacus.query.SqlBuilder;
import org.apache.ofbiz.persistence.entity.CustRequestItemEntity;

public interface CustRequestItemDao extends CrudDao<CustRequestItemEntity, CustRequestItemEntity, SqlBuilder.PSC, CustRequestItemDao> {
}
