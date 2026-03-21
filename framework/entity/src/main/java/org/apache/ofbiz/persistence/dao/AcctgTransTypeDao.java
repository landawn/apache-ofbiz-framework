package org.apache.ofbiz.persistence.dao;

import com.landawn.abacus.jdbc.dao.CrudDao;
import com.landawn.abacus.query.SqlBuilder;
import org.apache.ofbiz.persistence.entity.AcctgTransTypeEntity;

public interface AcctgTransTypeDao extends CrudDao<AcctgTransTypeEntity, String, SqlBuilder.PSC, AcctgTransTypeDao> {
}
