package net.nicovrc.dev.Service;

import java.util.ArrayList;
import java.util.List;

public class ServiceList {

    public static List<ServiceAPI> getServiceList(){
        final List<ServiceAPI> list = new ArrayList<>();
        list.add(new NicoVideo());
        return list;
    }
}
