package org.apache.ofbiz.persistence.dao;

import com.landawn.abacus.jdbc.dao.CrudDao;
import com.landawn.abacus.query.SqlBuilder;
import org.apache.ofbiz.persistence.entity.VendorEntity;

public interface VendorDao extends CrudDao<VendorEntity, String, SqlBuilder.PSC, VendorDao> {
}
