package org.apache.ofbiz.persistence.dao;

import com.landawn.abacus.jdbc.dao.CrudDao;
import com.landawn.abacus.query.SQLBuilder;
import org.apache.ofbiz.persistence.entity.AgreementProductApplEntity;

public interface AgreementProductApplDao extends CrudDao<AgreementProductApplEntity, AgreementProductApplEntity, SQLBuilder.PSC, AgreementProductApplDao> {
}
