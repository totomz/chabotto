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
import javaslang.control.Either;

public class TestServiceRegistration extends BaseTest {
    
    private static final DnsSrvResolver resolver = DnsSrvResolvers.newBuilder()
            .cachingLookups(false)
            .retainingDataOnFailures(false)
//            .metered(new StdoutReporter())
            .dnsLookupTimeoutMillis(1000)
            .build();
    
    @Test
    public void testServiceRegistration() throws URISyntaxException, UnknownHostException, InterruptedException {
        
        String name = "db.test.services.porketta";
        
        String uuid = Chabotto.registerService(name, "151.100.152.95", 8080).get();
        
        // Verify that the service has been registered
        List<LookupResult> lookup = resolver.resolve(name);
        
        assertEquals(1, lookup.size());
        assertEquals("151.100.152.95", InetAddress.getByName(name).getHostAddress());
        
        // Be sure that the service is going to autoexpire
        assertTrue(lookup.get(0).ttl() < 30);        
        
    }
    
//    @Test
    public void testServiceDeregistration() throws UnknownHostException {
        
        String name = "jack.test.services.porketta";
        String uuid = Chabotto.registerService(name, "151.100.152.220", 8080).get();
        
        System.out.println("---" + InetAddress.getByName(name).getHostAddress());
        
        // Try to unregister a service not registered by Chabotto
        String fakeUuid = "21309." + name;
        Optional<Exception> op = Chabotto.unregisterService(fakeUuid);
        assertTrue(op.isPresent());
        assertEquals(IllegalArgumentException.class, op.get().getClass());
        
        // Now deregister a valid service
        Optional<Exception> deregisterOp = Chabotto.unregisterService(uuid);
        assertFalse(deregisterOp.isPresent());
        
        System.out.println("---" + InetAddress.getByName(name).getHostAddress());
        
        // Check that the service has been removed from any queue. 
        // But wait to be sure that the heartbeat is not running
//        waitForHeartbeatTimeOut();        
        
        
        // Now, the service should not be available
//        Either<Exception, URI> insance = Chabotto.getServiceRoundRobin(name);
//        assertTrue(insance.isLeft());
//        assertEquals(IllegalArgumentException.class, insance.getLeft().getClass());
        
        // TODO Gira su tutte le altre chiavi!
        
        
    }
}
