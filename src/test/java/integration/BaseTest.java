package integration;

import java.util.ArrayList;

import org.junit.After;
import org.junit.Before;

import redis.clients.jedis.Jedis;
import redis.clients.jedis.ScanParams;

/**
 * Common init and cleanup for all the tests
 * @author Tommaso Doninelli
 *
 */
public class BaseTest {
    
    static {
        System.setProperty(org.slf4j.impl.SimpleLogger.DEFAULT_LOG_LEVEL_KEY, "TRACE");
    }
    
    protected Jedis jedis;    
    protected ArrayList<Process> processToDestroy;
    
    
    @Before
    public void init() {
        System.out.println("************ INIT");
        jedis = new Jedis();
        processToDestroy = new ArrayList<>();
        cleanup();
    }
    
    @After
    public void cleanup() {
        System.out.println("************ CLEANUP");
        
        // If an assert fail, subprocess may still be alive
        processToDestroy.forEach((process) -> {
            
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
}
