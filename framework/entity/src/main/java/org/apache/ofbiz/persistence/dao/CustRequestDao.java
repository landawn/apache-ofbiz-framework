package org.apache.ofbiz.persistence.dao;

import com.landawn.abacus.jdbc.dao.CrudDao;
import com.landawn.abacus.query.SQLBuilder;
import org.apache.ofbiz.persistence.entity.CustRequestEntity;

public interface CustRequestDao extends CrudDao<CustRequestEntity, String, SQLBuilder.PSC, CustRequestDao> {
}
