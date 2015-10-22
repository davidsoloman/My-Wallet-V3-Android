package info.blockchain.merchant.directory.util;

import android.content.Context;
import android.os.Looper;

import org.json.JSONObject;

import java.io.BufferedOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;

import info.blockchain.merchant.directory.BTCBusiness;
import info.blockchain.wallet.util.ToastCustom;

public class MerchantUtil {

    private static MerchantUtil instance = null;
    private static Context context = null;

    private MerchantUtil() { ; }

    public static MerchantUtil getInstance(Context ctx) {

        context = ctx;

        if(instance == null) {
            instance = new MerchantUtil();
        }

        return instance;
    }

    public void flagMerchant(final BTCBusiness b, final boolean acceptsBitcoin){

        new Thread(new Runnable() {
            @Override
            public void run() {

                Looper.prepare();

                InputStream is = null;
                OutputStream os = null;

                URL url = null;
                try {

                    url = new URL("https://merchant-directory.blockchain.info/api/report");
                    JSONObject json = new JSONObject();
                    json.put("merchantId", b.id);
                    json.put("acceptsBitcoin", acceptsBitcoin);
                    String message = json.toString();

                    HttpURLConnection conn = (HttpURLConnection) url.openConnection();

                    try {
                        conn.setReadTimeout(60000);
                        conn.setConnectTimeout(60000);
                        conn.setRequestMethod("PUT");
                        conn.setDoInput(true);
                        conn.setDoOutput(true);
                        conn.setFixedLengthStreamingMode(message.getBytes().length);
                        conn.setRequestProperty("Content-Type", "application/json");
                        conn.setRequestProperty("X-Requested-With", "XMLHttpRequest");
                        conn.connect();

                        os = new BufferedOutputStream(conn.getOutputStream());
                        os.write(message.getBytes());
                        os.flush();

                        if (conn.getResponseCode() == 200)
                            ToastCustom.makeText(context, "Successfully submitted", ToastCustom.LENGTH_LONG, ToastCustom.TYPE_OK);
                        else
                            ToastCustom.makeText(context, "Error: Please try again later.", ToastCustom.LENGTH_LONG, ToastCustom.TYPE_ERROR);

                    } finally {
                        if (os != null) os.close();
                        if (is != null) is.close();
                        conn.disconnect();
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }

                Looper.loop();

            }
        }).start();
    }
}
