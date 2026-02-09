package org.apache.ofbiz.persistence.dao;

import com.landawn.abacus.jdbc.dao.CrudDao;
import com.landawn.abacus.query.SQLBuilder;
import org.apache.ofbiz.persistence.entity.CustRequestTypeEntity;

public interface CustRequestTypeDao extends CrudDao<CustRequestTypeEntity, String, SQLBuilder.PSC, CustRequestTypeDao> {
}
