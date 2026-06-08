import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import 'package:pulsebrief/app/app.dart';
import 'package:pulsebrief/shared/theme/app_colors.dart';

void main() {
  WidgetsFlutterBinding.ensureInitialized();
  SystemChrome.setSystemUIOverlayStyle(
    const SystemUiOverlayStyle(
      statusBarColor: AppColors.backgroundTop,
      statusBarIconBrightness: Brightness.dark,
      systemNavigationBarColor: AppColors.background,
      systemNavigationBarIconBrightness: Brightness.dark,
    ),
  );
  runApp(const PulseBriefApp());
}
