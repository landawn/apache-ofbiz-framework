package org.apache.ofbiz.persistence.dao;

import com.landawn.abacus.jdbc.dao.CrudDao;
import com.landawn.abacus.query.SqlBuilder;
import org.apache.ofbiz.persistence.entity.AudioDataResourceEntity;

public interface AudioDataResourceDao extends CrudDao<AudioDataResourceEntity, String, SqlBuilder.PSC, AudioDataResourceDao> {
}
