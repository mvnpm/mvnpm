package org.mvnpm.file;

import java.util.HashMap;
import java.util.Map;
import org.mvnpm.Constants;
import io.mvnpm.importmap.ImportsDataBinding;
import io.mvnpm.importmap.model.Imports;

public class ImportMapUtil {

    private ImportMapUtil(){
        
    }
    
    public static byte[] createImportMap(org.mvnpm.npm.model.Package p) {
        
        String root = getImportMapRoot(p);
        
        String module = getModule(p);
        Map<String, String> v = new HashMap<>();
        
        v.put(p.name().npmFullName(), root + module);
        v.put(p.name().npmFullName() + Constants.SLASH, root + getModuleRoot(module));
        
        Imports imports = new Imports(v);
        
        String importmapJson = ImportsDataBinding.toJson(imports);
        
        return importmapJson.getBytes();
    }
    
    public static String getImportMapRoot(org.mvnpm.npm.model.Package p){
        String root = STATIC_ROOT + p.name().npmName();
        if(p.repository()!=null && p.repository().directory()!=null && !p.repository().directory().isEmpty()){
            String d = p.repository().directory();
            if(d.startsWith(PACKAGES + Constants.SLASH)){
                root = d.replaceFirst(PACKAGES + Constants.SLASH, STATIC_ROOT);
            }else if (d.startsWith(PACKAGE + Constants.SLASH)){
                root = d.replaceFirst(PACKAGE + Constants.SLASH, STATIC_ROOT);
            }
        }
        if(!root.endsWith(Constants.SLASH)){
            root = root + Constants.SLASH;
        }
        
        // TODO: Validate that the folder exist ?
        // Else search for the first "main" / "module" in the tree ?
        return root;
    }
    
    private static String getModule(org.mvnpm.npm.model.Package p){
        if(p.module()!=null && !p.module().isEmpty()){
            return cleanModule(p.module());
        }else if(p.main()!=null && !p.main().isBlank()){
            return cleanModule(p.main());
        }
        
        // Default
        return INDEX_JS;
    }
    
    private static String cleanModule(String module){
        if(module.startsWith(Constants.DOT + Constants.SLASH)){
            return module.substring(2);
        }
        return module;
    }
    
    private static String getModuleRoot(String module){
        if(!module.startsWith(Constants.SLASH) && module.contains(Constants.SLASH)){
            return module.split(Constants.SLASH)[0] + Constants.SLASH;
        }else {
            return Constants.EMPTY;
        }
    }
    
    private static final String INDEX_JS = "index.js";
    private static final String PACKAGES = "packages";
    private static final String STATIC_ROOT = "/_static/";
    private static final String PACKAGE = "package";
}
