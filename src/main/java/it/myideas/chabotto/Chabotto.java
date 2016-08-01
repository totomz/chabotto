package it.myideas.chabotto;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.HashMap;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.spotify.dns.DnsSrvResolver;
import com.spotify.dns.DnsSrvResolvers;
import it.myideas.chabotto.dnsjava.NoServiceCache;
import java.security.Security;

import javaslang.control.Either;
import org.xbill.DNS.Lookup;
import org.xbill.DNS.Type;

/**
 * Main class for this service registry. 
 * By default, it looks for an instance of redis listeining on localhost:6379
 * @author Tommaso Doninelli
 *
 */
public class Chabotto {
    
    private static final Logger log = LoggerFactory.getLogger(Chabotto.class);
    
    static {
        // Chabotto works with DNS. If DNS are not set correctly, notify to the user and quit
        
        if(System.getProperty("sun.net.spi.nameservice.provider.1") == null || !System.getProperty("sun.net.spi.nameservice.provider.1").equals("dns,dnsjava")) {
            log.error("Chabotto requires dnsjava as JVM dns resolver; please invoke jvm with -Dsun.net.spi.nameservice.provider.1=dns,dnsjava");
            System.exit(1);
        }
        
        if(Security.getProperty("networkaddress.cache.ttl") == null && System.getProperty("sun.net.inetaddr.ttl") == null) {
            log.error("Chabotto can't work with DNS caching enabled. Run JVM with -Dsun.net.inetaddr.ttl=0");
            System.exit(1);
        }
        
        if(Security.getProperty("networkaddress.cache.negative.ttl") == null && System.getProperty("sun.net.inetaddr.negative.ttl") == null) {
            log.error("Chabotto can't work with DNS caching enabled. Run JVM with -Dsun.net.inetaddr.negative.ttl=0");
            System.exit(1);
        }
        
        if(System.getProperty("chabotto.servname") == null) {
            log.error("Chabotto can't work without a root domain name for the service to serve; please run JVM with -Dchabotto.servname=");
            System.exit(1);            
        }
        
        // Now fake the cache
        String dname = System.getProperty("chabotto.servname");
        Lookup.setDefaultCache(new NoServiceCache(dname), Type.ANY);
        Lookup.setDefaultCache(new NoServiceCache(dname), Type.A);
        Lookup.setDefaultCache(new NoServiceCache(dname), Type.AAAA);
        Lookup.setDefaultCache(new NoServiceCache(dname), Type.SRV);        
    }

    private Chabotto(){}

    
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
    
    private static Optional<Exception> httpCall(String method, String path, String payload) {
        System.out.println("AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA");
        if (log.isDebugEnabled()) {
            log.debug(new LogMap()
                    .put("action", "httpCall")
                    .put("method", method)
                    .put("path", path)
                    .toString());
        }
        
        try {                  
            URL url = new URL(path);            
            HttpURLConnection connection = (HttpURLConnection)url.openConnection();
            connection.setRequestMethod(method);
            
            connection.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
            connection.setUseCaches (false);
            
            if(payload != null ) {
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
            }
            else {
                connection.connect();
                connection.getInputStream().close();
            }
             
                    
                               
        }
        catch (Exception e) {
            log.error(new LogMap().put("action", "putRecord")
                    .put("path", path)
                    .put("payload", payload)
                    .put("error", e.getMessage()).toString(), e );
            return Optional.of(e);
        }
        
        return Optional.empty();
    }
    
    /**
     * 
     * @param domain something like aa.bb.cc
     * @return A path like /cc/bb/aa
     */
    private static String domainToPath(String domain) {
        // This is how to reverse an array in Java?
        String[] tokens = domain.split("\\.");
        StringBuilder sb = new StringBuilder();
        for(int i=tokens.length - 1;i>-1;i--){
            sb.append("/").append(tokens[i]);
        }
        return sb.toString();
    }
    
    /**
     * 
     * Register a service
     * @param name The DNS name of the service to be registered (eg: redis.kuoko.porketta)
     * @param host The ip to register (eg: 192.168.14.201)
     * @param port The port on which this service is listening (eg: 6379)
     * @return {@link Either} an exception if something got wrong, or the service name 
     */
    public static Either<Exception, String> registerService(String name, String host, int port) {
    
        // Using only 8 digits we may occure in collisoin. 
        // But this UUIS will be prepended on a url, andI don0't think there will be more than 100s of servieces for the same endpoint
        String uuid = UUID.randomUUID().toString().substring(0, 8) + "." + name;

        // TODO usare un nome di doninio! o una configurazione!
        final String path = "http://127.0.0.1:2379/v2/keys/skydns" + domainToPath(uuid);
        final String payload = String.format("value={\"host\":\"%s\",\"port\":%s}&ttl=%s", host, port, HEARTBEAT_SEC);

        if (log.isDebugEnabled()) {
            log.debug(new LogMap()
                    .put("action", "registering service")
                    .put("name", name)
                    .put("payload", payload)
                    .put("etcd", path).toString());
        }
        
        
        ScheduledFuture<?> task = schedule.scheduleAtFixedRate(
                () -> {httpCall("PUT", path, payload);}, 
                0, // run NOW 
                (long) Math.floor(HEARTBEAT_SEC * 0.9),     // Refresh *before* of the expiry time
        TimeUnit.SECONDS);
            
        scheduledTasks.put(uuid, task);
                
        // Sleep a while to allow the baschedule to run
        try{Thread.sleep(10);}catch(Exception e){}
        
        return Either.right(uuid);
    }

