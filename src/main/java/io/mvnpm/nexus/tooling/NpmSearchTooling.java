package io.mvnpm.nexus.tooling;

import java.util.ArrayList;
import java.util.List;

import io.mvnpm.nexus.npm.model.NpmResponse;
import io.mvnpm.npm.model.SearchItem;
import io.mvnpm.npm.model.SearchResult;
import io.mvnpm.npm.model.SearchResults;
import io.quarkus.logging.Log;

/**
 * Assists in fluent API of {@link TypeConversionTool} when searching broadly.
 * Works with {@link SearchResults} type.
 */
public final class NpmSearchTooling {

    private final NpmResponse response;

    NpmSearchTooling(final NpmResponse response) {
        this.response = response;
    }

    /**
     * Returns a properly constructed {@link SearchResults} object on basis of the
     * parsed {@link NpmResponse}.
     *
     * @return The constructed {@link SearchResults}-object.
     * @throws TypeConversionException if an exception occured in the process.
     */
    public final SearchResults toSearchResults() throws TypeConversionException {
        if (response.items().size() == 0) {
            throw new TypeConversionException("no results found, try to broaden the search parameters!");
        }

        final List<SearchResult> results = new ArrayList<>();
        response.items().stream().forEach(item -> {
            final SearchItem searchItem = new SearchItem(item.name(), null, null, item.version(), null, null, null,
                    null, null, null);
            final SearchResult result = new SearchResult(searchItem, null);
            results.add(result);

            Log.infof("added item '%s' to search-results...", item.name());
        });

        return new SearchResults(results, response.items().size());
    }
}
