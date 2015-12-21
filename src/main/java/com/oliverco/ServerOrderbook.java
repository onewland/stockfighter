package com.oliverco;

import java.util.List;

public class ServerOrderbook {
    public boolean ok;
    public String venue;
    public String symbol;

    public static ServerOrderbook failure() {
        ServerOrderbook book = new ServerOrderbook();
        book.ok = false;
        return book;
    }

    public static class Order {
        public int price;
        public int qty;
        public boolean isBuy;

        public int getPrice() {
            return price;
        }

        public int getQty() {
            return qty;
        }
    }

    public List<Order> bids;
    public List<Order> asks;
}
