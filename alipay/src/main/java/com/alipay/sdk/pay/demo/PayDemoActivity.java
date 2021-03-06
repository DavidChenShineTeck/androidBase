package com.alipay.sdk.pay.demo;

import android.app.Activity;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.text.TextUtils;
import android.util.Log;
import android.widget.Toast;

import com.alipay.sdk.app.PayTask;

import java.io.UnsupportedEncodingException;
import java.lang.ref.WeakReference;
import java.net.URLEncoder;

public class PayDemoActivity extends Activity {

    private String payInfo;
    private String rsa_private, partner, seller, notify_url, total_fee,
            body, subject, payCode, sign;
    private static final int SDK_PAY_FLAG = 1;
    private static final int SDK_CHECK_FLAG = 2;
    private PayHandler mHandler;

    private static class PayHandler extends Handler {
        WeakReference<PayDemoActivity> mActivity;

        public PayHandler(PayDemoActivity activity) {
            mActivity = new WeakReference<>(activity);
        }

        @Override
        public void handleMessage(Message msg) {
            PayDemoActivity activity = mActivity.get();
            if (activity == null)
                return;
            switch (msg.what) {
                case SDK_PAY_FLAG: {
                    PayResult payResult = new PayResult((String) msg.obj);

                    // 支付宝返回此次支付结果及加签，建议对支付宝签名信息拿签约时支付宝提供的公钥做验签
                    String resultInfo = payResult.getResult();

                    String resultStatus = payResult.getResultStatus();

                    // 判断resultStatus 为“9000”则代表支付成功，具体状态码代表含义可参考接口文档
                    if (TextUtils.equals(resultStatus, "9000")) {
                        Toast.makeText(activity, "支付成功",
                                Toast.LENGTH_SHORT).show();

                        activity.setPayResult(true, "支付成功");
                    } else {
                        // 判断resultStatus 为非“9000”则代表可能支付失败
                        // “8000”代表支付结果因为支付渠道原因或者系统原因还在等待支付结果确认，最终交易是否成功以服务端异步通知为准（小概率状态）
                        if (TextUtils.equals(resultStatus, "8000")) {
                            Toast.makeText(activity, "支付结果确认中",
                                    Toast.LENGTH_SHORT).show();

                            activity.setPayResult(true, "支付结果确认中");
                        } else {
                            // 其他值就可以判断为支付失败，包括用户主动取消支付，或者系统返回的错误
                            Toast.makeText(activity, "支付失败",
                                    Toast.LENGTH_SHORT).show();
                            activity.setPayResult(false, "支付失败");
                        }
                    }
                    break;
                }
                case SDK_CHECK_FLAG: {
                    Toast.makeText(activity, "检查结果为：" + msg.obj,
                            Toast.LENGTH_SHORT).show();
                    break;
                }
                default:
                    break;
            }
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.pay_main);

        Bundle bundle = getIntent().getExtras();
        if (bundle == null){
            this.finish();
            return;
        }

        rsa_private = bundle.getString("rsa_private");
        partner = bundle.getString("partner");
        seller = bundle.getString("seller");
        notify_url = bundle.getString("notify_url");
        total_fee = bundle.getString("total_fee");
        body = bundle.getString("body");
        subject = bundle.getString("subject");
        payCode = bundle.getString("payCode");
        sign = bundle.getString("sign");

        mHandler = new PayHandler(this);
        pay();
    }

    private void setPayResult(boolean result, String reason){

        setResult(RESULT_OK, getIntent()
                .putExtra("reason", reason)
                .putExtra("success", result));

        this.finish();
    }

