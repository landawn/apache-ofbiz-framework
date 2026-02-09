package org.apache.ofbiz.persistence.dao;

import com.landawn.abacus.jdbc.dao.CrudDao;
import com.landawn.abacus.query.SQLBuilder;
import org.apache.ofbiz.persistence.entity.PartyContentEntity;

public interface PartyContentDao extends CrudDao<PartyContentEntity, PartyContentEntity, SQLBuilder.PSC, PartyContentDao> {
}
