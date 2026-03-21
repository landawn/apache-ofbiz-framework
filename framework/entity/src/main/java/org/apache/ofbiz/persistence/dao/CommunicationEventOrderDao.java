package org.apache.ofbiz.persistence.dao;

import com.landawn.abacus.jdbc.dao.CrudDao;
import com.landawn.abacus.query.SqlBuilder;
import org.apache.ofbiz.persistence.entity.CommunicationEventOrderEntity;

public interface CommunicationEventOrderDao extends CrudDao<CommunicationEventOrderEntity, CommunicationEventOrderEntity, SqlBuilder.PSC, CommunicationEventOrderDao> {
}
