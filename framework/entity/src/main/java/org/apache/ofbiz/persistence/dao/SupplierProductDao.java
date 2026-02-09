package org.apache.ofbiz.persistence.dao;

import com.landawn.abacus.jdbc.dao.CrudDao;
import com.landawn.abacus.query.SQLBuilder;
import org.apache.ofbiz.persistence.entity.SupplierProductEntity;

public interface SupplierProductDao extends CrudDao<SupplierProductEntity, SupplierProductEntity, SQLBuilder.PSC, SupplierProductDao> {
}
