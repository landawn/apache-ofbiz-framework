package org.apache.ofbiz.persistence.dao;

import com.landawn.abacus.jdbc.dao.CrudDao;
import com.landawn.abacus.query.SqlBuilder;
import org.apache.ofbiz.persistence.entity.DataResourcePurposeEntity;

public interface DataResourcePurposeDao extends CrudDao<DataResourcePurposeEntity, DataResourcePurposeEntity, SqlBuilder.PSC, DataResourcePurposeDao> {
}
