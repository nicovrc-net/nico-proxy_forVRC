package xyz.n7mn;

public class RequestHTTPServer extends Thread{

    private final int Port;

    public RequestHTTPServer(int port) {
        this.Port = port;
    }

    @Override
    public void run() {
        System.out.println("[Info] TCP Port "+Port+"で 処理受付用HTTPサーバー待機開始");



    }
}
