package com.yx.web2.api.service.impl;

import com.alibaba.fastjson.JSONObject;
import com.alibaba.fastjson.TypeReference;
import com.yx.pass.remote.pms.PmsRemoteSpecService;
import com.yx.pass.remote.pms.model.resp.sepc.ConfigContainerSpecResp;
import com.yx.web2.api.common.req.games.AppDeployStrategyDto;
import com.yx.web2.api.common.req.games.ConfigContainerSpecTypeDto;
import com.yx.web2.api.common.req.games.SpecGameDto;
import com.yx.web2.api.config.Web2ApiConfig;
import com.yx.web2.api.service.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.yx.lib.utils.util.R;

import javax.servlet.http.HttpServletRequest;
import java.util.ArrayList;
import java.util.List;

@Service
@Slf4j
@RequiredArgsConstructor
public class GamesServiceImpl implements GamesService {

    private final PmsRemoteSpecService pmsRemoteSpecService;

    private final Web2ApiConfig web2ApiConfig;

    @Override
    public R<List<ConfigContainerSpecResp>> specList(ConfigContainerSpecTypeDto configContainerSpecTypeDto) {
        List<ConfigContainerSpecResp> listSpec = pmsRemoteSpecService.specListByOsType(configContainerSpecTypeDto.getPlatformType()).getData();
        String adaptableGames = web2ApiConfig.getAdaptableGames();
        List<SpecGameDto> games = JSONObject.parseObject(adaptableGames, new TypeReference<List<SpecGameDto>>() {
        });
        listSpec.stream().forEach(configContainerSpecResp -> {
            configContainerSpecResp.setAdaptableGames(games.parallelStream().filter(mapping -> configContainerSpecResp.getSpec().equals(mapping.getSpec())).findFirst().map(SpecGameDto::getGames).orElse(new ArrayList<>()));
        });
        return R.ok(listSpec);
    }

    @Override
    public R<?> addAppDeploy(AppDeployStrategyDto appDeployStrategyDto) {
        return pmsRemoteSpecService.addAppDeploy(appDeployStrategyDto.getAppId(), appDeployStrategyDto.getRegionCode(), appDeployStrategyDto.getOrderCode(),
                appDeployStrategyDto.getTargetVersion(), appDeployStrategyDto.getContainerTargetCount(), appDeployStrategyDto.getTid());
    }

    @Override
    public R<?> pageAppDeploy(Integer current, Integer size, Long appId, Long tid, String regionCode, Integer enable, String query, String state, HttpServletRequest request, Integer appVersion, String listType) {
        return pmsRemoteSpecService.pageAppDeploy(current, size, appId, tid, regionCode, enable, query, state, request, appVersion, listType);
    }

    @Override
    public R<?> testUrl(Long id) {
        return pmsRemoteSpecService.testUrl(id);
    }
}
