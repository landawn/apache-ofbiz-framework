package org.apache.ofbiz.persistence.dao;

import com.landawn.abacus.jdbc.dao.CrudDao;
import com.landawn.abacus.query.SqlBuilder;
import org.apache.ofbiz.persistence.entity.AgreementTermEntity;

public interface AgreementTermDao extends CrudDao<AgreementTermEntity, String, SqlBuilder.PSC, AgreementTermDao> {
}
