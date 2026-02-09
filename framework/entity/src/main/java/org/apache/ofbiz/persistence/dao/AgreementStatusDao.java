package org.apache.ofbiz.persistence.dao;

import com.landawn.abacus.jdbc.dao.CrudDao;
import com.landawn.abacus.query.SQLBuilder;
import org.apache.ofbiz.persistence.entity.AgreementStatusEntity;

public interface AgreementStatusDao extends CrudDao<AgreementStatusEntity, AgreementStatusEntity, SQLBuilder.PSC, AgreementStatusDao> {
}
