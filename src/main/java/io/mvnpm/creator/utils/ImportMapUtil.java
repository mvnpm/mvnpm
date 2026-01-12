package io.mvnpm.creator.utils;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import com.fasterxml.jackson.databind.ObjectMapper;

import io.mvnpm.Constants;
import io.mvnpm.importmap.ImportsDataBinding;
import io.mvnpm.importmap.model.Imports;
import io.mvnpm.npm.model.Package;
import io.quarkus.logging.Log;

@ApplicationScoped
public class ImportMapUtil {

    @Inject
    ObjectMapper objectMapper;

    private static final String PACKAGE_JSON = "package.json";

    public byte[] createImportMap(Map<String, byte[]> packageJsonFiles) throws IOException {
        Map<String, io.mvnpm.npm.model.Package> packageJsonObjects = new HashMap<>();

        byte[] mainPackageJson = packageJsonFiles.remove(PACKAGE_JSON);
        Package mainPackageObject = null;
        if (mainPackageJson != null) {
            mainPackageObject = objectMapper.readValue(mainPackageJson, io.mvnpm.npm.model.Package.class);
        }

        for (Map.Entry<String, byte[]> packageJsonFile : packageJsonFiles.entrySet()) {
            String path = packageJsonFile.getKey();
            byte[] content = packageJsonFile.getValue();
            if (content != null) {
                try {
                    packageJsonObjects.put(path, objectMapper.readValue(content, io.mvnpm.npm.model.Package.class));
                } catch (IOException ex) {
                    Log.error(ex);
                }
            }
        }

        return createImportMap(mainPackageObject, packageJsonObjects);
    }

    public byte[] createImportMap(io.mvnpm.npm.model.Package mainPackage,
            Map<String, io.mvnpm.npm.model.Package> otherPackages) {

        Map<String, String> v = new HashMap<>();

        if (mainPackage != null) {
            String root = getImportMapRoot(mainPackage);
            String module = getModule(mainPackage);
            v.put(mainPackage.name().npmFullName, root + module);
            v.put(mainPackage.name().npmFullName + Constants.SLASH, root + getModuleRoot(module));
        }

        if (otherPackages != null && !otherPackages.isEmpty()) {
            for (Map.Entry<String, Package> otherPackage : otherPackages.entrySet()) {

                if (mainPackage == null) {
                    mainPackage = otherPackage.getValue();
                }
                String root = getImportMapRoot(mainPackage);
                String path = otherPackage.getKey();

                path = path.replace("/" + PACKAGE_JSON, "");

                io.mvnpm.npm.model.Package p = otherPackage.getValue();
                String otherModule = getModule(p);
                v.put(mainPackage.name().npmFullName + Constants.SLASH + path,
                        root + path + Constants.SLASH + otherModule);
                v.put(mainPackage.name().npmFullName + Constants.SLASH + path + Constants.SLASH,
                        root + path + Constants.SLASH);
            }
        }

        Imports imports = new Imports(v);

        String importmapJson = ImportsDataBinding.toJson(imports);

        return importmapJson.getBytes();

    }

    public byte[] createImportMap(io.mvnpm.npm.model.Package p) {
        String root = getImportMapRoot(p);

        String module = getModule(p);
        Map<String, String> v = new HashMap<>();

        v.put(p.name().npmFullName, root + module);
        v.put(p.name().npmFullName + Constants.SLASH, root + getModuleRoot(module));

        Imports imports = new Imports(v);

        String importmapJson = ImportsDataBinding.toJson(imports);

        return importmapJson.getBytes();
    }

    public String getImportMapRoot(io.mvnpm.npm.model.Package p) {
        final String nameSpaceRoot = p.name().npmNamespace != null && !p.name().npmNamespace.isEmpty()
                ? "at/" + p.name().npmNamespace.replace("@", "") + Constants.SLASH
                : "";
        String root = STATIC_ROOT + nameSpaceRoot + p.name().npmName;
        if (p.repository() != null && p.repository().directory() != null && !p.repository().directory().isEmpty()) {
            String d = p.repository().directory();
            if (d.startsWith(PACKAGES + Constants.SLASH)) {
                root = d.replaceFirst(PACKAGES + Constants.SLASH, STATIC_ROOT + nameSpaceRoot);
            } else if (d.startsWith(PACKAGE + Constants.SLASH)) {
                root = d.replaceFirst(PACKAGE + Constants.SLASH, STATIC_ROOT + nameSpaceRoot);
            }
        }
        if (!root.endsWith(Constants.SLASH)) {
            root = root + Constants.SLASH;
        }

        // TODO: Validate that the folder exist ?
        // Else search for the first "main" / "module" in the tree ?
        return root + p.version() + Constants.SLASH;
    }

    private String getModule(io.mvnpm.npm.model.Package p) {
        if (p.module() != null && !p.module().isEmpty()) {
            return cleanModule(p.module());
        } else if (p.main() != null && !p.main().isBlank()) {
            return cleanModule(p.main());
        }

        // Default
        return INDEX_JS;
    }

    private String cleanModule(String module) {
        if (module.startsWith(Constants.DOT + Constants.SLASH)) {
            return module.substring(2);
        }
        return module;
    }

    private String getModuleRoot(String module) {
        if (!module.startsWith(Constants.SLASH) && module.contains(Constants.SLASH)) {
            return module.split(Constants.SLASH)[0] + Constants.SLASH;
        } else {
            return Constants.EMPTY;
        }
    }

    private static final String INDEX_JS = "index.js";
    private static final String PACKAGES = "packages";
    private static final String STATIC_ROOT = "/_static/";
    private static final String PACKAGE = "package";
}
