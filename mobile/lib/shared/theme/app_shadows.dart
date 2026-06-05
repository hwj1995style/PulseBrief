import 'package:flutter/material.dart';

class AppShadows {
  const AppShadows._();

  static const List<BoxShadow> card = [
    BoxShadow(color: Color(0x12061331), offset: Offset(0, 10), blurRadius: 24),
  ];

  static const List<BoxShadow> nav = [
    BoxShadow(color: Color(0x1A061331), offset: Offset(0, -10), blurRadius: 30),
  ];
}
