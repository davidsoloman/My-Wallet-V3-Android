package info.blockchain.ui;

import android.content.Intent;
import android.support.v7.widget.RecyclerView;
import android.test.ActivityInstrumentationTestCase2;
import android.util.TypedValue;
import android.view.View;
import android.widget.Spinner;
import android.widget.TextView;

import com.robotium.solo.Solo;

import junit.framework.TestCase;

import java.text.DecimalFormat;

import info.blockchain.ui.util.UiUtil;
import info.blockchain.wallet.MainActivity;
import info.blockchain.wallet.util.AccountsUtil;
import info.blockchain.wallet.util.ExchangeRateFactory;
import info.blockchain.wallet.util.MonetaryUtil;
import info.blockchain.wallet.util.PrefsUtil;
import piuk.blockchain.android.R;

public class BalanceScreenTest extends ActivityInstrumentationTestCase2<MainActivity> {

    private Solo solo = null;

    RecyclerView txList = null;

    private static boolean loggedIn = false;

    public BalanceScreenTest() {
        super(MainActivity.class);
    }

    @Override
    public void setUp() throws Exception {

        super.setUp();
        solo = new Solo(getInstrumentation(), getActivity());

        if(!loggedIn){
            UiUtil.getInstance(getActivity()).enterPin(solo, solo.getString(R.string.qa_test_pin1));
            try{solo.sleep(4000);}catch (Exception e){}
            loggedIn = true;
        }
    }

    @Override
    public void tearDown() throws Exception {
        solo.finishOpenedActivities();
    }

    public void testA_ChangeCurrencyTapBalance() throws AssertionError{

        txList = (RecyclerView)solo.getView(R.id.txList2);
        if(txList.getChildCount()>0) {

            TextView balance = (TextView) solo.getView(R.id.balance1);

            //Set default fiat, btc
            PrefsUtil.getInstance(solo.getCurrentActivity()).setValue(PrefsUtil.KEY_SELECTED_FIAT, PrefsUtil.DEFAULT_CURRENCY);
            PrefsUtil.getInstance(solo.getCurrentActivity()).setValue(PrefsUtil.KEY_BTC_UNITS, MonetaryUtil.UNIT_BTC);

            String strFiat = PrefsUtil.getInstance(solo.getCurrentActivity()).getValue(PrefsUtil.KEY_SELECTED_FIAT, PrefsUtil.DEFAULT_CURRENCY);
            double btc_fx = ExchangeRateFactory.getInstance(solo.getCurrentActivity()).getLastPrice(strFiat);

            while(!balance.getText().toString().equals(getActivity().getString(R.string.show_balance)))
                solo.clickOnView(balance);

            solo.clickOnView(balance);

            //Now we are on btc
            String btc = balance.getText().toString();
            double fiat_balance = btc_fx * Double.parseDouble(btc.split(" ")[0]);

            solo.clickOnView(balance);

            DecimalFormat df = new DecimalFormat("#.##");
            String fiat = balance.getText().toString();

            //Test if btc converts to correct fiat
            TestCase.assertTrue(fiat.split(" ")[0].equals(df.format(fiat_balance)));
        }
    }

    public void testB_ChangeCurrencyTapTxAmount() throws AssertionError{

        TextView balance = (TextView)solo.getView(R.id.balance1);

        while(!balance.getText().toString().equals(getActivity().getString(R.string.show_balance)))
            solo.clickOnView(balance);

        solo.clickOnView(balance);

        //Now we are on fiat
        String btc = balance.getText().toString();

        //Set default fiat, btc
        PrefsUtil.getInstance(solo.getCurrentActivity()).setValue(PrefsUtil.KEY_SELECTED_FIAT, PrefsUtil.DEFAULT_CURRENCY);
        PrefsUtil.getInstance(solo.getCurrentActivity()).setValue(PrefsUtil.KEY_BTC_UNITS, MonetaryUtil.UNIT_BTC);

        String strFiat = PrefsUtil.getInstance(solo.getCurrentActivity()).getValue(PrefsUtil.KEY_SELECTED_FIAT, PrefsUtil.DEFAULT_CURRENCY);
        double btc_fx = ExchangeRateFactory.getInstance(solo.getCurrentActivity()).getLastPrice(strFiat);
        double fiat_balance = btc_fx * Double.parseDouble(btc.split(" ")[0]);

        txList = (RecyclerView)solo.getView(R.id.txList2);

        if(txList.getChildCount()>0) {

            solo.clickOnView(txList.getChildAt(0).findViewById(R.id.result));

            DecimalFormat df = new DecimalFormat("#.##");
            String fiat = balance.getText().toString();

            //Test if btc converts to correct fiat
            TestCase.assertTrue(fiat.split(" ")[0].equals(df.format(fiat_balance)));
        }
    }

    private float toPx(int dp){
        return TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dp, getActivity().getResources().getDisplayMetrics());
    }

    public void testC_BasicUI() throws AssertionError{

        int itemCount;
        View spinnerView = null;

        if (AccountsUtil.getInstance(getActivity()).getBalanceAccountMap().size() > 1) {
            //Multiple accounts
            Spinner mSpinner = solo.getView(Spinner.class, 0);
            itemCount = mSpinner.getAdapter().getCount();
            spinnerView = solo.getView(Spinner.class, 0);
        } else {
            //Single account - no spinner
            itemCount = 1;
        }

        txList = (RecyclerView) solo.getView(R.id.txList2);
        for(int i = itemCount-1; i >= 0; i--){

            if(spinnerView!=null) {
                solo.clickOnView(spinnerView);
                solo.scrollToTop();
                solo.clickOnView(solo.getView(TextView.class, i));
                try {solo.sleep(1000);} catch (Exception e) {}
                if(txList.getAdapter().getItemCount()==0)continue;
            }

            float actionBarHeight = toPx(56);
            float headerHeight = toPx(72);
            float rowHeight = toPx(72);
            float yd = actionBarHeight + headerHeight + (float) (rowHeight/2.0);

            solo.clickOnScreen(50,  yd);
            try{solo.sleep(200);}catch (Exception e){}

            //Test if expanded
            TestCase.assertTrue(solo.waitForText(solo.getString(R.string.from)) && solo.waitForText(solo.getString(R.string.transaction_fee)));

            //Toggle status/confirmation amount
            solo.clickOnScreen(info.blockchain.wallet.util.DeviceUtil.getInstance(getActivity()).getWidth() - toPx(50), yd+rowHeight);
            solo.clickOnScreen(info.blockchain.wallet.util.DeviceUtil.getInstance(getActivity()).getWidth() - toPx(50), yd+rowHeight);
            solo.clickOnScreen(50,  yd);

            //Test scrolling
            txList.fling(0, 20000);
            try{solo.sleep(1000);}catch (Exception e){}
            txList.fling(0, -20000);
            try{solo.sleep(1000);}catch (Exception e){}
        }

        //Test hash opens link
        if(AllTests.enableUserInteraction && txList.getAdapter().getItemCount()>0) {
            solo.clickOnView(txList.getChildAt(0));
            solo.clickOnView(solo.getView(R.id.tx_hash));
            try{solo.sleep(1000);}catch (Exception e){}
            Intent i = new Intent(getActivity(), MainActivity.class);
            i.setAction(Intent.ACTION_MAIN);
            i.addCategory(Intent.CATEGORY_LAUNCHER);
            getActivity().startActivity(i);
            try{solo.sleep(1000);}catch (Exception e){}
        }

        UiUtil.getInstance(getActivity()).exitApp(solo);
    }
}
