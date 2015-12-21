package com.oliverco.problems;

import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.google.gson.Gson;
import com.oliverco.*;
import com.sun.deploy.Environment;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.util.EntityUtils;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.Set;

public class DuelBulldozer {
    final String venue = "ULBEX";
    final String ticker = "OJWO";
    final String tradingAccount = "FMB34717574";
    final String apiKey = Environment.getenv("API_KEY");
    private final HashMap<Integer, ServerOrderStatus> openOrders = Maps.newHashMap();
    private int netShares = 0;

    public void execute(CloseableHttpClient httpClient) throws URISyntaxException, IOException, InterruptedException {
        ClientWrapper httpWrapper = new ClientWrapper(httpClient, apiKey);
        Gson gson = new Gson();

        OrderRequest buyOrderRequest = new OrderRequest();
        buyOrderRequest.account = tradingAccount;
        buyOrderRequest.venue = venue;
        buyOrderRequest.stock = ticker;
        buyOrderRequest.direction = "buy";
        buyOrderRequest.orderType = "limit";

        OrderRequest sellOrderRequest = new OrderRequest();
        sellOrderRequest.account = tradingAccount;
        sellOrderRequest.venue = venue;
        sellOrderRequest.stock = ticker;
        sellOrderRequest.direction = "sell";
        sellOrderRequest.orderType = "limit";

        URI postOrderURI = new URIBuilder().
                setScheme("https").
                setHost("api.stockfighter.io").
                setPath(String.format("/ob/api/venues/%s/stocks/%s/orders", venue, ticker)).
                build();
        HttpPost post = new HttpPost(postOrderURI);
        post.setHeader("X-Starfighter-Authorization", apiKey);

        for(int i = 0; i < 500; i++) {
            System.out.println(netShares + " shares");
            System.out.println(openOrders.size() + " open orders");

            ServerOrderbook serverOrderbook = httpWrapper.getOrderbook(venue, ticker);

            if(serverOrderbook.asks != null &&
                serverOrderbook.bids != null)
            {
                OrderStatistics askStats = OrderStatistics.fromList(serverOrderbook.asks);
                OrderStatistics bidStats = OrderStatistics.fromList(serverOrderbook.bids);

                System.out.println("asks: " + askStats);
                System.out.println("bids: " + bidStats);

                if(askStats.getAvgOrder() - bidStats.getAvgOrder() > 10) {
                    // buy 10 cents above average bid
                    buyOrderRequest.qty = 100;
                    buyOrderRequest.price = bidStats.getAvgOrder() + 10;
                    ServerOrderStatus result = httpWrapper.placeOrder(buyOrderRequest);
                    if(result.ok) {
                        openOrders.put(result.id, result);
                    } else {
                        System.out.println("uhhh");
                    }

                    // sell 10 cents below average ask
                    sellOrderRequest.qty = 100;
                    sellOrderRequest.price = askStats.getAvgOrder() - 10;
                    result = httpWrapper.placeOrder(sellOrderRequest);
                    if(result.ok) {
                        openOrders.put(result.id, result);
                    } else {
                        System.out.println("uhhh");
                    }

                } else {
                    System.out.println("Spread too small, not bothering to trade");
                }
            } else {
                System.out.println("asks or bids were null, skipping iteration");
            }

            Thread.sleep(2000);

            netShares = 0;

            Set<Integer> idsToRemove = Sets.newHashSet();

            for(ServerOrderStatus status : openOrders.values()) {
                URI getOrderStatusUri = new URIBuilder().
                        setScheme("https").
                        setHost("api.stockfighter.io").
                        setPath(String.format("/ob/api/venues/%s/stocks/%s/orders/%d", venue, ticker, status.id)).
                        build();
                HttpGet get2 = new HttpGet(getOrderStatusUri);
                get2.setHeader("X-Starfighter-Authorization", apiKey);
                CloseableHttpResponse get2Response = httpClient.execute(get2);

                if(get2Response.getStatusLine().getStatusCode() == 200) {
                    String buyResponse = EntityUtils.toString(get2Response.getEntity());
                    ServerOrderStatus orderStatus = gson.fromJson(buyResponse, ServerOrderStatus.class);
                    if(orderStatus.direction.equals("buy")) {
                        netShares += orderStatus.qty;
                    } else if(orderStatus.direction.equals("sell")) {
                        netShares -= orderStatus.qty;
                    }
                    openOrders.put(orderStatus.id, orderStatus);
                    if(orderStatus.qty == orderStatus.totalFilled) {
                        idsToRemove.add(orderStatus.id);
                    }
                }
            }

            for(Integer id : idsToRemove) { openOrders.remove(id); }
        }
    }
}
