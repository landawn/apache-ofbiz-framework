package org.apache.ofbiz.persistence.dao;

import com.landawn.abacus.jdbc.dao.CrudDao;
import com.landawn.abacus.query.SqlBuilder;
import org.apache.ofbiz.persistence.entity.ProductPromoCodePartyEntity;

public interface ProductPromoCodePartyDao extends CrudDao<ProductPromoCodePartyEntity, ProductPromoCodePartyEntity, SqlBuilder.PSC, ProductPromoCodePartyDao> {
}
