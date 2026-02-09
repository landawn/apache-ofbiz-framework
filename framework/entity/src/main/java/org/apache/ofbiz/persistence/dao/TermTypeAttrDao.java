package org.apache.ofbiz.persistence.dao;

import com.landawn.abacus.jdbc.dao.CrudDao;
import com.landawn.abacus.query.SQLBuilder;
import org.apache.ofbiz.persistence.entity.TermTypeAttrEntity;

public interface TermTypeAttrDao extends CrudDao<TermTypeAttrEntity, TermTypeAttrEntity, SQLBuilder.PSC, TermTypeAttrDao> {
}
