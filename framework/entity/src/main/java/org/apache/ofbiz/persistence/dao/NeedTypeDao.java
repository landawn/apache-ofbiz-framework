package org.apache.ofbiz.persistence.dao;

import com.landawn.abacus.jdbc.dao.CrudDao;
import com.landawn.abacus.query.SQLBuilder;
import org.apache.ofbiz.persistence.entity.NeedTypeEntity;

public interface NeedTypeDao extends CrudDao<NeedTypeEntity, String, SQLBuilder.PSC, NeedTypeDao> {
}
