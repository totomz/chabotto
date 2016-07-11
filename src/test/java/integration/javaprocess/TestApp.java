package integration.javaprocess;

import java.util.Arrays;

public class TestApp {

    public static void main(String[] args) throws InterruptedException {
        
        System.out.println("PARAMS:");
        Arrays.asList(args).forEach(System.out::println);
        
        while(true) {
            System.out.println("I'm alive!");
            Thread.sleep(2000);
        }
    }
    
}
