package info.blockchain.wallet;

import android.app.AlertDialog;
import android.app.Fragment;
import android.app.FragmentManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.Bitmap;
import android.graphics.Bitmap.CompressFormat;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.nfc.NdefMessage;
import android.nfc.NdefRecord;
import android.nfc.NfcAdapter;
import android.nfc.NfcEvent;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;
import android.support.v7.widget.Toolbar;
import android.text.Editable;
import android.text.TextWatcher;
import android.text.method.DigitsKeyListener;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.Spinner;
import android.widget.TableLayout;
import android.widget.TextView;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.WriterException;
import com.google.zxing.client.android.Contents;
import com.google.zxing.client.android.encode.QRCodeEncoder;
import com.sothree.slidinguppanel.SlidingUpPanelLayout;

import org.apache.commons.codec.DecoderException;
import org.bitcoinj.core.AddressFormatException;
import org.bitcoinj.core.Coin;
import org.bitcoinj.crypto.MnemonicException;
import org.bitcoinj.uri.BitcoinURI;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.math.BigInteger;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.text.NumberFormat;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import info.blockchain.wallet.payload.LegacyAddress;
import info.blockchain.wallet.payload.PayloadFactory;
import info.blockchain.wallet.payload.ReceiveAddress;
import info.blockchain.wallet.util.AccountsUtil;
import info.blockchain.wallet.util.AppUtil;
import info.blockchain.wallet.util.ExchangeRateFactory;
import info.blockchain.wallet.util.MonetaryUtil;
import info.blockchain.wallet.util.PrefsUtil;
import info.blockchain.wallet.util.ToastCustom;
import piuk.blockchain.android.R;

public class ReceiveFragment extends Fragment implements OnClickListener, CustomKeypadCallback {

    private Locale locale = null;

    private ImageView ivReceivingQR = null;
    private TextView edReceivingAddress = null;
    private String currentSelectedAddress = null;
    private ReceiveAddress currentSelectedReceiveAddress = null;

    private EditText edAmount1 = null;
    private EditText edAmount2 = null;
    private Spinner spAccounts = null;
    private TextView tvCurrency1 = null;
    private TextView tvFiat2 = null;

    private int currentSelectedAccount = 0;

    private String strBTC = "BTC";
    private String strFiat = null;
    private boolean isBTC = true;
    private double btc_fx = 319.13;

    private SlidingUpPanelLayout mLayout;
    private ListView sendPaymentCodeAppListlist;
    private View rootView;
    private LinearLayout mainContent;
    private LinearLayout mainContentShadow;

    private boolean textChangeAllowed = true;
    private String defaultSeperator;
    private TableLayout numpad;
    public static boolean isKeypadVisible = false;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        View rootView = inflater.inflate(R.layout.fragment_receive, container, false);
        this.rootView = rootView;

        rootView.setFilterTouchesWhenObscured(true);

        locale = Locale.getDefault();

        ((ActionBarActivity)getActivity()).getSupportActionBar().setDisplayShowTitleEnabled(true);
        ((ActionBarActivity)getActivity()).findViewById(R.id.account_spinner).setVisibility(View.GONE);
        ((ActionBarActivity)getActivity()).getSupportActionBar().setTitle(R.string.receive_bitcoin);
        setHasOptionsMenu(true);

