package xyz.n7mn.data;

public class QueueData {

    private String ID = null;
    private String URL = null;

    public QueueData(){

    }

    public QueueData(String id, String url){
        this.ID = id;
        this.URL = url;
    }

    public String getID() {
        return ID;
    }

    public void setID(String ID) {
        this.ID = ID;
    }

    public String getURL() {
        return URL;
    }

    public void setURL(String URL) {
        this.URL = URL;
    }
}
