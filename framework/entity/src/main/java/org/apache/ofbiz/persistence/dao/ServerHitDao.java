package org.apache.ofbiz.persistence.dao;

import com.landawn.abacus.jdbc.dao.CrudDao;
import com.landawn.abacus.query.SQLBuilder;
import org.apache.ofbiz.persistence.entity.ServerHitEntity;

public interface ServerHitDao extends CrudDao<ServerHitEntity, ServerHitEntity, SQLBuilder.PSC, ServerHitDao> {
}
