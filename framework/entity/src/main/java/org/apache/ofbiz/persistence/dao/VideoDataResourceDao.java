package org.apache.ofbiz.persistence.dao;

import com.landawn.abacus.jdbc.dao.CrudDao;
import com.landawn.abacus.query.SqlBuilder;
import org.apache.ofbiz.persistence.entity.VideoDataResourceEntity;

public interface VideoDataResourceDao extends CrudDao<VideoDataResourceEntity, String, SqlBuilder.PSC, VideoDataResourceDao> {
}
