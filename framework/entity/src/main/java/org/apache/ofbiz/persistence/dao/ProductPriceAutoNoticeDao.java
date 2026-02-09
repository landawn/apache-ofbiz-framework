package org.apache.ofbiz.persistence.dao;

import com.landawn.abacus.jdbc.dao.CrudDao;
import com.landawn.abacus.query.SQLBuilder;
import org.apache.ofbiz.persistence.entity.ProductPriceAutoNoticeEntity;

public interface ProductPriceAutoNoticeDao extends CrudDao<ProductPriceAutoNoticeEntity, String, SQLBuilder.PSC, ProductPriceAutoNoticeDao> {
}
