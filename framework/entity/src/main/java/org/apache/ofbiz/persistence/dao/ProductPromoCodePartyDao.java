package org.apache.ofbiz.persistence.dao;

import com.landawn.abacus.jdbc.dao.CrudDao;
import com.landawn.abacus.query.SQLBuilder;
import org.apache.ofbiz.persistence.entity.ProductPromoCodePartyEntity;

public interface ProductPromoCodePartyDao extends CrudDao<ProductPromoCodePartyEntity, ProductPromoCodePartyEntity, SQLBuilder.PSC, ProductPromoCodePartyDao> {
}
