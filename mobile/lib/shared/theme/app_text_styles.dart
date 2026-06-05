import 'package:flutter/material.dart';
import 'package:pulsebrief/shared/theme/app_colors.dart';

class AppTextStyles {
  const AppTextStyles._();

  static const TextStyle hero = TextStyle(
    fontSize: 34,
    height: 1.12,
    fontWeight: FontWeight.w800,
    color: AppColors.textPrimary,
    letterSpacing: 0,
  );

  static const TextStyle pageTitle = TextStyle(
    fontSize: 34,
    height: 1.1,
    fontWeight: FontWeight.w800,
    color: AppColors.primaryDark,
    letterSpacing: 0,
  );

  static const TextStyle title = TextStyle(
    fontSize: 22,
    height: 1.18,
    fontWeight: FontWeight.w800,
    color: AppColors.textPrimary,
    letterSpacing: 0,
  );

  static const TextStyle sectionTitle = TextStyle(
    fontSize: 20,
    height: 1.2,
    fontWeight: FontWeight.w800,
    color: AppColors.textPrimary,
    letterSpacing: 0,
  );

  static const TextStyle cardTitle = TextStyle(
    fontSize: 17,
    height: 1.28,
    fontWeight: FontWeight.w800,
    color: AppColors.textPrimary,
    letterSpacing: 0,
  );

  static const TextStyle body = TextStyle(
    fontSize: 15,
    height: 1.45,
    fontWeight: FontWeight.w400,
    color: AppColors.textSecondary,
    letterSpacing: 0,
  );

  static const TextStyle label = TextStyle(
    fontSize: 13,
    height: 1.2,
    fontWeight: FontWeight.w700,
    color: AppColors.primary,
    letterSpacing: 0,
  );

  static const TextStyle meta = TextStyle(
    fontSize: 13,
    height: 1.2,
    fontWeight: FontWeight.w400,
    color: AppColors.textTertiary,
    letterSpacing: 0,
  );
}