    /**
     * call alipay sdk pay. 调用SDK支付
     */
    public void pay() {
        if (TextUtils.isEmpty(partner)
                || TextUtils.isEmpty(seller)) {

            setPayResult(false, "partner or seller is empty");
           return;
        }
        // 订单
        String orderInfo = getOrderInfo();

        // 对订单做RSA 签名   如果已经签名，则不进行
        sign = sign == null ? sign(orderInfo) : sign;
        try {
            // 仅需对sign 做URL编码
            sign = URLEncoder.encode(sign, "utf-8");
            Log.i("===sign===", sign);
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }

        // 完整的符合支付宝参数规范的订单信息
        payInfo = orderInfo + "&sign=\"" + sign + "\"&"
                + getSignType();

        Runnable payRunnable = new Runnable() {

            @Override
            public void run() {
                // 构造PayTask 对象
                PayTask alipay = new PayTask(PayDemoActivity.this);
                // 调用支付接口，获取支付结果
                String result = alipay.pay(payInfo, true);

                Message msg = new Message();
                msg.what = SDK_PAY_FLAG;
                msg.obj = result;
                mHandler.sendMessage(msg);
            }
        };

        // 必须异步调用
        Thread payThread = new Thread(payRunnable);
        payThread.start();
    }

//    /**
//     * check whether the device has authentication alipay account.
//     * 查询终端设备是否存在支付宝认证账户
//     */
//    public void check(View v) {
//        Runnable checkRunnable = new Runnable() {
//
//            @Override
//            public void run() {
//                // 构造PayTask 对象
//                PayTask payTask = new PayTask(PayDemoActivity.this);
//                // 调用查询接口，获取查询结果
//                boolean isExist = payTask.checkAccountIfExist();
//
//                Message msg = new Message();
//                msg.what = SDK_CHECK_FLAG;
//                msg.obj = isExist;
//                mHandler.sendMessage(msg);
//            }
//        };
//
//        Thread checkThread = new Thread(checkRunnable);
//        checkThread.start();
//    }
//
//    /**
//     * get the sdk version. 获取SDK版本号
//     */
//    public void getSDKVersion() {
//        PayTask payTask = new PayTask(this);
//        String version = payTask.getVersion();
//        Toast.makeText(this, version, Toast.LENGTH_SHORT).show();
//    }

    /**
     * create the order info. 创建订单信息
     */
    public String getOrderInfo() {

        // 签约合作者身份ID
        String orderInfo = "partner=" + "\"" + partner + "\"";

        // 签约卖家支付宝账号
        orderInfo += "&seller_id=" + "\"" + seller + "\"";

        // 商户网站唯一订单号
        orderInfo += "&out_trade_no=" + "\"" + payCode + "\"";

        // 商品名称
        orderInfo += "&subject=" + "\"" + subject + "\"";

        // 商品详情
        orderInfo += "&body=" + "\"" + body + "\"";

        // 商品金额
        orderInfo += "&total_fee=" + "\"" + total_fee + "\"";

        // 服务器异步通知页面路径
        orderInfo += "&notify_url=" + "\"" + notify_url
                + "\"";

        // 服务接口名称， 固定值
        orderInfo += "&service=\"mobile.securitypay.pay\"";

        // 支付类型， 固定值
        orderInfo += "&payment_type=\"1\"";

        // 参数编码， 固定值
        orderInfo += "&_input_charset=\"utf-8\"";

        // 设置未付款交易的超时时间
        // 默认30分钟，一旦超时，该笔交易就会自动被关闭。
        // 取值范围：1m～15d。
        // m-分钟，h-小时，d-天，1c-当天（无论交易何时创建，都在0点关闭）。
        // 该参数数值不接受小数点，如1.5h，可转换为90m。
        orderInfo += "&it_b_pay=\"30m\"";

        // extern_token为经过快登授权获取到的alipay_open_id,带上此参数用户将使用授权的账户进行支付
        // orderInfo += "&extern_token=" + "\"" + extern_token + "\"";

        // 支付宝处理完请求后，当前页面跳转到商户指定页面的路径，可空
        orderInfo += "&return_url=\"m.alipay.com\"";

        // 调用银行卡支付，需配置此参数，参与签名， 固定值 （需要签约《无线银行卡快捷支付》才能使用）
        // orderInfo += "&paymethod=\"expressGateway\"";

        return orderInfo;
    }

    /**
     * sign the order info. 对订单信息进行签名
     *
     * @param content 待签名订单信息
     */
    public String sign(String content) {
        return SignUtils.sign(content, rsa_private);
    }

    /**
     * get the sign type we use. 获取签名方式
     */
    public String getSignType() {
        return "sign_type=\"RSA\"";
    }

}
