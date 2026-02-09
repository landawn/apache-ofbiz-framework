package org.apache.ofbiz.persistence.dao;

import com.landawn.abacus.jdbc.dao.CrudDao;
import com.landawn.abacus.query.SQLBuilder;
import org.apache.ofbiz.persistence.entity.CountryAddressFormatEntity;

public interface CountryAddressFormatDao extends CrudDao<CountryAddressFormatEntity, String, SQLBuilder.PSC, CountryAddressFormatDao> {
}
