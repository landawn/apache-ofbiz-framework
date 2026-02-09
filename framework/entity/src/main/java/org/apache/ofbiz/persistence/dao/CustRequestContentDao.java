package org.apache.ofbiz.persistence.dao;

import com.landawn.abacus.jdbc.dao.CrudDao;
import com.landawn.abacus.query.SQLBuilder;
import org.apache.ofbiz.persistence.entity.CustRequestContentEntity;

public interface CustRequestContentDao extends CrudDao<CustRequestContentEntity, CustRequestContentEntity, SQLBuilder.PSC, CustRequestContentDao> {
}
