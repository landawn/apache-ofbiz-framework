package org.apache.ofbiz.persistence.dao;

import com.landawn.abacus.jdbc.dao.CrudDao;
import com.landawn.abacus.query.SQLBuilder;
import org.apache.ofbiz.persistence.entity.PartyGroupEntity;

public interface PartyGroupDao extends CrudDao<PartyGroupEntity, String, SQLBuilder.PSC, PartyGroupDao> {
}
