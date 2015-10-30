package info.blockchain.ui;

import android.app.Activity;
import android.test.ActivityInstrumentationTestCase2;

import junit.framework.TestCase;
import junit.framework.TestSuite;

public class AllTests extends ActivityInstrumentationTestCase2<Activity> {

    /*
    Set this var to false to skip any tests that require user interaction:
    - ConfirmationCodeTest: confirmation code input - entering correct confirmation code received via email will not be tested
    - BalanceScreenTest: transaction hash opens link will be skipped - needs user interaction to close browser
    Note: if false, wallet might not pair if login needs to be approved
     */
    public static boolean requireUserInteraction = true;

    public AllTests(Class<Activity> activityClass) {
        super(Activity.class);
    }

    public static TestSuite suite() {

        TestSuite t = new TestSuite();

//        //Test creating a wallet
//        t.addTestSuite(ClearWalletData.class);
//        t.addTestSuite(CreateAWalletTest.class);
//
//        //Test email confirmation
//        t.addTestSuite(ClearWalletData.class);
//        t.addTestSuite(ConfirmationCodeTest.class);

        //Test pairing
        t.addTestSuite(ClearWalletData.class);
        t.addTestSuite(PairingTest.class);

        //Test balance screen
        t.addTestSuite(BalanceScreenTest.class);

        //Test send screen
        t.addTestSuite(SendScreenTest.class);

        //Test send screen
        t.addTestSuite(ReceiveScreenTest.class);

        //Test accounts screen
        t.addTestSuite(MyAccountsScreenTest.class);

        //Test settings screen
        t.addTestSuite(SettingsScreenTest.class);

        //Test support screen
        t.addTestSuite(SupportScreenTest.class);

        //Test change pin code
        t.addTestSuite(ChangePinModalTest.class);

        //Test backup wallet
        t.addTestSuite(BackupWalletTest.class);

        //Test backup wallet
        t.addTestSuite(UnpairWalletTest.class);
        t.addTestSuite(PairingTest.class);//repair after unpair

        return t;
    }

    @Override
    public void setUp() throws Exception {
    }

    @Override
    public void tearDown() throws Exception {
    }

    public void testEmpty() throws AssertionError{
        //to avoid warning we need an empty test
        TestCase.assertTrue(true);
    }
}