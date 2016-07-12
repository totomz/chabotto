package integration;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import integration.utils.SimpleService;
import integration.utils.javaprocess.JavaProcess;
import it.myideas.chabotto.Chabotto;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.ScanParams;

public class TestHeartBeat {

    private Jedis jedis;
    
    private static Logger log = LoggerFactory.getLogger(TestHeartBeat.class);
    private ArrayList<Process> cleanUpList;
    
    static {
        System.setProperty(org.slf4j.impl.SimpleLogger.DEFAULT_LOG_LEVEL_KEY, "TRACE");
    }
    
    @Before
    public void init() {
        jedis = new Jedis();
        cleanUpList = new ArrayList<>();
        cleanup();
    }
    
    @After
    public void cleanup() {
        
        // If an assert fail, subprocess may still be alive
        cleanUpList.forEach((process) -> {
            
            try{
                System.out.println("Stopping " + process);
                process.destroy();
                
                Thread.sleep(2000);
                System.out.println("Still running? " + process.isAlive());
            }
            catch (Exception e) {
                System.out.println(e.getMessage());
            }
            
        });
        
        jedis.scan("0", new ScanParams().match("*"))
            .getResult()
            .forEach(key -> {
                System.out.println("Deleting " + key + "..." + jedis.del(key));
            })
        ;
        jedis.close();
    }
    
    
    
//    @Test
    public void testServiceHeartBeatFailure() throws InterruptedException, IOException {
        
        // Register 2 services in a separate process
        log.info("Testing heartbeat functionality");
        Process serviceA = JavaProcess.exec(SimpleService.class, "hbeat");
        Process serviceB = JavaProcess.exec(SimpleService.class, "hbeat");
        
        // Register the process to be cleaned up in case of failures
        cleanUpList.add(serviceA);
        cleanUpList.add(serviceB);
        
        
        // DEBUG
        // see what is going on in the subprocess
        attachSysout(serviceA);
        attachSysout(serviceB);

        log.info("Subprocess started - wait for service registration");
        Thread.sleep(2 * 1000);
        assertEquals(2, jedis.scan("0", new ScanParams().match("cb8:service:hbeat:*")).getResult().size());
        
        log.info("Destroying subprocess");
        serviceA.destroy();
        serviceB.destroy();
        
        log.info("The service should expire after " + Chabotto.HEARTBEAT_SEC + " seconds. Waiting");
        int tick = Chabotto.HEARTBEAT_SEC + 5;
        for(int i=0;i<tick;i++) {
            log.info("waiting....." + i + "/" + tick);
            Thread.sleep(1 * 1000);    
        }
        
        assertFalse(serviceA.isAlive());
        assertFalse(serviceB.isAlive());
        
        log.info("Verify that are not there");
        assertEquals(0, jedis.scan("0", new ScanParams().match("cb8:service:hbeat:*")).getResult().size());
        
    }
    
    @Test
    public void testServiceHeartBeat() throws IOException, InterruptedException {
        
        // Register 2 services in a separate process
        Process serviceA = JavaProcess.exec(SimpleService.class, "peppapig");
        Process serviceB = JavaProcess.exec(SimpleService.class, "peppapig");
        
        // Register the process to be cleaned up in case of failures
        cleanUpList.add(serviceA);
        cleanUpList.add(serviceB);
        
        // DEBUG
        // see what is going on in the subprocess
        attachSysout(serviceA);
        attachSysout(serviceB);

        log.info("Subprocess started - wait for service registration");
        Thread.sleep(2 * 1000);
        assertEquals(2, jedis.scan("0", new ScanParams().match("cb8:service:peppapig:*")).getResult().size());
     
        log.info("The service should expire after " + Chabotto.HEARTBEAT_SEC + " seconds. Waiting"); 
        int tick = Chabotto.HEARTBEAT_SEC + 5;
        for(int i=0;i<tick;i++) {
            log.info("waiting....." + i + "/" + tick);
            Thread.sleep(1 * 1000);    
        }

        log.info("Verify that the services are still there");
        assertTrue(serviceA.isAlive());
        assertTrue(serviceB.isAlive());
        
        assertEquals(2, jedis.scan("0", new ScanParams().match("cb8:service:peppapig:*")).getResult().size());

    }
    
    private void attachSysout(Process proc){
        new Thread(() -> {
            BufferedReader input = new BufferedReader(new InputStreamReader(proc.getInputStream()));
            String line;
            try {
                while ((line = input.readLine()) != null) {
                  log.info("subProcess: " + line);                  
                }
                input.close();
            }
            catch (IOException e) {
                e.printStackTrace();
            }
            
        }).start();
    }
    
    
}
