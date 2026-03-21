package org.apache.ofbiz.persistence.dao;

import com.landawn.abacus.jdbc.dao.CrudDao;
import com.landawn.abacus.query.SqlBuilder;
import org.apache.ofbiz.persistence.entity.ProductStoreSurveyApplEntity;

public interface ProductStoreSurveyApplDao extends CrudDao<ProductStoreSurveyApplEntity, String, SqlBuilder.PSC, ProductStoreSurveyApplDao> {
}
