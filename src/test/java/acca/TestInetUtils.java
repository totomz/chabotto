package acca;

import java.net.InetAddress;

import org.junit.Test;

public class TestInetUtils {
    
    static {
        System.out.println("SARKAZZZZZZZZZZZZZZZZZOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOO");
        System.out.println("SARKAZZZZZZZZZZZZZZZZZOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOO");
        System.out.println("SARKAZZZZZZZZZZZZZZZZZOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOO");
        System.out.println("SARKAZZZZZZZZZZZZZZZZZOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOO");
        System.out.println("SARKAZZZZZZZZZZZZZZZZZOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOO");
        System.out.println("SARKAZZZZZZZZZZZZZZZZZOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOO");
        System.out.println("SARKAZZZZZZZZZZZZZZZZZOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOO");
    }

//    private static final String classToTest = "acca.TestInetUtils$ClassToTest";
    private static final String classToTest = "java.net.InetAddress";
    
    @Test
    public void lacacca() throws Exception, SecurityException {
        
        java.lang.reflect.Method m = ClassLoader.class.getDeclaredMethod("findLoadedClass", new Class[] { String.class });
        m.setAccessible(true);
        ClassLoader cl = ClassLoader.getSystemClassLoader();
        Object test1 = m.invoke(cl, classToTest);
        System.out.println(test1 != null);
        ClassToTest.reportLoaded();
        System.out.println(InetAddress.getByName("www.google.com").getHostAddress());
        Object test2 = m.invoke(cl, classToTest);
        System.out.println(test2 != null);
        
    }
    
    static class ClassToTest {
        static {
             System.out.println("Loading " + ClassToTest.class.getName());
        }
        static void reportLoaded() {
             System.out.println("Loaded");
        }
   }
    
}
