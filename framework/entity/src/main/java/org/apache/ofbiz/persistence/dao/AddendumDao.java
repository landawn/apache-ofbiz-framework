package org.apache.ofbiz.persistence.dao;

import com.landawn.abacus.jdbc.dao.CrudDao;
import com.landawn.abacus.query.SqlBuilder;
import org.apache.ofbiz.persistence.entity.AddendumEntity;

public interface AddendumDao extends CrudDao<AddendumEntity, String, SqlBuilder.PSC, AddendumDao> {
}
