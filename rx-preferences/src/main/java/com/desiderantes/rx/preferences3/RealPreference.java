package com.desiderantes.rx.preferences3;

import android.content.SharedPreferences;

import androidx.annotation.CheckResult;
import androidx.annotation.NonNull;

import java.util.Objects;

import io.reactivex.rxjava3.core.Observable;
import io.reactivex.rxjava3.functions.Consumer;

final class RealPreference<T> implements Preference<T> {
    private final SharedPreferences preferences;
    private final String key;
    private final T defaultValue;
    private final Adapter<T> adapter;
    private final Observable<T> values;

    RealPreference(SharedPreferences preferences, final String key, final T defaultValue,
                   Adapter<T> adapter, Observable<String> keyChanges) {
        this.preferences = preferences;
        this.key = key;
        this.defaultValue = defaultValue;
        this.adapter = adapter;
        this.values = keyChanges //
                .filter(key::equals) //
                .startWithItem("<init>") // Dummy value to trigger initial load.
                .map(s -> {
                    if (s.equals(RxSharedPreferences.NULL_KEY_EMISSION)) {
                        return defaultValue;
                    } else {
                        return get();
                    }
                });
    }

    @Override
    @NonNull
    public String key() {
        return key;
    }

    @Override
    @NonNull
    public T defaultValue() {
        return defaultValue;
    }

    @Override
    @NonNull
    public synchronized T get() {
        return adapter.get(key, preferences, defaultValue);
    }

    @Override
    public void set(@NonNull T value) {
        Objects.requireNonNull(value, "value == null");
        SharedPreferences.Editor editor = preferences.edit();
        adapter.set(key, value, editor);
        editor.apply();
    }

    @Override
    public boolean setSync(@NonNull T value) {
        Objects.requireNonNull(value, "value == null");
        SharedPreferences.Editor editor = preferences.edit();
        adapter.set(key, value, editor);
        return editor.commit();
    }

    @Override
    public boolean isSet() {
        return preferences.contains(key);
    }

    @Override
    public synchronized void delete() {
        preferences.edit().remove(key).apply();
    }

    @Override
    @CheckResult
    @NonNull
    public Observable<T> asObservable() {
        return values;
    }

    @Override
    @CheckResult
    @NonNull
    public Consumer<? super T> asConsumer() {
        return (Consumer<T>) this::set;
    }

    /**
     * Stores and retrieves instances of {@code T} in {@link SharedPreferences}.
     */
    interface Adapter<T> {
        /**
         * Retrieve the value for {@code key} from {@code preferences}, or {@code defaultValue}
         * if the preference is unset, or was set to {@code null}.
         */
        @NonNull
        T get(@NonNull String key, @NonNull SharedPreferences preferences,
              @NonNull T defaultValue);

        /**
         * Store non-null {@code value} for {@code key} in {@code editor}.
         * <p>
         * Note: Implementations <b>must not</b> call {@code commit()} or {@code apply()} on
         * {@code editor}.
         */
        void set(@NonNull String key, @NonNull T value, @NonNull SharedPreferences.Editor editor);
    }
}

