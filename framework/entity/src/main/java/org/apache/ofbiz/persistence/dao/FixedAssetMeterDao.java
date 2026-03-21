package org.apache.ofbiz.persistence.dao;

import com.landawn.abacus.jdbc.dao.CrudDao;
import com.landawn.abacus.query.SqlBuilder;
import org.apache.ofbiz.persistence.entity.FixedAssetMeterEntity;

public interface FixedAssetMeterDao extends CrudDao<FixedAssetMeterEntity, FixedAssetMeterEntity, SqlBuilder.PSC, FixedAssetMeterDao> {
}
