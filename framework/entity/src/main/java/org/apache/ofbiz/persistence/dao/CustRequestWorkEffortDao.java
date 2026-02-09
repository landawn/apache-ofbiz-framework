package org.apache.ofbiz.persistence.dao;

import com.landawn.abacus.jdbc.dao.CrudDao;
import com.landawn.abacus.query.SQLBuilder;
import org.apache.ofbiz.persistence.entity.CustRequestWorkEffortEntity;

public interface CustRequestWorkEffortDao extends CrudDao<CustRequestWorkEffortEntity, CustRequestWorkEffortEntity, SQLBuilder.PSC, CustRequestWorkEffortDao> {
}
