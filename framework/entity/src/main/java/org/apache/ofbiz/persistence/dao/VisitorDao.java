package org.apache.ofbiz.persistence.dao;

import com.landawn.abacus.jdbc.dao.CrudDao;
import com.landawn.abacus.query.SqlBuilder;
import org.apache.ofbiz.persistence.entity.VisitorEntity;

public interface VisitorDao extends CrudDao<VisitorEntity, String, SqlBuilder.PSC, VisitorDao> {
}
