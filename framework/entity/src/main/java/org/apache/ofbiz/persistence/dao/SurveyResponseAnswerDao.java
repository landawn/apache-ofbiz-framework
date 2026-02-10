package org.apache.ofbiz.persistence.dao;

import com.landawn.abacus.jdbc.dao.CrudDao;
import com.landawn.abacus.query.SQLBuilder;
import java.util.List;
import org.apache.ofbiz.entity.Delegator;
import org.apache.ofbiz.entity.GenericEntityException;
import org.apache.ofbiz.entity.GenericValue;
import org.apache.ofbiz.base.util.UtilMisc;
import org.apache.ofbiz.persistence.entity.SurveyResponseAnswerEntity;

public interface SurveyResponseAnswerDao extends CrudDao<SurveyResponseAnswerEntity, SurveyResponseAnswerEntity, SQLBuilder.PSC, SurveyResponseAnswerDao> {
    default List<GenericValue> listSurveyResponseAndAnswer(Delegator delegator, String orderId, String orderItemSeqId)
            throws GenericEntityException {
        return delegator.findByAnd("SurveyResponseAndAnswer", UtilMisc.toMap("orderId", orderId, "orderItemSeqId", orderItemSeqId), null, false);
    }
}
