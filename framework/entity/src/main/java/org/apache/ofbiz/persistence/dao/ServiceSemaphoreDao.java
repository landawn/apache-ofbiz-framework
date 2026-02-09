package org.apache.ofbiz.persistence.dao;

import com.landawn.abacus.jdbc.dao.CrudDao;
import com.landawn.abacus.query.SQLBuilder;
import org.apache.ofbiz.persistence.entity.ServiceSemaphoreEntity;

public interface ServiceSemaphoreDao extends CrudDao<ServiceSemaphoreEntity, String, SQLBuilder.PSC, ServiceSemaphoreDao> {
}
