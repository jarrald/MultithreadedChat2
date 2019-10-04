package kea.dat18c.multithreadedchat.server;

public class Users {
    private static Users instance = null;
    private Users(){}

    public static Users getInstance() {
        if(instance == null){
            instance = new Users();
        }
        return instance;
    }
}
