import 'dart:async';
import 'dart:io';
import 'dart:typed_data';

import 'package:flutter/services.dart';

/// Class to capture screenshots with native code working on background
class NativeScreenshot {
  /// Comunication property to talk to the native background code.
  static const MethodChannel _channel =
      const MethodChannel('native_screenshot');

  /// Captures everything as is shown in user's device.
  ///
  /// Returns [null] if an error ocurrs.
  /// Returns a [String] with the path of the screenshot.
  static Future<Uint8List?> takeScreenshot() async {
    final dynamic res = await _channel.invokeMethod('takeScreenshot');

    if (res == null) return null;
    if (res is String) {
      return await File(res).readAsBytes();
    }
    if (res is Uint8List) {
      return res;
    }
  }
}
