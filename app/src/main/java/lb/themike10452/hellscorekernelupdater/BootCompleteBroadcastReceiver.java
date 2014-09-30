package lb.themike10452.hellscorekernelupdater;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import lb.themike10452.hellscorekernelupdater.Services.BackgroundAutoCheckService;

/**
 * Created by Mike on 9/26/2014.
 */
public class BootCompleteBroadcastReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent.getAction() == Intent.ACTION_BOOT_COMPLETED) {
            context.startService(new Intent(context, BackgroundAutoCheckService.class));
        }
    }
}
