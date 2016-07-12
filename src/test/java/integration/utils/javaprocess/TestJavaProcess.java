package integration.utils.javaprocess;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

import org.junit.Test;

public class TestJavaProcess {
    
   
    @Test
//    @Ignore
    public void forkProcess() throws IOException, InterruptedException {
        
        final Process proc = JavaProcess.exec(TestApp.class, "pippo");
        
        new Thread(() -> {
            BufferedReader input = new BufferedReader(new InputStreamReader(proc.getInputStream()));
            String line;
            try {
                int i = 0;
                while ((line = input.readLine()) != null) {
                  System.out.println("subProcess: " + line);
                  if(i == 1) {
                      assertEquals("pippo", line); 
                  }
                  i++;
                }
                input.close();
            }
            catch (IOException e) {
                e.printStackTrace();
            }
            
        }).start();
        
        Thread.sleep(5000);
        
        proc.destroy();
        Thread.sleep(1500);
        
        assertFalse(proc.isAlive());
    }

}
