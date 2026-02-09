package org.apache.ofbiz.persistence.dao;

import com.landawn.abacus.jdbc.dao.CrudDao;
import com.landawn.abacus.query.SQLBuilder;
import org.apache.ofbiz.persistence.entity.PartyRoleEntity;

public interface PartyRoleDao extends CrudDao<PartyRoleEntity, PartyRoleEntity, SQLBuilder.PSC, PartyRoleDao> {
}
