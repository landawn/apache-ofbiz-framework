package org.apache.ofbiz.persistence.dao;

import com.landawn.abacus.jdbc.dao.CrudDao;
import com.landawn.abacus.query.SQLBuilder;
import org.apache.ofbiz.persistence.entity.VisitEntity;

public interface VisitDao extends CrudDao<VisitEntity, String, SQLBuilder.PSC, VisitDao> {
}
