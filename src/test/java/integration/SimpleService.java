package integration;

import it.myideas.chabotto.Chabotto;

public class SimpleService {

    public static void main(String[] args) throws InterruptedException {
        
        System.out.println("Registering service for host " + args[0] );
        
        Chabotto.registerService("peppapig", "pork://" + args[0] + ":8584/george");
        
        while (true) {
            System.out.println("sleeping");
            Thread.sleep(Long.MAX_VALUE);            
        }
    }
    
}
