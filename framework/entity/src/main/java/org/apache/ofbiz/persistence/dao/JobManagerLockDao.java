package org.apache.ofbiz.persistence.dao;

import com.landawn.abacus.jdbc.dao.CrudDao;
import com.landawn.abacus.query.SqlBuilder;
import org.apache.ofbiz.persistence.entity.JobManagerLockEntity;

public interface JobManagerLockDao extends CrudDao<JobManagerLockEntity, JobManagerLockEntity, SqlBuilder.PSC, JobManagerLockDao> {
}
