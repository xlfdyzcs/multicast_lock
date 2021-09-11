package com.mylisabox.multicastlock;

import android.content.Context;
import android.net.wifi.WifiManager;

import androidx.annotation.NonNull;

import io.flutter.embedding.engine.FlutterEngine;
import io.flutter.embedding.engine.plugins.FlutterPlugin;
import io.flutter.plugin.common.MethodCall;
import io.flutter.plugin.common.MethodChannel;
import io.flutter.plugin.common.MethodChannel.MethodCallHandler;
import io.flutter.plugin.common.MethodChannel.Result;
import io.flutter.plugin.common.PluginRegistry.Registrar;

/** MulticastLockPlugin */
public class MulticastLockPlugin implements FlutterPlugin, MethodCallHandler {
  private static final String CHANNEL = "multicast_lock";
  private WifiManager.MulticastLock multicastLock;
  private Context appContext;

  /** Plugin registration. */
  public static void registerWith(Registrar registrar) {
    final MethodChannel channel = new MethodChannel(registrar.messenger(), "multicast_lock");
    channel.setMethodCallHandler(new MulticastLockPlugin(registrar.context()));
  }

  private MulticastLockPlugin(Context context) {
    this.appContext = context;
  }

  public void onMethodCall(MethodCall call, @NonNull Result result) {
    switch (call.method) {
      case "acquire":
        if (acquire()) {
          result.success(null);
        } else {
          result.error("UNAVAILABLE", "WifiManager not present", null);
        }
        break;
      case "release":
        if (release()) {
          result.success(null);
        } else {
          result.error("UNAVAILABLE", "Lock is already released", null);
        }
        break;
      case "isHeld":
        result.success(isHeld());
        break;
      default:
        result.notImplemented();
        break;
    }
  }

  private boolean acquire() throws NullPointerException
  {
    WifiManager wifi = (WifiManager) appContext.getApplicationContext().getSystemService(Context.WIFI_SERVICE);
    if(wifi == null) {
      return false;
    }

    multicastLock = wifi.createMulticastLock("discovery");
    multicastLock.acquire();

    return true;
  }

  private boolean release() {
    try {
      multicastLock.release();
    } catch(RuntimeException e) {
      return false;
    }

    return true;
  }

  private boolean isHeld() {
    return multicastLock.isHeld();
  }



  /**
   * This {@code FlutterPlugin} has been associated with a {@link
   * FlutterEngine} instance.
   *
   * <p>Relevant resources that this {@code FlutterPlugin} may need are provided via the {@code
   * binding}. The {@code binding} may be cached and referenced until {@link
   * #onDetachedFromEngine(FlutterPluginBinding)} is invoked and returns.
   *
   * @param binding
   */
  @Override
  public void onAttachedToEngine(@NonNull FlutterPluginBinding binding) {
    appContext = binding.getApplicationContext();
  }

  /**
   * This {@code FlutterPlugin} has been removed from a {@link
   * FlutterEngine} instance.
   *
   * <p>The {@code binding} passed to this method is the same instance that was passed in {@link
   * #onAttachedToEngine(FlutterPluginBinding)}. It is provided again in this method as a
   * convenience. The {@code binding} may be referenced during the execution of this method, but it
   * must not be cached or referenced after this method returns.
   *
   * <p>{@code FlutterPlugin}s should release all resources in this method.
   *
   * @param binding
   */
  @Override
  public void onDetachedFromEngine(@NonNull FlutterPluginBinding binding) {

  }
}
