package org.apache.ofbiz.persistence.dao;

import com.landawn.abacus.jdbc.dao.CrudDao;
import com.landawn.abacus.query.SqlBuilder;
import org.apache.ofbiz.persistence.entity.AgreementItemTypeEntity;

public interface AgreementItemTypeDao extends CrudDao<AgreementItemTypeEntity, String, SqlBuilder.PSC, AgreementItemTypeDao> {
}
