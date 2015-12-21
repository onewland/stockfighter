package com.oliverco;

import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import com.oliverco.problems.DuelBulldozer;

import java.io.IOException;
import java.net.URISyntaxException;

public class App {
    public static void main(String[] args) throws URISyntaxException, IOException, InterruptedException {
        CloseableHttpClient httpClient = HttpClients.createDefault();

        new DuelBulldozer().execute(httpClient);
    }
}
