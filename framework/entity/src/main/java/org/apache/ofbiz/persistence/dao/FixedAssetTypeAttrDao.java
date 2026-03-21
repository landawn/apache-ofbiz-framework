package org.apache.ofbiz.persistence.dao;

import com.landawn.abacus.jdbc.dao.CrudDao;
import com.landawn.abacus.query.SqlBuilder;
import org.apache.ofbiz.persistence.entity.FixedAssetTypeAttrEntity;

public interface FixedAssetTypeAttrDao extends CrudDao<FixedAssetTypeAttrEntity, FixedAssetTypeAttrEntity, SqlBuilder.PSC, FixedAssetTypeAttrDao> {
}
