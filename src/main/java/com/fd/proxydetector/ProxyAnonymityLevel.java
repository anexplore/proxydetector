package com.fd.proxydetector;

public enum ProxyAnonymityLevel {
    TRANSPARENT("透明"), //透明
    ANONYMOUS("匿名"), //匿名
    ELITE("高匿名"), //高匿名
    UNKNOWN("未知"); //未知
    
    private String level;
    
    private ProxyAnonymityLevel(String level) {
        this.level = level;
    }
    
    public String toString() {
        return level;
    }
}
