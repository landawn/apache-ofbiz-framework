package org.apache.ofbiz.persistence.dao;

import com.landawn.abacus.jdbc.dao.CrudDao;
import com.landawn.abacus.query.SqlBuilder;
import org.apache.ofbiz.persistence.entity.ProductReviewEntity;

public interface ProductReviewDao extends CrudDao<ProductReviewEntity, String, SqlBuilder.PSC, ProductReviewDao> {
}
