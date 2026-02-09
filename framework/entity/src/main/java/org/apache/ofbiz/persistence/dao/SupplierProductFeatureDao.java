package org.apache.ofbiz.persistence.dao;

import com.landawn.abacus.jdbc.dao.CrudDao;
import com.landawn.abacus.query.SQLBuilder;
import org.apache.ofbiz.persistence.entity.SupplierProductFeatureEntity;

public interface SupplierProductFeatureDao extends CrudDao<SupplierProductFeatureEntity, SupplierProductFeatureEntity, SQLBuilder.PSC, SupplierProductFeatureDao> {
}
