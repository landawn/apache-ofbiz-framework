package org.apache.ofbiz.persistence.dao;

import com.landawn.abacus.jdbc.dao.CrudDao;
import com.landawn.abacus.query.SQLBuilder;
import org.apache.ofbiz.persistence.entity.CustRequestCategoryEntity;

public interface CustRequestCategoryDao extends CrudDao<CustRequestCategoryEntity, String, SQLBuilder.PSC, CustRequestCategoryDao> {
}
