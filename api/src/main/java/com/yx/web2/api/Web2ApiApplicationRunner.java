package com.yx.web2.api;

import com.yx.web2.api.common.constant.Web2LoggerEvents;
import com.yx.web2.api.config.Web2ApiConfig;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;
import org.yx.lib.utils.logger.KvLogger;
import org.yx.lib.utils.logger.LogFieldConstants;

import java.io.File;
import java.io.InputStreamReader;
import java.util.Properties;

@Slf4j
@Component
@RequiredArgsConstructor
public class Web2ApiApplicationRunner implements ApplicationRunner {

    private final Web2ApiConfig web2ApiConfig;

    @Override
    public void run(ApplicationArguments args) {
        try {
            Resource resource = new ClassPathResource("git.properties");
            Properties properties = new Properties();
            properties.load(new InputStreamReader(resource.getInputStream()));
            KvLogger.instance(this)
                    .p(LogFieldConstants.EVENT, Web2LoggerEvents.APPLICATION_RUNNER_EVENT)
                    .p(LogFieldConstants.ACTION, Web2LoggerEvents.Actions.APPLICATION_RUNNER_EVENT_INIT)
                    .p("ProjectGitInfo", properties)
                    .i();
            initConfig();
        } catch (Exception ex) {
            KvLogger.instance(this)
                    .p(LogFieldConstants.EVENT, Web2LoggerEvents.APPLICATION_RUNNER_EVENT)
                    .p(LogFieldConstants.ACTION, Web2LoggerEvents.Actions.APPLICATION_RUNNER_EVENT_INIT)
                    .p(LogFieldConstants.ERR_MSG, ex.getMessage())
                    .e(ex);
        }
    }

    private void initConfig() {
        File contractTemplatePath = new File(web2ApiConfig.getContract().getTemplatePath());
        if (!contractTemplatePath.exists()) {
            if (contractTemplatePath.mkdirs()) {
                KvLogger.instance(this)
                        .p(LogFieldConstants.EVENT, Web2LoggerEvents.APPLICATION_RUNNER_EVENT)
                        .p(LogFieldConstants.ACTION, Web2LoggerEvents.Actions.APPLICATION_RUNNER_EVENT_INIT)
                        .p("CreateContractTemplatePath", "Success")
                        .p("ContractTemplatePath", contractTemplatePath)
                        .i();
            }
        }
        File contractTmpPath = new File(web2ApiConfig.getContract().getTenantPath());
        if (!contractTmpPath.exists()) {
            if (contractTmpPath.mkdirs()) {
                KvLogger.instance(this)
                        .p(LogFieldConstants.EVENT, Web2LoggerEvents.APPLICATION_RUNNER_EVENT)
                        .p(LogFieldConstants.ACTION, Web2LoggerEvents.Actions.APPLICATION_RUNNER_EVENT_INIT)
                        .p("CreateContractTmpPath", "Success")
                        .p("ContractTmpPath", contractTmpPath)
                        .i();
            }
        }
    }
}
