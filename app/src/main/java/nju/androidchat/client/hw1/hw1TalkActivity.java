package nju.androidchat.client.hw1;

import android.graphics.drawable.Drawable;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import lombok.extern.java.Log;
import nju.androidchat.client.ClientMessage;
import nju.androidchat.client.R;
import nju.androidchat.client.Utils;
import nju.androidchat.client.component.ItemTextReceive;
import nju.androidchat.client.component.ItemTextSend;
import nju.androidchat.client.component.OnRecallMessageRequested;

@Log
public class hw1TalkActivity extends AppCompatActivity implements hw1Contract.View, TextView.OnEditorActionListener, OnRecallMessageRequested {
    private hw1Contract.Presenter presenter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        hw1TalkModel hwTalkModel = new hw1TalkModel();

        // Create the presenter
        this.presenter = new hw1TalkPresenter(hwTalkModel, this, new ArrayList<>());
        hwTalkModel.setihw1TalkPresenter(this.presenter);
    }

    @Override
    public void onResume() {
        super.onResume();
        presenter.start();
    }

    @Override
    public void showMessageList(List<ClientMessage> messages) {
        runOnUiThread(() -> {
                    LinearLayout content = findViewById(R.id.chat_content);

                    // 删除所有已有的ItemText
                    content.removeAllViews();

                    // 增加ItemText
            new Thread(() ->{
                    for (ClientMessage message : messages) {
                        String text = String.format("%s", message.getMessage());
                        if (isMdImg(text)) {
                            CharSequence charSequence;
                            String[] txts = text.spilt("!\\[.*\\]");
                            String url = txts[txts.length - 1];
                            url = url.substring(1, url.length() - 1);
                            String img = "<img src='" + url + "'>";
                            try {
                                charSequence = Html.fromHtml(img, new Html.ImageGetter() {
                                    @Override
                                    public Drawable getDrawable(String source) {
                                        Drawable drawable = getImg(source);
                                        drawable.setBounds(0, 0, drawable.getIntrinsicWidth(), drawable.getIntrinsicHeight());
                                        return drawable;
                                    }
                                }, null);
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                            CharSequence charSequence1 = charSequence;
                            runOnUiThread(() -> {
                                if (message.getSenderUsername().equals(this.presenter.getUsername())) {
                                    content.addView(new ItemTextSend(this, charSequence1, message.getMessageId(), this));
                                } else {
                                    content.addView(new ItemTextReceive(this, charSequence1, message.getMessageId()));
                                }
                                Utils.scrollListToBottom(this);
                            });

                        }
                        // 如果是自己发的，增加ItemTextSend
                        else {
                            runOnYUiThread(() -> {
                                if (message.getSenderUsername().equals(this.presenter.getUsername())) {
                                    content.addView(new ItemTextSend(this, text, message.getMessageId(), this));
                                } else {
                                    content.addView(new ItemTextReceive(this, text, message.getMessageId()));
                                }
                            });
                        }
                    }
                }).start();

                Utils.scrollListToBottom(this);
        }
        );
    }

    @Override
    public void setPresenter(hw1Contract.Presenter presenter) {
        this.presenter = presenter;
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (null != this.getCurrentFocus()) {
            return hideKeyboard();
        }
        return super.onTouchEvent(event);
    }

    private boolean hideKeyboard() {
        return Utils.hideKeyboard(this);
    }


    @Override
    public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
        if (Utils.send(actionId, event)) {
            hideKeyboard();
            // 异步地让Controller处理事件
            sendText();
        }
        return false;
    }

    private void sendText() {
        EditText text = findViewById(R.id.et_content);
        AsyncTask.execute(() -> {
            this.presenter.sendMessage(text.getText().toString());
        });
    }

    public void onBtnSendClicked(View v) {
        hideKeyboard();
        sendText();
    }

    // 当用户长按消息，并选择撤回消息时做什么，MVP-0不实现
    @Override
    public void onRecallMessageRequested(UUID messageId) {

    }

    private boolean isMdImg(String text){
        if(text.matches("!\\[.*\\]\\(.*\\)")){
            return true;
        }
        else{
            return false;
        }
    }

    private Drawable getImg(String url){
        Drawable drawable = null;
        try {
            URL myurl = new URL(url);
            HttpURLConnection conn = (HttpURLConnection) myurl.openConnection();
            conn.setConnectTimeout(3000);
            conn.setDoInput(true);
            conn.setUseCaches(false);
            conn.connect();

            InputStream is = conn.getInputStream();
            drawable = Drawable.createFromStream(is, "");
            is.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return drawable;
    }
}
