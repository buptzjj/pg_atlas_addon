package com.haohandata;

import lombok.Data;
import lombok.experimental.SuperBuilder;

/**
 * Altas中存储的Entity元数据信息
 */
@SuperBuilder
@Data
public class EntityModel {
    private String guid;
    private String qualifiedName;
}
