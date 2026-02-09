package org.apache.ofbiz.persistence.dao;

import com.landawn.abacus.jdbc.dao.CrudDao;
import com.landawn.abacus.query.SQLBuilder;
import org.apache.ofbiz.persistence.entity.JobManagerLockEntity;

public interface JobManagerLockDao extends CrudDao<JobManagerLockEntity, JobManagerLockEntity, SQLBuilder.PSC, JobManagerLockDao> {
}
