package org.apache.ofbiz.persistence.dao;

import com.landawn.abacus.jdbc.dao.CrudDao;
import com.landawn.abacus.query.SQLBuilder;
import org.apache.ofbiz.persistence.entity.AgreementEntity;

public interface AgreementDao extends CrudDao<AgreementEntity, String, SQLBuilder.PSC, AgreementDao> {
}
