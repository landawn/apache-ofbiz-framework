package org.apache.ofbiz.persistence.dao;

import com.landawn.abacus.jdbc.dao.CrudDao;
import com.landawn.abacus.query.SqlBuilder;
import org.apache.ofbiz.persistence.entity.AgreementItemAttributeEntity;

public interface AgreementItemAttributeDao extends CrudDao<AgreementItemAttributeEntity, AgreementItemAttributeEntity, SqlBuilder.PSC, AgreementItemAttributeDao> {
}
