package org.apache.ofbiz.persistence.dao;

import com.landawn.abacus.jdbc.dao.CrudDao;
import com.landawn.abacus.query.SqlBuilder;
import org.apache.ofbiz.persistence.entity.ElectronicTextEntity;

public interface ElectronicTextDao extends CrudDao<ElectronicTextEntity, String, SqlBuilder.PSC, ElectronicTextDao> {
}
