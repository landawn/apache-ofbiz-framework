package org.apache.ofbiz.persistence.dao;

import com.landawn.abacus.jdbc.dao.CrudDao;
import com.landawn.abacus.query.SqlBuilder;
import org.apache.ofbiz.persistence.entity.ProductStoreTelecomSettingEntity;

public interface ProductStoreTelecomSettingDao extends CrudDao<ProductStoreTelecomSettingEntity, ProductStoreTelecomSettingEntity, SqlBuilder.PSC, ProductStoreTelecomSettingDao> {
}
