package org.apache.ofbiz.persistence.dao;

import com.landawn.abacus.jdbc.dao.CrudDao;
import com.landawn.abacus.query.SQLBuilder;
import org.apache.ofbiz.persistence.entity.CustRequestStatusEntity;

public interface CustRequestStatusDao extends CrudDao<CustRequestStatusEntity, String, SQLBuilder.PSC, CustRequestStatusDao> {
}
