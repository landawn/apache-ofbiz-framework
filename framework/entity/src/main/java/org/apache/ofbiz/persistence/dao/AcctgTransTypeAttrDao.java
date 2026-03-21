package org.apache.ofbiz.persistence.dao;

import com.landawn.abacus.jdbc.dao.CrudDao;
import com.landawn.abacus.query.SqlBuilder;
import org.apache.ofbiz.persistence.entity.AcctgTransTypeAttrEntity;

public interface AcctgTransTypeAttrDao extends CrudDao<AcctgTransTypeAttrEntity, AcctgTransTypeAttrEntity, SqlBuilder.PSC, AcctgTransTypeAttrDao> {
}
