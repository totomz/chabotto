package integration;

import static org.junit.Assert.assertEquals;

import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import it.myideas.chabotto.Chabotto;

public class TestListServices extends BaseTest {

    static {
       System.setProperty(org.slf4j.impl.SimpleLogger.DEFAULT_LOG_LEVEL_KEY, "INFO");
    }
    
    private static Logger log = LoggerFactory.getLogger(TestListServices.class);
    
    @Test
    public void testListOfServices() throws InterruptedException {
        
        // Registering 1500 services
        int nServices = 1500;
//        int nServices = 3;
        
        log.info("Registering " + nServices + " services");
        for(int i=0;i<nServices; i++) {
            Chabotto.registerService("ciaone", "https://10.0.1.1:" + i + "/ciaone");
        }
        
        log.info("Counting using jedis.keys");
        assertEquals(nServices, jedis.keys(("cb8:service:ciaone:*")).size());
        
        
        log.info("Counting uing Chabotto");
        long start = System.currentTimeMillis();
        assertEquals(nServices, Chabotto.listInstances("ciaone").count());
        
        System.out.println("ELAPSED: " + (System.currentTimeMillis() - start) + "ms");
        
//        jedis.scan("0", new ScanParams().match("cb8:service:ciaone:*")).getResult().forEach(System.out::println);
    }
}
