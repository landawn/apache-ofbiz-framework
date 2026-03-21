package org.apache.ofbiz.persistence.dao;

import com.landawn.abacus.jdbc.dao.CrudDao;
import com.landawn.abacus.query.SqlBuilder;
import org.apache.ofbiz.persistence.entity.CustRequestEntity;

public interface CustRequestDao extends CrudDao<CustRequestEntity, String, SqlBuilder.PSC, CustRequestDao> {
}
