package com.yx.web2.api.mapper;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.toolkit.Constants;
import com.yx.web2.api.entity.SubscribedServiceEntity;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface SubscribedServiceMapper extends BaseMapper<SubscribedServiceEntity> {
    @Select("SELECT b.*, a.tenant_name AS tenantName FROM t_order a INNER JOIN t_subscribed_service b ON " +
            "a.order_id = b.order_id AND a.order_status = 5 AND a.deleted = 0 ${ew.customSqlSegment}")
    List<SubscribedServiceEntity> listServiceEndSubscribedList(@Param(Constants.WRAPPER) Wrapper queryWrapper);
}
