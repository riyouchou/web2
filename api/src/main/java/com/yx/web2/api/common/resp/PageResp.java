package com.yx.web2.api.common.resp;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class PageResp<T> {
    List<T> records;
    private Long total;
    private Long size;
    private Long current;
    private Long pages;
}
