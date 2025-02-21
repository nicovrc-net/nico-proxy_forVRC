package net.nicovrc.dev.api;

import java.awt.*;

public interface NicoVRCAPI {

    String getURI();
    String Run(String httpRequest) throws Exception;

}
