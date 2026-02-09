package org.apache.ofbiz.persistence.dao;

import com.landawn.abacus.jdbc.dao.CrudDao;
import com.landawn.abacus.query.SQLBuilder;
import org.apache.ofbiz.persistence.entity.ReturnHeaderEntity;

public interface ReturnHeaderDao extends CrudDao<ReturnHeaderEntity, String, SQLBuilder.PSC, ReturnHeaderDao> {
}
