package info.blockchain.wallet;

import android.app.Activity;
import android.app.Fragment;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Typeface;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.TextView;

import org.json.JSONException;
import org.json.JSONObject;

import info.blockchain.wallet.crypto.AESUtil;
import info.blockchain.wallet.pairing.PairingFactory;
import info.blockchain.wallet.payload.PayloadFactory;
import info.blockchain.wallet.util.AppUtil;
import info.blockchain.wallet.util.CharSequenceX;
import info.blockchain.wallet.util.PrefsUtil;
import info.blockchain.wallet.util.ToastCustom;
import info.blockchain.wallet.util.TypefaceUtil;
import piuk.blockchain.android.R;

public class ManualPairingFragment extends Fragment {

    private EditText edGuid = null;
    private EditText edPassword = null;
    private TextView next = null;

    private ProgressDialog progress = null;

    private boolean waitinForAuth = true;
    private int timer = 0;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_manual_pairing, container, false);

        rootView.setFilterTouchesWhenObscured(true);

        getActivity().setTitle(getResources().getString(R.string.manual_pairing));

        edGuid = (EditText)rootView.findViewById(R.id.wallet_id);
        edPassword = (EditText)rootView.findViewById(R.id.wallet_pass);
        next = (TextView)rootView.findViewById(R.id.command_next);

        next.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                final String guid = edGuid.getText().toString();
                final String pw = edPassword.getText().toString();

                if (guid == null || guid.length() == 0) {
                    ToastCustom.makeText(getActivity(), getString(R.string.invalid_guid), ToastCustom.LENGTH_SHORT, ToastCustom.TYPE_ERROR);
                } else if (pw == null || pw.length() == 0) {
                    ToastCustom.makeText(getActivity(), getString(R.string.invalid_password), ToastCustom.LENGTH_SHORT, ToastCustom.TYPE_ERROR);
                } else {
                    pairingThreadManual(guid, new CharSequenceX(pw));
                }
            }
        });

        Typeface typeface = TypefaceUtil.getInstance(getActivity()).getRobotoTypeface();
        edGuid.setTypeface(typeface);
        edPassword.setTypeface(typeface);

        return rootView;
    }

    private void pairingThreadManual(final String guid, final CharSequenceX password) {

        waitinForAuth = true;

        final Handler handler = new Handler();

        if(progress != null && progress.isShowing()) {
            progress.dismiss();
            progress = null;
        }

        final Activity thisActivity = getActivity();
        progress = new ProgressDialog(thisActivity);
        progress.setCancelable(false);
        progress.setTitle(R.string.app_name);
        progress.setMessage(getActivity().getString(R.string.pairing_wallet));
        progress.setOnCancelListener(new DialogInterface.OnCancelListener() {
            @Override
            public void onCancel(DialogInterface dialog) {
                waitinForAuth = false;
            }
        });
        progress.show();

        new Thread(new Runnable() {
            @Override
            public void run() {
                Looper.prepare();

                try {
                    String response = PairingFactory.getInstance(thisActivity).getWalletManualPairing(guid);

                    if(response.equals(PairingFactory.KEY_AUTH_REQUIRED)){
                        thisActivity.runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                progress.setCancelable(true);
                                progress.setMessage(getResources().getString(R.string.check_email_to_auth_login));
                            }
                        });
                        response = waitForAuthThread(guid, password);
                    }

                    if(response!=null && response.equals("Authorization Required")){
                        ToastCustom.makeText(thisActivity, getString(R.string.auth_failed), ToastCustom.LENGTH_SHORT, ToastCustom.TYPE_ERROR);
                        AppUtil.getInstance(thisActivity).clearCredentialsAndRestart();
                    }

                    JSONObject jsonObj = new JSONObject(response);

                    if(jsonObj != null && jsonObj.has("payload")) {
                        String encrypted_payload = (String)jsonObj.getString("payload");

                        String decrypted_payload = null;
                        try {
                            decrypted_payload = AESUtil.decrypt(encrypted_payload, password, AESUtil.PasswordPBKDF2Iterations);
                        }
                        catch(Exception e) {
                            e.printStackTrace();
                            ToastCustom.makeText(thisActivity, getString(R.string.pairing_failed_decrypt_error), ToastCustom.LENGTH_SHORT, ToastCustom.TYPE_ERROR);
                            AppUtil.getInstance(thisActivity).clearCredentialsAndRestart();
                        }

                        if(decrypted_payload != null) {
                            JSONObject payloadObj = new JSONObject(decrypted_payload);
                            if(payloadObj != null && payloadObj.has("sharedKey")) {
                                PrefsUtil.getInstance(thisActivity).setValue(PrefsUtil.KEY_GUID, guid);
                                PayloadFactory.getInstance().setTempPassword(password);
                                AppUtil.getInstance(thisActivity).setSharedKey((String)payloadObj.get("sharedKey"));

                                if(HDPayloadBridge.getInstance(thisActivity).init(password)) {
                                    PrefsUtil.getInstance(thisActivity).setValue(PrefsUtil.KEY_EMAIL_VERIFIED, true);
                                    PayloadFactory.getInstance().setTempPassword(password);
//                                    ToastCustom.makeText(getActivity(), getString(R.string.pairing_success), ToastCustom.LENGTH_SHORT, ToastCustom.TYPE_OK);
                                    Intent intent = new Intent(thisActivity, PinEntryActivity.class);
                                    intent.putExtra(PairingFactory.KEY_EXTRA_IS_PAIRING, true);
                                    thisActivity.startActivity(intent);
                                }
                                else {
                                    ToastCustom.makeText(thisActivity, getString(R.string.pairing_failed), ToastCustom.LENGTH_SHORT, ToastCustom.TYPE_ERROR);
                                    AppUtil.getInstance(thisActivity).clearCredentialsAndRestart();
                                }

                            }

                        }
                        else {
                            ToastCustom.makeText(thisActivity, getString(R.string.pairing_failed), ToastCustom.LENGTH_SHORT, ToastCustom.TYPE_ERROR);
                            AppUtil.getInstance(thisActivity).clearCredentialsAndRestart();
                        }

                    }

                }
                catch(JSONException je) {
                    je.printStackTrace();
                    if(thisActivity!=null) {
                        ToastCustom.makeText(thisActivity, getString(R.string.pairing_failed), ToastCustom.LENGTH_SHORT, ToastCustom.TYPE_ERROR);
                        AppUtil.getInstance(thisActivity).clearCredentialsAndRestart();
                    }
                }
                catch(Exception e) {
                    e.printStackTrace();
                    if(thisActivity!=null) {
                        ToastCustom.makeText(thisActivity, getString(R.string.pairing_failed), ToastCustom.LENGTH_SHORT, ToastCustom.TYPE_ERROR);
                        AppUtil.getInstance(thisActivity).clearCredentialsAndRestart();
                    }
                }finally {
                    if(progress != null && progress.isShowing()) {
                        progress.dismiss();
                        progress = null;
                    }
                }

                handler.post(new Runnable() {
                    @Override
                    public void run() {
                        ;
                    }
                });

                Looper.loop();

            }
        }).start();
    }

    private String waitForAuthThread(final String guid, final CharSequenceX password){

        final int waitTime = 2;//minutes to wait for auth
        timer = (waitTime*60);

        new Thread(new Runnable() {
            @Override
            public void run() {

                while(waitinForAuth) {

                    if(getActivity()!=null) {

                        getActivity().runOnUiThread(new Runnable() {

                            @Override
                            public void run() {
                                progress.setMessage(getResources().getString(R.string.check_email_to_auth_login) + " " + timer);
                                timer--;
                            }
                        });
                        try {
                            Thread.sleep(1000);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }

                        if (timer <= 0) {
                            ToastCustom.makeText(getActivity(), getString(R.string.pairing_failed), ToastCustom.LENGTH_SHORT, ToastCustom.TYPE_ERROR);
                            waitinForAuth = false;
                            progress.cancel();
                            AppUtil.getInstance(getActivity()).clearCredentialsAndRestart();
                        }
                    }else{
                        waitinForAuth = false;
                    }
                }
            }
        }).start();

        int sleep = 5;//second
        while(waitinForAuth){

            try {Thread.sleep(1000*sleep);} catch (InterruptedException e) {e.printStackTrace();}
            sleep = 1;//after initial sleep, poll every 1 second

            String response = null;
            try {
                response = PairingFactory.getInstance(getActivity()).getWalletManualPairing(guid);
                if(!response.equals(PairingFactory.KEY_AUTH_REQUIRED)) {
                    waitinForAuth = false;
                    return response;
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        waitinForAuth = false;
        return PairingFactory.KEY_AUTH_REQUIRED;
    }
}
