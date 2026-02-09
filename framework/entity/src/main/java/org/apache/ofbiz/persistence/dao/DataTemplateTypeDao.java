package org.apache.ofbiz.persistence.dao;

import com.landawn.abacus.jdbc.dao.CrudDao;
import com.landawn.abacus.query.SQLBuilder;
import org.apache.ofbiz.persistence.entity.DataTemplateTypeEntity;

public interface DataTemplateTypeDao extends CrudDao<DataTemplateTypeEntity, String, SQLBuilder.PSC, DataTemplateTypeDao> {
}
