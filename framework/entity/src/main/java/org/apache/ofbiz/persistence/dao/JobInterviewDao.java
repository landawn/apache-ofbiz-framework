package org.apache.ofbiz.persistence.dao;

import com.landawn.abacus.jdbc.dao.CrudDao;
import com.landawn.abacus.query.SqlBuilder;
import org.apache.ofbiz.persistence.entity.JobInterviewEntity;

public interface JobInterviewDao extends CrudDao<JobInterviewEntity, String, SqlBuilder.PSC, JobInterviewDao> {
}
