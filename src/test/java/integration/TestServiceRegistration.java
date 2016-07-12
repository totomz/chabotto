package integration;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import it.myideas.chabotto.Chabotto;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.ScanParams;

public class TestServiceRegistration {
private Jedis jedis;
    
    
    @Before
    public void init() {
        jedis = new Jedis();
    }
    
    @After
    public void cleanup() {
        
        jedis.scan("0", new ScanParams().match("*"))
            .getResult()
            .stream().map(s -> {System.out.println("Deleting " + s); return s;})
//            .count()
            .forEach(jedis::del)
        ;
        jedis.close();
    }
    
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
}
