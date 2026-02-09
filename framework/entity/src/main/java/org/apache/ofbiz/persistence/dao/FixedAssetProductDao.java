package org.apache.ofbiz.persistence.dao;

import com.landawn.abacus.jdbc.dao.CrudDao;
import com.landawn.abacus.query.SQLBuilder;
import org.apache.ofbiz.persistence.entity.FixedAssetProductEntity;

public interface FixedAssetProductDao extends CrudDao<FixedAssetProductEntity, FixedAssetProductEntity, SQLBuilder.PSC, FixedAssetProductDao> {
}
