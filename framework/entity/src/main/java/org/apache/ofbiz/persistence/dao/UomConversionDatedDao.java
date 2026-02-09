package org.apache.ofbiz.persistence.dao;

import com.landawn.abacus.jdbc.dao.CrudDao;
import com.landawn.abacus.query.SQLBuilder;
import org.apache.ofbiz.persistence.entity.UomConversionDatedEntity;

public interface UomConversionDatedDao extends CrudDao<UomConversionDatedEntity, UomConversionDatedEntity, SQLBuilder.PSC, UomConversionDatedDao> {
}
