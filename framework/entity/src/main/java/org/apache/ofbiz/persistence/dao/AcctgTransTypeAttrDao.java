package org.apache.ofbiz.persistence.dao;

import com.landawn.abacus.jdbc.dao.CrudDao;
import com.landawn.abacus.query.SQLBuilder;
import org.apache.ofbiz.persistence.entity.AcctgTransTypeAttrEntity;

public interface AcctgTransTypeAttrDao extends CrudDao<AcctgTransTypeAttrEntity, AcctgTransTypeAttrEntity, SQLBuilder.PSC, AcctgTransTypeAttrDao> {
}
