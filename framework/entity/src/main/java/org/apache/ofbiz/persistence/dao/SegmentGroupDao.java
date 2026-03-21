package org.apache.ofbiz.persistence.dao;

import com.landawn.abacus.jdbc.dao.CrudDao;
import com.landawn.abacus.query.SqlBuilder;
import org.apache.ofbiz.persistence.entity.SegmentGroupEntity;

public interface SegmentGroupDao extends CrudDao<SegmentGroupEntity, String, SqlBuilder.PSC, SegmentGroupDao> {
}
