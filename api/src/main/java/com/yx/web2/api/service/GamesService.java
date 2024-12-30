package com.yx.web2.api.service;


import com.yx.pass.remote.pms.model.resp.sepc.ConfigContainerSpecResp;
import com.yx.pass.remote.pms.model.resp.sepc.SpecPriceResp;
import com.yx.web2.api.common.req.games.AppDeployStrategyDto;
import com.yx.web2.api.common.req.games.ConfigContainerSpecTypeDto;
import org.yx.lib.utils.util.R;

import javax.servlet.http.HttpServletRequest;
import java.util.List;

public interface GamesService {

    R<List<ConfigContainerSpecResp>> specList(ConfigContainerSpecTypeDto configContainerSpecTypeDto);

    R<?> addAppDeploy(AppDeployStrategyDto appDeployStrategyDto);

    R<?> pageAppDeploy(Integer current, Integer size, Long appId, Long tid, String regionCode, Integer enable, String query, String state, HttpServletRequest request, Integer appVersion, String listType);

    R<?> testUrl(Long id);
}
