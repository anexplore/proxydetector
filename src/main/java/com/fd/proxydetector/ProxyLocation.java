package com.fd.proxydetector;

public class ProxyLocation {

    // 位置信息
    /*国家 中国*/
    private String country = "";
    /*地区 华北*/
    private String area = "";
    /*省 北京市*/
    private String region = "";
    /*市 北京市*/
    private String city = "";
    /*县 */
    private String county = "";
    /*运营商 天地祥云*/
    private String isp = "";
    private String countryId = "";
    private String areaId = "";
    private String regionId = "";
    private String cityId = "";
    private String countyId = "";
    private String ispId = "";
    
    public String getCountry() {
        return country;
    }

    public void setCountry(String country) {
        this.country = country;
    }

    public String getArea() {
        return area;
    }

    public void setArea(String area) {
        this.area = area;
    }

    public String getRegion() {
        return region;
    }

    public void setRegion(String region) {
        this.region = region;
    }

    public String getCity() {
        return city;
    }

    public void setCity(String city) {
        this.city = city;
    }

    public String getCounty() {
        return county;
    }

    public void setCounty(String county) {
        this.county = county;
    }

    public String getIsp() {
        return isp;
    }

    public void setIsp(String isp) {
        this.isp = isp;
    }

    public String getCountryId() {
        return countryId;
    }

    public void setCountryId(String countryId) {
        this.countryId = countryId;
    }

    public String getAreaId() {
        return areaId;
    }

    public void setAreaId(String areaId) {
        this.areaId = areaId;
    }

    public String getRegionId() {
        return regionId;
    }

    public void setRegionId(String regionId) {
        this.regionId = regionId;
    }

    public String getCityId() {
        return cityId;
    }

    public void setCityId(String cityId) {
        this.cityId = cityId;
    }

    public String getCountyId() {
        return countyId;
    }

    public void setCountyId(String countyId) {
        this.countyId = countyId;
    }

    public String getIspId() {
        return ispId;
    }

    public void setIspId(String ispId) {
        this.ispId = ispId;
    }

    public String getFullLocation() {
        return String.format("%s %s %s %s %s", country == null ? "" : country, 
                area == null ? "" : area,
                region == null ? "" : region,
                city == null ? "" : city,
                county == null ? "" : county).trim();
    }

}
