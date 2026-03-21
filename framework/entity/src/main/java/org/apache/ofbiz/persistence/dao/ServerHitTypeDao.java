package org.apache.ofbiz.persistence.dao;

import com.landawn.abacus.jdbc.dao.CrudDao;
import com.landawn.abacus.query.SqlBuilder;
import org.apache.ofbiz.persistence.entity.ServerHitTypeEntity;

public interface ServerHitTypeDao extends CrudDao<ServerHitTypeEntity, String, SqlBuilder.PSC, ServerHitTypeDao> {
}
