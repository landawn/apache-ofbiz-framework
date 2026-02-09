package org.apache.ofbiz.persistence.dao;

import com.landawn.abacus.jdbc.dao.CrudDao;
import com.landawn.abacus.query.SQLBuilder;
import org.apache.ofbiz.persistence.entity.FixedAssetGeoPointEntity;

public interface FixedAssetGeoPointDao extends CrudDao<FixedAssetGeoPointEntity, FixedAssetGeoPointEntity, SQLBuilder.PSC, FixedAssetGeoPointDao> {
}
