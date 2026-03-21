package org.apache.ofbiz.persistence.dao;

import com.landawn.abacus.jdbc.dao.CrudDao;
import com.landawn.abacus.query.SqlBuilder;
import org.apache.ofbiz.persistence.entity.MarketingCampaignPriceEntity;

public interface MarketingCampaignPriceDao extends CrudDao<MarketingCampaignPriceEntity, MarketingCampaignPriceEntity, SqlBuilder.PSC, MarketingCampaignPriceDao> {
}
