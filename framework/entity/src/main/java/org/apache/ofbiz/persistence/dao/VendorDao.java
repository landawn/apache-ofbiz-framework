package org.apache.ofbiz.persistence.dao;

import com.landawn.abacus.jdbc.dao.CrudDao;
import com.landawn.abacus.query.SQLBuilder;
import org.apache.ofbiz.persistence.entity.VendorEntity;

public interface VendorDao extends CrudDao<VendorEntity, String, SQLBuilder.PSC, VendorDao> {
}
