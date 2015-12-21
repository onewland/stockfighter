package com.oliverco;

import java.util.List;

public class OrderStatistics {
    public int getMinOrder() {
        return minOrder;
    }

    public int getMaxOrder() {
        return maxOrder;
    }

    public int getAvgOrder() {
        return avgOrder;
    }

    public int getOrderVolume() {
        return orderVolume;
    }

    private int minOrder = Integer.MAX_VALUE;
    private int maxOrder = Integer.MIN_VALUE;
    private int avgOrder = 0;
    private int orderSum = 0;
    private int orderVolume = 0;

    private OrderStatistics() {
    }

    public String toString() {
        return String.format("min = %d, max = %d, volume = %d, avg = %d",
                getMinOrder(),
                getMaxOrder(),
                getOrderVolume(),
                getAvgOrder());
    }

    public static OrderStatistics fromList(List<ServerOrderbook.Order> orders) {
        OrderStatistics statistics = new OrderStatistics();
        if(orders == null) {
            statistics.orderVolume = 0;
            statistics.avgOrder = 0;
            statistics.maxOrder = 0;
            statistics.minOrder = 0;
        } else {
            for (ServerOrderbook.Order ask : orders) {
                if (ask.price < statistics.minOrder) {
                    statistics.minOrder = ask.price;
                }
                if (ask.price > statistics.maxOrder) {
                    statistics.maxOrder = ask.price;
                }
                statistics.orderSum += ask.price * ask.qty;
                statistics.orderVolume += ask.qty;
            }
            statistics.avgOrder = statistics.orderSum/statistics.orderVolume;
        }

        return statistics;
    }
}
