package org.apache.ofbiz.persistence.dao;

import com.landawn.abacus.jdbc.dao.CrudDao;
import com.landawn.abacus.query.SQLBuilder;
import org.apache.ofbiz.persistence.entity.VisitorEntity;

public interface VisitorDao extends CrudDao<VisitorEntity, String, SQLBuilder.PSC, VisitorDao> {
}
