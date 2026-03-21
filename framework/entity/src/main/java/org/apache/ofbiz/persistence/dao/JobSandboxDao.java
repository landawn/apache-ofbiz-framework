package org.apache.ofbiz.persistence.dao;

import com.landawn.abacus.jdbc.dao.CrudDao;
import com.landawn.abacus.query.SqlBuilder;
import org.apache.ofbiz.persistence.entity.JobSandboxEntity;

public interface JobSandboxDao extends CrudDao<JobSandboxEntity, String, SqlBuilder.PSC, JobSandboxDao> {
}
