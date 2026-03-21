package org.apache.ofbiz.persistence.dao;

import com.landawn.abacus.jdbc.dao.CrudDao;
import com.landawn.abacus.query.SqlBuilder;
import org.apache.ofbiz.persistence.entity.ReorderGuidelineEntity;

public interface ReorderGuidelineDao extends CrudDao<ReorderGuidelineEntity, String, SqlBuilder.PSC, ReorderGuidelineDao> {
}
