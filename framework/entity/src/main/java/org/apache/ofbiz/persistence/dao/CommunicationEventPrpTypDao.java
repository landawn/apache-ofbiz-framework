package org.apache.ofbiz.persistence.dao;

import com.landawn.abacus.jdbc.dao.CrudDao;
import com.landawn.abacus.query.SqlBuilder;
import org.apache.ofbiz.persistence.entity.CommunicationEventPrpTypEntity;

public interface CommunicationEventPrpTypDao extends CrudDao<CommunicationEventPrpTypEntity, String, SqlBuilder.PSC, CommunicationEventPrpTypDao> {
}