        Toolbar toolbar = (Toolbar) getActivity().findViewById(R.id.toolbar);
        toolbar.setNavigationIcon(getResources().getDrawable(R.drawable.ic_arrow_back_white_24dp));
        toolbar.setNavigationOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                View view = getActivity().getCurrentFocus();
                if (view != null) {
                    InputMethodManager inputManager = (InputMethodManager) getActivity().getSystemService(Context.INPUT_METHOD_SERVICE);
                    inputManager.hideSoftInputFromWindow(view.getWindowToken(), InputMethodManager.HIDE_NOT_ALWAYS);
                }
                Fragment fragment = new BalanceFragment();
                FragmentManager fragmentManager = getFragmentManager();
                fragmentManager.beginTransaction().replace(R.id.content_frame, fragment).commit();
            }
        });

        mainContent = (LinearLayout)rootView.findViewById(R.id.receive_main_content);
        mainContentShadow = (LinearLayout)rootView.findViewById(R.id.receive_main_content_shadow);
        mainContentShadow.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                if(mLayout.getPanelState().equals(SlidingUpPanelLayout.PanelState.COLLAPSED)){
                    onShareClicked();
                }
            }
        });

        ivReceivingQR = (ImageView)rootView.findViewById(R.id.qr);
        ivReceivingQR.setOnClickListener(new OnClickListener() {
          @Override
          public void onClick(View v) {

              new AlertDialog.Builder(getActivity())
                      .setTitle(R.string.app_name)
                      .setMessage(R.string.receive_address_to_clipboard)
                      .setCancelable(false)
                      .setPositiveButton(R.string.yes, new DialogInterface.OnClickListener() {

                          public void onClick(DialogInterface dialog, int whichButton) {
                              android.content.ClipboardManager clipboard = (android.content.ClipboardManager)getActivity().getSystemService(android.content.Context.CLIPBOARD_SERVICE);
                              android.content.ClipData clip = null;
                              clip = android.content.ClipData.newPlainText("Send address", currentSelectedAddress);
                              ToastCustom.makeText(getActivity(), getString(R.string.copied_to_clipboard), ToastCustom.LENGTH_LONG, ToastCustom.TYPE_GENERAL);
                              clipboard.setPrimaryClip(clip);

                          }

                      }).setNegativeButton(R.string.no, new DialogInterface.OnClickListener() {

                  public void onClick(DialogInterface dialog, int whichButton) {
                      ;
                  }
              }).show();

            }
        });

        ivReceivingQR.setOnLongClickListener(new View.OnLongClickListener() {
          public boolean onLongClick(View view) {

              onShareClicked();

              return true;
          }
        });

        tvCurrency1 = (TextView)rootView.findViewById(R.id.currency1);
        tvFiat2 = (TextView)rootView.findViewById(R.id.fiat2);

        DecimalFormat format = (DecimalFormat) DecimalFormat.getInstance(Locale.getDefault());
        DecimalFormatSymbols symbols=format.getDecimalFormatSymbols();
        defaultSeperator=Character.toString(symbols.getDecimalSeparator());

        edAmount1 = (EditText)rootView.findViewById(R.id.amount1);
        edAmount1.setKeyListener(DigitsKeyListener.getInstance("0123456789" + defaultSeperator));
        edAmount1.setHint("0"+defaultSeperator+"00");
        edAmount1.addTextChangedListener(new TextWatcher() {
            public void afterTextChanged(Editable s) {

                edAmount1.removeTextChangedListener(this);

                int unit = PrefsUtil.getInstance(getActivity()).getValue(PrefsUtil.KEY_BTC_UNITS, MonetaryUtil.UNIT_BTC);
                int max_len = 8;
                NumberFormat btcFormat = NumberFormat.getInstance(Locale.getDefault());
                switch (unit) {
                    case MonetaryUtil.MICRO_BTC:
                        max_len = 2;
                        break;
                    case MonetaryUtil.MILLI_BTC:
                        max_len = 4;
                        break;
                    default:
                        max_len = 8;
                        break;
                }
                btcFormat.setMaximumFractionDigits(max_len + 1);
                btcFormat.setMinimumFractionDigits(0);

                try {
                    double d = Double.parseDouble(s.toString());
                    String s1 = btcFormat.format(d);
                    if (s1.indexOf(defaultSeperator) != -1) {
                        String dec = s1.substring(s1.indexOf(defaultSeperator));
                        if (dec.length() > 0) {
                            dec = dec.substring(1);
                            if (dec.length() > max_len) {
                                edAmount1.setText(s1.substring(0, s1.length() - 1));
                                edAmount1.setSelection(edAmount1.getText().length());
                                s = edAmount1.getEditableText();
                            }
                        }
                    }
                } catch (NumberFormatException nfe) {
                    ;
                }

                edAmount1.addTextChangedListener(this);

                if (textChangeAllowed) {
                    textChangeAllowed = false;
                    updateFiatTextField(s.toString());

                    if (currentSelectedAddress != null) {
                        displayQRCode();
                    }
                    textChangeAllowed = true;
                }

                if(s.toString().contains(defaultSeperator))
                    edAmount1.setKeyListener(DigitsKeyListener.getInstance("0123456789"));
                else
                    edAmount1.setKeyListener(DigitsKeyListener.getInstance("0123456789" + defaultSeperator));
            }

            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                ;
            }

            public void onTextChanged(CharSequence s, int start, int before, int count) {
                ;
            }
        });

        edAmount2 = (EditText)rootView.findViewById(R.id.amount2);
        edAmount2.setKeyListener(DigitsKeyListener.getInstance("0123456789" + defaultSeperator));
        edAmount2.setHint("0"+defaultSeperator+"00");
        edAmount2.addTextChangedListener(new TextWatcher() {

            public void afterTextChanged(Editable s) {

                edAmount2.removeTextChangedListener(this);

                int max_len = 2;
                NumberFormat fiatFormat = NumberFormat.getInstance(Locale.getDefault());
                fiatFormat.setMaximumFractionDigits(max_len + 1);
                fiatFormat.setMinimumFractionDigits(0);

                try	{
                    double d = Double.parseDouble(s.toString());
                    String s1 = fiatFormat.format(d);
                    if(s1.indexOf(defaultSeperator) != -1)	{
                        String dec = s1.substring(s1.indexOf(defaultSeperator));
                        if(dec.length() > 0)	{
                            dec = dec.substring(1);
                            if(dec.length() > max_len)	{
                                edAmount2.setText(s1.substring(0, s1.length() - 1));
                                edAmount2.setSelection(edAmount2.getText().length());
                                s = edAmount2.getEditableText();
                            }
                        }
                    }
                }
                catch(NumberFormatException nfe)	{
                    ;
                }

                edAmount2.addTextChangedListener(this);

                if(textChangeAllowed) {
                    textChangeAllowed = false;
                    updateBtcTextField(s.toString());

                    if (currentSelectedAddress != null) {
                        displayQRCode();
                    }
                    textChangeAllowed = true;
                }

                if(s.toString().contains(defaultSeperator))
                    edAmount2.setKeyListener(DigitsKeyListener.getInstance("0123456789"));
                else
                    edAmount2.setKeyListener(DigitsKeyListener.getInstance("0123456789" + defaultSeperator));
            }

            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                ;
            }

            public void onTextChanged(CharSequence s, int start, int before, int count) {
                ;
            }
        });

        spAccounts = (Spinner)rootView.findViewById(R.id.accounts);
        final List<String> _accounts = AccountsUtil.getInstance(getActivity()).getSendReceiveAccountList();

        if(_accounts.size()==1)rootView.findViewById(R.id.from_row).setVisibility(View.GONE);

        ArrayAdapter<String> dataAdapter = new ArrayAdapter<String>(getActivity(), R.layout.spinner_item, _accounts);
        dataAdapter.setDropDownViewResource(R.layout.spinner_dropdown);
        spAccounts.setAdapter(dataAdapter);
        spAccounts.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {

            @Override
            public void onGlobalLayout() {
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.JELLY_BEAN) {
                    spAccounts.getViewTreeObserver().removeOnGlobalLayoutListener(this);
                } else {
                    spAccounts.getViewTreeObserver().removeGlobalOnLayoutListener(this);
                }

                spAccounts.setDropDownWidth(spAccounts.getWidth());
            }
        });
        spAccounts.post(new Runnable() {
            public void run() {
                spAccounts.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                    @Override
                    public void onItemSelected(AdapterView<?> arg0, View arg1, int arg2, long arg3) {
                        int position = spAccounts.getSelectedItemPosition();
                        AccountsUtil.getInstance(getActivity()).setCurrentSpinnerIndex(position + 1);//all account included
                        selectAccount(position);
                    }

                    @Override
                    public void onNothingSelected(AdapterView<?> arg0) {
                        ;
                    }
                });
            }
        });

        strBTC = MonetaryUtil.getInstance().getBTCUnit(PrefsUtil.getInstance(getActivity()).getValue(PrefsUtil.KEY_BTC_UNITS, MonetaryUtil.UNIT_BTC));
        strFiat = PrefsUtil.getInstance(getActivity()).getValue(PrefsUtil.KEY_SELECTED_FIAT, PrefsUtil.DEFAULT_CURRENCY);
        btc_fx = ExchangeRateFactory.getInstance(getActivity()).getLastPrice(strFiat);

        tvCurrency1.setText(strBTC);
        tvFiat2.setText(strFiat);

        if(PayloadFactory.getInstance().get().isUpgraded()) {
            assignHDReceiveAddress();
        }
        else {
            currentSelectedAddress = AccountsUtil.getInstance(getActivity()).getLegacyAddress(0).getAddress();
        }

        edReceivingAddress = (TextView)rootView.findViewById(R.id.receiving_address);

        mLayout = (SlidingUpPanelLayout) rootView.findViewById(R.id.sliding_layout);
        mLayout.setPanelState(SlidingUpPanelLayout.PanelState.HIDDEN);
        mLayout.setTouchEnabled(false);
        mLayout.setPanelSlideListener(new SlidingUpPanelLayout.PanelSlideListener() {
            @Override
            public void onPanelSlide(View panel, float slideOffset) {
            }

            @Override
            public void onPanelExpanded(View panel) {

            }

            @Override
            public void onPanelCollapsed(View panel) {

            }

            @Override
            public void onPanelAnchored(View panel) {
            }

            @Override
            public void onPanelHidden(View panel) {
            }
        });

        decimalCompatCheck(rootView);

        return rootView;
    }


    /*
    Custom keypad implementation
    Numerous Samsung devices have keypad issues where decimal separators are absent.
     */
    private void decimalCompatCheck(View rootView){

        if (Build.MANUFACTURER.toLowerCase().contains("samsung")) {

            numpad = ((TableLayout) rootView.findViewById(R.id.numericPad));
            numpad.setVisibility(View.GONE);
            isKeypadVisible = false;
            edAmount1.setTextIsSelectable(true);
            edAmount1.setOnFocusChangeListener(new View.OnFocusChangeListener() {
                @Override
                public void onFocusChange(View v, boolean hasFocus) {
                    if (hasFocus) {
                        View view = getActivity().getCurrentFocus();
                        if (view != null) {
                            InputMethodManager inputManager = (InputMethodManager) getActivity().getSystemService(Context.INPUT_METHOD_SERVICE);
                            inputManager.hideSoftInputFromWindow(view.getWindowToken(), InputMethodManager.HIDE_NOT_ALWAYS);
                        }
                        numpad.setVisibility(View.VISIBLE);
                        isKeypadVisible = true;
                    } else {
                        numpad.setVisibility(View.GONE);
                        isKeypadVisible = false;
                    }
                }
            });
            edAmount1.setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View v) {
                    numpad.setVisibility(View.VISIBLE);
                    isKeypadVisible = true;
                }
            });

            edAmount2.setTextIsSelectable(true);
            edAmount2.setOnFocusChangeListener(new View.OnFocusChangeListener() {
                @Override
                public void onFocusChange(View v, boolean hasFocus) {

                    if (hasFocus) {
                        View view = getActivity().getCurrentFocus();
                        if (view != null) {
                            InputMethodManager inputManager = (InputMethodManager) getActivity().getSystemService(Context.INPUT_METHOD_SERVICE);
                            inputManager.hideSoftInputFromWindow(view.getWindowToken(), InputMethodManager.HIDE_NOT_ALWAYS);
                        }
                        numpad.setVisibility(View.VISIBLE);
                        isKeypadVisible = true;
                    } else {
                        numpad.setVisibility(View.GONE);
                        isKeypadVisible = false;
                    }
                }
            });
            edAmount2.setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View v) {
                    numpad.setVisibility(View.VISIBLE);
                    isKeypadVisible = true;
                }
            });

            rootView.findViewById(R.id.button1).setOnClickListener(this);
            rootView.findViewById(R.id.button2).setOnClickListener(this);
            rootView.findViewById(R.id.button3).setOnClickListener(this);
            rootView.findViewById(R.id.button4).setOnClickListener(this);
            rootView.findViewById(R.id.button5).setOnClickListener(this);
            rootView.findViewById(R.id.button6).setOnClickListener(this);
            rootView.findViewById(R.id.button7).setOnClickListener(this);
            rootView.findViewById(R.id.button8).setOnClickListener(this);
            rootView.findViewById(R.id.button9).setOnClickListener(this);
            rootView.findViewById(R.id.button10).setOnClickListener(this);
            rootView.findViewById(R.id.button0).setOnClickListener(this);
            rootView.findViewById(R.id.buttonDeleteBack).setOnClickListener(this);
            rootView.findViewById(R.id.buttonDone).setOnClickListener(this);

        }
    }

    @Override
    public void setUserVisibleHint(boolean isVisibleToUser) {
        super.setUserVisibleHint(isVisibleToUser);

        if(isVisibleToUser) {
            strBTC = MonetaryUtil.getInstance().getBTCUnit(PrefsUtil.getInstance(getActivity()).getValue(PrefsUtil.KEY_BTC_UNITS, MonetaryUtil.UNIT_BTC));
            strFiat = PrefsUtil.getInstance(getActivity()).getValue(PrefsUtil.KEY_SELECTED_FIAT, PrefsUtil.DEFAULT_CURRENCY);
            btc_fx = ExchangeRateFactory.getInstance(getActivity()).getLastPrice(strFiat);
            tvCurrency1.setText(isBTC ? strBTC : strFiat);
            tvFiat2.setText(isBTC ? strFiat : strBTC);
        }
        else {
            ;
        }
    }

    @Override
    public void onResume() {
        super.onResume();

        AppUtil.getInstance(getActivity()).setAllowLockTimer(true);

        MainActivity.currentFragment = this;

        strBTC = MonetaryUtil.getInstance().getBTCUnit(PrefsUtil.getInstance(getActivity()).getValue(PrefsUtil.KEY_BTC_UNITS, MonetaryUtil.UNIT_BTC));
        strFiat = PrefsUtil.getInstance(getActivity()).getValue(PrefsUtil.KEY_SELECTED_FIAT, PrefsUtil.DEFAULT_CURRENCY);
        btc_fx = ExchangeRateFactory.getInstance(getActivity()).getLastPrice(strFiat);
        tvCurrency1.setText(isBTC ? strBTC : strFiat);
        tvFiat2.setText(isBTC ? strFiat : strBTC);

        if(spAccounts != null) {

            int currentSelected = AccountsUtil.getInstance(getActivity()).getCurrentSpinnerIndex();
            if(currentSelected!=0)currentSelected--;//exclude 'all account'
            spAccounts.setSelection(currentSelected);
            selectAccount(currentSelected);
        }
    }

    @Override
    public void onPause() {
        super.onPause();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
    }

    private void selectAccount(int position){
        if (position >= AccountsUtil.getInstance(getActivity()).getLastHDIndex()) {
            //Legacy addresses

            LegacyAddress legacyAddress = AccountsUtil.getInstance(getActivity()).getLegacyAddress(position - AccountsUtil.getInstance(getActivity()).getLastHDIndex());

            if(legacyAddress.getTag() == PayloadFactory.WATCHONLY_ADDRESS)    {
                spAccounts.setSelection(0);
                ToastCustom.makeText(getActivity(), getString(R.string.watchonly_address), ToastCustom.LENGTH_SHORT, ToastCustom.TYPE_GENERAL);
                return;
            }
            else if(legacyAddress.getTag() == PayloadFactory.ARCHIVED_ADDRESS)   {
                spAccounts.setSelection(0);
                ToastCustom.makeText(getActivity(), getString(R.string.archived_address), ToastCustom.LENGTH_SHORT, ToastCustom.TYPE_GENERAL);
                return;
            }
            else    {
                currentSelectedAddress = legacyAddress.getAddress();
                displayQRCode();
            }

        } else {
            //hd accounts
            currentSelectedAccount = AccountsUtil.getInstance(getActivity()).getSendReceiveAccountIndexResolver().get(position);
            assignHDReceiveAddress();
            displayQRCode();
        }
    }

    private void displayQRCode() {

        edReceivingAddress.setText(currentSelectedAddress);

        BigInteger bamount = null;
        try {
            long lamount = 0L;
            if(isBTC) {
                lamount = (long)(Math.round(NumberFormat.getInstance(locale).parse(edAmount1.getText().toString()).doubleValue() * 1e8));
            }
            else {
                lamount = (long)(Math.round(NumberFormat.getInstance(locale).parse(edAmount2.getText().toString()).doubleValue() * 1e8));
            }
            bamount = MonetaryUtil.getInstance(getActivity()).getUndenominatedAmount(lamount);
            if(bamount.compareTo(BigInteger.valueOf(2100000000000000L)) == 1)    {
                ToastCustom.makeText(getActivity(), getActivity().getString(R.string.invalid_amount), ToastCustom.LENGTH_LONG, ToastCustom.TYPE_ERROR);
                return;
            }
            if(!bamount.equals(BigInteger.ZERO)) {
                generateQRCode(BitcoinURI.convertToBitcoinURI(currentSelectedAddress, Coin.valueOf(bamount.longValue()), "", ""));
                write2NFC(BitcoinURI.convertToBitcoinURI(currentSelectedAddress, Coin.valueOf(bamount.longValue()), "", ""));
            }
            else {
                generateQRCode("bitcoin:" + currentSelectedAddress);
                write2NFC("bitcoin:" + currentSelectedAddress);
            }
        }
        catch(NumberFormatException | ParseException e) {
            generateQRCode("bitcoin:" + currentSelectedAddress);
            write2NFC("bitcoin:" + currentSelectedAddress);
        }

    }

    private void generateQRCode(final String uri) {

        new AsyncTask<Void, Void, Bitmap>(){

            @Override
            protected void onPreExecute() {
                super.onPreExecute();

                ivReceivingQR.setVisibility(View.GONE);
                edReceivingAddress.setVisibility(View.GONE);
                rootView.findViewById(R.id.progressBar2).setVisibility(View.VISIBLE);
            }

            @Override
            protected Bitmap doInBackground(Void... params) {

                Bitmap bitmap = null;
                int qrCodeDimension = 260;

                QRCodeEncoder qrCodeEncoder = new QRCodeEncoder(uri, null, Contents.Type.TEXT, BarcodeFormat.QR_CODE.toString(), qrCodeDimension);

                try {
                    bitmap = qrCodeEncoder.encodeAsBitmap();
                } catch (WriterException e) {
                    e.printStackTrace();
                }

                return bitmap;
            }

            @Override
            protected void onPostExecute(Bitmap bitmap) {
                super.onPostExecute(bitmap);
                rootView.findViewById(R.id.progressBar2).setVisibility(View.GONE);
                ivReceivingQR.setVisibility(View.VISIBLE);
                edReceivingAddress.setVisibility(View.VISIBLE);
                ivReceivingQR.setImageBitmap(bitmap);

                setupBottomSheet();
            }
        }.execute();
    }

    private void assignHDReceiveAddress() {

        try {
            currentSelectedReceiveAddress = HDPayloadBridge.getInstance(getActivity()).getReceiveAddress(currentSelectedAccount);
            currentSelectedAddress = currentSelectedReceiveAddress.getAddress();
        }
        catch(IOException | MnemonicException.MnemonicLengthException | MnemonicException.MnemonicChecksumException
                | MnemonicException.MnemonicWordException | AddressFormatException
                | DecoderException e) {
            e.printStackTrace();
        }

    }

    private void updateFiatTextField(String cBtc) {
        double btc_amount = 0.0;
        try {
            btc_amount = MonetaryUtil.getInstance(getActivity()).getUndenominatedAmount(NumberFormat.getInstance(locale).parse(cBtc).doubleValue());
        }
        catch(NumberFormatException | ParseException e) {
            btc_amount = 0.0;
        }
        double fiat_amount = btc_fx * btc_amount;
        edAmount2.setText(MonetaryUtil.getInstance().getFiatFormat(strFiat).format(fiat_amount));
    }

    private void updateBtcTextField(String cfiat){

        double fiat_amount = 0.0;
        try {
            fiat_amount = NumberFormat.getInstance(locale).parse(cfiat).doubleValue();
        }
        catch(NumberFormatException | ParseException e) {
            fiat_amount = 0.0;
        }
        double btc_amount = fiat_amount / btc_fx;
        edAmount1.setText(MonetaryUtil.getInstance().getBTCFormat().format(MonetaryUtil.getInstance(getActivity()).getDenominatedAmount(btc_amount)));
    }

    private void write2NFC(final String uri) {

        if (Build.VERSION.SDK_INT < 16){
            return;
        }

        NfcAdapter nfc = NfcAdapter.getDefaultAdapter(getActivity());
        if (nfc != null && nfc.isNdefPushEnabled() ) {
            nfc.setNdefPushMessageCallback(new NfcAdapter.CreateNdefMessageCallback() {
                @Override
                public NdefMessage createNdefMessage(NfcEvent event) {
                    NdefRecord uriRecord = NdefRecord.createUri(uri);
                    return new NdefMessage(new NdefRecord[]{ uriRecord });
                }
            }, getActivity());
        }

    }

    @Override
    public void onPrepareOptionsMenu(Menu menu) {
        super.onPrepareOptionsMenu(menu);

        menu.findItem(R.id.action_merchant_directory).setVisible(false);
        menu.findItem(R.id.action_qr).setVisible(false);
        menu.findItem(R.id.action_send).setVisible(false);
        MenuItem i = menu.findItem(R.id.action_share_receive).setVisible(true);

        i.setOnMenuItemClickListener(new MenuItem.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem item) {

                onShareClicked();

                return false;
            }
        });
    }

    private void onShareClicked(){

        if (mLayout != null) {
            if (mLayout.getPanelState() != SlidingUpPanelLayout.PanelState.HIDDEN) {
                mLayout.setPanelState(SlidingUpPanelLayout.PanelState.HIDDEN);
                mainContentShadow.setVisibility(View.GONE);
            } else {

                new AlertDialog.Builder(getActivity())
                        .setTitle(R.string.app_name)
                        .setMessage(R.string.receive_address_to_share)
                        .setCancelable(false)
                        .setPositiveButton(R.string.yes, new DialogInterface.OnClickListener() {

                            public void onClick(DialogInterface dialog, int whichButton) {

                                mLayout.setPanelState(SlidingUpPanelLayout.PanelState.COLLAPSED);
                                mainContentShadow.setVisibility(View.VISIBLE);
                                mainContentShadow.bringToFront();

                            }

                        }).setNegativeButton(R.string.no, new DialogInterface.OnClickListener() {

                    public void onClick(DialogInterface dialog, int whichButton) {
                        ;
                    }
                }).show();

            }
        }

    }

    private void setupBottomSheet(){

        //Re-Populate list
        String strFileName = AppUtil.getInstance(getActivity()).getReceiveQRFilename();
        File file = new File(strFileName);
        if (!file.exists()) {
            try {
                file.createNewFile();
            } catch (Exception e) {
                ToastCustom.makeText(getActivity(), e.getMessage(), ToastCustom.LENGTH_LONG, ToastCustom.TYPE_ERROR);
            }
        }
        file.setReadable(true, false);

        FileOutputStream fos = null;
        try {
            fos = new FileOutputStream(file);
        } catch (FileNotFoundException fnfe) {
            fnfe.printStackTrace();
        }

        if (file != null && fos != null) {
            Bitmap bitmap = ((BitmapDrawable) ivReceivingQR.getDrawable()).getBitmap();
            bitmap.compress(CompressFormat.PNG, 0, fos);

            try {
                fos.close();
            } catch (IOException ioe) {
                ioe.printStackTrace();
            }

            ArrayList<SendPaymentCodeData> dataList = new ArrayList<SendPaymentCodeData>();

            PackageManager pm = getActivity().getPackageManager();

            Intent intent = new Intent();
            intent.setAction(Intent.ACTION_SEND);
            intent.setType("image/png");
            intent.putExtra(Intent.EXTRA_STREAM, Uri.fromFile(file));
            List<ResolveInfo> resInfos = pm.queryIntentActivities(intent, 0);

            SendPaymentCodeData d;
            for (ResolveInfo resInfo : resInfos) {

                String context = resInfo.activityInfo.packageName;
                String packageClassName = resInfo.activityInfo.name;
                CharSequence label = resInfo.loadLabel(pm);
                Drawable icon = resInfo.loadIcon(pm);

                Intent shareIntent = new Intent();
                shareIntent.setAction(Intent.ACTION_SEND);
                shareIntent.setType("image/png");
                shareIntent.putExtra(Intent.EXTRA_STREAM, Uri.fromFile(file));
                shareIntent.setClassName(context, packageClassName);

                d = new SendPaymentCodeData();
                d.setTitle(label.toString());
                d.setLogo(icon);
                d.setIntent(shareIntent);
                dataList.add(d);
            }

            ArrayAdapter adapter = new SendPaymentCodeAdapter(getActivity(), dataList);
            sendPaymentCodeAppListlist = (ListView) rootView.findViewById(R.id.share_app_list);
            sendPaymentCodeAppListlist.setAdapter(adapter);
            adapter.notifyDataSetChanged();
        }
    }

    class SendPaymentCodeAdapter extends ArrayAdapter<SendPaymentCodeData>
    {
        private final Context context;
        private final ArrayList<SendPaymentCodeData> repoDataArrayList;

        public SendPaymentCodeAdapter(Context context, ArrayList<SendPaymentCodeData> repoDataArrayList) {

            super(context, R.layout.fragment_receive_share_row, repoDataArrayList);

            this.context = context;
            this.repoDataArrayList = repoDataArrayList;
        }

        @Override
        public View getView(final int position, View convertView, ViewGroup parent)
        {
            LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);

            View rowView = null;
            rowView = inflater.inflate(R.layout.fragment_receive_share_row, parent, false);

            ImageView image = (ImageView) rowView.findViewById(R.id.share_app_image);
            TextView title = (TextView) rowView.findViewById(R.id.share_app_title);

            image.setImageDrawable(repoDataArrayList.get(position).getLogo());
            title.setText(repoDataArrayList.get(position).getTitle());

            rowView.setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View v) {
                    AppUtil.getInstance(getActivity()).setAllowLockTimer(false);
                    startActivity(repoDataArrayList.get(position).getIntent());
                }
            });

            return rowView;
        }
    }

    class SendPaymentCodeData
    {
        private Drawable logo;
        private String title;
        private Intent intent;

        public SendPaymentCodeData() {

        }

        public Intent getIntent() {
            return intent;
        }

        public void setIntent(Intent intent) {
            this.intent = intent;
        }

        public String getTitle() {
            return title;
        }

        public void setTitle(String title) {
            this.title = title;
        }

        public Drawable getLogo() {
            return logo;
        }

        public void setLogo(Drawable logo) {
            this.logo = logo;
        }
    }


    @Override
    public void onClick(View v) {

        String pad = "";
        switch (v.getId()) {
            case R.id.button1:pad = v.getTag().toString().substring(0, 1);break;
            case R.id.button2:pad = v.getTag().toString().substring(0, 1);break;
            case R.id.button3:pad = v.getTag().toString().substring(0, 1);break;
            case R.id.button4:pad = v.getTag().toString().substring(0, 1);break;
            case R.id.button5:pad = v.getTag().toString().substring(0, 1);break;
            case R.id.button6:pad = v.getTag().toString().substring(0, 1);break;
            case R.id.button7:pad = v.getTag().toString().substring(0, 1);break;
            case R.id.button8:pad = v.getTag().toString().substring(0, 1);break;
            case R.id.button9:pad = v.getTag().toString().substring(0, 1);break;
            case R.id.button10:pad = ".";break;
            case R.id.button0:pad = v.getTag().toString().substring(0, 1);break;
            case R.id.buttonDeleteBack:pad = null;break;
            case R.id.buttonDone:numpad.setVisibility(View.GONE);break;
        }

        if(pad!=null) {
            // Append tapped #
            if (edAmount1.hasFocus()) {
                edAmount1.append(pad);
            } else if (edAmount2.hasFocus()) {
                edAmount2.append(pad);
            }
        }else{
            // Back clicked
            if (edAmount1.hasFocus()) {
                String e1 = edAmount1.getText().toString();
                if(e1.length()>0)edAmount1.setText(e1.substring(0, e1.length() - 1));
            } else if (edAmount2.hasFocus()) {
                String e2 = edAmount2.getText().toString();
                if(e2.length()>0)edAmount2.setText(e2.substring(0, e2.length() - 1));
            }
        }

        if (edAmount1.hasFocus() && edAmount1.getText().length()>0) {
            edAmount1.post(new Runnable() {
                @Override
                public void run() {
                    edAmount1.setSelection(edAmount1.getText().toString().length());
                }
            });
        } else if (edAmount2.hasFocus() && edAmount2.getText().length()>0) {
            edAmount2.post(new Runnable() {
                @Override
                public void run() {
                    edAmount2.setSelection(edAmount2.getText().toString().length());
                }
            });
        }
    }

    @Override
    public void onKeypadClose() {
        numpad.setVisibility(View.GONE);
        isKeypadVisible = false;
    }
}
