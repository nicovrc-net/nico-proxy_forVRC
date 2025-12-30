package net.nicovrc.dev.data;

import redis.clients.jedis.RedisClient;

public class Config {

    private int httpPort;
    private String TwitcastingClientID;
    private String TwitcastingClientSecret;
    private String DiscordWebhookURL;
    private boolean ViewLog;

    private String RedisServer;
    private int RedisPort;
    private String RedisPass;
    private boolean RedisSSL;

    private String nicosid;
    private String user_session;

    private RedisClient RedisClient;

    public int getHttpPort() {
        return httpPort;
    }

    public void setHttpPort(int httpPort) {
        this.httpPort = httpPort;
    }

    public String getTwitcastingClientID() {
        return TwitcastingClientID;
    }

    public void setTwitcastingClientID(String twitcastingClientID) {
        TwitcastingClientID = twitcastingClientID;
    }

    public String getTwitcastingClientSecret() {
        return TwitcastingClientSecret;
    }

    public void setTwitcastingClientSecret(String twitcastingClientSecret) {
        TwitcastingClientSecret = twitcastingClientSecret;
    }

    public String getDiscordWebhookURL() {
        return DiscordWebhookURL;
    }

    public void setDiscordWebhookURL(String discordWebhookURL) {
        DiscordWebhookURL = discordWebhookURL;
    }

    public boolean isViewLog() {
        return ViewLog;
    }

    public void setViewLog(boolean viewLog) {
        ViewLog = viewLog;
    }

    public String getRedisServer() {
        return RedisServer;
    }

    public void setRedisServer(String redisServer) {
        RedisServer = redisServer;
    }

    public int getRedisPort() {
        return RedisPort;
    }

    public void setRedisPort(int redisPort) {
        RedisPort = redisPort;
    }

    public String getRedisPass() {
        return RedisPass;
    }

    public void setRedisPass(String redisPass) {
        RedisPass = redisPass;
    }

    public boolean isRedisSSL() {
        return RedisSSL;
    }

    public void setRedisSSL(boolean redisSSL) {
        RedisSSL = redisSSL;
    }

    public String getNicosid() {
        return nicosid;
    }

    public void setNicosid(String nicosid) {
        this.nicosid = nicosid;
    }

    public String getUser_session() {
        return user_session;
    }

    public void setUser_session(String user_session) {
        this.user_session = user_session;
    }

    public RedisClient getRedisClient() {
        return RedisClient;
    }

    public void setRedisClient(RedisClient redisClient) {
        RedisClient = redisClient;
    }
}
