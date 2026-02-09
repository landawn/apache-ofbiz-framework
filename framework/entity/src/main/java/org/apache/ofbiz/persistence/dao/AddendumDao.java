package org.apache.ofbiz.persistence.dao;

import com.landawn.abacus.jdbc.dao.CrudDao;
import com.landawn.abacus.query.SQLBuilder;
import org.apache.ofbiz.persistence.entity.AddendumEntity;

public interface AddendumDao extends CrudDao<AddendumEntity, String, SQLBuilder.PSC, AddendumDao> {
}
