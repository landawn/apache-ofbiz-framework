package org.apache.ofbiz.persistence.dao;

import com.landawn.abacus.jdbc.dao.CrudDao;
import com.landawn.abacus.query.SQLBuilder;
import org.apache.ofbiz.persistence.entity.FixedAssetDepMethodEntity;

public interface FixedAssetDepMethodDao extends CrudDao<FixedAssetDepMethodEntity, FixedAssetDepMethodEntity, SQLBuilder.PSC, FixedAssetDepMethodDao> {
}
