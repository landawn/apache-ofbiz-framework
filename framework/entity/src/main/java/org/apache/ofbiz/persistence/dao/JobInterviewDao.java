package org.apache.ofbiz.persistence.dao;

import com.landawn.abacus.jdbc.dao.CrudDao;
import com.landawn.abacus.query.SQLBuilder;
import org.apache.ofbiz.persistence.entity.JobInterviewEntity;

public interface JobInterviewDao extends CrudDao<JobInterviewEntity, String, SQLBuilder.PSC, JobInterviewDao> {
}
