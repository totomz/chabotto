package it.myideas.chabotto;

import java.net.URI;
import java.util.HashMap;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
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
    
    /** Scheduled tasks are saved in  map {uuid,task} */
    private static final HashMap<String, ScheduledFuture<?>> scheduledTasks = new HashMap<>();
    
    /////////////
    // METHODS //
    /////////////
    
    // TODO Add a cleanup method to stop the threadpool 
    
    
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
            String keyService = getServiceKey(name, uuid);
            String value = new URI(uri).toString(); // We cast to URI to perform consistency check - this may be changed in the near future
            
            // Register this service
            jedis.set(keyService, value);
            jedis.expire(keyService, 30);
            
            // Add it to the rrobin list of available services
            logdebug(new LogMap()
                    .put("action", "pushing to rrobin queue")
                    .put("instance", uuid)
                    .put("queue", getServiceList(name)));
            
            jedis.lpush(getServiceList(name), uuid);
            
            ScheduledFuture<?> task = schedule.scheduleAtFixedRate(() -> {
                try(Jedis _redis = jedispool.getResource()) {
                    
                    logdebug(new LogMap().put("action", "heartbeat").put("keyservice", keyService).put("instance", value));
                    
                    _redis.set(keyService, value);
                    _redis.expire(keyService, 30);
                }                
                catch (Exception e) {
                    log.error(new LogMap().put("action", "heartbet")
                            .put("service", keyService)
                            .put("error", e.getMessage()).toString(), e );
                }
            }, 20, 20, TimeUnit.SECONDS);
            
            scheduledTasks.put(uuid, task);
            
            return Either.right(uuid);
        }
        catch (Exception e) {
            log.error(new LogMap().put("action", "registerService")
                    .put("service", name)
                    .put("uri", uri)
                    .put("error", e.getMessage()).toString(), e );
            return Either.left(e);
        }               
    }

    /**
     * Remove the service identified by its <b>uuid</b> from the registry, and from any other collection. </br>
     * If the service is not managed by this vm (which means, the service has not been registered by <b>this</b> instance of Chabotto), the service is not removed 
     * and an {@link IllegalArgumentException} is returned 
     * @param uuid the unique identifier of the service to remove
     * @param name The name of the service to be deleted
     * @return {@link Optional}ly an Exception, ONLY if something goes wrong. 
     */
    public static Optional<Exception> unregisterService(String name, String uuid) {

        /*
         * Return an exception if the service is not managed by me - what does is mean? 
         * When I strt a service, I regi
         */
        if(!scheduledTasks.containsKey(uuid)) {
            return Optional.of(new IllegalArgumentException("No service with uuid="+uuid));
        }
        
        logdebug(new LogMap().put("action", "stop heartbeat").put("uuid", uuid));        
        scheduledTasks.get(uuid).cancel(true);
        
        logdebug(new LogMap().put("action", "removing from registry").put("uuid", uuid));       
        try(Jedis jedis = jedispool.getResource()) {
            
            jedis.del(getServiceKey(name, uuid));
        }
        
        catch (Exception e) {
            log.error("Can't unregister service " + uuid, e);
            return Optional.of(e);
        }
        
        return Optional.empty();
    }

    private static String getServiceKey(String name, String uuid) {
        return "cb8:service:" + name + ":" + uuid;
    }
    
    private static String getServiceList(String name) {
        return "cb8:serlist:" + name;
    }

    /**
     * Returns an {@link URI} to connect to a node providing the service 
     * @param name The name of the service to look for
     * @return {@link URI} to connect to the service
     */
    public static Either<Exception, URI> getServiceRoundRobin(String name) {

        boolean keepLooking = true;
        Either<Exception, URI> instance = null;
        
        while(keepLooking) {
            instance = getRr(name);
            
            if(instance.isRight()) {
                break; 
            }
            
            // If NoSuchElementException, the instance has been removed from the registry
            keepLooking = instance.isLeft() && instance.getLeft().getClass().equals(NoSuchElementException.class);
        }          
        
        return instance;
    }
    
    private static Either<Exception, URI> getRr(String name) {
        
        try(Jedis jedis = jedispool.getResource()) {
           
            String key = getServiceList(name);
            String uuid = jedis.rpoplpush(key, key);
            String value = jedis.get(getServiceKey(name, uuid));
            
            if(uuid == null) {
                return Either.left(new IllegalArgumentException("No instance found for service " + name));
            }
             
            if(value == null) {
                logdebug(new LogMap().put("action", "foundRoundRobin").put("service", name).put("uuid", uuid).put("message", "nstance has been deregistered - removing it"));
                jedis.lrem(key, 0, uuid);
                return Either.left(new NoSuchElementException("Instance " + uuid + " is missing - maybe has been deregistered?"));
            }
            
            return Either.right(new URI(value));
        }
        catch (Exception e) {
            log.error(new LogMap().put("action", "getServiceDetail")
                    .put("service", name)
                    .put("error", e.getMessage()).toString(), e );
            return Either.left(e);
        }     
    }
    
    private static void logdebug(LogMap message) {
        if(log.isDebugEnabled()){
            log.debug(message.toString());
        }
    }
}
