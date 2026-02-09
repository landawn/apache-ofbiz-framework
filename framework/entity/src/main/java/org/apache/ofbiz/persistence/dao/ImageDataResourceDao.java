package org.apache.ofbiz.persistence.dao;

import com.landawn.abacus.jdbc.dao.CrudDao;
import com.landawn.abacus.query.SQLBuilder;
import org.apache.ofbiz.persistence.entity.ImageDataResourceEntity;

public interface ImageDataResourceDao extends CrudDao<ImageDataResourceEntity, String, SQLBuilder.PSC, ImageDataResourceDao> {
}
