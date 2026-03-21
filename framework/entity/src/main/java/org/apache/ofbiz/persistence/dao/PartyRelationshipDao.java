package org.apache.ofbiz.persistence.dao;

import com.landawn.abacus.jdbc.dao.CrudDao;
import com.landawn.abacus.query.SqlBuilder;
import org.apache.ofbiz.persistence.entity.PartyRelationshipEntity;

public interface PartyRelationshipDao extends CrudDao<PartyRelationshipEntity, PartyRelationshipEntity, SqlBuilder.PSC, PartyRelationshipDao> {
}
