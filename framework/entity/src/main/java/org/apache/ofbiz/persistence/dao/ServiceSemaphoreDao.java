package org.apache.ofbiz.persistence.dao;

import com.landawn.abacus.jdbc.dao.CrudDao;
import com.landawn.abacus.query.SqlBuilder;
import org.apache.ofbiz.persistence.entity.ServiceSemaphoreEntity;

public interface ServiceSemaphoreDao extends CrudDao<ServiceSemaphoreEntity, String, SqlBuilder.PSC, ServiceSemaphoreDao> {
}
