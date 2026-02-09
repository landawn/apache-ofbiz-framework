package org.apache.ofbiz.persistence.dao;

import com.landawn.abacus.jdbc.dao.CrudDao;
import com.landawn.abacus.query.SQLBuilder;
import org.apache.ofbiz.persistence.entity.X509IssuerProvisionEntity;

public interface X509IssuerProvisionDao extends CrudDao<X509IssuerProvisionEntity, String, SQLBuilder.PSC, X509IssuerProvisionDao> {
}
