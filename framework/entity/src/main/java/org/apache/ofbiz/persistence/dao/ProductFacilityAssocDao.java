package org.apache.ofbiz.persistence.dao;

import com.landawn.abacus.jdbc.dao.CrudDao;
import com.landawn.abacus.query.SqlBuilder;
import org.apache.ofbiz.persistence.entity.ProductFacilityAssocEntity;

public interface ProductFacilityAssocDao extends CrudDao<ProductFacilityAssocEntity, ProductFacilityAssocEntity, SqlBuilder.PSC, ProductFacilityAssocDao> {
}
