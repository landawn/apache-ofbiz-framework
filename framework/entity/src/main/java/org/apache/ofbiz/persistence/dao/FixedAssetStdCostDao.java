package org.apache.ofbiz.persistence.dao;

import com.landawn.abacus.jdbc.dao.CrudDao;
import com.landawn.abacus.query.SqlBuilder;
import org.apache.ofbiz.persistence.entity.FixedAssetStdCostEntity;

public interface FixedAssetStdCostDao extends CrudDao<FixedAssetStdCostEntity, FixedAssetStdCostEntity, SqlBuilder.PSC, FixedAssetStdCostDao> {
}
