package org.apache.ofbiz.persistence.dao;

import com.landawn.abacus.jdbc.dao.CrudDao;
import com.landawn.abacus.query.SqlBuilder;
import org.apache.ofbiz.persistence.entity.FixedAssetIdentTypeEntity;

public interface FixedAssetIdentTypeDao extends CrudDao<FixedAssetIdentTypeEntity, String, SqlBuilder.PSC, FixedAssetIdentTypeDao> {
}
