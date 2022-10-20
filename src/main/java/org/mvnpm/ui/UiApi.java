package org.mvnpm.ui;

import java.io.IOException;
import java.io.StringWriter;
import java.util.Calendar;
import java.util.Map;
import java.util.Set;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.mvnpm.importmap.Aggregator;
import org.mvnpm.importmap.model.Imports;


/**
 * Some generated resources used by the UI
 * @author Phillip Kruger (phillip.kruger@gmail.com)
 * 
 * See https://www.honeybadger.io/blog/import-maps/
  */
@Path("/_static")
public class UiApi {

    @ConfigProperty(name = "quarkus.application.version")
    String version;        
    
    int year = Calendar.getInstance().get(Calendar.YEAR);
    
    /**
     * This generates a /_static/importmap.json that contains all the imports.
     * However, external importmaps are not yet supported in Browsers
     * see https://github.com/WICG/import-maps/issues/235
     * 
     * Once this is supported, users can just add this to the top of their index.html:
     * <script type="importmap" src="/_static/importmap.json">
    */
    @GET
    @Path("/importmap.json")
    @Produces("application/importmap+json")
    public Imports importmapJson() {
        return Aggregator.aggregate();
    }

    
    /**
     * To make up for the above that is not supported, we provide a js file that will create the import map inline
     * 
     * TODO: Importmaps are not supported by default in all browsers, so we still need to handle that fallback with a polyfill.
     * see https://caniuse.com/import-maps
     */
    @GET
    @Path("/importmap.js")
    @Produces("application/javascript")
    public String importmapJs() {
        Imports imports = Aggregator.aggregate();
        Set<Map.Entry<String, String>> importSet = imports.imports().entrySet();
        
        try(StringWriter sw = new StringWriter()){
            for(Map.Entry<String, String> set : importSet){
                sw.write("\t\t'" + set.getKey() + "': '" + set.getValue() + "',\n");
            }
            
            return PRE +
                sw.toString() +
               POST;
        } catch (IOException ex) {
            throw new RuntimeException("Error while creating importmap.js", ex);
        }
    }

    @GET
    @Path("footer.json")
    public Footer footer() {
        return new Footer(year, version);
    }
    
    private static final String PRE = "const importMap = {\n" +
                "\t imports: {\n";
    
    private static final String POST = "\t }\n" +
                "};\n" +
                "const im = document.createElement('script');\n" +
                "im.type = 'importmap';\n" +
                "im.textContent = JSON.stringify(importMap);\n" +
                "document.currentScript.after(im);";
}