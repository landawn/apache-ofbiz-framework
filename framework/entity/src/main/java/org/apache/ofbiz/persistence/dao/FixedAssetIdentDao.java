package org.apache.ofbiz.persistence.dao;

import com.landawn.abacus.jdbc.dao.CrudDao;
import com.landawn.abacus.query.SQLBuilder;
import org.apache.ofbiz.persistence.entity.FixedAssetIdentEntity;

public interface FixedAssetIdentDao extends CrudDao<FixedAssetIdentEntity, FixedAssetIdentEntity, SQLBuilder.PSC, FixedAssetIdentDao> {
}
