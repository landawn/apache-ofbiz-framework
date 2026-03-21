package org.apache.ofbiz.persistence.dao;

import com.landawn.abacus.jdbc.dao.CrudDao;
import com.landawn.abacus.query.SqlBuilder;
import org.apache.ofbiz.persistence.entity.AgreementRoleEntity;

public interface AgreementRoleDao extends CrudDao<AgreementRoleEntity, AgreementRoleEntity, SqlBuilder.PSC, AgreementRoleDao> {
}
