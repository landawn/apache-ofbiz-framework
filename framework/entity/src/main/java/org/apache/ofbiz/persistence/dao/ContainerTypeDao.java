package org.apache.ofbiz.persistence.dao;

import com.landawn.abacus.jdbc.dao.CrudDao;
import com.landawn.abacus.query.SQLBuilder;
import org.apache.ofbiz.persistence.entity.ContainerTypeEntity;

public interface ContainerTypeDao extends CrudDao<ContainerTypeEntity, String, SQLBuilder.PSC, ContainerTypeDao> {
}
