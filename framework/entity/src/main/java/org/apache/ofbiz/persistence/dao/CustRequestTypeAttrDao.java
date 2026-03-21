package org.apache.ofbiz.persistence.dao;

import com.landawn.abacus.jdbc.dao.CrudDao;
import com.landawn.abacus.query.SqlBuilder;
import org.apache.ofbiz.persistence.entity.CustRequestTypeAttrEntity;

public interface CustRequestTypeAttrDao extends CrudDao<CustRequestTypeAttrEntity, CustRequestTypeAttrEntity, SqlBuilder.PSC, CustRequestTypeAttrDao> {
}
