package net.nicovrc.dev.Service;

import java.util.ArrayList;
import java.util.List;

public class ServiceList {

    public static List<ServiceAPI> getServiceList(){
        final List<ServiceAPI> list = new ArrayList<>();
        list.add(new NicoVideo());
        list.add(new bilibili_com());
        list.add(new Youtube());
        list.add(new Twitter());
        list.add(new TikTok());
        list.add(new OPENREC());
        list.add(new Twitcasting());
        list.add(new Abema());
        list.add(new TVer());
        list.add(new piapro());


        return list;
    }
}
