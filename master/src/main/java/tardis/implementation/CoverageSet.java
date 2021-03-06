package tardis.implementation;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * A data structure that stores the coverage information
 * of the generated tests.
 * 
 * @author Pietro Braione
 */
public final class CoverageSet {
    /**
     * The covered items.
     */
    private final HashSet<String> coverage = new HashSet<>();

    /**
     * Adds a set of covered items.
     * 
     * @param coverageInfo a {@link Collection}{@code <}{@link String}{@code >}, whose
     *        elements identify a coverage item.
     * @return the {@link Set} of the elements in {@code coverageInfo} that were not 
     *         in this coverage set before the invocation of this method.
     */
    public synchronized Set<String> addAll(Collection<? extends String> coverageInfo) {
        final Set<String> retVal = coverageInfo.stream().filter(s -> !covers(s)).collect(Collectors.toSet());
        this.coverage.addAll(coverageInfo);
        return retVal;
    }

    /**
     * Checks whether an item is covered.
     * 
     * @param branch the item to be checked.
     * @return {@code true} iff it was previously added as covered 
     *         with {@link #addAll(Collection) addAll}.
     */
    public synchronized boolean covers(String branch) {
        return this.coverage.contains(branch);
    }

    /**
     * Returns the number of covered items.
     * 
     * @return a positive {@code int}, the total number of items added 
     *         as covered with {@link #addAll(Collection) addAll}.
     */
    public synchronized int size() {
        return this.coverage.size();
    }

    /**
     * Returns the number of covered items matching
     * a given pattern.
     * 
     * @param pattern a {@link String}, a regular expression.
     * @return a positive {@code int}, the total number of items added 
     *         as covered with {@link #addAll(Collection) addAll} and
     *         matching {@code pattern}.
     */
    public synchronized int size(String pattern) {
        final Pattern p = Pattern.compile(pattern); 
        final Set<String> filtered = this.coverage.stream().filter(s -> { final Matcher m = p.matcher(s); return m.matches(); }).collect(Collectors.toSet());
        return filtered.size();
    }
}
