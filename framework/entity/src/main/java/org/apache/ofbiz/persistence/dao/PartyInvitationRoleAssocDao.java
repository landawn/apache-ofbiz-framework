package org.apache.ofbiz.persistence.dao;

import com.landawn.abacus.jdbc.dao.CrudDao;
import com.landawn.abacus.query.SQLBuilder;
import org.apache.ofbiz.persistence.entity.PartyInvitationRoleAssocEntity;

public interface PartyInvitationRoleAssocDao extends CrudDao<PartyInvitationRoleAssocEntity, PartyInvitationRoleAssocEntity, SQLBuilder.PSC, PartyInvitationRoleAssocDao> {
}
