package com.fd.proxydetector.proxydetail;

import org.apache.commons.lang3.StringUtils;
import org.asynchttpclient.AsyncHttpClient;
import org.asynchttpclient.ListenableFuture;
import org.asynchttpclient.Response;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.fd.proxydetector.Proxy;
import com.fd.proxydetector.ProxyLocation;
import com.fd.proxydetector.http.AbstractHttpClientFactory;


public class TaobaoIpLocationService implements ProxyLocationService {
    
    public static final String TAOBAO_IP_SERVICE_URL_PREFIX = 
            "http://ip.taobao.com/service/getIpInfo.php?ip=";
    private final AsyncHttpClient httpClient;
    
    public TaobaoIpLocationService(AbstractHttpClientFactory<AsyncHttpClient> httpClientFactory) {
        httpClient = httpClientFactory.getHttpClient();
    }
    
    @Override
    public ProxyLocation lookup(Proxy proxy) {
        if (proxy == null || StringUtils.isBlank(proxy.host)) {
            return null;
        }
        String requestUrl = TAOBAO_IP_SERVICE_URL_PREFIX + proxy.host;
        try {
            ListenableFuture<Response> responseFuture = httpClient.prepareGet(requestUrl).execute();
            Response response = responseFuture.get();
            String body = response.getResponseBody();
            if (StringUtils.isEmpty(body)) {
                return null;
            }
            JSONObject jsonObj = JSON.parseObject(body);
            if (jsonObj.getIntValue("code") != 0) {
                return null;
            }
            JSONObject data = jsonObj.getJSONObject("data");
            if (data == null) {
                return null;
            }
            ProxyLocation location = new ProxyLocation();
            location.setCountry(data.getString("country"));
            location.setCountryId(data.getString("country_id"));
            location.setArea(data.getString("area"));
            location.setAreaId(data.getString("area_id"));
            location.setRegion(data.getString("region"));
            location.setRegionId(data.getString("region_id"));
            location.setCity(data.getString("city"));
            location.setCityId(data.getString("city_id"));
            location.setCounty(data.getString("county"));
            location.setCountyId(data.getString("county_id"));
            location.setIsp(data.getString("isp"));
            location.setIspId(data.getString("isp_id"));
            return location;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }
}
