package com.yx.web2.api.common.req.games;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * spec 对应的推荐游戏列表
 * @author yijian
 * @date 2024/9/9 17:49
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class SpecGameDto {
    private String spec;

    private List<String> games;
}
