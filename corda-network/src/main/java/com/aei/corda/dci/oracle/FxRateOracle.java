package com.aei.corda.dci.oracle;

import co.paralleluniverse.fibers.Suspendable;
import com.jayway.jsonpath.JsonPath;
import com.mashape.unirest.http.HttpResponse;
import com.mashape.unirest.http.JsonNode;
import com.mashape.unirest.http.Unirest;
import com.mashape.unirest.http.exceptions.UnirestException;
import net.corda.core.node.ServiceHub;
import net.corda.core.node.services.CordaService;
import net.corda.core.serialization.SingletonSerializeAsToken;

import java.math.BigDecimal;
import java.util.List;

@CordaService
public class FxRateOracle extends SingletonSerializeAsToken {

    public FxRateOracle(ServiceHub serviceHub) {
    }

    @Suspendable
    public List<BigDecimal> querySpot() throws UnirestException {
        HttpResponse<JsonNode> response = getQuotes();
        String body = response.getBody().getObject().toString();
        List<List<BigDecimal>> priceResult = JsonPath.read(body, "$.Elements[0].ComponentSeries[?(@.Type=~/close/i)].Values");
        List<BigDecimal> prices = priceResult.get(0);
        return prices;
    }

    @Suspendable
    public List<BigDecimal> queryStrike() throws UnirestException {
        HttpResponse<JsonNode> response = getQuotes();
        String body = response.getBody().getObject().toString();
        List<List<BigDecimal>> priceResult = JsonPath.read(body, "$.Elements[0].ComponentSeries[?(@.Type=~/close/i)].Values");
        List<BigDecimal> prices = priceResult.get(0);
        return prices;
    }

    private HttpResponse<JsonNode> getQuotes() throws UnirestException {
        return Unirest.post("https://api.markitondemand.com/apiman-gateway/MOD/chartworks-data/1.0/chartapi/series")
                .header("Accept", "*/*")
                .header("Accept-Encoding", "gzip, deflate, br")
                .header("Accept-Language", "en-US,en;q=0.9,ja;q=0.8,zh-CN;q=0.7,zh;q=0.6,zh-TW;q=0.5,la;q=0.4")
                .header("authorization", "Bearer VUnYAl4GuBEZEWPjiYecfKbdATQr")
                .header("Cache-Control", "no-cache")
                .header("Connection", "keep-alive")
                .header("content-type", "application/json")
                .header("Host", "api.markitondemand.com")
                .header("Origin", "https//www.reuters.com")
                .header("Pragma", "no-cache")
                .header("Referer", "https//www.reuters.com/finance/currencies")
                .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/63.0.3239.132 Safari/537.36")
                .body("{\"days\":91,\"dataNormalized\":false,\"dataPeriod\":\"Day\",\"dataInterval\":1,\"realtime\":false,\"yFormat\":\"0.###\",\"timeServiceFormat\":\"JSON\",\"rulerIntradayStart\":26,\"rulerIntradayStop\":3,\"rulerInterdayStart\":10957,\"rulerInterdayStop\":365,\"returnDateType\":\"ISO8601\",\"elements\":[{\"Label\":\"05d8deb9\",\"Type\":\"price\",\"Symbol\":\"GBP|USD\",\"OverlayIndicators\":[],\"Params\":{},\"IsCurrencyCrossRate\":true}]}")
                .asJson();
    }
}
