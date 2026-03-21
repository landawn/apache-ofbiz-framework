package org.apache.ofbiz.persistence.dao;

import com.landawn.abacus.jdbc.dao.CrudDao;
import com.landawn.abacus.query.SqlBuilder;
import org.apache.ofbiz.persistence.entity.CommunicationEventEntity;

public interface CommunicationEventDao extends CrudDao<CommunicationEventEntity, String, SqlBuilder.PSC, CommunicationEventDao>, DelegatorQueryDao {
}
