package integration.utils.javaprocess;

import java.io.File;
import java.io.IOException;

public final class JavaProcess {

    private JavaProcess() {}        

    public static Process exec(Class<?> klass, String params) throws IOException,InterruptedException {
        String javaHome = System.getProperty("java.home");
        String javaBin = javaHome +
                File.separator + "bin" +
                File.separator + "java";
        String classpath = System.getProperty("java.class.path");
        String className = klass.getCanonicalName();

        ProcessBuilder builder = new ProcessBuilder(javaBin, "-cp", classpath, "-Dsun.net.spi.nameservice.provider.1=dns,dnsjava -Ddns.server=127.0.0.1 -Dsun.net.inetaddr.ttl=0 -Dsun.net.inetaddr.negative.ttl=0 -Dchabotto.servname=services.porketta.", className, params);

        return  builder.start();        
    }

}