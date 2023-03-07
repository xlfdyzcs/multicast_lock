import 'dart:async';

import 'package:flutter/foundation.dart';
import 'package:flutter/services.dart';

/// Allows an application to receive Wifi Multicast packets. Normally the Wifi
/// stack filters out packets not explicitly addressed to this device. Acquring
/// a MulticastLock will cause the stack to receive packets addressed to
/// multicast addresses. Processing these extra packets can cause a noticeable
/// battery drain and should be disabled when not needed.
class MulticastLock {

  static const MethodChannel _channel = MethodChannel('multicast_lock');
  static const BasicMessageChannel messageChannel = BasicMessageChannel('moying_mobilelib_multicast_message_channel', StandardMessageCodec());

  static MulticastLock? _instance;

  factory MulticastLock() {
    _instance ??= MulticastLock.private();
    return _instance!;
  }

  MulticastLock.private();

  /// Locks Wifi Multicast on until release() is called.
  Future<void> acquire() async {
    if (defaultTargetPlatform == TargetPlatform.android) {
      await _channel.invokeMethod('acquire');
    }
  }

  /// Unlocks Wifi Multicast, restoring the filter of packets not addressed specifically to this device and saving power.
  Future<void> release() async {
    if (defaultTargetPlatform == TargetPlatform.android) {
      await _channel.invokeMethod('release');
    }
  }

  /// Checks whether this MulticastLock is currently held.
  Future<bool?> isHeld() async {
    bool? result;
    if (defaultTargetPlatform == TargetPlatform.android) {
      result = await _channel.invokeMethod('isHeld');
    }
    return result ?? false;
  }

  /// 指定网口监听组播数据-默认为 tethering 网口 "rndis0"
  ///
  /// 通过指定[networkInterfaceName]来指定网口，比如在使用usb网络共享时，网口名一般都会是"rndis0"
  /// 当[networkInterfaceName]为空时，默认使用"rndis0"网口接收数据
  /// TODO: 通过添加参数[allNetworkInterface]控制是否所有网卡都加入组播
  /// [hostname]是组播地址，224.0.1.0 到 238.255.255.255，详细介绍可看 https://winddoing.github.io/post/18736.html
  /// [port]是接收组播数据的端口，通过指定端口来过滤其他端口的数据。
  /// 一个组播地址上允许有多个端口，IP层会收到组内所有端口的数据，经由UDP层对端口进行处理，非UDP层指定的端口报会被丢弃
  Future<void> listenMulticastOnTethering({String? networkInterfaceName, String? hostname, int? port}) async {
    if (defaultTargetPlatform == TargetPlatform.android) {
      await _channel.invokeMethod('listenMulticastOnTethering', {
        'networkInterfaceName': networkInterfaceName,
        'hostname': hostname,
        'port': port,
      });
    }
  }

  Future closeMulticastListening() async {
    if (defaultTargetPlatform == TargetPlatform.android) {
      await _channel.invokeMethod('closeMulticastListening');
    }
  }

  /// 嗅探 UsbTethering 模式下首个子网络的IP
  ///
  /// 利用 InetAddress.isReachable 查看主机是否可 ping 通。
  /// 注意！！！只有在子网络主机防火墙允许ICMPv4回显请求时此方法才会有效，
  /// 否则即使 ping 到正确的子网络 ip 也只会返回null
  Future<String?> sniffUsbTetheringSubnetworkIp(String usbTetheringIp) async {
    if (defaultTargetPlatform == TargetPlatform.android) {
      return await _channel.invokeMethod('sniffUsbTetheringSubnetworkIp', {
        'usbTetheringIp': usbTetheringIp
      });
    }
    return null;
  }

  Future closeSniff() async {
    if (defaultTargetPlatform == TargetPlatform.android) {
      await _channel.invokeMethod('closeSniff');
    }
  }
}

