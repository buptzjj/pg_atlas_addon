package com.haohandata.httpclient;

public class HttpParam<T> {
    private String paramName;
    private T paramValue;

    public String getParamName() {
        return paramName;
    }

    public void setParamName(String paramName) {
        this.paramName = paramName;
    }

    public T getParamValue() {
        return paramValue;
    }

    public void setParamValue(T paramValue) {
        this.paramValue = paramValue;
    }

    public HttpParam() {
    }
    
    public HttpParam(String paramName, T paramValue) {
        this.paramName = paramName;
        this.paramValue = paramValue;
    }
}

