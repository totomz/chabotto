package it.myideas.chabotto;

import java.net.URI;
import java.util.UUID;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javaslang.control.Either;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;

/**
 * Main class for this service registry. 
 * By default, it looks for an instance of redis listeining on localhost:6379
 * @author Tommaso Doninelli
 *
 */
public class Chabotto {

    private Chabotto(){}

    private static JedisPool jedispool = new JedisPool();
    
    private static final Logger log = LoggerFactory.getLogger(Chabotto.class);
    
    /** Interval between two heartbeat signal in seconds */
    public static final int HEARTBEAT_SEC = 30;
    
    /** Pool for scheduled operations. All the services in the same jvm share this pool. 2 threads should be enough */
    private static final ScheduledExecutorService schedule = Executors.newScheduledThreadPool(2);   
    
    /////////////
    // METHODS //
    /////////////
    
    /**
     * Set the {@link JedisPool} to connect to the redis instance;
     * @param pool
     */
    public static void setJedisPool(JedisPool pool) {
        jedispool = pool;        
    }
    
    /**
     * 
     * Register a service
     * @param name The name of the service
     * @param uri A full URI string: <protocol>://<host>[:port][/path])
     * @return {@link Either} an exception if something got wrong, or the service unique identifier 
     */
    public static Either<Exception, String> registerService(String name, String uri) {

        
        
        try(Jedis jedis = jedispool.getResource()) {
            
            String uuid = UUID.randomUUID().toString().replaceAll("-", "");
            String key = "cb8:service:" + name + ":" + uuid;
            String value = new URI(uri).toString(); // We cast to URI to perform consistency check - this may be changed in the near future
            
            jedis.set(key, value);
            jedis.expire(key, 30);
            
            schedule.scheduleAtFixedRate(() -> {
                try(Jedis redis = jedispool.getResource()) {
                    
                    if(log.isDebugEnabled()){
                        log.debug("Heartbeat for " + key);
                    }
                    
                    jedis.set(key, value);
                    jedis.expire(key, 30);
                }                
                catch (Exception e) {
                    log.error("Heartbeat failed for service " + key, e);
                }
            }, 20, 20, TimeUnit.SECONDS);
            
            
            return Either.right(uuid);
        }
        catch (Exception e) {
            log.error("Could not register the service " + name + " + with uri " + uri, e);
            return Either.left(e);
        }               
    }
    
    
}
