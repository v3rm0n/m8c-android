package io.maido.m8client.log;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.util.Pair;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import androidx.annotation.Nullable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import io.maido.m8client.R;

public abstract class LogCollectorActivity extends Activity implements CollectLogsTask.OnSendLogsDialogListener {
    private static final String TAG = "LogCollectorActivity";

    private LogcatHelper logcatHelper;
    private boolean isLogging = false;
    private int tapCounter = 0;

    private void initializeLogging() {
        logcatHelper = new LogcatHelper();
        Button loggingButton = findViewById(R.id.loggingButton);
        loggingButton.setOnClickListener(view -> {
            if (isLogging) {
                isLogging = false;
                new CollectLogsTask(this).execute();
                logcatHelper.stop();
            }
            loggingButton.setVisibility(View.INVISIBLE);
            tapCounter = 0;
        });
        View messageView = findViewById(R.id.connectDeviceMessage);
        messageView.setOnClickListener(view -> {
            if (tapCounter == 4 && !isLogging) {
                isLogging = true;
                logcatHelper.prepareNewLogFile();
                logcatHelper.start(null);
                loggingButton.setVisibility(View.VISIBLE);
                Toast toast = Toast.makeText(this, "Started collecting logs!", Toast.LENGTH_SHORT);
                toast.show();
            } else {
                tapCounter++;
            }
        });
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        initializeLogging();
    }

    @Override
    public void onShowSendLogsDialog(Pair<String[], String> stringPair) {
        String emailTo = "logs@maido.io";
        String emailSubj = "M8C logs";
        String chooserTitle = "Title";
        List<String> fileNames = new ArrayList<>(Arrays.asList(stringPair.first));
        sendEmail(fileNames, emailTo, emailSubj, chooserTitle, stringPair.second);
    }

    private void sendEmail(List<String> fileNames,
                           String emailTo, String emailSubj, String chooserTitle,
                           String msg) {
        ArrayList<Uri> attachments = new ArrayList<>();
        if (fileNames != null) {
            for (String fileName : fileNames) {
                Uri uri = Uri.parse(this.getString(R.string.uri_content_cache, fileName));
                attachments.add(uri);
            }
        }
        try {
            Intent intent = new Intent(Intent.ACTION_SEND_MULTIPLE);
            intent.addCategory(Intent.CATEGORY_DEFAULT);
            intent.setType("plain/text");
            intent.putExtra(Intent.EXTRA_EMAIL,
                    new String[]{emailTo});
            intent.putExtra(Intent.EXTRA_SUBJECT, emailSubj);
            intent.putExtra(Intent.EXTRA_TEXT, msg);
            if (attachments.size() != 0) {
                Log.i(TAG, "add attachment $attachments");
                intent.putParcelableArrayListExtra(Intent.EXTRA_STREAM, attachments);
            }
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            Intent chooserIntent;
            if (chooserTitle == null) {
                chooserIntent = intent;
            } else {
                chooserIntent = Intent.createChooser(intent, chooserTitle);
                chooserIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP
                        | Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED
                        | Intent.FLAG_GRANT_READ_URI_PERMISSION);
            }

            startActivity(chooserIntent);
        } catch (Exception e) {
            Log.e(TAG, e.getMessage());
        }
    }
}
