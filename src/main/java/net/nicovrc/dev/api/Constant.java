package net.nicovrc.dev.api;

public class Constant {

    private static final String Version = "2.13.0-beta";
    private static final String UserAgent = "Mozilla/5.0 (Windows NT 10.0; Win64; x64; rv:127.0) Gecko/20100101 Firefox/127.0 nicovrc/"+Version;

    public static String getVersion(){
        return Version;
    }

    public static String getUserAgent() {
        return UserAgent;
    }
}
