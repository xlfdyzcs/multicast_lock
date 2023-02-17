package com.mylisabox.multicastlock

import android.annotation.SuppressLint
import android.os.AsyncTask
import io.flutter.embedding.engine.plugins.FlutterPlugin
import io.flutter.plugin.common.BasicMessageChannel
import io.flutter.plugin.common.StandardMessageCodec
import java.io.IOException
import java.net.*
import java.nio.channels.DatagramChannel

class MulticastMessageKt(flutterPluginBinding: FlutterPlugin.FlutterPluginBinding) : BasicMessageChannel.MessageHandler<Any> {
    private var channel: BasicMessageChannel<Any> =
            BasicMessageChannel(flutterPluginBinding.binaryMessenger, "moying_mobilelib_multicast_message_channel",
                    StandardMessageCodec())

    private var datagramChannel: DatagramChannel
    // 定义接收网络数据的字节数组
    var inBuff = ByteArray(4096)

    // 以指定字节数组创建准备接受数据的DatagramPacket对象
    private val inPacket = DatagramPacket(inBuff, inBuff.size)

    init {
        channel.setMessageHandler(this)
        val ni = NetworkInterface.getByName("rndis0")
        val re = ni.supportsMulticast()
        this.channel.send("NetworkInterface-rndis0 $re")
        this.channel.send(ni.toString())
//        this.channel.send(ni.inetAddresses.toList().joinToString("\n") { inetAddress -> inetAddress.hostName })
//        this.channel.send(ni.subInterfaces.toList().joinToString("\n") { inetAddress -> inetAddress.name })
        val group = InetSocketAddress("192.168.178.119", 7873)
        val channel = DatagramChannel.open(StandardProtocolFamily.INET)
                .setOption(StandardSocketOptions.SO_REUSEADDR, true)
                .setOption(StandardSocketOptions.IP_MULTICAST_IF, ni)
                .bind(group)
        datagramChannel = channel;
//                .setOption(StandardSocketOptions.IP_MULTICAST_IF, ni)
        val key = channel.join(InetSocketAddress("224.77.82.73", 7873).address, ni)
        this.channel.send("joined group")
//    InetAddress group = InetAddress("224.77.82.73");
//    channel.configureBlocking(true);
//    channel.connect();
        // Android 4.0 之后不能在主线程中请求HTTP请求
        //    InetAddress group = InetAddress("224.77.82.73");
//    channel.configureBlocking(true);
//    channel.connect();
        // Android 4.0 之后不能在主线程中请求HTTP请求
        AsyncTaskUdp().executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR)
//        Thread {
//            try {
//                this.channel.send("start receive")
//                while (true) {
//                    channel.socket().receive(inPacket)
//                    this.channel.send(inBuff)
//                    println("聊天信息：" + String(inBuff, 0, inPacket.getLength()))
//                }
//            } catch (e: IOException) {
//                this.channel.send("error: ${e.message}")
//                e.printStackTrace()
//            }
//        }.start()
    }

    override fun onMessage(message: Any?, reply: BasicMessageChannel.Reply<Any>) {
        TODO("Not yet implemented")
    }

    @SuppressLint("StaticFieldLeak")
    inner class AsyncTaskUdp : AsyncTask<Int, String, String>(){
        override fun doInBackground(vararg p0: Int?): String? {
            while (true){
                Thread.sleep(10)
                try {
                    publishProgress("start receive")
                    while (true) {
                        datagramChannel.socket().receive(inPacket)
//                        channel.send(inBuff)
                        publishProgress(String(inBuff, 0, inPacket.getLength()))
                        println("信息：" + String(inBuff, 0, inPacket.getLength()))
                    }
                } catch (e: IOException) {
                    publishProgress("error: ${e.message}")
                    e.printStackTrace()
                }
            }
        }

        override fun onProgressUpdate(vararg values: String?) {
            super.onProgressUpdate(*values)
            channel.send(values.toString())
        }
    }

}
