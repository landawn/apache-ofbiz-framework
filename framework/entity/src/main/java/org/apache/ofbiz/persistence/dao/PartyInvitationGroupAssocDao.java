package org.apache.ofbiz.persistence.dao;

import com.landawn.abacus.jdbc.dao.CrudDao;
import com.landawn.abacus.query.SQLBuilder;
import org.apache.ofbiz.persistence.entity.PartyInvitationGroupAssocEntity;

public interface PartyInvitationGroupAssocDao extends CrudDao<PartyInvitationGroupAssocEntity, PartyInvitationGroupAssocEntity, SQLBuilder.PSC, PartyInvitationGroupAssocDao> {
}
