package MWC.Utilities.Errors.Testing;


public class TestEmpty extends EmptyTestCase {
    /**
     * Basic constructor - called by the test runners.
     */
    public TestEmpty (final String s) {
        super (s);
    }

    public static class InnerTestEmpty extends EmptyTestCase {
        public InnerTestEmpty (final String s) {
            super (s);
        }
        public static final String TEST_ALL_TEST_TYPE1 = "UNIT";
    }

    public static final String TEST_ALL_TEST_TYPE = "UNIT";
}