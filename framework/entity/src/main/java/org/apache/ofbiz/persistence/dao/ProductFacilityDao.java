package org.apache.ofbiz.persistence.dao;

import com.landawn.abacus.jdbc.dao.CrudDao;
import com.landawn.abacus.query.SqlBuilder;
import org.apache.ofbiz.persistence.entity.ProductFacilityEntity;

public interface ProductFacilityDao extends CrudDao<ProductFacilityEntity, ProductFacilityEntity, SqlBuilder.PSC, ProductFacilityDao> , DelegatorQueryDao{
}
