package org.apache.ofbiz.persistence.dao;

import com.landawn.abacus.jdbc.dao.CrudDao;
import com.landawn.abacus.query.SqlBuilder;
import org.apache.ofbiz.persistence.entity.ReturnHeaderEntity;

public interface ReturnHeaderDao extends CrudDao<ReturnHeaderEntity, String, SqlBuilder.PSC, ReturnHeaderDao>, DelegatorQueryDao {
}
