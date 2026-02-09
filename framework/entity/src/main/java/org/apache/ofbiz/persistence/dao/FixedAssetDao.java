package org.apache.ofbiz.persistence.dao;

import com.landawn.abacus.jdbc.dao.CrudDao;
import com.landawn.abacus.query.SQLBuilder;
import org.apache.ofbiz.persistence.entity.FixedAssetEntity;

public interface FixedAssetDao extends CrudDao<FixedAssetEntity, String, SQLBuilder.PSC, FixedAssetDao> {
}
