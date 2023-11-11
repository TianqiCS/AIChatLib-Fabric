package com.citrusmc.aichatlib;

import okhttp3.OkHttpClient;

import java.util.concurrent.TimeUnit;

public class HttpClientFactory {
    public static OkHttpClient createClient() {
        return new OkHttpClient.Builder()
                .connectTimeout(30, TimeUnit.SECONDS) // Set the connection timeout
                .readTimeout(30, TimeUnit.SECONDS)    // Set the read timeout
                .writeTimeout(30, TimeUnit.SECONDS)   // Set the write timeout
                .build();
    }
}
