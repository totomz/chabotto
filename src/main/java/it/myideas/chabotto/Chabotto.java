package it.myideas.chabotto;

import java.net.URI;
import java.util.UUID;

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
    
    /**
     * Interval between two heartbeat signal in seconds
     */
    public static final int HEARTBEAT_SEC = 15;
    
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
            
            jedis.set(key, new URI(uri).toString());
            jedis.expire(key, 30);      
            
            return Either.right(uuid);
        }
        catch (Exception e) {
            log.error("Could not register the service " + name + " + with uri " + uri, e);
            return Either.left(e);
        }               
    }
    
    
}
