package org.apache.ofbiz.persistence.dao;

import com.landawn.abacus.jdbc.dao.CrudDao;
import com.landawn.abacus.query.SQLBuilder;
import org.apache.ofbiz.persistence.entity.ProductStoreTelecomSettingEntity;

public interface ProductStoreTelecomSettingDao extends CrudDao<ProductStoreTelecomSettingEntity, ProductStoreTelecomSettingEntity, SQLBuilder.PSC, ProductStoreTelecomSettingDao> {
}
