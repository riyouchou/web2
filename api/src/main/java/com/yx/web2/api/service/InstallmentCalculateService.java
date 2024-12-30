package com.yx.web2.api.service;

import com.alibaba.fastjson.JSON;
import com.google.common.collect.Lists;
import com.yx.web2.api.common.constant.Web2LoggerEvents;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.stereotype.Service;
import org.yx.lib.utils.logger.KvLogger;
import org.yx.lib.utils.logger.LogFieldConstants;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class InstallmentCalculateService {
    public InstallmentOutput calculate(InstallmentInput input) {
        InstallmentOutput output = new InstallmentOutput();

        long totalHours = ServiceDurationCalculate.calculate(input.getServiceDuration(), input.serviceDurationPeriod);

        KvLogger.instance(this)
                .p(LogFieldConstants.EVENT, Web2LoggerEvents.INSTALLMENT_CALCULATE_EVENT)
                .p(LogFieldConstants.ACTION, Web2LoggerEvents.Actions.INSTALLMENT_CALCULATE_EVENT_CALCULATE)
                .p("订单周期(Hours)", totalHours)
                .p("免费测试周期(Hours)", input.getFreeServiceTermHours())
                .p("BD设置的订单预付款金额", input.getPrePaymentPrice())
                .d();

        // 分期数=订单周期/730,不能整除+1
        BigDecimal instalmentMonthly = new BigDecimal(totalHours).divide(new BigDecimal(730), 0, RoundingMode.UP);

        KvLogger.instance(this)
                .p(LogFieldConstants.EVENT, Web2LoggerEvents.INSTALLMENT_CALCULATE_EVENT)
                .p(LogFieldConstants.ACTION, Web2LoggerEvents.Actions.INSTALLMENT_CALCULATE_EVENT_CALCULATE)
                .p("Formula", "分期数=订单周期/730,不能整除+1")
                .p("分期数(月)", instalmentMonthly)
                .d();

        BigDecimal totalAmount = input.getTotalAmount();
        if (totalAmount == null) {
            // 合同额（订单总价）=【订单周期（小时）-免费测试周期（小时）】*BD设定的单价*数量，保留小数点后两位，超过两位则进位
            BigDecimal bdSetPriceAmount = new BigDecimal("0.0").setScale(2, RoundingMode.UP);
            for (InstallmentInput.InstallmentInputDetail installmentDetail : input.getInputDetails()) {
                BigDecimal unitPriceAmount = new BigDecimal(String.valueOf(installmentDetail.getQuantity())).multiply(new BigDecimal(installmentDetail.getUnitPrice()));
                bdSetPriceAmount = bdSetPriceAmount.add(unitPriceAmount);
            }
            totalAmount = (new BigDecimal(String.valueOf((totalHours - input.getFreeServiceTermHours())))).multiply(bdSetPriceAmount).setScale(2, RoundingMode.UP);

            KvLogger.instance(this)
                    .p(LogFieldConstants.EVENT, Web2LoggerEvents.INSTALLMENT_CALCULATE_EVENT)
                    .p(LogFieldConstants.ACTION, Web2LoggerEvents.Actions.INSTALLMENT_CALCULATE_EVENT_CALCULATE)
                    .p("Formula", "合同额（订单总价）=【订单周期（小时）-免费测试周期（小时）】*BD设定的单价*数量，保留小数点后两位，超过两位则进位")
                    .p("合同额（订单总价）", totalAmount)
                    .d();
        }

        // 每月分期金额=总金额/分期数，保留小数点后2位，除不尽余额加到最后一期，显示的分期金额使用第一个月的分期金额即可
        BigDecimal monthlyInstalmentPrice = totalAmount.divide(instalmentMonthly, 2, RoundingMode.UP);
        KvLogger.instance(this)
                .p(LogFieldConstants.EVENT, Web2LoggerEvents.INSTALLMENT_CALCULATE_EVENT)
                .p(LogFieldConstants.ACTION, Web2LoggerEvents.Actions.INSTALLMENT_CALCULATE_EVENT_CALCULATE)
                .p("Formula", "每月分期金额=总金额/分期数，保留小数点后2位，除不尽余额加到最后一期，显示的分期金额使用第一个月的分期金额即可")
                .p("每月分期金额", monthlyInstalmentPrice)
                .d();

        // 月余额实际付费金额=每月分期金额-定金金额/分期数(定金金额/分期数保留小数点后两位，除不尽余额加到最后一期)
        BigDecimal monthlyPrePayment = (input.getPrePaymentPrice()).divide(instalmentMonthly, 2, RoundingMode.UP);
        KvLogger.instance(this)
                .p(LogFieldConstants.EVENT, Web2LoggerEvents.INSTALLMENT_CALCULATE_EVENT)
                .p(LogFieldConstants.ACTION, Web2LoggerEvents.Actions.INSTALLMENT_CALCULATE_EVENT_CALCULATE)
                .p("Formula", "定金金额/分期数(定金金额/分期数保留小数点后两位，除不尽余额加到最后一期)")
                .p("定金金额/分期数", monthlyPrePayment)
                .d();

        BigDecimal monthlyRealPayment = monthlyInstalmentPrice.subtract(monthlyPrePayment);
        KvLogger.instance(this)
                .p(LogFieldConstants.EVENT, Web2LoggerEvents.INSTALLMENT_CALCULATE_EVENT)
                .p(LogFieldConstants.ACTION, Web2LoggerEvents.Actions.INSTALLMENT_CALCULATE_EVENT_CALCULATE)
                .p("Formula", "月余额实际付费金额=每月分期金额-定金金额/分期数(定金金额/分期数保留小数点后两位，除不尽余额加到最后一期)")
                .p("月余额实际付费金额", monthlyRealPayment)
                .d();

        // 首次付费金额=定金+首月实际付费金额
        BigDecimal prePaymentAmount = input.getPrePaymentPrice().add(monthlyRealPayment);
        KvLogger.instance(this)
                .p(LogFieldConstants.EVENT, Web2LoggerEvents.INSTALLMENT_CALCULATE_EVENT)
                .p(LogFieldConstants.ACTION, Web2LoggerEvents.Actions.INSTALLMENT_CALCULATE_EVENT_CALCULATE)
                .p("Formula", "首次付费金额=定金+首月实际付费金额")
                .p("首次付费金额", prePaymentAmount)
                .d();

        // 分期总金额
        BigDecimal instalmentAmount = totalAmount.subtract(prePaymentAmount);
        KvLogger.instance(this)
                .p(LogFieldConstants.EVENT, Web2LoggerEvents.INSTALLMENT_CALCULATE_EVENT)
                .p(LogFieldConstants.ACTION, Web2LoggerEvents.Actions.INSTALLMENT_CALCULATE_EVENT_CALCULATE)
                .p("分期总金额", instalmentAmount)
                .d();

        List<InstallmentOutput.InstallmentOutputDetail> outputDetails = Lists.newArrayList(
                output.new InstallmentOutputDetail(0, input.getPrePaymentPrice(), BigDecimal.valueOf(0), true),
                output.new InstallmentOutputDetail(1, monthlyRealPayment, monthlyPrePayment, true));

        if (instalmentMonthly.intValue() - 1 == 1) {
            KvLogger.instance(this)
                    .p(LogFieldConstants.EVENT, Web2LoggerEvents.INSTALLMENT_CALCULATE_EVENT)
                    .p(LogFieldConstants.ACTION, Web2LoggerEvents.Actions.INSTALLMENT_CALCULATE_EVENT_CALCULATE)
                    .p("Stripe分期总月份", 1)
                    .p("Stripe还款金额", totalAmount.subtract(prePaymentAmount))
                    .d();
            outputDetails.add(
                    output.new InstallmentOutputDetail(2, totalAmount.subtract(prePaymentAmount), BigDecimal.valueOf(0), false)
            );
        } else if (instalmentMonthly.intValue() - 1 == 0) {
            KvLogger.instance(this)
                    .p(LogFieldConstants.EVENT, Web2LoggerEvents.INSTALLMENT_CALCULATE_EVENT)
                    .p(LogFieldConstants.ACTION, Web2LoggerEvents.Actions.INSTALLMENT_CALCULATE_EVENT_CALCULATE)
                    .p("Stripe分期总月份", 1)
                    .p("Stripe还款金额", totalAmount.subtract(prePaymentAmount))
                    .d();
        } else {
            BigDecimal tempRePaymentPriceSum = new BigDecimal("0").setScale(2, RoundingMode.UP);
            BigDecimal tempRePrePaymentPriceSum = new BigDecimal(monthlyPrePayment.toString()).setScale(2, RoundingMode.UP);
            for (int i = 0; i < instalmentMonthly.intValue() - 1; i++) {
                BigDecimal rePaymentPrice;
                BigDecimal rePrePaymentPrice;
                if (i == instalmentMonthly.intValue() - 2) {
                    rePaymentPrice = instalmentAmount.subtract(tempRePaymentPriceSum);
                    rePrePaymentPrice = input.getPrePaymentPrice().subtract(tempRePrePaymentPriceSum);
                    KvLogger.instance(this)
                            .p(LogFieldConstants.EVENT, Web2LoggerEvents.INSTALLMENT_CALCULATE_EVENT)
                            .p(LogFieldConstants.ACTION, Web2LoggerEvents.Actions.INSTALLMENT_CALCULATE_EVENT_CALCULATE)
                            .p("Month", (i + 1))
                            .p("RepaymentPrice", rePaymentPrice)
                            .d();
                } else {
                    tempRePaymentPriceSum = tempRePaymentPriceSum.add(monthlyRealPayment);
                    rePaymentPrice = monthlyRealPayment;

                    tempRePrePaymentPriceSum = tempRePrePaymentPriceSum.add(monthlyPrePayment);
                    rePrePaymentPrice = monthlyPrePayment;
                    KvLogger.instance(this)
                            .p(LogFieldConstants.EVENT, Web2LoggerEvents.INSTALLMENT_CALCULATE_EVENT)
                            .p(LogFieldConstants.ACTION, Web2LoggerEvents.Actions.INSTALLMENT_CALCULATE_EVENT_CALCULATE)
                            .p("Month", (i + 1))
                            .p("RepaymentPrice", rePaymentPrice)
                            .d();
                }
                outputDetails.add(
                        output.new InstallmentOutputDetail((i + 2), rePaymentPrice, rePrePaymentPrice, false)
                );
            }
        }
        output.setTotalAmount(totalAmount);
        output.setPrePaymentPrice(prePaymentAmount);
        output.setAvgMonthlyPaymentAmount(monthlyRealPayment);
        output.setContractAvgAmount(monthlyInstalmentPrice);
        output.setTotalInstalmentCount(instalmentMonthly.intValue());
        output.setTotalUsdInstalmentCount(instalmentMonthly.intValue() - 1);
        output.setOutputDetails(outputDetails.stream().sorted(Comparator.comparing(InstallmentOutput.InstallmentOutputDetail::getMonth)).collect(Collectors.toList()));
        KvLogger.instance(this)
                .p(LogFieldConstants.EVENT, Web2LoggerEvents.INSTALLMENT_CALCULATE_EVENT)
                .p(LogFieldConstants.ACTION, Web2LoggerEvents.Actions.INSTALLMENT_CALCULATE_EVENT_CALCULATE)
                .p("CalculateResult", JSON.toJSONString(output))
                .d();
        return output;
    }

    @Getter
    @Setter
    @AllArgsConstructor
    @NoArgsConstructor
    public static class InstallmentInput {
        public int serviceDuration;
        private int serviceDurationPeriod;
        private long freeServiceTermHours = 0;
        private BigDecimal totalAmount;
        private BigDecimal prePaymentPrice;

        private List<InstallmentInputDetail> inputDetails;

        @Getter
        @Setter
        @AllArgsConstructor
        public class InstallmentInputDetail {
            private int quantity;
            private String unitPrice;
        }
    }

    @Getter
    @Setter
    public class InstallmentOutput {
        private BigDecimal totalAmount;
        private BigDecimal prePaymentPrice;
        private BigDecimal avgMonthlyPaymentAmount;
        private BigDecimal contractAvgAmount;
        private int totalInstalmentCount;
        private int totalUsdInstalmentCount;
        private List<InstallmentOutputDetail> outputDetails;

        @Getter
        @Setter
        @AllArgsConstructor
        public class InstallmentOutputDetail {
            private int month;
            private BigDecimal hpPrice;
            private BigDecimal hpPrePaymentPrice;
            private boolean isPrePayment;
        }
    }
}
