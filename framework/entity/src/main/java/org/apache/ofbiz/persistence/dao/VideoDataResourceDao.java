package org.apache.ofbiz.persistence.dao;

import com.landawn.abacus.jdbc.dao.CrudDao;
import com.landawn.abacus.query.SQLBuilder;
import org.apache.ofbiz.persistence.entity.VideoDataResourceEntity;

public interface VideoDataResourceDao extends CrudDao<VideoDataResourceEntity, String, SQLBuilder.PSC, VideoDataResourceDao> {
}
