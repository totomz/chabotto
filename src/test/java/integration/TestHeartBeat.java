package integration;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import integration.utils.SimpleService;
import integration.utils.javaprocess.JavaProcess;
import it.myideas.chabotto.Chabotto;

public class TestHeartBeat extends BaseTest {

//    private static Logger log = LoggerFactory.getLogger(TestHeartBeat.class);
//    
////    @Test
//    public void testServiceHeartBeatFailure() throws InterruptedException, IOException {
//        
//        // Register 2 services in a separate process
//        log.info("Testing heartbeat functionality");
//        Process serviceA = JavaProcess.exec(SimpleService.class, "hbeat");
//        Process serviceB = JavaProcess.exec(SimpleService.class, "hbeat");
//        
//        // Register the process to be cleaned up in case of failures
//        processToDestroy.add(serviceA);
//        processToDestroy.add(serviceB);
//        
//        
//        // DEBUG
//        // see what is going on in the subprocess
//        attachSysout(serviceA);
//        attachSysout(serviceB);
//
//        log.info("Subprocess started - wait for service registration");
//        Thread.sleep(2 * 1000);
//        assertEquals(2, jedis.scan("0", new ScanParams().match("cb8:service:hbeat:*")).getResult().size());
//        
//        log.info("Destroying subprocess");
//        serviceA.destroy();
//        serviceB.destroy();
//        
//        waitForHeartbeatTimeOut();
//        
//        assertFalse(serviceA.isAlive());
//        assertFalse(serviceB.isAlive());
//        
//        log.info("Verify that are not there");
////        assertEquals(0, jedis.scan("0", new ScanParams().match("cb8:service:hbeat:*")).getResult().size());
//        
//    }
//    
//    @Test
//    public void testServiceHeartBeat() throws IOException, InterruptedException {
//        
//        // Register 2 services in a separate process
//        Process serviceA = JavaProcess.exec(SimpleService.class, "peppapig");
//        Process serviceB = JavaProcess.exec(SimpleService.class, "peppapig");
//        
//        // Register the process to be cleaned up in case of failures
//        processToDestroy.add(serviceA);
//        processToDestroy.add(serviceB);
//        
//        // DEBUG
//        // see what is going on in the subprocess
//        attachSysout(serviceA);
//        attachSysout(serviceB);
//
//        log.info("Subprocess started - wait for service registration");
//        Thread.sleep(2 * 1000);
//        assertEquals(2, jedis.scan("0", new ScanParams().match("cb8:service:peppapig:*")).getResult().size());
//     
//        waitForHeartbeatTimeOut();
//
//        log.info("Verify that the services are still there");
//        assertTrue(serviceA.isAlive());
//        assertTrue(serviceB.isAlive());
//        
//        assertEquals(2, jedis.scan("0", new ScanParams().match("cb8:service:peppapig:*")).getResult().size());
//
//    }
//    
//    private void attachSysout(Process proc){
//        new Thread(() -> {
//            BufferedReader input = new BufferedReader(new InputStreamReader(proc.getInputStream()));
//            String line;
//            try {
//                while ((line = input.readLine()) != null) {
//                  log.info("subProcess: " + line);                  
//                }
//                input.close();
//            }
//            catch (IOException e) {
//                e.printStackTrace();
//            }
//            
//        }).start();
//    }
    
    
}
