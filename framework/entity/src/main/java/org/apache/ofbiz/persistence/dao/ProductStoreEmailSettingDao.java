package org.apache.ofbiz.persistence.dao;

import com.landawn.abacus.jdbc.dao.CrudDao;
import com.landawn.abacus.query.SQLBuilder;
import org.apache.ofbiz.persistence.entity.ProductStoreEmailSettingEntity;

public interface ProductStoreEmailSettingDao extends CrudDao<ProductStoreEmailSettingEntity, ProductStoreEmailSettingEntity, SQLBuilder.PSC, ProductStoreEmailSettingDao>, DelegatorQueryDao {
}
