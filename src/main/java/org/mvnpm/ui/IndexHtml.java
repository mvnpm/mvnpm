package org.mvnpm.ui;

import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.ApplicationScoped;
import java.util.Calendar;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.mvnpm.importmap.Aggregator;


/**
 * Generated index.html
 * @author Phillip Kruger (phillip.kruger@gmail.com
  */
@ApplicationScoped
public class IndexHtml {

    Aggregator aggregator = new Aggregator();
    @ConfigProperty(name = "quarkus.application.version")
    String version;        
    
    private int year = Calendar.getInstance().get(Calendar.YEAR);
    
    private String indexHtml;
    
    @PostConstruct
    public void init(){
        String importmap = aggregator.aggregateAsJson();
        this.indexHtml = """
               <!DOCTYPE html>
               <html>
                   <head>
                       <title>mvnpm</title>
                       <meta charset="UTF-8">
                       <meta name="viewport" content="width=device-width, initial-scale=1.0">
                       <link rel="shortcut icon" type="image/png" href="./favicon.ico">
               
                       <script async src="/_static/es-module-shims/dist/es-module-shims.js"></script>
                       <script type="importmap">
                           %s
                       </script>
                       
                       <script type="module" src="./mvnpm-nav.js"></script>
                       <link rel="stylesheet" href="./mvnpm.css">
                   </head>
                   
                   <body>
                       <header>
                           <img src="banner.png" alt="mvnpm"/>
                           <mvnpm-nav></mvnpm-nav>
                       </header>
               
                       <main id="outlet">
               
                       </main>
               
                       <footer>
                           <span>&copy; %d mvnpm.org | v%s</span>
                       </footer>
                   </body>
               </html>
               
               """.formatted(importmap, year, version);
    }
    
    public String getHomePage(){
        return indexHtml;
    }
    
}