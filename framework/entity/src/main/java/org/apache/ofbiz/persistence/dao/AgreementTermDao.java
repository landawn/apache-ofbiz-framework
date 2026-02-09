package org.apache.ofbiz.persistence.dao;

import com.landawn.abacus.jdbc.dao.CrudDao;
import com.landawn.abacus.query.SQLBuilder;
import org.apache.ofbiz.persistence.entity.AgreementTermEntity;

public interface AgreementTermDao extends CrudDao<AgreementTermEntity, String, SQLBuilder.PSC, AgreementTermDao> {
}
