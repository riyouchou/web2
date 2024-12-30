package com.yx.pass.remote.pms.model.resp.resource;

import lombok.*;
import lombok.experimental.SuperBuilder;

import java.io.Serializable;
import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class PmsOrderContainerInfoResp implements Serializable {

    private int total;

    private int current;

    private List<PmsOrderContainerPageResp> pages;

    private int size;


}
