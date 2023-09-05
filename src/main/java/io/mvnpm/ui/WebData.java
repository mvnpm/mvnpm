package io.mvnpm.ui;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Named;

import java.util.Calendar;

@ApplicationScoped
@Named("web")
public class WebData {
    public int year = Calendar.getInstance().get(Calendar.YEAR);
}
