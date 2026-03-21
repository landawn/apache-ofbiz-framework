package org.apache.ofbiz.persistence.dao;

import com.landawn.abacus.jdbc.dao.CrudDao;
import com.landawn.abacus.query.SqlBuilder;
import org.apache.ofbiz.persistence.entity.PartyContactMechEntity;

public interface PartyContactMechDao extends CrudDao<PartyContactMechEntity, PartyContactMechEntity, SqlBuilder.PSC, PartyContactMechDao> {
}
