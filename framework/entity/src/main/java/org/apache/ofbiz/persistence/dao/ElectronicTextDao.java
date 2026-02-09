package org.apache.ofbiz.persistence.dao;

import com.landawn.abacus.jdbc.dao.CrudDao;
import com.landawn.abacus.query.SQLBuilder;
import org.apache.ofbiz.persistence.entity.ElectronicTextEntity;

public interface ElectronicTextDao extends CrudDao<ElectronicTextEntity, String, SQLBuilder.PSC, ElectronicTextDao> {
}
