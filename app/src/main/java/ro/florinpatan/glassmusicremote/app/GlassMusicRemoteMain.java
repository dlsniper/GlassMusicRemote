/*
 * Copyright 2014 Florin Patan
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 */

package ro.florinpatan.glassmusicremote.app;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.widget.Toast;


public class GlassMusicRemoteMain extends Activity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        String toastMessage = getResources().getString(R.string.app_already_running, getString(R.string.app_name));
        if (!GlassMusicRemoteService.isServiceRunning) {
            GlassMusicRemoteService.isServiceRunning = true;
            startService(new Intent(this, GlassMusicRemoteService.class));
            toastMessage = getResources().getString(R.string.app_started, getString(R.string.app_name));
        }

        Toast.makeText(this, toastMessage, Toast.LENGTH_SHORT).show();
        finish();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }
}
