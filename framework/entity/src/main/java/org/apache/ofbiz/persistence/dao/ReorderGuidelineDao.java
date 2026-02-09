package org.apache.ofbiz.persistence.dao;

import com.landawn.abacus.jdbc.dao.CrudDao;
import com.landawn.abacus.query.SQLBuilder;
import org.apache.ofbiz.persistence.entity.ReorderGuidelineEntity;

public interface ReorderGuidelineDao extends CrudDao<ReorderGuidelineEntity, String, SQLBuilder.PSC, ReorderGuidelineDao> {
}
