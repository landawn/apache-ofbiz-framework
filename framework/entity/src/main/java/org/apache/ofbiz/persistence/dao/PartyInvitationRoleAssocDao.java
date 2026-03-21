package org.apache.ofbiz.persistence.dao;

import com.landawn.abacus.jdbc.dao.CrudDao;
import com.landawn.abacus.query.SqlBuilder;
import org.apache.ofbiz.persistence.entity.PartyInvitationRoleAssocEntity;

public interface PartyInvitationRoleAssocDao extends CrudDao<PartyInvitationRoleAssocEntity, PartyInvitationRoleAssocEntity, SqlBuilder.PSC, PartyInvitationRoleAssocDao> {
}
