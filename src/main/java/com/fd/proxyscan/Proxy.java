package com.fd.proxyscan;

import java.util.Date;

public class Proxy {
    private ProxyProtocol protocol;
	private String host;
    private int port;
    private double speed = 0.0;
    private short beyondGfw;
    private Date foundDate;
    private Date lastCheckDate;
    private short isValid = 1;
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

	@Override
	public String toString() {
		return "Proxy [protocol=" + protocol + ", host=" + host + ", port=" + port + ", speed=" + speed + ", beyondGfw="
				+ beyondGfw + ", foundDate=" + foundDate + ", lastCheckDate=" + lastCheckDate + ", isValid=" + isValid
				+ ", level=" + level + ", district=" + district + ", country=" + country + ", province=" + province
				+ ", city=" + city + ", carrier=" + carrier + "]";
	}

	public synchronized ProxyProtocol getProtocol() {
		return protocol;
	}

	public synchronized Proxy setProtocol(ProxyProtocol protocol) {
		this.protocol = protocol;
		return this;
	}

	public synchronized String getHost() {
		return host;
	}

	public synchronized Proxy setHost(String host) {
		this.host = host;
		return this;
	}

	public synchronized int getPort() {
		return port;
	}

	public synchronized Proxy setPort(int port) {
		this.port = port;
		return this;
	}

	public synchronized double getSpeed() {
		return speed;
	}

	public synchronized Proxy setSpeed(double speed) {
		this.speed = speed;
		return this;
	}

	public synchronized short getBeyondGfw() {
		return beyondGfw;
	}

	public synchronized Proxy setBeyondGfw(short beyondGfw) {
		this.beyondGfw = beyondGfw;
		return this;
	}

	public synchronized Date getFoundDate() {
		return foundDate;
	}

	public synchronized Proxy setFoundDate(Date foundDate) {
		this.foundDate = foundDate;
		return this;
	}

	public synchronized Date getLastCheckDate() {
		return lastCheckDate;
	}

	public synchronized Proxy setLastCheckDate(Date lastCheckDate) {
		this.lastCheckDate = lastCheckDate;
		return this;
	}

	public synchronized short getIsValid() {
		return isValid;
	}

	public synchronized Proxy setIsValid(short isValid) {
		this.isValid = isValid;
		return this;
	}

	public synchronized String getLevel() {
		return level;
	}

	public synchronized Proxy setLevel(String level) {
		this.level = level;
		return this;
	}

	public synchronized String getDistrict() {
		return district;
	}

	public synchronized Proxy setDistrict(String district) {
		this.district = district;
		return this;
	}

	public synchronized String getCountry() {
		return country;
	}

	public synchronized Proxy setCountry(String country) {
		this.country = country;
		return this;
	}

	public synchronized String getProvince() {
		return province;
	}

	public synchronized Proxy setProvince(String province) {
		this.province = province;
		return this;
	}

	public synchronized String getCity() {
		return city;
	}

	public synchronized Proxy setCity(String city) {
		this.city = city;
		return this;
	}

	public synchronized String getCarrier() {
		return carrier;
	}

	public synchronized Proxy setCarrier(String carrier) {
		this.carrier = carrier;
		return this;
	}
}
