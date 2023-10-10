package io.mvnpm.ui;

import java.util.Calendar;

import jakarta.inject.Named;
import jakarta.inject.Singleton;

import org.eclipse.microprofile.config.inject.ConfigProperty;

@Singleton
@Named("web")
public class WebData {
    @ConfigProperty(name = "quarkus.application.version")
    public String version;

    public int year = Calendar.getInstance().get(Calendar.YEAR);
}
