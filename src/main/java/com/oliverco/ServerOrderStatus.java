package com.oliverco;

import java.util.List;

public class ServerOrderStatus {
    public static ServerOrderStatus failure() {
        ServerOrderStatus status = new ServerOrderStatus();
        status.ok = false;
        return status;
    }

    public static class Fill {
        public int price;
        public int qty;
        public String ts;
    }

    public boolean ok;
    public String symbol;
    public String venue;
    public String direction;
    public int originalQty;
    public int qty;
    public int price;
    public String orderType;
    public int id;
    public String account;
    public String ts;
    public int totalFilled;
    public boolean open;
    public List<Fill> fills;
}
