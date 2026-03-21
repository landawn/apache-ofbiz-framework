package org.apache.ofbiz.persistence.dao;

import com.landawn.abacus.jdbc.dao.CrudDao;
import com.landawn.abacus.query.SqlBuilder;
import org.apache.ofbiz.persistence.entity.CommunicationEventProductEntity;

public interface CommunicationEventProductDao extends CrudDao<CommunicationEventProductEntity, CommunicationEventProductEntity, SqlBuilder.PSC, CommunicationEventProductDao> {
}
