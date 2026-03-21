package org.apache.ofbiz.persistence.dao;

import com.landawn.abacus.jdbc.dao.CrudDao;
import com.landawn.abacus.query.SqlBuilder;
import org.apache.ofbiz.persistence.entity.FixedAssetIdentEntity;

public interface FixedAssetIdentDao extends CrudDao<FixedAssetIdentEntity, FixedAssetIdentEntity, SqlBuilder.PSC, FixedAssetIdentDao> {
}
