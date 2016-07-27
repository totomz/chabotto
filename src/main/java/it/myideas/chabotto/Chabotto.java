package it.myideas.chabotto;

import com.spotify.dns.DnsSrvResolver;
import com.spotify.dns.DnsSrvResolvers;
import com.sun.corba.se.impl.naming.cosnaming.NamingContextImpl;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javaslang.control.Either;

/**
 * Main class for this service registry. 
 * By default, it looks for an instance of redis listeining on localhost:6379
 * @author Tommaso Doninelli
 *
 */
public class Chabotto {

    private Chabotto(){}

    private static final Logger log = LoggerFactory.getLogger(Chabotto.class);
    
    /** Interval between two heartbeat signal in seconds */
    public static final int HEARTBEAT_SEC = 30;
    
    /** Pool for scheduled operations. All the services in the same jvm share this pool. 2 threads should be enough */
    private static final ScheduledExecutorService schedule = Executors.newScheduledThreadPool(2);   
    
    /** Scheduled tasks are saved in  map {uuid,task} */
    private static final HashMap<String, ScheduledFuture<?>> scheduledTasks = new HashMap<>();
    
    private static final DnsSrvResolver resolver = DnsSrvResolvers.newBuilder()
            .cachingLookups(false)
            .retainingDataOnFailures(false)
//            .metered(new StdoutReporter())
            .dnsLookupTimeoutMillis(1000)
            .build();
    
    /////////////
    // METHODS //
    /////////////
    
    // TODO Add a cleanup method to stop the threadpool 
    
    
    /**
     * 
     * Register a service
     * @param name The DNS name of the service to be registered (eg: redis.kuoko.porketta)
     * @param host The ip to register (eg: 192.168.14.201)
     * @param port The port on which this service is listening (eg: 6379)
     * @return {@link Either} an exception if something got wrong, or the service name 
     */
    public static Either<Exception, String> registerService(String name, String host, int port) {
    
        String uuid = UUID.randomUUID().toString().replaceAll("-", "");                        
        String payload = String.format("value={\"host\":\"%s\",\"port\":%s}&ttl=%s", host, port, HEARTBEAT_SEC);

        // This is how to reverse an array in Java?
        List<String> cheMerdaDiApi = Arrays.asList(name.split("\\."));
        Collections.reverse(cheMerdaDiApi);

        // TODO usare un nome di doninio! o una configurazione!
        String path = "http://127.0.0.1:2379/v2/keys/skydns/" + String.join("/", cheMerdaDiApi);

        if (log.isDebugEnabled()) {
            log.debug(new LogMap()
                    .put("action", "registering service")
                    .put("name", name)
                    .put("payload", payload)
                    .put("etcd", path).toString());
        }
        
        try {                  
            URL url = new URL(path);            
            HttpURLConnection connection = (HttpURLConnection)url.openConnection();
            connection.setRequestMethod("PUT");
            connection.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");

            connection.setUseCaches (false);
            connection.setDoInput(true);
            connection.setDoOutput(true);
            
            try(OutputStreamWriter wr = new OutputStreamWriter(connection.getOutputStream());){
                wr.write(payload);
                wr.flush();
                
                if(log.isDebugEnabled()) {
                    try(BufferedReader rd = new BufferedReader(new InputStreamReader(connection.getInputStream()));) {                
                        String line;
                        while ((line = rd.readLine()) != null) {
                            log.debug(new LogMap().put("response", line).toString());
                        }      
                    }    
                }                
            }            
            
            return Either.right(uuid);
        }
        catch (Exception e) {
            log.error(new LogMap().put("action", "registerService")
                    .put("service", name)
                    .put("host", host)
                    .put("port", port)
                    .put("error", e.getMessage()).toString(), e );
            return Either.left(e);
        }
                   
    }

    /**
     * Remove the service identified by its <b>uuid</b> from the registry, and from any other collection. 
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

    public static Stream<URI> listInstances(String name) {

        /*
         *  This method is a clusterfuck. 
         *  I need it only for internal testing. I know it wil still be here in 2018. 
         *  If you are reding this:
         *      1) Sorry for this method
         *      2) Rewrite it  
         */
        
        ArrayList<URI> temp = new ArrayList<>(1000);
        
        try(Jedis jedis = jedispool.getResource()) {
        
            ScanResult<String> keys;
            String scanIndex = "0";
            
            do {
                keys = jedis.scan(scanIndex, new ScanParams().match("cb8:service:"+name+":*"));
                scanIndex = keys.getStringCursor();
                temp.addAll(keys.getResult().stream()
                        .map(key -> {
                            return jedis.get(key);
                        })
                        .map(uri -> {
                            
                            try{return new URI(uri);}
                            catch (Exception e) {
                                e.printStackTrace();
                                return null;
                            }                
                        })            
                        
                        .collect(Collectors.toList()));
            }
            while (!scanIndex.equals("0"));
        
            
            
        }

        return temp.stream();

    }
}
