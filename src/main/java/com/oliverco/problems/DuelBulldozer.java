package com.oliverco.problems;

import com.google.common.collect.Maps;
import com.google.gson.Gson;
import com.oliverco.OrderRequest;
import com.oliverco.OrderStatistics;
import com.oliverco.ServerOrderStatus;
import com.oliverco.ServerOrderbook;
import org.apache.http.HttpEntity;
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

public class DuelBulldozer {
    final String venue = "RBSBEX";
    final String ticker = "RYYA";
    final String tradingAccount = "CCL7444232";
    private final HashMap<Integer, ServerOrderStatus> openBids = Maps.newHashMap();
    private final HashMap<Integer, ServerOrderStatus> openAsks = Maps.newHashMap();
    private int netShares = 0;

    public void execute(CloseableHttpClient httpClient) throws URISyntaxException, IOException, InterruptedException {
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

        URI getOrderbookUri = new URIBuilder().
                setScheme("https").
                setHost("api.stockfighter.io").
                setPath(String.format("/ob/api/venues/%s/stocks/%s", venue, ticker)).
                build();
        HttpGet get = new HttpGet(getOrderbookUri);
        get.setHeader("X-Starfighter-Authorization","215ba92dd840fc8ddafad64ee5803149afc6a913");

        URI postOrderURI = new URIBuilder().
                setScheme("https").
                setHost("api.stockfighter.io").
                setPath(String.format("/ob/api/venues/%s/stocks/%s/orders", venue, ticker)).
                build();
        HttpPost post = new HttpPost(postOrderURI);
        post.setHeader("X-Starfighter-Authorization","215ba92dd840fc8ddafad64ee5803149afc6a913");

        for(int i = 0; i < 500; i++) {
            System.out.println(netShares + " shares");
            CloseableHttpResponse response1 = httpClient.execute(get);
            System.out.println(response1.getStatusLine());

            String responseStr = EntityUtils.toString(response1.getEntity());

            ServerOrderbook serverOrderbook = gson.fromJson(responseStr, ServerOrderbook.class);
            if(serverOrderbook.asks != null &&
                serverOrderbook.bids != null)
            {
                OrderStatistics askStats = OrderStatistics.fromList(serverOrderbook.asks);
                OrderStatistics bidStats = OrderStatistics.fromList(serverOrderbook.bids);

                System.out.println("asks: " + askStats);
                System.out.println("bids: " + bidStats);

                if(askStats.getAvgOrder() - bidStats.getAvgOrder() > 10) {
                    // buy 10 cents above average bid, sell 10 cents below average ask
                    buyOrderRequest.qty = 100;
                    buyOrderRequest.price = bidStats.getAvgOrder() + 10;
                    System.out.println(String.format(
                            "[order] buy %d at %d",
                            buyOrderRequest.qty,
                            buyOrderRequest.price)
                    );
                    post.setEntity(new StringEntity(gson.toJson(buyOrderRequest)));
                    CloseableHttpResponse postResponse = httpClient.execute(post);
                    if(postResponse.getStatusLine().getStatusCode() == 200) {
                        String buyResponse = EntityUtils.toString(postResponse.getEntity());
                        ServerOrderStatus orderStatus = gson.fromJson(buyResponse, ServerOrderStatus.class);
                        openBids.put(orderStatus.id, orderStatus);
                    }

                    sellOrderRequest.qty = 100;
                    sellOrderRequest.price = askStats.getAvgOrder() - 10;
                    System.out.println(String.format(
                            "[order] sell %d at %d",
                            sellOrderRequest.qty,
                            sellOrderRequest.price)
                    );
                    post.setEntity(new StringEntity(gson.toJson(sellOrderRequest)));
                    postResponse = httpClient.execute(post);
                    if(postResponse.getStatusLine().getStatusCode() == 200) {
                        String sellResponse = EntityUtils.toString(postResponse.getEntity());
                        ServerOrderStatus orderStatus = gson.fromJson(sellResponse, ServerOrderStatus.class);
                        openAsks.put(orderStatus.id, orderStatus);
                    }
                } else {
                    System.out.println("Spread too small, not bothering to trade");
                }
            } else {
                System.out.println("asks or bids were null, skipping iteration");
            }


            Thread.sleep(2000);

            netShares = 0;

            // check on outstanding orders
            for(ServerOrderStatus status : openBids.values()) {
                URI getOrderStatusUri = new URIBuilder().
                        setScheme("https").
                        setHost("api.stockfighter.io").
                        setPath(String.format("/ob/api/venues/%s/stocks/%s/orders/%d", venue, ticker, status.id)).
                        build();
                HttpGet get2 = new HttpGet(getOrderStatusUri);
                get2.setHeader("X-Starfighter-Authorization","215ba92dd840fc8ddafad64ee5803149afc6a913");
                CloseableHttpResponse get2Response = httpClient.execute(get2);
                if(get2Response.getStatusLine().getStatusCode() == 200) {
                    String buyResponse = EntityUtils.toString(get2Response.getEntity());
                    ServerOrderStatus orderStatus = gson.fromJson(buyResponse, ServerOrderStatus.class);
                    netShares += orderStatus.qty;
                    openBids.put(orderStatus.id, orderStatus);
                    System.out.println(buyResponse);
                }
            }

            for(ServerOrderStatus status : openAsks.values()) {
                URI getOrderStatusUri = new URIBuilder().
                        setScheme("https").
                        setHost("api.stockfighter.io").
                        setPath(String.format("/ob/api/venues/%s/stocks/%s/orders/%d", venue, ticker, status.id)).
                        build();
                HttpGet get2 = new HttpGet(getOrderStatusUri);
                get2.setHeader("X-Starfighter-Authorization","215ba92dd840fc8ddafad64ee5803149afc6a913");
                CloseableHttpResponse get2Response = httpClient.execute(get2);
                if(get2Response.getStatusLine().getStatusCode() == 200) {
                    String buyResponse = EntityUtils.toString(get2Response.getEntity());
                    ServerOrderStatus orderStatus = gson.fromJson(buyResponse, ServerOrderStatus.class);
                    netShares -= orderStatus.qty;
                    openBids.put(orderStatus.id, orderStatus);
                    System.out.println(buyResponse);
                }
            }
        }
    }
}
