package org.apache.ofbiz.persistence.dao;

import com.landawn.abacus.jdbc.dao.CrudDao;
import com.landawn.abacus.query.SQLBuilder;
import org.apache.ofbiz.persistence.entity.DataResourcePurposeEntity;

public interface DataResourcePurposeDao extends CrudDao<DataResourcePurposeEntity, DataResourcePurposeEntity, SQLBuilder.PSC, DataResourcePurposeDao> {
}
