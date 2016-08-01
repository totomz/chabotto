package it.myideas.chabotto.dnsjava;

import javaslang.control.Try;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xbill.DNS.Cache;
import org.xbill.DNS.Name;
import org.xbill.DNS.SetResponse;

/**
 * Implementation of a {@link Cache} that does not cache records of a given domain.
 * @author Tommaso Doninelli
 */
public class NoServiceCache extends Cache{

    private static final Logger log = LoggerFactory.getLogger(NoServiceCache.class);
    
    /** Domains that ends with this suffix are not cached*/
    private final Name noCacheRootDomain;
    
    /**
     * Create a {@link Cache} that never resolve any sub domain of a given domain. 
     * @param domain Sub domain for which entries are never resolved
     */
    public NoServiceCache(String domain) {               
        
        if(domain.endsWith(".")) {
            this.noCacheRootDomain = Try.of(() -> {return new Name(domain);})
                .onFailure(x -> {log.error("Invalid domain for no-cache requests", x);})
                .getOrElseThrow((x) -> {return new IllegalArgumentException(x);});           
        }
        else {
            IllegalArgumentException x = new IllegalArgumentException("Domain must end with a .");
            x.printStackTrace();    // <-- make noise so they won't ignore you
            throw x;
        }               
    }

//    @Override
//    public SetResponse addMessage(Message in) {
          // I didn't override this method because: no time and who cares. Yes the cache will be dirty, but of few bytes...and the cache already have an expire mechanism
//        return super.addMessage(in); //To change body of generated methods, choose Tools | Templates.
//    }

    @Override
    public SetResponse lookupRecords(Name name, int type, int minCred) {
        
        
        if(name.subdomain(noCacheRootDomain)){
            // SetResponse declare everything as protected; no way to return SetResponse.UNKNOWN
            // So we fake the request. How many side-effect will have this call?
            return super.lookupRecords(Try.of(() -> {return new Name("sarkazzo");}).get(), type, minCred); // This can not throw an exception, since is immutable...
        }
        
        return super.lookupRecords(name, type, minCred);
    }
    
}
