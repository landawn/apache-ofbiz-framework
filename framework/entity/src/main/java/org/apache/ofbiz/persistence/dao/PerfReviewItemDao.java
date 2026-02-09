package org.apache.ofbiz.persistence.dao;

import com.landawn.abacus.jdbc.dao.CrudDao;
import com.landawn.abacus.query.SQLBuilder;
import org.apache.ofbiz.persistence.entity.PerfReviewItemEntity;

public interface PerfReviewItemDao extends CrudDao<PerfReviewItemEntity, PerfReviewItemEntity, SQLBuilder.PSC, PerfReviewItemDao> {
}
