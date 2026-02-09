package org.apache.ofbiz.persistence.dao;

import com.landawn.abacus.jdbc.dao.CrudDao;
import com.landawn.abacus.query.SQLBuilder;
import org.apache.ofbiz.persistence.entity.FixedAssetAttributeEntity;

public interface FixedAssetAttributeDao extends CrudDao<FixedAssetAttributeEntity, FixedAssetAttributeEntity, SQLBuilder.PSC, FixedAssetAttributeDao> {
}
