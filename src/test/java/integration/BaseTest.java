package integration;

import com.spotify.dns.DnsSrvResolver;
import com.spotify.dns.DnsSrvResolvers;
import java.util.ArrayList;

import org.junit.After;
import org.junit.Before;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import it.myideas.chabotto.Chabotto;
import java.net.InetAddress;
import java.util.Random;
import javaslang.control.Try;
import net._01001111.text.LoremIpsum;

/**
 * Common init and cleanup for all the tests
 * @author Tommaso Doninelli
 *
 */
public class BaseTest {
    
    static {
        System.setProperty(org.slf4j.impl.SimpleLogger.DEFAULT_LOG_LEVEL_KEY, "DEBUG");
        System.setProperty(org.slf4j.impl.SimpleLogger.LOG_FILE_KEY, "System.out");               
     }
    
    private static final Logger log = LoggerFactory.getLogger(BaseTest.class);
    protected ArrayList<Process> processToDestroy;
    
    
    @Before
    public void init() {
        processToDestroy = new ArrayList<>();
        cleanup();
    }
    
    @After
    public void cleanup() {
        // If an assert fail, subprocess may still be alive
        processToDestroy.forEach((process) -> {
            
            try{
                process.destroy();
                Thread.sleep(2000);
            }
            catch (Exception e) {
                System.out.println(e.getMessage());
            }
            
        });        
    }
    
    protected void waitForHeartbeatTimeOut() {
        log.info("The service should expire after " + Chabotto.HEARTBEAT_SEC + " seconds. Waiting");
        int tick = Chabotto.HEARTBEAT_SEC + 5;
        for(int i=0;i<tick;i++) {
            log.info("waiting....." + i + "/" + tick);
            Try.of(() ->{Thread.sleep(1 * 1000);return "";});    
        }
    }
    
    /**
     * 
     * @param string The {@link String} that will be randomized 
     * @return the input string + Math.random()
     */
    public String randomize(String string) {
        return string + (int)(Math.random()*1000);
    }
    
    public DnsSrvResolver resolver() {
        return DnsSrvResolvers.newBuilder()
            .cachingLookups(false)
            .retainingDataOnFailures(false)
            .dnsLookupTimeoutMillis(1000)
            .build();
    }
    
    public String randomSubDomain(String domain){
        return new LoremIpsum().randomWord() + "." + domain;
    }
    
    public String randomIp(){        
        return "151.100.152." + Integer.toString( new Random().nextInt(254) );
    }
}
