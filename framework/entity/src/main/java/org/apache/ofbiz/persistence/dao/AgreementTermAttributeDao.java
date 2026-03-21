package org.apache.ofbiz.persistence.dao;

import com.landawn.abacus.jdbc.dao.CrudDao;
import com.landawn.abacus.query.SqlBuilder;
import org.apache.ofbiz.persistence.entity.AgreementTermAttributeEntity;

public interface AgreementTermAttributeDao extends CrudDao<AgreementTermAttributeEntity, AgreementTermAttributeEntity, SqlBuilder.PSC, AgreementTermAttributeDao> {
}
