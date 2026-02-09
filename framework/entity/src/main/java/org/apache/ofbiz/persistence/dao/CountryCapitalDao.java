package org.apache.ofbiz.persistence.dao;

import com.landawn.abacus.jdbc.dao.CrudDao;
import com.landawn.abacus.query.SQLBuilder;
import org.apache.ofbiz.persistence.entity.CountryCapitalEntity;

public interface CountryCapitalDao extends CrudDao<CountryCapitalEntity, String, SQLBuilder.PSC, CountryCapitalDao> {
}
