package org.apache.ofbiz.persistence.dao;

import com.landawn.abacus.jdbc.dao.CrudDao;
import com.landawn.abacus.query.SQLBuilder;
import org.apache.ofbiz.persistence.entity.SupplierRatingTypeEntity;

public interface SupplierRatingTypeDao extends CrudDao<SupplierRatingTypeEntity, String, SQLBuilder.PSC, SupplierRatingTypeDao> {
}
