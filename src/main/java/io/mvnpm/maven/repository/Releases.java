package io.mvnpm.maven.repository;

import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.ElementType.PARAMETER;
import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import jakarta.inject.Qualifier;

import org.eclipse.aether.repository.RemoteRepository;

/**
 * The annotation for the {@link RemoteRepository} containing release-artifacts.
 *
 * @author Luca Pfaffinger (luca.pfaffinger@gmail.com)
 */
@Qualifier
@Retention(RUNTIME)
@Target({ FIELD, PARAMETER, METHOD, TYPE })
public @interface Releases {
}
