package org.apache.ofbiz.persistence.dao;

import com.landawn.abacus.jdbc.dao.CrudDao;
import com.landawn.abacus.query.SqlBuilder;
import org.apache.ofbiz.persistence.entity.PartyContentTypeEntity;

public interface PartyContentTypeDao extends CrudDao<PartyContentTypeEntity, String, SqlBuilder.PSC, PartyContentTypeDao> {
}
