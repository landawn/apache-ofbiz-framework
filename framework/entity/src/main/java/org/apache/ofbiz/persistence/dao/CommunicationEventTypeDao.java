package org.apache.ofbiz.persistence.dao;

import com.landawn.abacus.jdbc.dao.CrudDao;
import com.landawn.abacus.query.SQLBuilder;
import org.apache.ofbiz.persistence.entity.CommunicationEventTypeEntity;

public interface CommunicationEventTypeDao extends CrudDao<CommunicationEventTypeEntity, String, SQLBuilder.PSC, CommunicationEventTypeDao> {
}
