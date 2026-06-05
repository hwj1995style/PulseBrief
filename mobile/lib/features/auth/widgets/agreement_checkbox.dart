import 'package:flutter/gestures.dart';
import 'package:flutter/material.dart';
import 'package:pulsebrief/shared/theme/app_colors.dart';
import 'package:pulsebrief/shared/theme/app_radius.dart';
import 'package:pulsebrief/shared/theme/app_text_styles.dart';

class AgreementCheckbox extends StatelessWidget {
  const AgreementCheckbox({
    super.key,
    required this.value,
    required this.onChanged,
    this.onAgreementTap,
    this.onPrivacyTap,
  });

  final bool value;
  final ValueChanged<bool> onChanged;
  final VoidCallback? onAgreementTap;
  final VoidCallback? onPrivacyTap;

  @override
  Widget build(BuildContext context) {
    return Row(
      mainAxisAlignment: MainAxisAlignment.center,
      crossAxisAlignment: CrossAxisAlignment.center,
      children: [
        InkWell(
          key: const ValueKey('agreement-checkbox'),
          borderRadius: BorderRadius.circular(AppRadius.sm),
          onTap: () => onChanged(!value),
          child: AnimatedContainer(
            duration: const Duration(milliseconds: 160),
            width: 22,
            height: 22,
            decoration: BoxDecoration(
              color: value ? AppColors.primary : AppColors.surface,
              borderRadius: BorderRadius.circular(6),
              border: Border.all(
                color: value ? AppColors.primary : AppColors.line,
              ),
            ),
            child: value
                ? const Icon(Icons.check_rounded, color: Colors.white, size: 17)
                : null,
          ),
        ),
        const SizedBox(width: 10),
        Flexible(
          child: RichText(
            text: TextSpan(
              style: AppTextStyles.body.copyWith(fontSize: 14),
              children: [
                const TextSpan(text: '我已阅读并同意 '),
                TextSpan(
                  text: '《用户协议》',
                  style: AppTextStyles.label.copyWith(fontSize: 14),
                  recognizer: TapGestureRecognizer()..onTap = onAgreementTap,
                ),
                const TextSpan(text: ' 和 '),
                TextSpan(
                  text: '《隐私政策》',
                  style: AppTextStyles.label.copyWith(fontSize: 14),
                  recognizer: TapGestureRecognizer()..onTap = onPrivacyTap,
                ),
              ],
            ),
          ),
        ),
      ],
    );
  }
}
