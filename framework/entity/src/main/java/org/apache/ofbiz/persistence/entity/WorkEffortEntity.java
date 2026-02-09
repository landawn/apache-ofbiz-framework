package org.apache.ofbiz.persistence.entity;

import com.landawn.abacus.annotation.Column;
import com.landawn.abacus.annotation.Entity;
import com.landawn.abacus.annotation.Id;
import com.landawn.abacus.annotation.Table;
import java.math.BigDecimal;
import java.sql.Timestamp;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
@Entity(name = "WORK_EFFORT")
@Table(name = "WORK_EFFORT")
public class WorkEffortEntity {
    @Id
    @Column(name = "WORK_EFFORT_ID")
    private String workEffortId;

    @Column(name = "WORK_EFFORT_TYPE_ID")
    private String workEffortTypeId;

    @Column(name = "CURRENT_STATUS_ID")
    private String currentStatusId;

    @Column(name = "LAST_STATUS_UPDATE")
    private Timestamp lastStatusUpdate;

    @Column(name = "WORK_EFFORT_PURPOSE_TYPE_ID")
    private String workEffortPurposeTypeId;

    @Column(name = "WORK_EFFORT_PARENT_ID")
    private String workEffortParentId;

    @Column(name = "SCOPE_ENUM_ID")
    private String scopeEnumId;

    @Column(name = "PRIORITY")
    private BigDecimal priority;

    @Column(name = "PERCENT_COMPLETE")
    private BigDecimal percentComplete;

    @Column(name = "WORK_EFFORT_NAME")
    private String workEffortName;

    @Column(name = "SHOW_AS_ENUM_ID")
    private String showAsEnumId;

    @Column(name = "SEND_NOTIFICATION_EMAIL")
    private String sendNotificationEmail;

    @Column(name = "DESCRIPTION")
    private String description;

    @Column(name = "LOCATION_DESC")
    private String locationDesc;

    @Column(name = "ESTIMATED_START_DATE")
    private Timestamp estimatedStartDate;

    @Column(name = "ESTIMATED_COMPLETION_DATE")
    private Timestamp estimatedCompletionDate;

    @Column(name = "ACTUAL_START_DATE")
    private Timestamp actualStartDate;

    @Column(name = "ACTUAL_COMPLETION_DATE")
    private Timestamp actualCompletionDate;

    @Column(name = "ESTIMATED_MILLI_SECONDS")
    private Double estimatedMilliSeconds;

    @Column(name = "ESTIMATED_SETUP_MILLIS")
    private Double estimatedSetupMillis;

    @Column(name = "ESTIMATE_CALC_METHOD")
    private String estimateCalcMethod;

    @Column(name = "ACTUAL_MILLI_SECONDS")
    private Double actualMilliSeconds;

    @Column(name = "ACTUAL_SETUP_MILLIS")
    private Double actualSetupMillis;

    @Column(name = "TOTAL_MILLI_SECONDS_ALLOWED")
    private Double totalMilliSecondsAllowed;

    @Column(name = "TOTAL_MONEY_ALLOWED")
    private BigDecimal totalMoneyAllowed;

    @Column(name = "MONEY_UOM_ID")
    private String moneyUomId;

    @Column(name = "SPECIAL_TERMS")
    private String specialTerms;

    @Column(name = "TIME_TRANSPARENCY")
    private BigDecimal timeTransparency;

    @Column(name = "UNIVERSAL_ID")
    private String universalId;

    @Column(name = "SOURCE_REFERENCE_ID")
    private String sourceReferenceId;

    @Column(name = "FIXED_ASSET_ID")
    private String fixedAssetId;

    @Column(name = "FACILITY_ID")
    private String facilityId;

    @Column(name = "INFO_URL")
    private String infoUrl;

    @Column(name = "RECURRENCE_INFO_ID")
    private String recurrenceInfoId;

    @Column(name = "TEMP_EXPR_ID")
    private String tempExprId;

    @Column(name = "RUNTIME_DATA_ID")
    private String runtimeDataId;

    @Column(name = "NOTE_ID")
    private String noteId;

    @Column(name = "SERVICE_LOADER_NAME")
    private String serviceLoaderName;

    @Column(name = "QUANTITY_TO_PRODUCE")
    private BigDecimal quantityToProduce;

    @Column(name = "QUANTITY_PRODUCED")
    private BigDecimal quantityProduced;

    @Column(name = "QUANTITY_REJECTED")
    private BigDecimal quantityRejected;

    @Column(name = "RESERV_PERSONS")
    private BigDecimal reservPersons;

    @Column(name = "RESERV2ND_P_P_PERC")
    private BigDecimal reserv2ndPPPerc;

    @Column(name = "RESERV_NTH_P_P_PERC")
    private BigDecimal reservNthPPPerc;

    @Column(name = "ACCOMMODATION_MAP_ID")
    private String accommodationMapId;

    @Column(name = "ACCOMMODATION_SPOT_ID")
    private String accommodationSpotId;

    @Column(name = "REVISION_NUMBER")
    private BigDecimal revisionNumber;

    @Column(name = "CREATED_DATE")
    private Timestamp createdDate;

    @Column(name = "CREATED_BY_USER_LOGIN")
    private String createdByUserLogin;

    @Column(name = "LAST_MODIFIED_DATE")
    private Timestamp lastModifiedDate;

    @Column(name = "LAST_MODIFIED_BY_USER_LOGIN")
    private String lastModifiedByUserLogin;
}
