package integration.utils;

import java.net.URI;

import it.myideas.chabotto.Chabotto;

public class SimpleService {

    public static void main(String[] args) throws InterruptedException {
        
        String uri = "";
        
        try {
            int port = (int)(Math.random()*1000);
            uri = new URI("pork://a.test.com:" + port + "/george").toString();
            
            System.out.println("Registering service name " + args[0] + " on " + uri);
            Chabotto.registerService(args[0], uri);
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
