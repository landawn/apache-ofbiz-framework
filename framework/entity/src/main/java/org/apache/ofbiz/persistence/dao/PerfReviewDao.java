package org.apache.ofbiz.persistence.dao;

import com.landawn.abacus.jdbc.dao.CrudDao;
import com.landawn.abacus.query.SQLBuilder;
import org.apache.ofbiz.persistence.entity.PerfReviewEntity;

public interface PerfReviewDao extends CrudDao<PerfReviewEntity, PerfReviewEntity, SQLBuilder.PSC, PerfReviewDao> {
}
