package com.yx.web2.api.controller.filter;

import cn.hutool.http.ContentType;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.yx.pass.remote.pms.PmsRemoteAccountService;
import com.yx.web2.api.common.constant.RequestAttributeConstants;
import com.yx.web2.api.common.constant.Web2LoggerEvents;
import com.yx.web2.api.common.enums.AccountType;
import com.yx.web2.api.common.enums.SysCode;
import com.yx.web2.api.common.model.AccountModel;
import com.yx.web2.api.config.Web2ApiConfig;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.annotation.Order;
import org.springframework.util.AntPathMatcher;
import org.springframework.util.PathMatcher;
import org.yx.lib.utils.logger.KvLogger;
import org.yx.lib.utils.logger.LogFieldConstants;
import org.yx.lib.utils.util.Base64Util;
import org.yx.lib.utils.util.R;
import org.yx.lib.utils.util.StringUtil;

import javax.servlet.*;
import javax.servlet.annotation.WebFilter;
import javax.servlet.annotation.WebInitParam;
import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Arrays;

@WebFilter(filterName = "TokenFilter",
        /*通配符（*）表示对所有的web资源进行拦截*/
        urlPatterns = "/*",
        initParams = {
                /*这里可以放一些初始化的参数*/
                @WebInitParam(name = "charset", value = "utf-8")
        })
@Order(1)
@Slf4j
@RequiredArgsConstructor
public class TokenFilter implements Filter {
    private final PmsRemoteAccountService pmsRemoteAccountService;
    private final Web2ApiConfig web2ApiConfig;

    @Override
    public void init(FilterConfig filterConfig) throws ServletException {
        KvLogger.instance(this).p(LogFieldConstants.EVENT, Web2LoggerEvents.TOKEN_FILTER_EVENT)
                .p(LogFieldConstants.ACTION, Web2LoggerEvents.Actions.TOKEN_FILTER_EVENT_ACTION_INIT).i();
    }

    @Override
    public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse, FilterChain filterChain)
            throws IOException, ServletException {
        HttpServletRequest httpRequest = (HttpServletRequest) servletRequest;
        if (web2ApiConfig.getTokenSkipPatterns() != null && web2ApiConfig.getTokenSkipPatterns().length > 0) {
            PathMatcher matcher = new AntPathMatcher();
            boolean isMatch = Arrays.stream(web2ApiConfig.getTokenSkipPatterns()).anyMatch(pattern -> matcher.match(pattern, httpRequest.getRequestURI()));
            if (isMatch) {
                filterChain.doFilter(servletRequest, servletResponse);
                return;
            }
        }
//        String xUser = httpRequest.getHeader("x-user");
//        if (StringUtil.isBlank(xUser)) {
//            KvLogger.instance(this).p(LogFieldConstants.EVENT, Web2LoggerEvents.TOKEN_FILTER_EVENT)
//                    .p(LogFieldConstants.ACTION, Web2LoggerEvents.Actions.TOKEN_FILTER_EVENT_ACTION_DO_FILTER)
//                    .p(LogFieldConstants.ERR_MSG, "x-user is null")
//                    .i();
//            write403(servletResponse);
//            return;
//        }
//        String decode = Base64Util.decode(xUser);
//        JSONObject jsonObject = JSON.parseObject(decode);
//        Long uid = jsonObject.getLong("uid");
//        String privyId = jsonObject.getString("privyId");
//
//        if (uid == null) {
//            KvLogger.instance(this).p(LogFieldConstants.EVENT, Web2LoggerEvents.TOKEN_FILTER_EVENT)
//                    .p(LogFieldConstants.ACTION, Web2LoggerEvents.Actions.TOKEN_FILTER_EVENT_ACTION_DO_FILTER)
//                    .p(LogFieldConstants.ERR_MSG, "get uid from x-user is null")
//                    .i();
//            write403(servletResponse);
//            return;
//        }
//        R<String> accountR = pmsRemoteAccountService.getLoginAccountInfo(privyId);
//        if (accountR.getCode() != R.ok().getCode() || StringUtil.isBlank(accountR.getData())) {
//            KvLogger.instance(this).p(LogFieldConstants.EVENT, Web2LoggerEvents.TOKEN_FILTER_EVENT)
//                    .p(LogFieldConstants.ACTION, Web2LoggerEvents.Actions.TOKEN_FILTER_EVENT_ACTION_DO_FILTER)
//                    .p(LogFieldConstants.ERR_MSG, "get loginUser from pms is null, privyId:" + privyId)
//                    .i();
//            if (accountR.getCode() == 103503) {
//                write(servletResponse, accountR);
//            } else {
//                write401(servletResponse);
//            }
//            return;
//        }
//        AccountModel accountModel = JSONObject.parseObject(accountR.getData(), AccountModel.class);

        AccountModel accountModel = new AccountModel();
        accountModel.setAccountId(174L);
        accountModel.setAccountName("12345678@qq.com");
        accountModel.setTenantId(20247L);
        accountModel.setTenantName("admin");
        accountModel.setTenantType("GP");
        accountModel.setBdAccountId(1212L);
        accountModel.setBdAccountName("yechao.li@sunwayig.com");
        accountModel.setAccountType(AccountType.AI_BD.getValue());

        servletRequest.setAttribute(RequestAttributeConstants.TENANT_ID, accountModel.getTenantId());
        servletRequest.setAttribute(RequestAttributeConstants.ACCOUNT_INFO, accountModel);
        filterChain.doFilter(servletRequest, servletResponse);
    }

    @Override
    public void destroy() {
        KvLogger.instance(this).p(LogFieldConstants.EVENT, Web2LoggerEvents.TOKEN_FILTER_EVENT)
                .p(LogFieldConstants.ACTION, Web2LoggerEvents.Actions.TOKEN_FILTER_EVENT_ACTION_DESTROY).i();
    }

    private void write401(ServletResponse servletResponse) throws IOException {
        servletResponse.setContentType(ContentType.JSON.getValue());
        PrintWriter printWriter = servletResponse.getWriter();
        printWriter.write(JSON.toJSONString(R.failed(SysCode.x00000401.getValue(), SysCode.x00000401.getMsg())));
        printWriter.flush();
    }

    private void write403(ServletResponse servletResponse) throws IOException {
        servletResponse.setContentType(ContentType.JSON.getValue());
        PrintWriter printWriter = servletResponse.getWriter();
        printWriter.write(JSON.toJSONString(R.failed(SysCode.x00000403.getValue(), SysCode.x00000403.getMsg())));
        printWriter.flush();
    }

    private void write(ServletResponse servletResponse, R<?> data) throws IOException {
        servletResponse.setContentType(ContentType.JSON.getValue());
        PrintWriter printWriter = servletResponse.getWriter();
        printWriter.write(JSON.toJSONString(data));
        printWriter.flush();
    }
}