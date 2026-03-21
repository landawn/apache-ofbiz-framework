package org.apache.ofbiz.persistence.dao;

import com.landawn.abacus.jdbc.dao.CrudDao;
import com.landawn.abacus.query.SqlBuilder;
import org.apache.ofbiz.persistence.entity.UomEntity;

public interface UomDao extends CrudDao<UomEntity, String, SqlBuilder.PSC, UomDao> , DelegatorQueryDao{
}
