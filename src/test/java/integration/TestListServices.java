package integration;

import static org.junit.Assert.assertEquals;

import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;

import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import it.myideas.chabotto.Chabotto;

public class TestListServices extends BaseTest {

    static {
        // The default is DEBUG - and it will log *A LOT*
        System.setProperty("org.slf4j.simpleLogger.log.it.myideas.chabotto.Chabotto", "INFO");
    }
    
    
    private static Logger log = LoggerFactory.getLogger(TestListServices.class);
    
    @Test
    public void testListOfServices() throws InterruptedException, URISyntaxException, MalformedURLException {
        
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
        
        log.info("ELAPSED: " + (System.currentTimeMillis() - start) + "ms");
        
    }
}
