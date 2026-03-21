package org.apache.ofbiz.persistence.dao;

import com.landawn.abacus.jdbc.dao.CrudDao;
import com.landawn.abacus.query.SqlBuilder;
import org.apache.ofbiz.persistence.entity.CommContentAssocTypeEntity;

public interface CommContentAssocTypeDao extends CrudDao<CommContentAssocTypeEntity, String, SqlBuilder.PSC, CommContentAssocTypeDao> {
}
