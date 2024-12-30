package com.yx.web2.api.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
@RefreshScope
@Data
@ConfigurationProperties(prefix = "web2")
public class Web2ApiConfig {
    private Long athAdminTid = 10000L;
    private String athAdminName = "admin";
    private String[] tokenSkipPatterns;

    private OrderConfig order;

    private InvoiceConfig billInvoice;

    private Double unitPriceCoefficient;

    private String adaptableGames;

    private ContractConfig contract;

    private String gpuManufacturer;

    @Data
    @RefreshScope
    public static class OrderConfig {
        private Integer idempotentInterval = 1;
    }

    @Data
    @RefreshScope
    public static class InvoiceConfig {
        private String billTo;
        private String bankName;
        private String ifsCode;
        private String swiftCode;
        private String account;
        private String email;
    }

    @Data
    @RefreshScope
    public static class ContractConfig {
        private boolean useDefaultTemplate = true;
        private String templatePath = "/data/pass/web2-api/contract/template";
        private String tenantPath = "/data/pass/web2-api/contract/tenant";
        private String sendEmailSubject = "Please sign this document set Aethir Earth MSA";
        private String sendEmailDocument = "Aethir Earth MSA";
        private Integer idempotentInterval = 40;
        private List<String> countries;
        private SignConfig signConfig = new SignConfig();
    }

    @Data
    @RefreshScope
    public static class SignConfig {
        private String fullSignOneAnchor = "[SIGNER1]";
        private String fullSignOneAnchorXOffset = "-150";
        private String fullSignOneAnchorYOffset = "-12";

        private String fullSignTwoAnchor = "[SIGNER2]";
        private String fullSignTwoAnchorXOffset = "-150";
        private String fullSignTwoAnchorYOffset = "-12";

        private String smartSignAnchor = "[SIGNER1]";
        private String smartSignAnchorXOffset = "-150";
        private String smartSignAnchorYOffset = "-12";
    }
}
