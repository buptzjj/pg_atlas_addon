package com.haohandata;

import com.google.gson.annotations.SerializedName;

import lombok.Data;
import lombok.experimental.SuperBuilder;

@SuperBuilder
@Data
public class DDLEvent {
    @SerializedName("event")
    private String eventType;
    @SerializedName("tag")
    private String eventTag;

    @SerializedName("classid")
    private int classId;
    @SerializedName("objid")
    private int objId;
    @SerializedName("objecttype")
    private String objectType;
    @SerializedName("schemaname")
    private String schemaName;
    @SerializedName("objectidentity")
    private String objectIdentity;
}
