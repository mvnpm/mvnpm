package io.mvnpm.ui;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Named;
import org.eclipse.microprofile.config.inject.ConfigProperty;

import java.util.Calendar;

@ApplicationScoped
@Named("web")
public class WebData {
    @ConfigProperty(name = "quarkus.application.version")
    public String version;

    public int year = Calendar.getInstance().get(Calendar.YEAR);
}
