package org.apache.ofbiz.persistence.dao;

import com.landawn.abacus.jdbc.dao.CrudDao;
import com.landawn.abacus.query.SqlBuilder;
import org.apache.ofbiz.persistence.entity.PartyRoleEntity;

public interface PartyRoleDao extends CrudDao<PartyRoleEntity, PartyRoleEntity, SqlBuilder.PSC, PartyRoleDao>, DelegatorQueryDao {
}
