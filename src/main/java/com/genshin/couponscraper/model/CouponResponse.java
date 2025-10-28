package com.genshin.couponscraper.model;

import com.fasterxml.jackson.annotation.JsonProperty;

public class CouponResponse {
    
    @JsonProperty("code")
    private String code;
    
    @JsonProperty("reward")
    private String reward;
    
    @JsonProperty("date")
    private String date;
    
    @JsonProperty("status")
    private String status;
    
    @JsonProperty("server")
    private String server;
    
    @JsonProperty("raw")
    private String raw;
    
    public CouponResponse() {}
    
    public CouponResponse(String code, String reward, String date, String status, String server) {
        this.code = code;
        this.reward = reward;
        this.date = date;
        this.status = status;
        this.server = server;
        this.raw = null;
    }
    
    // Getters and setters
    public String getCode() {
        return code;
    }
    
    public void setCode(String code) {
        this.code = code;
    }
    
    public String getReward() {
        return reward;
    }
    
    public void setReward(String reward) {
        this.reward = reward;
    }
    
    public String getDate() {
        return date;
    }
    
    public void setDate(String date) {
        this.date = date;
    }
    
    public String getStatus() {
        return status;
    }
    
    public void setStatus(String status) {
        this.status = status;
    }
    
    public String getServer() {
        return server;
    }
    
    public void setServer(String server) {
        this.server = server;
    }
    
    public String getRaw() {
        return raw;
    }
    
    public void setRaw(String raw) {
        this.raw = raw;
    }
    
    @Override
    public String toString() {
        return "CouponResponse{" +
                "code='" + code + '\'' +
                ", reward='" + reward + '\'' +
                ", date='" + date + '\'' +
                ", status='" + status + '\'' +
                ", server='" + server + '\'' +
                '}';
    }
}