import 'package:flutter/material.dart';
import 'package:pulsebrief/shared/theme/app_colors.dart';
import 'package:pulsebrief/shared/theme/app_text_styles.dart';

class VerificationCodeButton extends StatelessWidget {
  const VerificationCodeButton({
    super.key,
    required this.sent,
    required this.onPressed,
  });

  final bool sent;
  final VoidCallback onPressed;

  @override
  Widget build(BuildContext context) {
    return TextButton(
      onPressed: sent ? null : onPressed,
      style: TextButton.styleFrom(
        padding: EdgeInsets.zero,
        minimumSize: const Size(88, 40),
        tapTargetSize: MaterialTapTargetSize.shrinkWrap,
      ),
      child: Text(
        sent ? '已发送' : '获取验证码',
        style: AppTextStyles.label.copyWith(
          fontSize: 16,
          color: sent ? AppColors.textTertiary : AppColors.primary,
        ),
      ),
    );
  }
}
