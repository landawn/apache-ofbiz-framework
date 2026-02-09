package org.apache.ofbiz.persistence.dao;

import com.landawn.abacus.jdbc.dao.CrudDao;
import com.landawn.abacus.query.SQLBuilder;
import org.apache.ofbiz.persistence.entity.FixedAssetMaintEntity;

public interface FixedAssetMaintDao extends CrudDao<FixedAssetMaintEntity, FixedAssetMaintEntity, SQLBuilder.PSC, FixedAssetMaintDao> {
}
