package integration;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

//import java.net.InetAddress;
import java.net.URISyntaxException;
import java.net.UnknownHostException;
import java.util.List;
import java.util.Optional;
import java.util.Random;

import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.spotify.dns.DnsSrvResolver;
import com.spotify.dns.DnsSrvResolvers;
import com.spotify.dns.LookupResult;

import it.myideas.chabotto.Chabotto;
import javaslang.control.Try;
import net._01001111.text.LoremIpsum;

public class TestServiceRegistration extends BaseTest {
   
    private static final Logger log = LoggerFactory.getLogger(TestServiceRegistration.class);
    
    static {
        try {            
            TestServiceRegistration.class.getClassLoader().loadClass("org.eclipse.jdt.internal.junit4.runner.JUnit4TestLoader");
            log.error("Running test FROM Eclipse does not work. QUITTING"); 
            /*
                Long answer:
                Chabotto needs some JVM flags to work. maven-surefire set these variables (see the pom), but 
                when tests are run "Run as JUnit" from inside Eclipse, Eclipse ignore the pom. To make these test works we should set the environment in a static block
                which is bad and not portable. You can safely run the tests from Eclipse invoking a mave goal instead by doing right-click on the class
            */
            System.exit(1);            
        }
        catch (Exception e) {}
        
    }
    
    
    @Test
    public void testServiceRegistration() throws URISyntaxException, UnknownHostException, InterruptedException {
        
        String name = randomSubDomain("test.services.porketta");
        String ip = randomIp();
        
        String uuid = Chabotto.registerService(name, ip, 8080).get();
        
        /* UUID is the fqdn name for the registered service. This is NOT API and *can* change in the future */
        assertTrue("Can't resolve test domain name " + uuid, Try.of(()-> {return java.net.InetAddress.getByName(uuid).getHostAddress();}).isSuccess());        
        assertTrue("Can't resolve a common domain", Try.of(()-> {return java.net.InetAddress.getByName("www.google.com").getHostAddress();}).isSuccess());        
        assertEquals("dnsjava not found in classpath", "class org.xbill.DNS.ARecord", org.xbill.DNS.ARecord.class.toString());
        
        // Verify that the service has been registered by JVM
        assertTrue("Can't resolve test domain name " + name, Try.of(()-> {return java.net.InetAddress.getByName(name).getHostAddress();}).isSuccess());
        
        // Check that it is being resolved as SRV record
        List<LookupResult> lookup = resolver().resolve(name);        
        assertEquals(1, lookup.size());
        assertEquals(ip, java.net.InetAddress.getByName(name).getHostAddress());
        
        // Be sure that the service is going to autoexpire
        assertTrue("TTL should be lower than Chabotto.HEARTBEAT_SEC, but is" + lookup.get(0).ttl(), lookup.get(0).ttl() <= Chabotto.HEARTBEAT_SEC);
        
        /* For testing it may be usefull to increase the heartbeat different than 30sec. A test will be commented and never re-enabled. So we log a warning, a visible one.*/        
        if(Chabotto.HEARTBEAT_SEC > 30) {            
            log.warn("*************************************");
            log.warn("*************************************");
            log.warn("*************************************");
            log.warn("");
            log.warn("");
            log.warn("Chabotto.HEARTBEAT_SEC is set to " + Chabotto.HEARTBEAT_SEC + "! It is supposed to be 30!!!");
            log.warn("");
            log.warn("");
            log.warn("*************************************");
            log.warn("*************************************");
            log.warn("*************************************");
        }
        
    }
    
    @Test
    public void testServiceDeregistration() throws UnknownHostException, InterruptedException, Exception {
        
        String name = randomSubDomain("test.services.porketta");
        String ip = randomIp();
        
        String uuid = Chabotto.registerService(name, ip, 8080).get();
        assertTrue("Can't resolve registered service name " + uuid, Try.of(()-> {return java.net.InetAddress.getByName(uuid).getHostAddress();}).isSuccess());
        
        // Try to unregister a service not registered by Chabotto
        String fakeUuid = "zzz." + name;
        Optional<Exception> op = Chabotto.unregisterService(fakeUuid);
        assertEquals(Boolean.TRUE, op.isPresent());
        assertEquals(IllegalArgumentException.class, op.get().getClass());
                
        // Now deregister a valid service
        Optional<Exception> deregisterOp = Chabotto.unregisterService(uuid);
        assertEquals(Boolean.FALSE, deregisterOp.isPresent());
        
        // Now we should not be able to resolve an host
        Try<String> resolveNXDOMAIN =  Try.of(() -> {return java.net.InetAddress.getByName(name).getHostAddress();});
        assertEquals("Unregistere service is still being resolved; did you forget to set -Dnetworkaddress.cache.ttl? ", Boolean.TRUE, resolveNXDOMAIN.isFailure());        
        assertEquals(resolveNXDOMAIN.getCause().getClass(), UnknownHostException.class);
        
        // UUID NXDOMAIN
        assertTrue("Still resolving removed service " + uuid, Try.of(()-> {return java.net.InetAddress.getByName(uuid).getHostAddress();}).isFailure());
    }   
}
