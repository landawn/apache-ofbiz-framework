package org.apache.ofbiz.persistence.dao;

import com.landawn.abacus.jdbc.dao.CrudDao;
import com.landawn.abacus.query.SqlBuilder;
import org.apache.ofbiz.persistence.entity.ProtocolTypeEntity;

public interface ProtocolTypeDao extends CrudDao<ProtocolTypeEntity, String, SqlBuilder.PSC, ProtocolTypeDao> {
}
