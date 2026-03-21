package org.apache.ofbiz.persistence.dao;

import com.landawn.abacus.jdbc.dao.CrudDao;
import com.landawn.abacus.query.SqlBuilder;
import org.apache.ofbiz.persistence.entity.RespondingPartyEntity;

public interface RespondingPartyDao extends CrudDao<RespondingPartyEntity, RespondingPartyEntity, SqlBuilder.PSC, RespondingPartyDao> {
}
