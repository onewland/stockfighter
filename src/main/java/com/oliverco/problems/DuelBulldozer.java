package com.oliverco.problems;

import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.oliverco.*;
import com.sun.deploy.Environment;
import org.apache.http.impl.client.CloseableHttpClient;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.Set;

public class DuelBulldozer {
    final String venue = "KHJEX";
    final String ticker = "EQXO";
    final String tradingAccount = "HAI5175137";
    final String apiKey = Environment.getenv("API_KEY");
    private final HashMap<Integer, ServerOrderStatus> openOrders = Maps.newHashMap();
    private int netShares = 0;

    public void execute(CloseableHttpClient httpClient) throws URISyntaxException, IOException, InterruptedException {
        ClientWrapper httpWrapper = new ClientWrapper(httpClient, apiKey);
        int position = 0;

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

        for(int i = 0; i < 500; i++) {
            System.out.println(netShares + " shares");
            System.out.println(openOrders.size() + " open orders");
            System.out.println("position = " + position);

            ServerOrderbook serverOrderbook = httpWrapper.getOrderbook(venue, ticker);

            if(serverOrderbook.asks != null &&
                serverOrderbook.bids != null)
            {
                OrderStatistics askStats = OrderStatistics.fromList(serverOrderbook.asks);
                OrderStatistics bidStats = OrderStatistics.fromList(serverOrderbook.bids);

                System.out.println("asks: " + askStats);
                System.out.println("bids: " + bidStats);

                if(askStats.getAvgOrder() - bidStats.getAvgOrder() > 10) {
                    if(position < 300) {
                        // buy 10 cents above average bid
                        buyOrderRequest.qty = 100;
                        buyOrderRequest.price = bidStats.getAvgOrder() + 10;
                        ServerOrderStatus result = httpWrapper.placeOrder(buyOrderRequest);
                        if (result.ok) {
                            openOrders.put(result.id, result);
                        } else {
                            System.out.println("uhhh");
                        }
                    }

                    if(position > -300) {
                        // sell 10 cents below average ask
                        sellOrderRequest.qty = 100;
                        sellOrderRequest.price = askStats.getAvgOrder() - 10;
                        ServerOrderStatus result = httpWrapper.placeOrder(sellOrderRequest);
                        if (result.ok) {
                            openOrders.put(result.id, result);
                        } else {
                            System.out.println("uhhh");
                        }
                    }

                } else {
                    System.out.println("Spread too small, not bothering to trade");
                }
            } else {
                System.out.println("asks or bids were null, skipping iteration");
            }

            Thread.sleep(500);

            netShares = 0;
            position = 0;

            Set<Integer> idsToRemove = Sets.newHashSet();

            for(ServerOrderStatus status : openOrders.values()) {
                ServerOrderStatus newOrderStatus = httpWrapper.getOrderStatus(venue, ticker, status.id);

                if(newOrderStatus.ok) {
                    int fillTotal = 0;

                    for(ServerOrderStatus.Fill f: newOrderStatus.fills) {
                        fillTotal += f.qty;
                    }

                    if(newOrderStatus.direction.equals("buy")) {
                        netShares += newOrderStatus.qty;
                        position += fillTotal;
                    } else if(newOrderStatus.direction.equals("sell")) {
                        netShares -= newOrderStatus.qty;
                        position -= fillTotal;
                    }

                    openOrders.put(status.id, newOrderStatus);
                    if(status.qty == fillTotal || !newOrderStatus.open) {
                        idsToRemove.add(newOrderStatus.id);
                    }
                }
            }

            for(Integer id : idsToRemove) { openOrders.remove(id); }
        }
    }
}
