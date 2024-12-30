package com.yx.web2.api.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.yx.web2.api.entity.ContractEntity;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface ContractMapper extends BaseMapper<ContractEntity> {

    @Select("SELECT t.* " +
            "FROM t_contract t " +
            "LEFT JOIN ath_order_info a ON t.order_id = a.order_id " +
            "LEFT JOIN t_order o ON o.order_id = t.order_id " +
            "WHERE t.started_time <= #{endDate} " +
            "AND t.started_time >= #{startDate} " +
            "AND o.order_status = 4 " +
            "AND o.deleted = 0 " +
            "AND o.order_resource_pool = 2 " +
            "AND a.order_id IS NULL ")
    List<ContractEntity> findContractsWithoutMatchingOrders(@Param("startDate") String startDate, @Param("endDate") String endDate);

}
