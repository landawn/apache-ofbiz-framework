package org.apache.ofbiz.persistence.dao;

import com.landawn.abacus.jdbc.dao.CrudDao;
import com.landawn.abacus.query.SQLBuilder;
import org.apache.ofbiz.persistence.entity.CustRequestCommEventEntity;

public interface CustRequestCommEventDao extends CrudDao<CustRequestCommEventEntity, CustRequestCommEventEntity, SQLBuilder.PSC, CustRequestCommEventDao> {
}
