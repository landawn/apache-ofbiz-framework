package org.apache.ofbiz.persistence.dao;

import com.landawn.abacus.jdbc.dao.CrudDao;
import com.landawn.abacus.query.SQLBuilder;
import org.apache.ofbiz.persistence.entity.JobRequisitionEntity;

public interface JobRequisitionDao extends CrudDao<JobRequisitionEntity, String, SQLBuilder.PSC, JobRequisitionDao> {
}
