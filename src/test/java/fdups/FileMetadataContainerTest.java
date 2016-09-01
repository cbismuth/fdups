package fdups;

import com.codahale.metrics.MetricRegistry;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

import static com.google.common.collect.Lists.newArrayList;
import static java.util.Arrays.asList;
import static java.util.UUID.randomUUID;
import static java.util.stream.Collectors.toList;
import static org.junit.Assert.assertEquals;

@RunWith(Parameterized.class)
public class FileMetadataContainerTest {

    private static final String WORKING_DIRECTORY = System.getProperty("user.dir");

    private static final long UNIQUE_DIRECTORIES_COUNT = 5L;
    private static final long UNIQUE_FILES_COUNT = 3L;
    private static final long DUPLICATION_FACTOR = 2L;

    private final DuplicateFileTreeWalker systemUnderTest;

    private final FileMetadataContainerTestHelper helper = new FileMetadataContainerTestHelper(UNIQUE_DIRECTORIES_COUNT,
                                                                                               UNIQUE_FILES_COUNT,
                                                                                               DUPLICATION_FACTOR);

    @Parameterized.Parameters
    public static Collection<Object[]> data() {
        return asList(
            new Object[][] {
                // test with native support if present
                { new Md5SumHelper() },
                // test with jvm md5 implementation
                { new Md5SumHelper(Optional.empty()) }
            }
        );
    }

    public FileMetadataContainerTest(final Md5SumHelper md5SumHelper) {
        final MetricRegistry metricRegistry = new MetricRegistry();
        final FileMetadataContainer fileMetadataContainer = new FileMetadataContainer(metricRegistry, md5SumHelper);

        systemUnderTest = new DuplicateFileTreeWalker(metricRegistry, fileMetadataContainer);
    }

    @Test
    public void testExtractDuplicates_fakePath() throws Exception {
        // GIVEN
        final Collection<Path> rootPaths = newArrayList();

        rootPaths.add(Paths.get(randomUUID().toString()));

        final List<String> rootAbsolutePaths = rootPaths.stream()
                                                        .map(Path::toString)
                                                        .collect(toList());

        // WHEN
        final Collection<String> duplicates = systemUnderTest.extractDuplicates(rootAbsolutePaths);

        // THEN
        final long expectedDuplicatesByMd5SumCount = 0L;
        final long actualDuplicatesByMd5SumCount = (long) duplicates.size();
        assertEquals(expectedDuplicatesByMd5SumCount, actualDuplicatesByMd5SumCount);
    }

    @Test
    public void testExtractDuplicates_emptyPath() throws Exception {
        // GIVEN
        final Collection<Path> rootPaths = newArrayList();

        helper.addEmptyPath(rootPaths);

        final List<String> rootAbsolutePaths = rootPaths.stream()
                                                        .map(Path::toString)
                                                        .collect(toList());

        // WHEN
        final Collection<String> duplicates = systemUnderTest.extractDuplicates(rootAbsolutePaths);

        // THEN
        final long expectedDuplicatesByMd5SumCount = 0L;
        final long actualDuplicatesByMd5SumCount = (long) duplicates.size();
        assertEquals(expectedDuplicatesByMd5SumCount, actualDuplicatesByMd5SumCount);
    }

    @Test
    public void testExtractDuplicates_uniqueEmptyFile() throws Exception {
        // GIVEN
        final Collection<Path> rootPaths = newArrayList();

        helper.addUniqueEmptyFile(rootPaths);

        final List<String> rootAbsolutePaths = rootPaths.stream()
                                                        .map(Path::toString)
                                                        .collect(toList());

        // WHEN
        final Collection<String> duplicates = systemUnderTest.extractDuplicates(rootAbsolutePaths);

        // THEN
        final long expectedDuplicatesByMd5SumCount = 0L;
        final long actualDuplicatesByMd5SumCount = (long) duplicates.size();
        assertEquals(expectedDuplicatesByMd5SumCount, actualDuplicatesByMd5SumCount);
    }

    @Test
    public void testExtractDuplicates_duplicatesBySizeOnly() throws Exception {
        // GIVEN
        final Collection<Path> rootPaths = newArrayList();

        helper.addDuplicatesBySize(rootPaths);

        final List<String> rootAbsolutePaths = rootPaths.stream()
                                                        .map(Path::toString)
                                                        .collect(toList());

        // WHEN
        final Collection<String> duplicates = systemUnderTest.extractDuplicates(rootAbsolutePaths);

        // THEN
        final long expectedDuplicatesByMd5SumCount = 0L;
        final long actualDuplicatesByMd5SumCount = (long) duplicates.size();
        assertEquals(expectedDuplicatesByMd5SumCount, actualDuplicatesByMd5SumCount);
    }

    @Test
    public void testExtractDuplicates_duplicatesByMd5SumOnly() throws Exception {
        // GIVEN
        final Collection<Path> originals = helper.listClassFiles(Paths.get(WORKING_DIRECTORY));
        final Collection<Path> rootPaths = newArrayList();

        helper.addDuplicatesByMd5Sum(originals, rootPaths);

        final List<String> rootAbsolutePaths = rootPaths.stream()
                                                        .map(Path::toString)
                                                        .collect(toList());

        // WHEN
        final Collection<String> duplicates = systemUnderTest.extractDuplicates(rootAbsolutePaths);

        // THEN
        final long expectedDuplicatesByMd5SumCount = ((long) originals.size() * DUPLICATION_FACTOR * UNIQUE_DIRECTORIES_COUNT) - (long) originals.size();
        final long actualDuplicatesByMd5SumCount = (long) duplicates.size();
        assertEquals(expectedDuplicatesByMd5SumCount, actualDuplicatesByMd5SumCount);
    }

    @Test
    public void testExtractDuplicates_all() throws Exception {
        // GIVEN
        final Collection<Path> originals = helper.listClassFiles(Paths.get(WORKING_DIRECTORY));
        final Collection<Path> rootPaths = newArrayList();

        helper.addEmptyPath(rootPaths);
        helper.addUniqueEmptyFile(rootPaths);
        helper.addDuplicatesBySize(rootPaths);
        helper.addDuplicatesByMd5Sum(originals, rootPaths);

        final List<String> rootAbsolutePaths = rootPaths.stream()
                                                        .map(Path::toString)
                                                        .collect(toList());

        // WHEN
        final Collection<String> duplicates = systemUnderTest.extractDuplicates(rootAbsolutePaths);

        // THEN
        final long expectedDuplicatesByMd5SumCount = ((long) originals.size() * DUPLICATION_FACTOR * UNIQUE_DIRECTORIES_COUNT) - (long) originals.size();
        final long actualDuplicatesByMd5SumCount = (long) duplicates.size();
        assertEquals(expectedDuplicatesByMd5SumCount, actualDuplicatesByMd5SumCount);
    }

}