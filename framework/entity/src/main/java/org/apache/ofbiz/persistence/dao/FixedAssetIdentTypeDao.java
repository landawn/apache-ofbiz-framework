package org.apache.ofbiz.persistence.dao;

import com.landawn.abacus.jdbc.dao.CrudDao;
import com.landawn.abacus.query.SQLBuilder;
import org.apache.ofbiz.persistence.entity.FixedAssetIdentTypeEntity;

public interface FixedAssetIdentTypeDao extends CrudDao<FixedAssetIdentTypeEntity, String, SQLBuilder.PSC, FixedAssetIdentTypeDao> {
}
