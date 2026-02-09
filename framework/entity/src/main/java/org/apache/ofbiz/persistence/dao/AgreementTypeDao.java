package org.apache.ofbiz.persistence.dao;

import com.landawn.abacus.jdbc.dao.CrudDao;
import com.landawn.abacus.query.SQLBuilder;
import org.apache.ofbiz.persistence.entity.AgreementTypeEntity;

public interface AgreementTypeDao extends CrudDao<AgreementTypeEntity, String, SQLBuilder.PSC, AgreementTypeDao> {
}
