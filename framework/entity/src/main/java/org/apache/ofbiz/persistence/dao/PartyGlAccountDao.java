package org.apache.ofbiz.persistence.dao;

import com.landawn.abacus.jdbc.dao.CrudDao;
import com.landawn.abacus.query.SqlBuilder;
import org.apache.ofbiz.persistence.entity.PartyGlAccountEntity;

public interface PartyGlAccountDao extends CrudDao<PartyGlAccountEntity, PartyGlAccountEntity, SqlBuilder.PSC, PartyGlAccountDao> {
}
