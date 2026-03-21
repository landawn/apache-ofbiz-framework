package org.apache.ofbiz.persistence.dao;

import com.landawn.abacus.jdbc.dao.CrudDao;
import com.landawn.abacus.query.SqlBuilder;
import org.apache.ofbiz.persistence.entity.FixedAssetEntity;

public interface FixedAssetDao extends CrudDao<FixedAssetEntity, String, SqlBuilder.PSC, FixedAssetDao>, DelegatorQueryDao {
}
