import 'package:android_multicast_lock/android_multicast_lock.dart';
import 'package:flutter/material.dart';

void main() => runApp(MyApp());

class MyApp extends StatelessWidget {
  @override
  Widget build(BuildContext context) {
    return MaterialApp(
      theme: ThemeData.light(),
      home: HomePage(),
    );
  }
}

class HomePage extends StatefulWidget {
  @override
  _HomePageState createState() => _HomePageState();
}

class _HomePageState extends State<HomePage> {

  List<String?> tempPrintStr = [];

  @override
  void initState() {
    MulticastLock().acquire();
    super.initState();
  }

  @override
  void dispose() {
    MulticastLock().release();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    return MaterialApp(
      theme: ThemeData.light(),
      home: Scaffold(
        appBar: AppBar(
          title: const Text('Plugin example app'),
        ),
        body: Padding(
          padding: const EdgeInsets.all(8.0),
          child: Column(
            mainAxisSize: MainAxisSize.max,
            mainAxisAlignment: MainAxisAlignment.center,
            crossAxisAlignment: CrossAxisAlignment.stretch,
            children: <Widget>[
              ElevatedButton(
                onPressed: () async {
                  final isHeld = await MulticastLock().isHeld();
                  print(isHeld);
                },
                child: Text("Test multicastLock if held"),
              ),
              ElevatedButton(
                key: Key('openMulticast'),
                onPressed: () async {
                  await MulticastLock().listenMulticastOnTethering();
                  MulticastLock.messageChannel.setMessageHandler((message) async {
                    debugPrint(message.toString());
                    tempPrintStr.add(message);
                    setState(() {

                    });
                  });
                },
                child: Text("Test multicast on usb tethering"),
              ),
              Expanded(child: SingleChildScrollView(
                child: Column(
                  children: tempPrintStr.map((e) => Text(e ?? '')).toList(),
                ),
              ))
            ],
          ),
        ),
      ),
    );
  }
}
