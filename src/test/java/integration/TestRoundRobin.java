package integration;

import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;

import java.net.URI;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.junit.Test;

import it.myideas.chabotto.Chabotto;
import javaslang.control.Either;

public class TestRoundRobin extends BaseTest {

//    @Test 
//    public void testRoundRobin() {
//        
//        String name = randomize("simplerrobin");
//        
//        List<String> services = Stream.of(100, 200, 300, 400, 500)
//                .map(n -> {return Chabotto.registerService(name, "http://localhost:"+n+"/pippo");})
//                .map(Either::get)
//                .collect(Collectors.toList());
//        
//        // When requesting using rrobin, each time i should have a different service. The order is not guarrantees
//        String lastService = "";
//        for(int i=0;i<services.size() + 9; i++) {
//            
//            Either<Exception, URI> instance = Chabotto.getServiceRoundRobin(name);
//            assertTrue("Can't get an instance from roundrobin", instance.isRight());
//            
//            assertNotEquals(lastService, instance.get().toString());
//            lastService = instance.get().toString();
//        }
//        
//        
//    }
//    
//    // TODO testa il roundrobin con dei servizi che muoiono
    
}
