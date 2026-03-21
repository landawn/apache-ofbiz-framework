package org.apache.ofbiz.persistence.dao;

import com.landawn.abacus.jdbc.dao.CrudDao;
import com.landawn.abacus.query.SqlBuilder;
import org.apache.ofbiz.persistence.entity.CommunicationEventTypeEntity;

public interface CommunicationEventTypeDao extends CrudDao<CommunicationEventTypeEntity, String, SqlBuilder.PSC, CommunicationEventTypeDao> {
}
