
package org.icatproject.topcatdoiplugin;

import java.io.FileInputStream;
import java.io.IOException;

public class Properties extends java.util.Properties {
    
    private static Properties instance = null;

    public synchronized static Properties getInstance() {
       if(instance == null) {
          instance = new Properties();
       }
       return instance;
    }
    
    public Properties(){
        super();
        try {
            load(new FileInputStream("topcat_doi_plugin.properties"));
        } catch(IOException e){
            
        }
    }
    
}
