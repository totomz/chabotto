package integration.utils;

import java.net.URI;

import it.myideas.chabotto.Chabotto;

public class SimpleService {

    public static void main(String[] args) throws InterruptedException {
        
        String[] tmp = args[0].split(";");
        
        try {
            int port = (int)(Math.random()*1000);
            
            System.out.println("Registering service name " + tmp[0] + " with ip " + tmp[1]);
            Chabotto.registerService(tmp[0], tmp[1], port);
            System.out.println("Fatto");
        }
        catch (Exception e) {
            System.out.println("ERROR:" + e.getMessage());
        }
        
        
        
        while (true) {
            System.out.println("sleeping");
            Thread.sleep(Long.MAX_VALUE);            
        }
    }
    
}
