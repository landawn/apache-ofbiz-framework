package org.apache.ofbiz.persistence.dao;

import com.landawn.abacus.jdbc.dao.CrudDao;
import com.landawn.abacus.query.SQLBuilder;
import org.apache.ofbiz.persistence.entity.PostalAddressEntity;

public interface PostalAddressDao extends CrudDao<PostalAddressEntity, String, SQLBuilder.PSC, PostalAddressDao> {
}
