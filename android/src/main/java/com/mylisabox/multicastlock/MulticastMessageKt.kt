package com.mylisabox.multicastlock

import io.flutter.embedding.engine.plugins.FlutterPlugin
import io.flutter.plugin.common.BasicMessageChannel
import io.flutter.plugin.common.MethodChannel.Result
import io.flutter.plugin.common.StandardMessageCodec
import kotlinx.coroutines.*
import java.io.IOException
import java.net.*
import java.nio.channels.DatagramChannel
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.Future

class MulticastMessageKt(flutterPluginBinding: FlutterPlugin.FlutterPluginBinding, networkInterfaceName: String?, inetSocketAddress: InetSocketAddress?) : BasicMessageChannel.MessageHandler<Any> {
    private var channel: BasicMessageChannel<Any> =
            BasicMessageChannel(flutterPluginBinding.binaryMessenger, "moying_mobilelib_multicast_message_channel",
                    StandardMessageCodec())

    private var datagramChannel: DatagramChannel? = null
    private var coroutineScope: CoroutineScope? = null
    // 定义接收网络数据的字节数组
    var inBuff = ByteArray(4096)

    // 以指定字节数组创建准备接受数据的DatagramPacket对象
    private val inPacket = DatagramPacket(inBuff, inBuff.size)

    init {
        channel.setMessageHandler(this)
        startListening(networkInterfaceName, inetSocketAddress)
    }

    override fun onMessage(message: Any?, reply: BasicMessageChannel.Reply<Any>) {
        TODO("Not yet implemented")
    }

    fun startListening(networkInterfaceName: String?, inetSocketAddress: InetSocketAddress?) {
        val interfaceName = networkInterfaceName?: "rndis0"
//        val interfaceName = "wlan0"
        val useTethering = interfaceName.startsWith("rndis")
        val ni = NetworkInterface.getByName(interfaceName)
//        val re = ni.supportsMulticast()
        val interfaceIp: String? = getIpAddressString(interfaceName)
        if (interfaceIp != null && interfaceIp.isNotEmpty()) {
            val group = inetSocketAddress?: InetSocketAddress("224.77.82.73", 7873)
            val groupTethering = InetSocketAddress(interfaceIp, 7873)
            val socketChannel = DatagramChannel.open(StandardProtocolFamily.INET)
                .setOption(StandardSocketOptions.SO_REUSEADDR, true)
                .setOption(StandardSocketOptions.IP_MULTICAST_LOOP, false)
                .bind((if (useTethering) groupTethering else group))
                .setOption(StandardSocketOptions.IP_MULTICAST_IF, ni)
            datagramChannel = socketChannel
            socketChannel.join(group.address, ni)
            coroutineScope = CoroutineScope(Dispatchers.IO)
            coroutineScope?.launch {
                try {
                    while (true) {
                        socketChannel.socket().receive(inPacket)
                        withContext(Dispatchers.Main) {
//                            val b_utf8 = "你好".toByteArray(charset("UTF-8"))
                            channel.send(String(inPacket.data, inPacket.offset, inPacket.length, charset("UTF-8")))
//                            channel.send(String(b_utf8, 0, b_utf8.size, charset("UTF-8")))
//                            val list = inPacket.data.asList().subList(0, inPacket.length)
//                            channel.send(b_utf8)
//                            println("聊天信息：" + String(inPacket.data, inPacket.offset, inPacket.length, charset("UTF-8")))
//                            println("聊天信息：" + String(b_utf8, 0, b_utf8.size, charset("UTF-8")))
                            inPacket.length = 4096
                        }
//                        println("聊天信息：" + String(inBuff, 0, inPacket.getLength()))
                    }
                } catch (e: IOException) {
                    withContext(Dispatchers.Main) {
                        channel.send("error: ${e.message}")
                    }
                    e.printStackTrace()
                }
            }
        } else {
            channel.send("error: 未获取到网口 $interfaceName 的ip")
        }
    }

    fun stopCoroutineScope() {
        coroutineScope?.cancel()
    }

    fun stopDatagramChannel() {
        datagramChannel?.close()
    }

    private fun getIpAddressString(interfaceName: String?): String? {
        try {
            val enNetI = NetworkInterface.getNetworkInterfaces()
            while (enNetI.hasMoreElements()) {
                val netI = enNetI.nextElement()
                if (interfaceName != null) {
                    if (!netI.name.equals(interfaceName, ignoreCase = true)) {
                        continue
                    }
                }
                val enumIpAddr = netI.inetAddresses
                while (enumIpAddr.hasMoreElements()) {
                    val inetAddress = enumIpAddr.nextElement()
                    if (inetAddress is Inet4Address && !inetAddress.isLoopbackAddress()) {
                        return inetAddress.getHostAddress()
                    }
                }
            }
        } catch (e: SocketException) {
            e.printStackTrace()
        }
        return ""
    }

    companion object {
        private var coroutineScope: CoroutineScope? = null
        private var threadPool: ExecutorService? = null
        fun getUsbTetheringSubnetwork(usbTetheringIp: String, result: Result) {
            val parts = usbTetheringIp.split(".")
            val newIp = parts.dropLast(1).plus("").joinToString(".")
            var subnetworkIp: String? = null
            coroutineScope = CoroutineScope(Dispatchers.IO)
            coroutineScope?.launch {
                try {
                    threadPool = Executors.newFixedThreadPool(10)
                    var future: Future<*>? = null
                    for (i in 0..255) {
                        future = threadPool?.submit {
                            try {
                                val address = "$newIp$i"
                    //           val inetAddressIp = inetAddressIsReachable(newIp, usbTetheringIp, i)
                                if (!usbTetheringIp.equals(address, ignoreCase = true)) {
                                    val inetAddress = InetAddress.getByName(address)
                                        println("ping address $address")
                                    if (inetAddress.isReachable(1000)) {
                                        subnetworkIp = inetAddress.hostAddress
                                        CoroutineScope(Dispatchers.Main).launch {
                                            result.success(subnetworkIp)
                                        }
                                        future?.cancel(true)
                                        threadPool?.shutdownNow()
                                        coroutineScope?.cancel()
                                    }
                                }
                            } catch (e: Exception) {
                                e.printStackTrace()
                                threadPool?.shutdownNow()
                    //                                result.error("", e.message, null)
                                this.cancel()
                            }
                        }
                    }

                } catch (e: IOException) {
                    withContext(Dispatchers.Main) {
                        result.error("", e.message, null)
                    }
                    this.cancel()
                    e.printStackTrace()
                }
            }
        }

        fun stopShip() {
            threadPool?.shutdownNow()
            coroutineScope?.cancel()
        }

        private fun inetAddressIsReachable(newIp: String?, usbTetheringIp: String?, i: Int): String? {
            var subnetworkIp: String? = null
            val address = "$newIp$i"
            try {
                if (!usbTetheringIp.equals(address, ignoreCase = true)) {
                    val inetAddress = InetAddress.getByName(address)
//                    println("ping address $address")
                    if (inetAddress.isReachable(1000)) {
//                        println("The first available address is $address")
                        subnetworkIp = inetAddress.hostAddress
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
                throw Exception(e.message)
//                result.error("", e.message, null)
            }
            return subnetworkIp
        }

    }

}
