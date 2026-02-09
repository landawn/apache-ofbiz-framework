package org.apache.ofbiz.persistence.dao;

import com.landawn.abacus.jdbc.dao.CrudDao;
import com.landawn.abacus.query.SQLBuilder;
import org.apache.ofbiz.persistence.entity.PartyContentTypeEntity;

public interface PartyContentTypeDao extends CrudDao<PartyContentTypeEntity, String, SQLBuilder.PSC, PartyContentTypeDao> {
}
