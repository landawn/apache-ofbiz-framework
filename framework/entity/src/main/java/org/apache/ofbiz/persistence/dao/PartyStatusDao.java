package org.apache.ofbiz.persistence.dao;

import com.landawn.abacus.jdbc.dao.CrudDao;
import com.landawn.abacus.query.SQLBuilder;
import org.apache.ofbiz.persistence.entity.PartyStatusEntity;

public interface PartyStatusDao extends CrudDao<PartyStatusEntity, PartyStatusEntity, SQLBuilder.PSC, PartyStatusDao> {
}
