package org.apache.ofbiz.persistence.dao;

import com.landawn.abacus.jdbc.dao.CrudDao;
import com.landawn.abacus.query.SqlBuilder;
import org.apache.ofbiz.persistence.entity.AgreementEmploymentApplEntity;

public interface AgreementEmploymentApplDao extends CrudDao<AgreementEmploymentApplEntity, AgreementEmploymentApplEntity, SqlBuilder.PSC, AgreementEmploymentApplDao> {
}
