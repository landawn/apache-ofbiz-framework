package org.apache.ofbiz.persistence.dao;

import com.landawn.abacus.jdbc.dao.CrudDao;
import com.landawn.abacus.query.SqlBuilder;
import org.apache.ofbiz.persistence.entity.AgreementAttributeEntity;

public interface AgreementAttributeDao extends CrudDao<AgreementAttributeEntity, AgreementAttributeEntity, SqlBuilder.PSC, AgreementAttributeDao> {
}