    /**
     * Remove the service identified by its <b>uuid</b> from the registry, and from any other collection. 
     * If the service is not managed by this vm (which means, the service has not been registered by <b>this</b> instance of Chabotto), the service is not removed 
     * and an {@link IllegalArgumentException} is returned 
     * @param uuid the unique identifier of the service to remove
     * @return {@link Optional}ly an Exception, ONLY if something goes wrong. 
     */
    public static Optional<Exception> unregisterService(String uuid) {

        /*
         * Return an exception if the service is not managed by me - what does is mean? 
         * When I strt a service, I regi
         */
        if(!scheduledTasks.containsKey(uuid)) {
            return Optional.of(new IllegalArgumentException("No service with uuid="+uuid));
        }
        
        if (log.isDebugEnabled()) {
            log.debug(new LogMap().put("action", "stop heartbeat").put("uuid", uuid).toString());
        }
        scheduledTasks.get(uuid).cancel(true);
        
        // TODO remove the record from the registry. I don't want to do it now.
        httpCall("DELETE",  "http://127.0.0.1:2379/v2/keys/skydns" + domainToPath(uuid), null);
        
        return Optional.empty();
    }


//    /**
//     * Returns an {@link URI} to connect to a node providing the service 
//     * @param name The name of the service to look for
//     * @return {@link URI} to connect to the service
//     */
//    public static Either<Exception, URI> getServiceRoundRobin(String name) {
//
//        boolean keepLooking = true;
//        Either<Exception, URI> instance = null;
//        
//        while(keepLooking) {
//            instance = getRr(name);
//            
//            if(instance.isRight()) {
//                break; 
//            }
//            
//            // If NoSuchElementException, the instance has been removed from the registry
//            keepLooking = instance.isLeft() && instance.getLeft().getClass().equals(NoSuchElementException.class);
//        }          
//        
//        return instance;
//    }
//    
//    private static Either<Exception, URI> getRr(String name) {
//        
//        try(Jedis jedis = jedispool.getResource()) {
//           
//            String key = getServiceList(name);
//            String uuid = jedis.rpoplpush(key, key);
//            String value = jedis.get(getServiceKey(name, uuid));
//            
//            if(uuid == null) {
//                return Either.left(new IllegalArgumentException("No instance found for service " + name));
//            }
//             
//            if(value == null) {
//                logdebug(new LogMap().put("action", "foundRoundRobin").put("service", name).put("uuid", uuid).put("message", "nstance has been deregistered - removing it"));
//                jedis.lrem(key, 0, uuid);
//                return Either.left(new NoSuchElementException("Instance " + uuid + " is missing - maybe has been deregistered?"));
//            }
//            
//            return Either.right(new URI(value));
//        }
//        catch (Exception e) {
//            log.error(new LogMap().put("action", "getServiceDetail")
//                    .put("service", name)
//                    .put("error", e.getMessage()).toString(), e );
//            return Either.left(e);
//        }     
//    }
//    
//    private static void logdebug(LogMap message) {
//        if(log.isDebugEnabled()){
//            log.debug(message.toString());
//        }
//    }
//
//    public static Stream<URI> listInstances(String name) {
//
//        /*
//         *  This method is a clusterfuck. 
//         *  I need it only for internal testing. I know it wil still be here in 2018. 
//         *  If you are reding this:
//         *      1) Sorry for this method
//         *      2) Rewrite it  
//         */
//        
//        ArrayList<URI> temp = new ArrayList<>(1000);
//        
//        try(Jedis jedis = jedispool.getResource()) {
//        
//            ScanResult<String> keys;
//            String scanIndex = "0";
//            
//            do {
//                keys = jedis.scan(scanIndex, new ScanParams().match("cb8:service:"+name+":*"));
//                scanIndex = keys.getStringCursor();
//                temp.addAll(keys.getResult().stream()
//                        .map(key -> {
//                            return jedis.get(key);
//                        })
//                        .map(uri -> {
//                            
//                            try{return new URI(uri);}
//                            catch (Exception e) {
//                                e.printStackTrace();
//                                return null;
//                            }                
//                        })            
//                        
//                        .collect(Collectors.toList()));
//            }
//            while (!scanIndex.equals("0"));
//        
//            
//            
//        }
//
//        return temp.stream();
//
//    }
}
