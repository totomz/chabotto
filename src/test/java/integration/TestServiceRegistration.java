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
    
    /*
     * Another thing to be ashamed of
     * I use Eclipse. I know that Netbeans *is* better, and is better integrated with maven. 
     * But, after 10 years, I am *NOT* able to leave Eclipse. 
     * 
     * These tests need some jvm flags to be set up. These flags are declared in maven-surefire, so in clever environment (commandline, IDE) it works by definition.
     * To "Run as JUnit" in Eclipse, I have to manually add these flags. 
     */    
    /*
     * Ok, new info: it is *NOT* possible to run these tests from Eclipse. 
     * java.net.InetAddress is initialized before of this class, so the required jvm flags are ignored.
     */
//    private static final String classToTest = "java.net.InetAddress";
//    private static final String classToTest = "acca.TestInetUtils";
//    private static final String classToTest = "org.eclipse.jdt.internal.junit4.runner.JUnit4TestLoader";
    static {
        
        try {            
            
//            System.out.println("========================================");
//            
//            java.lang.reflect.Method m = ClassLoader.class.getDeclaredMethod("findLoadedClass", new Class[] { String.class });
//            m.setAccessible(true);
//            ClassLoader cl = ClassLoader.getSystemClassLoader();
//            Object test1 = m.invoke(cl, classToTest);
//            System.out.println(test1 != null);
//            System.out.println(InetAddress.getByName("www.google.com").getHostAddress());
//            TestInetUtils.class.getName();
//            Object test2 = m.invoke(cl, classToTest);
//            System.out.println(test2 != null);
//            
//            System.out.println("========================================");
//            
            TestServiceRegistration.class.getClassLoader().loadClass("org.eclipse.jdt.internal.junit4.runner.JUnit4TestLoader");
            log.error("Running test INSIDE eclipse does not work. And I don't know why. QUITTING");
            System.exit(1);

//            System.setProperty("dns.server", "127.0.0.1");                          // Questo da dnsjava; see org.xbill.DNS.ResolverConfig
//            System.setProperty("sun.net.spi.nameservice.provider.1", "dns,dnsjava");
//            System.setProperty("sun.net.spi.nameservice.nameservers", "127.0.0.1"); // Questo viene letto da sun 
//            
//            System.setProperty("dnsjava.options", "verbose");            
            
        }
        catch (Exception e) {/*e.printStackTrace();*//* It HAS to thrown an exception */}
        
    }
    
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
        
        String name = new LoremIpsum().randomWord() + ".test.services.porketta";
        String ip = "151.100.152." + Integer.toString( new Random().nextInt(254) );
        
        String uuid = Chabotto.registerService(name, ip, 8080).get();
        
        // Ok, the following 3 checks are the resultof 2 days of battle between me, java, nad dns. 
        // These linesare wrong, I know        
        resolver().resolve(name).forEach(System.out::println);
        System.out.println(org.xbill.DNS.ARecord.class);                                // Be sure that dnsjava is in the classpath        
        System.out.println(java.net.InetAddress.getByName("www.google.com").getHostAddress());   // I can resolve a real domain
        System.out.println(java.net.InetAddress.getByName(name).getHostAddress());               // I can resolve my domain
        
        // Verify that the service has been registered
        List<LookupResult> lookup = resolver().resolve(name);
        
        assertEquals(1, lookup.size());
        assertEquals(ip, java.net.InetAddress.getByName(name).getHostAddress());
        
        // Be sure that the service is going to autoexpire
        assertTrue("TTL should be lower than Chabotto.HEARTBEAT_SEC, but is" + lookup.get(0).ttl(), lookup.get(0).ttl() <= Chabotto.HEARTBEAT_SEC);
        
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
    public void testServiceDeregistration() throws UnknownHostException, InterruptedException {
        
        String name = new LoremIpsum().randomWord() + ".test.services.porketta";
        String ip = "151.100.152." + Integer.toString( new Random().nextInt(254) );
        
        System.out.println("Registering " + name + " to " + ip);
        
//        System.out.println(Try.of(() -> {return java.net.InetAddress.getByName(name).getHostAddress();})
//                .getOption()
//                .map(v -> {System.out.println("SIIIIII " + v); return v;})                
//                .getOrElse("NOOOOO"));

        
        String uuid = Chabotto.registerService(name, ip, 8080).get();
        
        System.out.println("---" + java.net.InetAddress.getByName(name).getHostAddress());
        
        // Try to unregister a service not registered by Chabotto
        String fakeUuid = "21309." + name;
        Optional<Exception> op = Chabotto.unregisterService(fakeUuid);
        assertEquals(Boolean.TRUE, op.isPresent());
        assertEquals(IllegalArgumentException.class, op.get().getClass());
        
        // Now deregister a valid service
        System.out.println("#############################################################" + uuid);
        Optional<Exception> deregisterOp = Chabotto.unregisterService(uuid);
        assertEquals(Boolean.FALSE, deregisterOp.isPresent());
        
        Thread.sleep(2500);
        
        // Now we should not be able to resolve an host
        Try<String> resolveNXDOMAIN =  Try.of(() -> {System.out.println("§§§§§§§§§§§§§§§§§§§§§§§§§§§§ " + java.net.InetAddress.getByName(name).getHostAddress()); return java.net.InetAddress.getByName(name).getHostAddress();});
        assertEquals(Boolean.TRUE, resolveNXDOMAIN.isFailure());        
        assertEquals(resolveNXDOMAIN.getCause().getClass(), UnknownHostException.class);
        
        
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
