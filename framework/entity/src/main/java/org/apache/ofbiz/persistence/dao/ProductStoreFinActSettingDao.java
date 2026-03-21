package org.apache.ofbiz.persistence.dao;

import com.landawn.abacus.jdbc.dao.CrudDao;
import com.landawn.abacus.query.SqlBuilder;
import org.apache.ofbiz.persistence.entity.ProductStoreFinActSettingEntity;

public interface ProductStoreFinActSettingDao extends CrudDao<ProductStoreFinActSettingEntity, ProductStoreFinActSettingEntity, SqlBuilder.PSC, ProductStoreFinActSettingDao> {
}
