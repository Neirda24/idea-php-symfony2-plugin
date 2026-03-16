package fr.adrienbrault.idea.symfony2plugin.tests.httpClientRecorder;

import com.intellij.psi.PsiFile;
import fr.adrienbrault.idea.symfony2plugin.Symfony2Icons;
import fr.adrienbrault.idea.symfony2plugin.tests.SymfonyLightCodeInsightFixtureTestCase;

public class HttpClientRecorderLineMarkerProviderTest extends SymfonyLightCodeInsightFixtureTestCase {
    public void setUp() throws Exception {
        super.setUp();
        myFixture.addFileToProject("phpunit.xml",
            "<phpunit>\n" +
            "    <extensions>\n" +
            "        <bootstrap class=\"Symfony\\HttpClientRecorderBundle\\PHPUnit\\RecorderExtension\">\n" +
            "            <parameter name=\"defaultDirectory\" value=\"./tests/\"/>\n" +
            "        </bootstrap>\n" +
            "    </extensions>\n" +
            "</phpunit>"
        );
        myFixture.addFileToProject("tests/my_record.har", "");
        myFixture.addFileToProject("tests/unused.har", "");
    }

    public void testThatHarFileHasLineMarker() {
        myFixture.configureByText("test.php",
            "<?php\n" +
            "use Symfony\\HttpClientRecorderBundle\\PHPUnit\\Attribute\\UseRecord;\n" +
            "#[UseRecord(record: '@my_record.har')]\n" +
            "class WebTest {}"
        );

        PsiFile harFile = myFixture.configureByFile("tests/my_record.har");
        assertLineMarker(harFile, markerInfo -> markerInfo.getIcon() == Symfony2Icons.HTTP_CLIENT_RECORDER_LINE_MARKER);
    }

    public void testThatUnusedHarFileHasNoLineMarker() {
        PsiFile harFile = myFixture.configureByFile("tests/unused.har");
        assertLineMarkerIsEmpty(harFile);
    }
}
