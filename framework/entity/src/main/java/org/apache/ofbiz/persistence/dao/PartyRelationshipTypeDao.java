package org.apache.ofbiz.persistence.dao;

import com.landawn.abacus.jdbc.dao.CrudDao;
import com.landawn.abacus.query.SQLBuilder;
import org.apache.ofbiz.persistence.entity.PartyRelationshipTypeEntity;

public interface PartyRelationshipTypeDao extends CrudDao<PartyRelationshipTypeEntity, String, SQLBuilder.PSC, PartyRelationshipTypeDao> {
}
