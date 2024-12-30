package com.yx.web2.api.common.resp.order;

import com.baomidou.mybatisplus.annotation.TableField;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.yx.web2.api.entity.OrderDeviceEntity;
import lombok.*;
import org.springframework.cloud.context.config.annotation.RefreshScope;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.util.Date;
import java.util.List;

@Getter
@Setter
@Builder
@Data
@NoArgsConstructor
@AllArgsConstructor
@RefreshScope
public class OrderInvoiceResp {

    private String clientName;

    private String clientAddress;

    private String clientContactEmailAddress;

    private String invoiceNo;

    private String poNo;
    @JsonFormat(timezone = "UTC")
    private Timestamp date;
    @JsonFormat(timezone = "UTC")
    private Timestamp dueDate;

    private String terms;

    private BigDecimal totalAmount;

    private BigDecimal balanceDue;

    private Integer instalmentMonth;

    private Integer instalmentMonthTotal;

    private BigDecimal prePaidAmount;
    @JsonFormat(timezone = "UTC")
    private Timestamp serviceBeginTime;
    @JsonFormat(timezone = "UTC")
    private Timestamp serviceEndTime;

    private String description;

    private List<OrderDeviceEntity> orderDevices;
}
