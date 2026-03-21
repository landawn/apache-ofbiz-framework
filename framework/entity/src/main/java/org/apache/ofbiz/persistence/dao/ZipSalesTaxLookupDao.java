package org.apache.ofbiz.persistence.dao;

import com.landawn.abacus.jdbc.dao.CrudDao;
import com.landawn.abacus.query.SqlBuilder;
import org.apache.ofbiz.persistence.entity.ZipSalesTaxLookupEntity;

public interface ZipSalesTaxLookupDao extends CrudDao<ZipSalesTaxLookupEntity, ZipSalesTaxLookupEntity, SqlBuilder.PSC, ZipSalesTaxLookupDao> {
}
