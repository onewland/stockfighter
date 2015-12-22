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
    public static final int ASK_BID_DELTA = 50;
    final String venue = "KUTEX";
    final String ticker = "BSH";
    final String tradingAccount = "KFB61990514";
    final String apiKey = Environment.getenv("API_KEY");
    private final HashMap<Integer, ServerOrderStatus> openOrders = Maps.newHashMap();
    private int netShares = 0;

    public void execute(CloseableHttpClient httpClient) throws URISyntaxException, IOException, InterruptedException {
        ClientWrapper httpWrapper = new ClientWrapper(httpClient, apiKey);
        int position = 0;
        int exposure = 0;
        int totalSold = 0;
        int totalBought = 0;

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
            System.out.println();
            System.out.println(
                    String.format(
                            "shares %5d open %5d position %4d exposure %4d fbuy %8d fsell %8d",
                            netShares,
                            openOrders.size(),
                            position,
                            exposure,
                            totalBought,
                            totalSold));

            ServerOrderbook serverOrderbook = httpWrapper.getOrderbook(venue, ticker);

            if(serverOrderbook.asks != null && serverOrderbook.bids != null) {
                OrderStatistics askStats = OrderStatistics.fromList(serverOrderbook.asks);
                OrderStatistics bidStats = OrderStatistics.fromList(serverOrderbook.bids);

                System.out.println("asks: " + askStats);
                System.out.println("bids: " + bidStats);

                if(askStats.getAvgOrder() - bidStats.getAvgOrder() > ASK_BID_DELTA) {
                    if(position < 300) {
                        // buy 10 cents above average bid
                        buyOrderRequest.qty = 100;
                        buyOrderRequest.price = bidStats.getAvgOrder() + 10;
                        exposure += 100;
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
                        exposure += 100;
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

            Thread.sleep(2000);

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

                    exposure -= fillTotal;

                    if(newOrderStatus.direction.equals("buy")) {
                        netShares += newOrderStatus.qty;
                        position += fillTotal;
                        totalBought += newOrderStatus.price * newOrderStatus.qty;
                    } else if(newOrderStatus.direction.equals("sell")) {
                        netShares -= newOrderStatus.qty;
                        position -= fillTotal;
                        totalSold += newOrderStatus.price * newOrderStatus.qty;
                    }

                    openOrders.put(status.id, newOrderStatus);
                    if(status.qty == fillTotal || !newOrderStatus.open) {
                        System.out.println(
                                String.format("[fill] %s %d at %d", newOrderStatus.direction, fillTotal, newOrderStatus.price)
                        );
                        idsToRemove.add(newOrderStatus.id);
                    }
                }
            }

            for(Integer id : idsToRemove) { openOrders.remove(id); }
        }
    }
}
