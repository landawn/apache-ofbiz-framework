package org.apache.ofbiz.persistence.dao;

import com.landawn.abacus.jdbc.dao.CrudDao;
import com.landawn.abacus.query.SQLBuilder;
import org.apache.ofbiz.persistence.entity.AcctgTransEntity;

public interface AcctgTransDao extends CrudDao<AcctgTransEntity, String, SQLBuilder.PSC, AcctgTransDao> {
}
