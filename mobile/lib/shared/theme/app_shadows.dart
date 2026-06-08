import 'package:flutter/material.dart';

class AppShadows {
  const AppShadows._();

  static const List<BoxShadow> card = [
    BoxShadow(color: Color(0x0F061331), offset: Offset(0, 10), blurRadius: 26),
  ];

  static const List<BoxShadow> softCard = [
    BoxShadow(color: Color(0x0A061331), offset: Offset(0, 8), blurRadius: 20),
  ];

  static const List<BoxShadow> primaryButton = [
    BoxShadow(color: Color(0x330B55D9), offset: Offset(0, 10), blurRadius: 22),
  ];

  static const List<BoxShadow> nav = [
    BoxShadow(color: Color(0x1A061331), offset: Offset(0, -10), blurRadius: 34),
  ];
}
