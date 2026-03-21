package org.apache.ofbiz.persistence.dao;

import com.landawn.abacus.jdbc.dao.CrudDao;
import com.landawn.abacus.query.SqlBuilder;
import org.apache.ofbiz.persistence.entity.DataCategoryEntity;

public interface DataCategoryDao extends CrudDao<DataCategoryEntity, String, SqlBuilder.PSC, DataCategoryDao> {
}
