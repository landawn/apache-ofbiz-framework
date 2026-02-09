package org.apache.ofbiz.persistence.dao;

import com.landawn.abacus.jdbc.dao.CrudDao;
import com.landawn.abacus.query.SQLBuilder;
import org.apache.ofbiz.persistence.entity.DataCategoryEntity;

public interface DataCategoryDao extends CrudDao<DataCategoryEntity, String, SQLBuilder.PSC, DataCategoryDao> {
}
