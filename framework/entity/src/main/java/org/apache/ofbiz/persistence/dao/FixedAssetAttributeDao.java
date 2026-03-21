package org.apache.ofbiz.persistence.dao;

import com.landawn.abacus.jdbc.dao.CrudDao;
import com.landawn.abacus.query.SqlBuilder;
import org.apache.ofbiz.persistence.entity.FixedAssetAttributeEntity;

public interface FixedAssetAttributeDao extends CrudDao<FixedAssetAttributeEntity, FixedAssetAttributeEntity, SqlBuilder.PSC, FixedAssetAttributeDao> {
}
