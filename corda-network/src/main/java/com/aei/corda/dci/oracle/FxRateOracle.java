package com.aei.corda.dci.oracle;

import co.paralleluniverse.fibers.Suspendable;
import com.google.common.base.Charsets;
import com.google.common.io.Resources;
import com.jayway.jsonpath.JsonPath;
import net.corda.core.node.ServiceHub;
import net.corda.core.node.services.CordaService;
import net.corda.core.serialization.SingletonSerializeAsToken;
import net.minidev.json.JSONArray;

import java.io.IOException;
import java.math.BigDecimal;
import java.net.URL;
import java.util.List;
import java.util.stream.Collectors;

@CordaService
public class FxRateOracle extends SingletonSerializeAsToken {

    public FxRateOracle(ServiceHub serviceHub) {
    }

    @Suspendable
    public List<BigDecimal> query() throws IOException {
        URL resource = Resources.getResource("fxrate.json");
        String body = Resources.toString(resource, Charsets.UTF_8);
        List<JSONArray> priceResult = JsonPath.read(body, "$.Elements[0].ComponentSeries[?(@.Type=~/close/i)].Values");
        JSONArray objects = priceResult.get(0);
        List<BigDecimal> prices = objects.stream().map(o -> new BigDecimal((Double) o)).collect(Collectors.toList());
        return prices;
    }
}
