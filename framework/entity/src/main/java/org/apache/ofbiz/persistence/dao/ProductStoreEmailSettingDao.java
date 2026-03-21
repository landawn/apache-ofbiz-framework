package org.apache.ofbiz.persistence.dao;

import com.landawn.abacus.jdbc.dao.CrudDao;
import com.landawn.abacus.query.SqlBuilder;
import org.apache.ofbiz.persistence.entity.ProductStoreEmailSettingEntity;

public interface ProductStoreEmailSettingDao extends CrudDao<ProductStoreEmailSettingEntity, ProductStoreEmailSettingEntity, SqlBuilder.PSC, ProductStoreEmailSettingDao>, DelegatorQueryDao {
}
