package com.fd.proxydetector;

public enum ProxyAnonymityLevel {
    TRANSPARENT("透明"), //透明
    ANONYMOUS("匿名"), //匿名
    DISTORTING("混淆"), // 混淆
    ELITE("高匿名"); //高匿名

    private String level;
    private ProxyAnonymityLevel(String level) {
        this.level = level;
    }
    
    public String toString() {
        return level;
    }
}
