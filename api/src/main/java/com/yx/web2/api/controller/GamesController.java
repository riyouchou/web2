package com.yx.web2.api.controller;


import com.yx.pass.remote.pms.model.resp.sepc.ConfigContainerSpecResp;
import com.yx.web2.api.common.constant.RequestAttributeConstants;
import com.yx.web2.api.common.req.games.AppDeployStrategyDto;
import com.yx.web2.api.common.req.games.ConfigContainerSpecTypeDto;
import com.yx.web2.api.service.GamesService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import org.yx.lib.utils.util.R;

import javax.servlet.http.HttpServletRequest;
import java.util.List;


@RestController
@RequestMapping("/games")
@RequiredArgsConstructor
public class GamesController {

    private final GamesService gamesService;

    /**
     * 查询规格列表
     *
     * @return R
     */
    @PostMapping("/specList")
    public R<List<ConfigContainerSpecResp>> specList(
            @RequestBody ConfigContainerSpecTypeDto configContainerSpecTypeDto) {
        return gamesService.specList(configContainerSpecTypeDto);
    }

    /**
     * 添加游戏部署
     *
     * @return R
     */
    @PostMapping("/addAppDeploy")
    public R<?> addAppDeploy(@RequestBody AppDeployStrategyDto appDeployStrategyDto) {
        return gamesService.addAppDeploy(appDeployStrategyDto);
    }

    /**
     * 添加游戏部署
     *
     * @return R
     */
    @GetMapping("/pageAppDeploy")
    public R<?> pageAppDeploy(Integer current, Integer size,
                              @RequestParam(required = false) Long appId,
                              @RequestParam(required = false) Long tid,
                              @RequestParam(required = false) String regionCode,
                              @RequestParam(required = false) Integer enable,
                              @RequestParam(required = false) String query,
                              @RequestParam(required = false) String state,
                              @RequestParam(required = false) Integer appVersion,
                              @RequestParam(required = false) String listType,
                              @RequestAttribute(RequestAttributeConstants.TENANT_ID) Long tenantId,
                              HttpServletRequest request) {
        return gamesService.pageAppDeploy(current, size, appId, tenantId, regionCode, enable, query, state, request, appVersion, listType);
    }

    /**
     * 获取测试地址
     *
     * @return R
     */
    @GetMapping("/testUrl")
    public R<?> testUrl(Long id) {
        return gamesService.testUrl(id);
    }

}
