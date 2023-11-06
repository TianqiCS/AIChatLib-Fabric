package com.citrusmc.chatbot;

import okhttp3.OkHttpClient;

import java.util.concurrent.TimeUnit;

public class HttpClientFactory {
    public static OkHttpClient createClient() {
        return new OkHttpClient.Builder()
                .connectTimeout(100, TimeUnit.SECONDS) // Set the connection timeout
                .readTimeout(100, TimeUnit.SECONDS)    // Set the read timeout
                .writeTimeout(100, TimeUnit.SECONDS)   // Set the write timeout
                .build();
    }
}
