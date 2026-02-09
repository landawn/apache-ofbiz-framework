package org.apache.ofbiz.persistence.dao;

import com.landawn.abacus.jdbc.dao.CrudDao;
import com.landawn.abacus.query.SQLBuilder;
import org.apache.ofbiz.persistence.entity.AgreementTermAttributeEntity;

public interface AgreementTermAttributeDao extends CrudDao<AgreementTermAttributeEntity, AgreementTermAttributeEntity, SQLBuilder.PSC, AgreementTermAttributeDao> {
}
