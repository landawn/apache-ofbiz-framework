package org.apache.ofbiz.persistence.dao;

import com.landawn.abacus.jdbc.dao.CrudDao;
import com.landawn.abacus.query.SQLBuilder;
import org.apache.ofbiz.persistence.entity.JobInterviewTypeEntity;

public interface JobInterviewTypeDao extends CrudDao<JobInterviewTypeEntity, String, SQLBuilder.PSC, JobInterviewTypeDao> {
}
