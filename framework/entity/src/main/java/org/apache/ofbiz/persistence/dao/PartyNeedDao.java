package org.apache.ofbiz.persistence.dao;

import com.landawn.abacus.jdbc.dao.CrudDao;
import com.landawn.abacus.query.SqlBuilder;
import org.apache.ofbiz.persistence.entity.PartyNeedEntity;

public interface PartyNeedDao extends CrudDao<PartyNeedEntity, PartyNeedEntity, SqlBuilder.PSC, PartyNeedDao> {
}
