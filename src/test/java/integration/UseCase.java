package integration;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import integration.javaprocess.JavaProcess;
import it.myideas.chabotto.Chabotto;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.ScanParams;

public class UseCase {

    private Jedis jedis;
    
    private static Logger log = LoggerFactory.getLogger(UseCase.class);
    
    @Before
    public void init() {
        jedis = new Jedis();
    }
    
    @After
    public void cleanup() {
        
        jedis.scan("0", new ScanParams().match("*"))
            .getResult()
//            .stream().map(s -> {System.out.println(s); return s;})
//            .count()
            .forEach(jedis::del)
        ;
        jedis.close();
    }
    
    @Test
    public void testServiceRegistration() throws URISyntaxException {
        
        String name = "simpleService";
        
        Chabotto.registerService(name, "myprotocol://ahost.name.com:125/pippo/pluto/paperino");
        
        // Verify that the service has been registered        
        List<String> services = jedis.scan("0", new ScanParams().match(String.format("cb8:service:%s:*", name))).getResult();
        assertEquals(1, services.size());
        
        String uuid = services.get(0).substring(services.get(0).lastIndexOf(":") + 1);
        
        URI uri = new URI(jedis.get("cb8:service:" + name + ":" + uuid));
        
        assertEquals("myprotocol", uri.getScheme());
        assertEquals("ahost.name.com", uri.getHost());
        assertEquals("/pippo/pluto/paperino", uri.getPath());
        assertEquals(125, uri.getPort());       
        
        // Be sure that the service is going to autoexpire
        assertTrue("key TTL seems not set", jedis.ttl("cb8:service:" + name + ":" + uuid) > 20);
    }
    
    @Test
    public void testServiceHeartBeatFailure() throws InterruptedException, IOException {
        
        // Register 2 services in a separate process
        Process serviceA = JavaProcess.exec(SimpleService.class, "host1.com");
        Process serviceB = JavaProcess.exec(SimpleService.class, "host2.com");
        
        // DEBUG
        // see what is going on in the subprocess
        attachSysout(serviceA);
        attachSysout(serviceB);

        log.info("Subprocess started - wait for service registration");
        Thread.sleep(2 * 1000);
        assertEquals(2, jedis.scan("0", new ScanParams().match("cb8:service:peppapig:*")).getResult().size());
        
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
        assertEquals(0, jedis.scan("0", new ScanParams().match("cb8:service:peppapig:*")).getResult().size());
        
    }
    
    @Test
    public void testServiceHeartBeat() throws IOException, InterruptedException {
        
        // Register 2 services in a separate process
        Process serviceA = JavaProcess.exec(SimpleService.class, "host1.com");
        Process serviceB = JavaProcess.exec(SimpleService.class, "host2.com");
        
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
