package org.apache.ofbiz.persistence.dao;

import com.landawn.abacus.jdbc.dao.CrudDao;
import com.landawn.abacus.query.SQLBuilder;
import org.apache.ofbiz.persistence.entity.CustRequestAttributeEntity;

public interface CustRequestAttributeDao extends CrudDao<CustRequestAttributeEntity, CustRequestAttributeEntity, SQLBuilder.PSC, CustRequestAttributeDao> {
}
