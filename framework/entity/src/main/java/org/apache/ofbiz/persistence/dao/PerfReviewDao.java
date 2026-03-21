package org.apache.ofbiz.persistence.dao;

import com.landawn.abacus.jdbc.dao.CrudDao;
import com.landawn.abacus.query.SqlBuilder;
import org.apache.ofbiz.persistence.entity.PerfReviewEntity;

public interface PerfReviewDao extends CrudDao<PerfReviewEntity, PerfReviewEntity, SqlBuilder.PSC, PerfReviewDao> {
}
