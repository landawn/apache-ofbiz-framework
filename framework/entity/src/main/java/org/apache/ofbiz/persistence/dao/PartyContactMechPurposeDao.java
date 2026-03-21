package org.apache.ofbiz.persistence.dao;

import com.landawn.abacus.jdbc.dao.CrudDao;
import com.landawn.abacus.query.SqlBuilder;
import java.util.List;
import org.apache.ofbiz.base.util.UtilMisc;
import org.apache.ofbiz.entity.Delegator;
import org.apache.ofbiz.entity.GenericEntityException;
import org.apache.ofbiz.entity.GenericValue;
import org.apache.ofbiz.persistence.entity.PartyContactMechPurposeEntity;

public interface PartyContactMechPurposeDao extends CrudDao<PartyContactMechPurposeEntity, PartyContactMechPurposeEntity, SqlBuilder.PSC, PartyContactMechPurposeDao> {
    default List<GenericValue> listPartyContactWithPurpose(Delegator delegator, String partyId, String contactMechId,
            String contactMechPurposeTypeId) throws GenericEntityException {
        return delegator.findByAnd("PartyContactWithPurpose",
                UtilMisc.toMap("partyId", partyId, "contactMechId", contactMechId, "contactMechPurposeTypeId", contactMechPurposeTypeId),
                null, false);
    }
}
