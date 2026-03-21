package org.apache.ofbiz.persistence.dao;

import com.landawn.abacus.jdbc.dao.CrudDao;
import com.landawn.abacus.query.SqlBuilder;
import org.apache.ofbiz.persistence.entity.ReturnHeaderTypeEntity;

public interface ReturnHeaderTypeDao extends CrudDao<ReturnHeaderTypeEntity, String, SqlBuilder.PSC, ReturnHeaderTypeDao> {
}
