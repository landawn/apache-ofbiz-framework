package org.apache.ofbiz.persistence.dao;

import com.landawn.abacus.jdbc.dao.CrudDao;
import com.landawn.abacus.query.SqlBuilder;
import org.apache.ofbiz.persistence.entity.CommunicationEventReturnEntity;

public interface CommunicationEventReturnDao extends CrudDao<CommunicationEventReturnEntity, CommunicationEventReturnEntity, SqlBuilder.PSC, CommunicationEventReturnDao> {
}
