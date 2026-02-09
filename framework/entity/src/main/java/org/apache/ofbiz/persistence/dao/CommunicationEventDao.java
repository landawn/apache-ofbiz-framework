package org.apache.ofbiz.persistence.dao;

import com.landawn.abacus.jdbc.dao.CrudDao;
import com.landawn.abacus.query.SQLBuilder;
import org.apache.ofbiz.persistence.entity.CommunicationEventEntity;

public interface CommunicationEventDao extends CrudDao<CommunicationEventEntity, String, SQLBuilder.PSC, CommunicationEventDao> {
}
