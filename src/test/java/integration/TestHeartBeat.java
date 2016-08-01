package integration;

import com.spotify.dns.LookupResult;
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
import java.util.List;
import org.junit.Ignore;

public class TestHeartBeat extends BaseTest {

    private static Logger log = LoggerFactory.getLogger(TestHeartBeat.class);

    @Ignore
    public void testServiceHeartBeatFailure() throws InterruptedException, IOException {
        
        // Register 2 services in a separate process
        log.info("Testing heartbeat functionality");
        String service = "hbeat.test.services.porketta";
        
        Process serviceA = JavaProcess.exec(SimpleService.class, service + ";" + randomIp());
        Process serviceB = JavaProcess.exec(SimpleService.class, service + ";" + randomIp());
        
        // Register the process to be cleaned up in case of failures
        processToDestroy.add(serviceA);
        processToDestroy.add(serviceB);
        
        // DEBUG
        // see what is going on in the subprocess
        attachSysout(serviceA);
        attachSysout(serviceB);

        log.info("Subprocess started - wait for service registration");
        Thread.sleep(2 * 1000);
        List<LookupResult> records = resolver().resolve(service);
        assertEquals(2, records.size());
        
        log.info("Destroying subprocess");
        serviceA.destroy();
        serviceB.destroy();
        
        waitForHeartbeatTimeOut();
        
        assertFalse(serviceA.isAlive());
        assertFalse(serviceB.isAlive());
        
        log.info("Verify that are not there");
        assertEquals(0, resolver().resolve(service).size());
        
    }
    
    @Ignore
    public void testServiceHeartBeat() throws IOException, InterruptedException {
        
        // Register 2 services in a separate process
        String service = "hbeat2.test.services.porketta";
        
        Process serviceA = JavaProcess.exec(SimpleService.class, service + ";" + randomIp());
        Process serviceB = JavaProcess.exec(SimpleService.class, service + ";" + randomIp());
        
        // Register the process to be cleaned up in case of failures
        processToDestroy.add(serviceA);
        processToDestroy.add(serviceB);
        
        // DEBUG
        // see what is going on in the subprocess
        attachSysout(serviceA);
        attachSysout(serviceB);

        log.info("Subprocess started - wait for service registration");
        Thread.sleep(2 * 1000);
        assertEquals(2, resolver().resolve(service).size());
     
        waitForHeartbeatTimeOut();
        waitForHeartbeatTimeOut();

        log.info("Verify that the services are still there");
        assertTrue(serviceA.isAlive());
        assertTrue(serviceB.isAlive());
        
        assertEquals(2, resolver().resolve(service).size());

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
