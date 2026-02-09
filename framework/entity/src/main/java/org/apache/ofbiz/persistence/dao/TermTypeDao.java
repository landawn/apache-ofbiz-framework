package org.apache.ofbiz.persistence.dao;

import com.landawn.abacus.jdbc.dao.CrudDao;
import com.landawn.abacus.query.SQLBuilder;
import org.apache.ofbiz.persistence.entity.TermTypeEntity;

public interface TermTypeDao extends CrudDao<TermTypeEntity, String, SQLBuilder.PSC, TermTypeDao> {
}
