package org.apache.ofbiz.persistence.dao;

import com.landawn.abacus.jdbc.dao.CrudDao;
import com.landawn.abacus.query.SQLBuilder;
import org.apache.ofbiz.persistence.entity.ItemIssuanceRoleEntity;

public interface ItemIssuanceRoleDao extends CrudDao<ItemIssuanceRoleEntity, ItemIssuanceRoleEntity, SQLBuilder.PSC, ItemIssuanceRoleDao> {
}
