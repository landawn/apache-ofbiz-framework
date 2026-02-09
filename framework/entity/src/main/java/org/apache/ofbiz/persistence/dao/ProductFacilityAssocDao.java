package org.apache.ofbiz.persistence.dao;

import com.landawn.abacus.jdbc.dao.CrudDao;
import com.landawn.abacus.query.SQLBuilder;
import org.apache.ofbiz.persistence.entity.ProductFacilityAssocEntity;

public interface ProductFacilityAssocDao extends CrudDao<ProductFacilityAssocEntity, ProductFacilityAssocEntity, SQLBuilder.PSC, ProductFacilityAssocDao> {
}
