package org.apache.ofbiz.persistence.dao;

import com.landawn.abacus.jdbc.dao.CrudDao;
import com.landawn.abacus.query.SqlBuilder;
import org.apache.ofbiz.persistence.entity.FixedAssetMaintOrderEntity;

public interface FixedAssetMaintOrderDao extends CrudDao<FixedAssetMaintOrderEntity, FixedAssetMaintOrderEntity, SqlBuilder.PSC, FixedAssetMaintOrderDao> {
}
