package net.nicovrc.dev.api;

public class Constant {

    private static final String Version = "2.17.1";
    private static final String UserAgent = "Mozilla/5.0 (Windows NT 10.0; Win64; x64; rv:128.0) Gecko/20100101 Firefox/128.0 nicovrc/"+Version;

    public static String getVersion(){
        return Version;
    }

    public static String getUserAgent() {
        return UserAgent;
    }
}
