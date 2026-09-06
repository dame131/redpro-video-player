package com.mr131.redplayer;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import com.google.android.gms.cast.CastMediaControlIntent;
import com.google.android.gms.cast.framework.CastOptions;
import com.google.android.gms.cast.framework.OptionsProvider;
import com.google.android.gms.cast.framework.SessionProvider;

import java.util.Collections;
import java.util.List;

public final class CastOptionsProvider implements OptionsProvider {
    @NonNull @Override public CastOptions getCastOptions(@NonNull Context context) {
        return new CastOptions.Builder()
                .setReceiverApplicationId(CastMediaControlIntent.DEFAULT_MEDIA_RECEIVER_APPLICATION_ID)
                .setStopReceiverApplicationWhenEndingSession(true)
                .build();
    }
    @Nullable @Override public List<SessionProvider> getAdditionalSessionProviders(@NonNull Context context) {
        return Collections.emptyList();
    }
}
