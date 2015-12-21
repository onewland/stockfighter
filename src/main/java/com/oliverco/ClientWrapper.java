package com.oliverco;

import com.google.gson.Gson;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.util.EntityUtils;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;


public class ClientWrapper {
    private final CloseableHttpClient httpClient;
    private final String apiKey;
    private final Gson gson = new Gson();

    public ClientWrapper(CloseableHttpClient httpClient, String apiKey) {
        this.httpClient = httpClient;
        this.apiKey = apiKey;
    }

    public ServerOrderStatus placeOrder(OrderRequest request) throws IOException, URISyntaxException {
        URI postOrderURI = new URIBuilder().
                setScheme("https").
                setHost("api.stockfighter.io").
                setPath(String.format("/ob/api/venues/%s/stocks/%s/orders", request.venue, request.stock)).
                build();

        HttpPost orderPost = new HttpPost(postOrderURI);

        orderPost.setHeader("X-Starfighter-Authorization", apiKey);
        orderPost.setEntity(new StringEntity(gson.toJson(request)));

        CloseableHttpResponse response = httpClient.execute(orderPost);

        if(response.getStatusLine().getStatusCode() == 200) {
            String textResponse = EntityUtils.toString(response.getEntity());
            ServerOrderStatus orderStatus = gson.fromJson(textResponse, ServerOrderStatus.class);
            System.out.println(String.format(
                    "[order] %s %d at %d",
                    request.direction,
                    request.qty,
                    request.price)
            );
            return orderStatus;
        } else {
            System.out.println(response.getStatusLine());
            return ServerOrderStatus.failure();
        }
    }
}
