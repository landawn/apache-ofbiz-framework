package org.apache.ofbiz.persistence.dao;

import com.landawn.abacus.jdbc.dao.CrudDao;
import com.landawn.abacus.query.SqlBuilder;
import org.apache.ofbiz.persistence.entity.CommunicationEventPurposeEntity;

public interface CommunicationEventPurposeDao extends CrudDao<CommunicationEventPurposeEntity, CommunicationEventPurposeEntity, SqlBuilder.PSC, CommunicationEventPurposeDao> {
}
