package org.apache.ofbiz.persistence.dao;

import com.landawn.abacus.jdbc.dao.CrudDao;
import com.landawn.abacus.query.SQLBuilder;
import org.apache.ofbiz.persistence.entity.PartyTypeEntity;

public interface PartyTypeDao extends CrudDao<PartyTypeEntity, String, SQLBuilder.PSC, PartyTypeDao> {
}
