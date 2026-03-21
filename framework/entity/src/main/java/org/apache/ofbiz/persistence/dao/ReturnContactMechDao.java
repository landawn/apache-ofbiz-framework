package org.apache.ofbiz.persistence.dao;

import com.landawn.abacus.jdbc.dao.CrudDao;
import com.landawn.abacus.query.SqlBuilder;
import org.apache.ofbiz.persistence.entity.ReturnContactMechEntity;

public interface ReturnContactMechDao extends CrudDao<ReturnContactMechEntity, ReturnContactMechEntity, SqlBuilder.PSC, ReturnContactMechDao> {
}
