package org.apache.ofbiz.persistence.dao;

import com.landawn.abacus.jdbc.dao.CrudDao;
import com.landawn.abacus.query.SqlBuilder;
import org.apache.ofbiz.persistence.entity.AgreementStatusEntity;

public interface AgreementStatusDao extends CrudDao<AgreementStatusEntity, AgreementStatusEntity, SqlBuilder.PSC, AgreementStatusDao> {
}
