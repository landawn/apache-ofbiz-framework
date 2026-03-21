package org.apache.ofbiz.persistence.dao;

import com.landawn.abacus.jdbc.dao.CrudDao;
import com.landawn.abacus.query.SqlBuilder;
import org.apache.ofbiz.persistence.entity.CountryCodeEntity;

public interface CountryCodeDao extends CrudDao<CountryCodeEntity, String, SqlBuilder.PSC, CountryCodeDao> {
}
