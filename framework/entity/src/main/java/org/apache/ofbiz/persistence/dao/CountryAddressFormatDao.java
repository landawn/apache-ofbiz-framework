package org.apache.ofbiz.persistence.dao;

import com.landawn.abacus.jdbc.dao.CrudDao;
import com.landawn.abacus.query.SqlBuilder;
import org.apache.ofbiz.persistence.entity.CountryAddressFormatEntity;

public interface CountryAddressFormatDao extends CrudDao<CountryAddressFormatEntity, String, SqlBuilder.PSC, CountryAddressFormatDao> {
}
