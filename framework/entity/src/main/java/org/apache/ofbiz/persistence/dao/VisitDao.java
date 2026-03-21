package org.apache.ofbiz.persistence.dao;

import com.landawn.abacus.jdbc.dao.CrudDao;
import com.landawn.abacus.query.SqlBuilder;
import org.apache.ofbiz.persistence.entity.VisitEntity;

public interface VisitDao extends CrudDao<VisitEntity, String, SqlBuilder.PSC, VisitDao> {
}
