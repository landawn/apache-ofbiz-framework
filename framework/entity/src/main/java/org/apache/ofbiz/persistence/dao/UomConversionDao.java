package org.apache.ofbiz.persistence.dao;

import com.landawn.abacus.jdbc.dao.CrudDao;
import com.landawn.abacus.query.SQLBuilder;
import org.apache.ofbiz.persistence.entity.UomConversionEntity;

public interface UomConversionDao extends CrudDao<UomConversionEntity, UomConversionEntity, SQLBuilder.PSC, UomConversionDao> {
}
