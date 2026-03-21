package org.apache.ofbiz.persistence.dao;

import com.landawn.abacus.jdbc.dao.CrudDao;
import com.landawn.abacus.query.SqlBuilder;
import org.apache.ofbiz.persistence.entity.StatusValidChangeEntity;

public interface StatusValidChangeDao extends CrudDao<StatusValidChangeEntity, StatusValidChangeEntity, SqlBuilder.PSC, StatusValidChangeDao>, DelegatorQueryDao {
}
