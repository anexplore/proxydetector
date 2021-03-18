package com.fd.proxyscan;

import java.util.Date;

public class Proxy {
    private ProxyProtocol protocol;
    private String host;
    private int port;
    private double speed = 0.0;
    // over great wall
    private int beyondGfw;
    private Date foundDate;
    private Date lastCheckDate;
    private int valid = 1;
    private String level;
    private String district;
    private String country;
    private String province;
    private String city;
    private String carrier;

    public Proxy(ProxyProtocol protocol, String host, int port) {
        this.protocol = protocol;
        this.host = host;
        this.port = port;
    }

    public ProxyProtocol getProtocol() {
        return protocol;
    }

    public Proxy setProtocol(ProxyProtocol protocol) {
        this.protocol = protocol;
        return this;
    }

    public String getHost() {
        return host;
    }

    public Proxy setHost(String host) {
        this.host = host;
        return this;
    }

    public int getPort() {
        return port;
    }

    public Proxy setPort(int port) {
        this.port = port;
        return this;
    }

    public double getSpeed() {
        return speed;
    }

    public Proxy setSpeed(double speed) {
        this.speed = speed;
        return this;
    }

    public int getBeyondGfw() {
        return beyondGfw;
    }

    public Proxy setBeyondGfw(int beyondGfw) {
        this.beyondGfw = beyondGfw;
        return this;
    }

    public Date getFoundDate() {
        return foundDate;
    }

    public Proxy setFoundDate(Date foundDate) {
        this.foundDate = foundDate;
        return this;
    }

    public Date getLastCheckDate() {
        return lastCheckDate;
    }

    public Proxy setLastCheckDate(Date lastCheckDate) {
        this.lastCheckDate = lastCheckDate;
        return this;
    }

    public int getValid() {
        return valid;
    }

    public Proxy setValid(int valid) {
        this.valid = valid;
        return this;
    }

    public String getLevel() {
        return level;
    }

    public Proxy setLevel(String level) {
        this.level = level;
        return this;
    }

    public String getDistrict() {
        return district;
    }

    public Proxy setDistrict(String district) {
        this.district = district;
        return this;
    }

    public String getCountry() {
        return country;
    }

    public Proxy setCountry(String country) {
        this.country = country;
        return this;
    }

    public String getProvince() {
        return province;
    }

    public Proxy setProvince(String province) {
        this.province = province;
        return this;
    }

    public String getCity() {
        return city;
    }

    public Proxy setCity(String city) {
        this.city = city;
        return this;
    }

    public String getCarrier() {
        return carrier;
    }

    public Proxy setCarrier(String carrier) {
        this.carrier = carrier;
        return this;
    }

    @Override
    public String toString() {
        return "Proxy{" +
                "protocol=" + protocol +
                ", host='" + host + '\'' +
                ", port=" + port +
                ", speed=" + speed +
                ", beyondGfw=" + beyondGfw +
                ", foundDate=" + foundDate +
                ", lastCheckDate=" + lastCheckDate +
                ", valid=" + valid +
                ", level='" + level + '\'' +
                ", district='" + district + '\'' +
                ", country='" + country + '\'' +
                ", province='" + province + '\'' +
                ", city='" + city + '\'' +
                ", carrier='" + carrier + '\'' +
                '}';
    }
}
