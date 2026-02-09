package org.apache.ofbiz.persistence.dao;

import com.landawn.abacus.jdbc.dao.CrudDao;
import com.landawn.abacus.query.SQLBuilder;
import org.apache.ofbiz.persistence.entity.SupplierPrefOrderEntity;

public interface SupplierPrefOrderDao extends CrudDao<SupplierPrefOrderEntity, String, SQLBuilder.PSC, SupplierPrefOrderDao> {
}
