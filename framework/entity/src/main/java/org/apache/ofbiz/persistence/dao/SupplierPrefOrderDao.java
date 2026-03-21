package org.apache.ofbiz.persistence.dao;

import com.landawn.abacus.jdbc.dao.CrudDao;
import com.landawn.abacus.query.SqlBuilder;
import org.apache.ofbiz.persistence.entity.SupplierPrefOrderEntity;

public interface SupplierPrefOrderDao extends CrudDao<SupplierPrefOrderEntity, String, SqlBuilder.PSC, SupplierPrefOrderDao> {
}
