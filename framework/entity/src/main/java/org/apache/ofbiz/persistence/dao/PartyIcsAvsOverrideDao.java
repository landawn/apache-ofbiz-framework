package org.apache.ofbiz.persistence.dao;

import com.landawn.abacus.jdbc.dao.CrudDao;
import com.landawn.abacus.query.SqlBuilder;
import org.apache.ofbiz.persistence.entity.PartyIcsAvsOverrideEntity;

public interface PartyIcsAvsOverrideDao extends CrudDao<PartyIcsAvsOverrideEntity, String, SqlBuilder.PSC, PartyIcsAvsOverrideDao> {
}
