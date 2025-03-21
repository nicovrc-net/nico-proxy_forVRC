package net.nicovrc.dev.Service;

import java.util.ArrayList;
import java.util.List;

public class ServiceList {

    public static List<ServiceAPI> getServiceList(){
        final List<ServiceAPI> list = new ArrayList<>();
        list.add(new NicoVideo());
        list.add(new bilibili_com());
        list.add(new Youtube());
        list.add(new XVIDEOS());
        list.add(new TikTok());
        list.add(new Twitter());
        list.add(new OPENREC());
        list.add(new Pornhub());
        list.add(new Twitcasting());
        list.add(new Abema());
        list.add(new TVer());
        list.add(new Iwara());
        list.add(new piapro());
        list.add(new SoundCloud());
        list.add(new Vimeo());
        list.add(new fc2());
        list.add(new Youjizz());
        list.add(new Sonicbowl());
        list.add(new Mixcloud());
        list.add(new bandcamp());
        list.add(new SpankBang());

        return list;
    }
}
