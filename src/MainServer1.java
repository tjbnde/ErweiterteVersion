public class MainServer1 {
    public static void main(String[] args) {
        int server1 = 6666;
        int server2 = 8888;

        int[] servers = {server1, server2};
        MainServer myMainServer = new MainServer(servers);
        myMainServer.start();
    }
}
