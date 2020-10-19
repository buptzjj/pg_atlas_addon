package com.haohandata.httpclient;

import lombok.Data;

@Data
public class RemoteResponse {
    private String ip;  //跳板IP
    private int port;  //跳板端口
    private String response;   //该跳板返回的结果，JSON字符串
}
