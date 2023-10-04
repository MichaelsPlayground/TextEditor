package de.androidcrypto.texteditor;

import static android.content.ClipDescription.MIMETYPE_TEXT_PLAIN;

import androidx.activity.result.ActivityResult;
import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import android.app.Activity;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.provider.DocumentsContract;
import android.text.TextUtils;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ScrollView;
import android.widget.Toast;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.util.List;
import java.util.Objects;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = MainActivity.class.getSimpleName();
    private Button openFile, saveFile, copyToClipboard, pasteFromClipboard, scrollToTop;
    private com.google.android.material.textfield.TextInputEditText textData;
    private ScrollView scrollView;
    private Context contextSave; // used for read a file from uri
    private String DEFAULT_FILENAME = "file01.txt";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar_main);
        setSupportActionBar(toolbar);

        openFile = findViewById(R.id.btnOpenFile);
        saveFile = findViewById(R.id.btnSaveFile);
        copyToClipboard = findViewById(R.id.btnCopyToClipboard);
        pasteFromClipboard = findViewById(R.id.btnPasteFromClipboard);
        textData = findViewById(R.id.etTextData);
        scrollToTop = findViewById(R.id.btnScrollToTop);
        scrollView = findViewById(R.id.svScrollView);

        // hide soft keyboard from showing up on startup
        getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN);

        contextSave = getApplicationContext();

        saveFile.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Log.d(TAG, "saveFile");
                onSaveFile();
            }
        });

        openFile.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Log.d(TAG, "openFile");
                onOpenFile();
            }
        });

        copyToClipboard.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Log.d(TAG, "copyToClipboard");
                onCopyToClipboard();
            }
        });

        pasteFromClipboard.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Log.d(TAG, "pasteFromClipboard");
                onPasteFromClipboard();
            }
        });

        scrollToTop.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Log.d(TAG, "scrollToTop");
                onScrollToTop();
            }
        });

    }

    private void onOpenFile() {
        // https://developer.android.com/training/data-storage/shared/documents-files
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("*/*");
        // Optionally, specify a URI for the file that should appear in the
        // system file picker when it loads.
        boolean pickerInitialUri = false;
        intent.putExtra(DocumentsContract.EXTRA_INITIAL_URI, pickerInitialUri);
        fileChooserActivityResultLauncher.launch(intent);
    }

    private void onSaveFile() {
        if (TextUtils.isEmpty(textData.getText().toString())) {
            showToast("please enter any text before saving");
            return;
        }
        Intent intent = new Intent(Intent.ACTION_CREATE_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("*/*");
        // Optionally, specify a URI for the file that should appear in the
        // system file picker when it loads.
        //boolean pickerInitialUri = false;
        //intent.putExtra(DocumentsContract.EXTRA_INITIAL_URI, pickerInitialUri);
        intent.putExtra(Intent.EXTRA_TITLE, DEFAULT_FILENAME);
        fileSaverActivityResultLauncher.launch(intent);
    }

    private void onCopyToClipboard() {
        String data = textData.getText().toString();
        if (TextUtils.isEmpty(data)) {
            showToast("no available data to copy");
        } else {
            copyTextToClipboard(data);
            showToast("data copied to clipboard");
        }
    }

    private void onPasteFromClipboard() {
        String data = getTextFromClipboard();
        if (TextUtils.isEmpty(data)) {
            showToast("no available data to paste");
        } else {
            // append data
            String availableData = textData.getText().toString();
            textData.setText(availableData + data);
        }
    }

    private void onScrollToTop() {
        scrollView.fullScroll(ScrollView.FOCUS_UP);
        scrollView.smoothScrollTo(0, 0);
    }

    /**
     * section for reading data from a text file
     */


    ActivityResultLauncher<Intent> fileChooserActivityResultLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            new ActivityResultCallback<ActivityResult>() {
                @Override
                public void onActivityResult(ActivityResult result) {
                    if (result.getResultCode() == Activity.RESULT_OK) {
                        // There are no request codes
                        Intent resultData = result.getData();
                        // The result data contains a URI for the document or directory that
                        // the user selected.
                        Uri uri = null;
                        if (resultData != null) {
                            uri = resultData.getData();
                            // Perform operations on the document using its URI.
                            try {
                                String fileContent = readTextFromUri(uri);
                                Log.d(TAG, "import data:\n" + fileContent);
                                textData.setText(fileContent);
                                showToast("file content read");
                            } catch (IOException e) {
                                e.printStackTrace();
                                showToast("error on reading the file");
                            }
                        }
                    }
                }
            });

    private String readTextFromUri(Uri uri) throws IOException {
        StringBuilder stringBuilder = new StringBuilder();
        //try (InputStream inputStream = getContentResolver().openInputStream(uri);
        try (InputStream inputStream = contextSave.getContentResolver().openInputStream(uri);
             BufferedReader reader = new BufferedReader(
                     new InputStreamReader(Objects.requireNonNull(inputStream)))) {
            String line;
            while ((line = reader.readLine()) != null) {
                stringBuilder.append(line + "\n");
            }
        }
        return stringBuilder.toString();
    }

    /**
     * section for writing data to a text file
     */

    ActivityResultLauncher<Intent> fileSaverActivityResultLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            new ActivityResultCallback<ActivityResult>() {
                @Override
                public void onActivityResult(ActivityResult result) {
                    if (result.getResultCode() == Activity.RESULT_OK) {
                        // There are no request codes
                        Intent resultData = result.getData();
                        // The result data contains a URI for the document or directory that
                        // the user selected.
                        Uri uri = null;
                        if (resultData != null) {
                            uri = resultData.getData();
                            // Perform operations on the document using its URI.
                            try {
                                writeTextToUri(uri, textData.getText().toString());
                                showToast("data is written to file");
                            } catch (IOException e) {
                                Log.e(TAG, "IOException: " + e.getMessage());
                                showToast("error on writing data to file");
                            }
                        }
                    }
                }
            });

    private void writeTextToUri(Uri uri, String data) throws IOException {
        try {
            OutputStreamWriter outputStreamWriter = new OutputStreamWriter(contextSave.getContentResolver().openOutputStream(uri));
            outputStreamWriter.write(data);
            outputStreamWriter.close();
        } catch (IOException e) {
            Log.e(TAG, "IOException: " + e.getMessage());
            return;
        }
    }

    /**
     * clipboard
     */

    private void copyTextToClipboard(String data) {
        // Gets a handle to the clipboard service.
        ClipboardManager clipboard = (ClipboardManager)
                getSystemService(Context.CLIPBOARD_SERVICE);
        ClipData clip = ClipData.newPlainText("simple text", data);
        // Set the clipboard's primary clip.
        clipboard.setPrimaryClip(clip);
    }

    private String getTextFromClipboard() {
        // Gets a handle to the clipboard service.
        ClipboardManager clipboard = (ClipboardManager)
                getSystemService(Context.CLIPBOARD_SERVICE);
        // is there any data in the clipboard ?
        if (clipboard.hasPrimaryClip()) {
            // is the data plaintext ?
            if (clipboard.getPrimaryClipDescription().hasMimeType(MIMETYPE_TEXT_PLAIN)) {
                // Examines the item on the clipboard. If getText() does not return null, the clip item contains the
                // text. Assumes that this application can only handle one item at a time.
                ClipData.Item item = clipboard.getPrimaryClip().getItemAt(0);
                // Gets the clipboard as text.
                return String.valueOf(item.getText());
            }
        }
        return "";
    }

    /**
     * UI
     */

    private void showToast(String message) {
        Toast.makeText(MainActivity.this, message, Toast.LENGTH_SHORT).show();
    }

    /**
     * section for options menu
     */

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_activity_main, menu);

        MenuItem mLoadFile = menu.findItem(R.id.action_load_from_file);
        mLoadFile.setOnMenuItemClickListener(new MenuItem.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem item) {
                onOpenFile();
                return false;
            }
        });

        MenuItem mSaveFile = menu.findItem(R.id.action_save_to_file);
        mSaveFile.setOnMenuItemClickListener(new MenuItem.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem item) {
                onSaveFile();
                return false;
            }
        });

        MenuItem mCopyToClipboard = menu.findItem(R.id.action_copy_to_clipboard);
        mCopyToClipboard.setOnMenuItemClickListener(new MenuItem.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem item) {
                onCopyToClipboard();
                return false;
            }
        });

        MenuItem mPasteFromClipboard = menu.findItem(R.id.action_paste_from_clipboard);
        mPasteFromClipboard.setOnMenuItemClickListener(new MenuItem.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem item) {
                onPasteFromClipboard();
                return false;
            }
        });

        return super.onCreateOptionsMenu(menu);
    }

}