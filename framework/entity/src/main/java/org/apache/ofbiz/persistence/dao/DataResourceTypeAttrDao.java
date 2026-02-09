package org.apache.ofbiz.persistence.dao;

import com.landawn.abacus.jdbc.dao.CrudDao;
import com.landawn.abacus.query.SQLBuilder;
import org.apache.ofbiz.persistence.entity.DataResourceTypeAttrEntity;

public interface DataResourceTypeAttrDao extends CrudDao<DataResourceTypeAttrEntity, DataResourceTypeAttrEntity, SQLBuilder.PSC, DataResourceTypeAttrDao> {
}
