package org.apache.ofbiz.persistence.dao;

import com.landawn.abacus.jdbc.dao.CrudDao;
import com.landawn.abacus.query.SQLBuilder;
import org.apache.ofbiz.persistence.entity.AddressMatchMapEntity;

public interface AddressMatchMapDao extends CrudDao<AddressMatchMapEntity, AddressMatchMapEntity, SQLBuilder.PSC, AddressMatchMapDao> {
}
