package org.apache.ofbiz.persistence.dao;

import com.landawn.abacus.jdbc.dao.CrudDao;
import com.landawn.abacus.query.SqlBuilder;
import org.apache.ofbiz.persistence.entity.PerfReviewItemTypeEntity;

public interface PerfReviewItemTypeDao extends CrudDao<PerfReviewItemTypeEntity, String, SqlBuilder.PSC, PerfReviewItemTypeDao> {
}
