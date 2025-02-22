package net.nicovrc.dev.api;


public interface NicoVRCAPI {

    String getURI();
    String Run(String httpRequest) throws Exception;

}
