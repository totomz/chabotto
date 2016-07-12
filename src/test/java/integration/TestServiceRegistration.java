package integration;

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
import javaslang.control.Either;
import redis.clients.jedis.ScanParams;

public class TestServiceRegistration extends BaseTest {
    
    @Test
    public void testServiceRegistration() throws URISyntaxException {
        
        String name = "testServiceRegistration";
        
        Chabotto.registerService(name, "myprotocol://ahost.name.com:125/pippo/pluto/paperino");
        
        // Verify that the service has been registered        
        List<String> services = jedis.scan("0", new ScanParams().match(String.format("cb8:service:%s:*", name))).getResult();
        assertEquals(1, services.size());
        
        String uuid = services.get(0).substring(services.get(0).lastIndexOf(":") + 1);
        
        URI uri = new URI(jedis.get("cb8:service:" + name + ":" + uuid));
        
        assertEquals("myprotocol", uri.getScheme());
        assertEquals("ahost.name.com", uri.getHost());
        assertEquals("/pippo/pluto/paperino", uri.getPath());
        assertEquals(125, uri.getPort());       
        
        // Be sure that the service is going to autoexpire
        assertTrue("key TTL seems not set", jedis.ttl("cb8:service:" + name + ":" + uuid) > 20);
        
    }
    
    @Test
    public void testServiceDeregistration() {
        
        String name = randomize("deregister");        
        Either<Exception , String> uuid = Chabotto.registerService(name, "myprotocol://ahost.name.com:125/pippo/pluto/paperino");        
        assertTrue(uuid.isRight());
        
        // Try to unregister a service not registered by Chabotto
        String fakeUuid = "21309";
        String fakeService = "cb8:service:fake:" + fakeUuid;
        jedis.set(fakeService, "lalalal");
        Optional<Exception> op = Chabotto.unregisterService(name, fakeUuid);
        assertTrue(op.isPresent());
        assertEquals(IllegalArgumentException.class, op.get().getClass());
        
        // Now deregister a valid service
        Optional<Exception> deregisterOp = Chabotto.unregisterService(name, uuid.get());
        assertFalse(deregisterOp.isPresent());
        
        // Check that the service has been removed from any queue. 
        // But wait to be sure that the heartbeat is not running
        waitForHeartbeatTimeOut();        
        assertNull(jedis.get("cb8:service:" + name + ":" + uuid.get()));
        
        // Now, the service should not be available
        Either<Exception, URI> insance = Chabotto.getServiceRoundRobin(name);
        assertTrue(insance.isLeft());
        assertEquals(IllegalArgumentException.class, insance.getLeft().getClass());
        
        // TODO Gira su tutte le altre chiavi!
        
        
    }
}
