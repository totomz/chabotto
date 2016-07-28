package integration;

import com.spotify.dns.DnsSrvResolver;
import com.spotify.dns.DnsSrvResolvers;
import com.spotify.dns.LookupResult;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;
import java.util.Optional;

import org.junit.Test;

import it.myideas.chabotto.Chabotto;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Random;
import javaslang.control.Either;
import javaslang.control.Try;
import net._01001111.text.LoremIpsum;

public class TestServiceRegistration extends BaseTest {
   
//    static {
//        // Chabotto works with DNS. So we are setting dns here
//        
//        System.setProperty("sun.net.spi.nameservice.nameservers", "127.0.0.1");
//        System.setProperty("sun.net.spi.nameservice.provider.1", "dns,dnsjava");
//
//        java.security.Security.setProperty("sun.net.spi.nameservice.nameservers", "127.0.0.1");
//        java.security.Security.setProperty("sun.net.spi.nameservice.provider.1", "dns,dnsjava");
//        
//        java.security.Security.setProperty("networkaddress.cache.ttl" , "0");
//        java.security.Security.setProperty("networkaddress.cache.negative.ttl" , "0");
//        
//        // Don't know why, but if I do not resolve a host right after setting the properties, it won't work....
//        try {System.out.println("STOKAZZOOOO" + InetAddress.getByName("www.google.com").getHostAddress());}
//        catch (Exception e) {e.printStackTrace();}
//    }
    
    private DnsSrvResolver resolver() {
        return DnsSrvResolvers.newBuilder()
            .cachingLookups(false)
            .retainingDataOnFailures(false)
//            .metered(new StdoutReporter())
            .dnsLookupTimeoutMillis(1000)
            .build();
    }
    
    
    @Test
    public void testServiceRegistration() throws URISyntaxException, UnknownHostException, InterruptedException {
        
        String name = "db.test.services.porketta";
        
        printp("sun.net.spi.nameservice.nameservers");
        printp("sun.net.spi.nameservice.provider.1");
        printp("networkaddress.cache.ttl");
        printp("networkaddress.cache.negative.ttl");
        
        String uuid = Chabotto.registerService(name, "151.100.152.95", 8080).get();
        
        // Verify that the service has been registered
        List<LookupResult> lookup = resolver().resolve(name);
        
        assertEquals(1, lookup.size());
        assertEquals("151.100.152.95", InetAddress.getByName(name).getHostAddress());
        
        // Be sure that the service is going to autoexpire
//        assertTrue(lookup.get(0).ttl() < 30);        
        System.out.println("NETBEANS MI FA CAGARE");
        
    }
    
//    @Test
    public void testServiceDeregistration() throws UnknownHostException {
        
        String name = new LoremIpsum().randomWord() + ".test.services.porketta";
        String ip = "151.100.152." + Integer.toString( new Random().nextInt(254) );
        
        System.out.println("Registering " + name + " to " + ip);
        
        System.out.println(Try.of(() -> {return InetAddress.getByName(name).getHostAddress();})
                .getOption()
                .map(v -> {System.out.println("SIIIIII " + v); return v;})                
                .getOrElse("NOOOOO"));

        
        String uuid = Chabotto.registerService(name, ip, 8080).get();
        
        System.out.println("---" + InetAddress.getByName(name).getHostAddress());
        
        // Try to unregister a service not registered by Chabotto
        String fakeUuid = "21309." + name;
        Optional<Exception> op = Chabotto.unregisterService(fakeUuid);
        assertTrue(op.isPresent());
        assertEquals(IllegalArgumentException.class, op.get().getClass());
        
        // Now deregister a valid service
        Optional<Exception> deregisterOp = Chabotto.unregisterService(uuid);
        assertFalse(deregisterOp.isPresent());
        
        // Now we should not be able to resolve an host
        Try<String> resolveNXDOMAIN =  Try.of(() -> {return InetAddress.getByName(name).getHostAddress();});
        assertTrue(resolveNXDOMAIN.isFailure());
        assertTrue(resolveNXDOMAIN.getCause() instanceof UnknownHostException);
        
        
        // Check that the service has been removed from any queue. 
        // But wait to be sure that the heartbeat is not running
//        waitForHeartbeatTimeOut();        
        
        
        // Now, the service should not be available
//        Either<Exception, URI> insance = Chabotto.getServiceRoundRobin(name);
//        assertTrue(insance.isLeft());
//        assertEquals(IllegalArgumentException.class, insance.getLeft().getClass());
        
        // TODO Gira su tutte le altre chiavi!
        
        
    }
    
    private static void printp(String property) {        
        System.out.println("System.setproperty:" + property + " = " + System.getProperty(property));
        System.out.println("System.getenv     :" + property + " = " + System.getenv(property));
        System.out.println("System.Security   :" + property + " = " + java.security.Security.getProperty(property));        
    }
}
