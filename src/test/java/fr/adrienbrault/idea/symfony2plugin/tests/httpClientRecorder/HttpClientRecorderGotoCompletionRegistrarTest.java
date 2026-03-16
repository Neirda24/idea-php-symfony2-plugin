package fr.adrienbrault.idea.symfony2plugin.tests.httpClientRecorder;

import fr.adrienbrault.idea.symfony2plugin.tests.SymfonyLightCodeInsightFixtureTestCase;

public class HttpClientRecorderGotoCompletionRegistrarTest extends SymfonyLightCodeInsightFixtureTestCase {
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
        myFixture.addFileToProject("tests/sub/other.har", "");
        myFixture.addFileToProject("relative.har", "");
    }

    public void testThatAttributeRecordArgumentProvidesCompletion() {
        assertCompletionContains(
            "test.php",
            "<?php\n" +
            "use Symfony\\HttpClientRecorderBundle\\PHPUnit\\Attribute\\UseRecord;\n" +
            "#[UseRecord(record: '@<caret>')]\n" +
            "class WebTest {}",
            "@my_record.har", "@sub/other.har"
        );

        assertCompletionContains(
            "test.php",
            "<?php\n" +
            "use Symfony\\HttpClientRecorderBundle\\PHPUnit\\Attribute\\UseRecord;\n" +
            "#[UseRecord('<caret>')]\n" +
            "class WebTest {}",
            "relative.har"
        );
    }

    public void testThatAttributeRecordArgumentProvidesNavigation() {
        assertNavigationContains(
            "test.php",
            "<?php\n" +
            "use Symfony\\HttpClientRecorderBundle\\PHPUnit\\Attribute\\UseRecord;\n" +
            "#[UseRecord(record: '@my_record.h<caret>ar')]\n" +
            "class WebTest {}",
            "tests/my_record.har"
        );

        assertNavigationContains(
            "test.php",
            "<?php\n" +
            "use Symfony\\HttpClientRecorderBundle\\PHPUnit\\Attribute\\UseRecord;\n" +
            "#[UseRecord('@sub/othe<caret>r.har')]\n" +
            "class WebTest {}",
            "tests/sub/other.har"
        );
    }
}
