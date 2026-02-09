package org.apache.ofbiz.persistence.dao;

import com.landawn.abacus.jdbc.dao.CrudDao;
import com.landawn.abacus.query.SQLBuilder;
import org.apache.ofbiz.persistence.entity.FixedAssetStdCostTypeEntity;

public interface FixedAssetStdCostTypeDao extends CrudDao<FixedAssetStdCostTypeEntity, String, SQLBuilder.PSC, FixedAssetStdCostTypeDao> {
}
