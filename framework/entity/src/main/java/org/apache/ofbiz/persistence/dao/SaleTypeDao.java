package org.apache.ofbiz.persistence.dao;

import com.landawn.abacus.jdbc.dao.CrudDao;
import com.landawn.abacus.query.SQLBuilder;
import org.apache.ofbiz.persistence.entity.SaleTypeEntity;

public interface SaleTypeDao extends CrudDao<SaleTypeEntity, String, SQLBuilder.PSC, SaleTypeDao> {
}
