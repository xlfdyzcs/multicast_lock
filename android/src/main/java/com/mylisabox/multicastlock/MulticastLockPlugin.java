package com.mylisabox.multicastlock;

import com.mylisabox.multicastlock.MulticastMessageKt;
import android.content.Context;
import android.net.ConnectivityManager;
import android.net.wifi.WifiManager;

import androidx.annotation.NonNull;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.net.StandardProtocolFamily;
import java.net.StandardSocketOptions;
import java.nio.ByteBuffer;
import java.nio.channels.Channels;
import java.nio.channels.DatagramChannel;
import java.nio.channels.MembershipKey;
import java.nio.channels.MulticastChannel;
import java.nio.channels.WritableByteChannel;

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
  private FlutterPluginBinding binding;
  private MulticastMessageKt multicastMessageKt;

  public MulticastLockPlugin() {}

  public void onMethodCall(MethodCall call, @NonNull Result result) {
    switch (call.method) {
      case "acquire":
        try {
//          new MulticastMessageKt(binding);
          if (acquire()) {
            result.success(null);
          } else {
            result.error("UNAVAILABLE", "WifiManager not present", null);
          }
        } catch (IOException e) {
          e.printStackTrace();
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
      case "listenMulticastOnTethering":
        String hostname = call.argument("hostname");
        Integer port = call.argument("port");
        InetSocketAddress socketAddress = null;
        if (hostname != null && port != null) {
          socketAddress = new InetSocketAddress(hostname, port);
        }
        if (multicastMessageKt == null) {
          multicastMessageKt = new MulticastMessageKt(binding, call.<String>argument("networkInterfaceName"), socketAddress);
        } else {
          multicastMessageKt.startListening(call.<String>argument("networkInterfaceName"), socketAddress);
        }
        result.success(null);
        break;
      case "closeMulticastListening":
        if (multicastMessageKt != null) {
          multicastMessageKt.stopDatagramChannel();
          multicastMessageKt.stopCoroutineScope();
          result.success(true);
        }
        break;
      case "sniffUsbTetheringSubnetworkIp":
        String usbTetheringIp = call.argument("usbTetheringIp");
        if (usbTetheringIp != null) {
          MulticastMessageKt.Companion.getUsbTetheringSubnetwork(usbTetheringIp, result);
        } else {
          result.error("", "usbTetheringIp不能为null", null);
        }
        break;
      case "closeSniff":
        MulticastMessageKt.Companion.stopShip();
        break;
      default:
        result.notImplemented();
        break;
    }
  }

  private boolean acquire() throws NullPointerException, IOException {
    WifiManager wifi = (WifiManager) appContext.getApplicationContext().getSystemService(Context.WIFI_SERVICE);

//    MembershipKey key = dc.join(group, ni);
//    dc.socket().receive();
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
    this.binding = binding;
    appContext = binding.getApplicationContext();
    final MethodChannel channel = new MethodChannel(binding.getBinaryMessenger(), "multicast_lock");
    channel.setMethodCallHandler(this);
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
