import 'package:flutter/cupertino.dart';
import 'package:flutter/material.dart';
import 'package:pulsebrief/app/routes.dart';
import 'package:pulsebrief/features/auth/widgets/agreement_checkbox.dart';
import 'package:pulsebrief/features/auth/widgets/brand_header.dart';
import 'package:pulsebrief/features/auth/widgets/guest_entry_button.dart';
import 'package:pulsebrief/features/auth/widgets/login_input_field.dart';
import 'package:pulsebrief/features/auth/widgets/verification_code_button.dart';
import 'package:pulsebrief/shared/theme/app_colors.dart';
import 'package:pulsebrief/shared/theme/app_radius.dart';
import 'package:pulsebrief/shared/theme/app_shadows.dart';
import 'package:pulsebrief/shared/theme/app_spacing.dart';
import 'package:pulsebrief/shared/theme/app_text_styles.dart';

class LoginPage extends StatefulWidget {
  const LoginPage({super.key});

  @override
  State<LoginPage> createState() => _LoginPageState();
}

class _LoginPageState extends State<LoginPage> {
  final _accountController = TextEditingController();
  final _codeController = TextEditingController();
  bool _agreementAccepted = true;
  bool _codeSent = false;

  @override
  void dispose() {
    _accountController.dispose();
    _codeController.dispose();
    super.dispose();
  }

  void _sendCode() {
    setState(() => _codeSent = true);
    ScaffoldMessenger.of(
      context,
    ).showSnackBar(const SnackBar(content: Text('验证码已发送')));
  }

  void _login() {
    if (!_agreementAccepted) {
      ScaffoldMessenger.of(
        context,
      ).showSnackBar(const SnackBar(content: Text('请先同意用户协议和隐私政策')));
      return;
    }

    Navigator.pushReplacementNamed(context, PulseRoutes.main);
  }

  void _enterAsGuest() {
    Navigator.pushReplacementNamed(context, PulseRoutes.main);
  }

  void _showLinkTip(String title) {
    ScaffoldMessenger.of(
      context,
    ).showSnackBar(SnackBar(content: Text('$title 将在后续版本接入')));
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      resizeToAvoidBottomInset: true,
      body: SafeArea(
        bottom: false,
        child: LayoutBuilder(
          builder: (context, constraints) {
            return SingleChildScrollView(
              padding: EdgeInsets.only(
                bottom: MediaQuery.paddingOf(context).bottom + AppSpacing.xxl,
              ),
              child: ConstrainedBox(
                constraints: BoxConstraints(minHeight: constraints.maxHeight),
                child: Column(
                  children: [
                    const BrandHeader(),
                    Padding(
                      padding: const EdgeInsets.symmetric(
                        horizontal: AppSpacing.pagePadding,
                      ),
                      child: Column(
                        children: [
                          _LoginPanel(
                            accountController: _accountController,
                            codeController: _codeController,
                            codeSent: _codeSent,
                            onSendCode: _sendCode,
                            onLogin: _login,
                          ),
                          const SizedBox(height: AppSpacing.lg),
                          GuestEntryButton(
                            label: '游客模式进入',
                            icon: CupertinoIcons.person,
                            iconColor: AppColors.primary,
                            onTap: _enterAsGuest,
                          ),
                          const SizedBox(height: AppSpacing.md),
                          GuestEntryButton(
                            label: '稍后登录',
                            icon: CupertinoIcons.clock,
                            iconColor: AppColors.cyan,
                            onTap: _enterAsGuest,
                          ),
                          const SizedBox(height: AppSpacing.lg),
                          AgreementCheckbox(
                            value: _agreementAccepted,
                            onChanged: (value) {
                              setState(() => _agreementAccepted = value);
                            },
                            onAgreementTap: () => _showLinkTip('用户协议'),
                            onPrivacyTap: () => _showLinkTip('隐私政策'),
                          ),
                        ],
                      ),
                    ),
                  ],
                ),
              ),
            );
          },
        ),
      ),
    );
  }
}

class _LoginPanel extends StatelessWidget {
  const _LoginPanel({
    required this.accountController,
    required this.codeController,
    required this.codeSent,
    required this.onSendCode,
    required this.onLogin,
  });

  final TextEditingController accountController;
  final TextEditingController codeController;
  final bool codeSent;
  final VoidCallback onSendCode;
  final VoidCallback onLogin;

  @override
  Widget build(BuildContext context) {
    return Container(
      padding: const EdgeInsets.fromLTRB(22, 22, 22, 20),
      decoration: BoxDecoration(
        color: AppColors.surface.withValues(alpha: 0.96),
        borderRadius: BorderRadius.circular(30),
        boxShadow: AppShadows.nav,
      ),
      child: Column(
        children: [
          LoginInputField(
            controller: accountController,
            hintText: '手机号 / 邮箱',
            icon: LoginIcons.account,
            keyboardType: TextInputType.emailAddress,
          ),
          const SizedBox(height: 18),
          LoginInputField(
            controller: codeController,
            hintText: '验证码',
            icon: LoginIcons.code,
            keyboardType: TextInputType.number,
            trailing: VerificationCodeButton(
              sent: codeSent,
              onPressed: onSendCode,
            ),
          ),
          const SizedBox(height: 18),
          SizedBox(
            width: double.infinity,
            height: 64,
            child: DecoratedBox(
              decoration: BoxDecoration(
                gradient: const LinearGradient(
                  colors: [AppColors.primary, Color(0xFF0846CE)],
                  begin: Alignment.centerLeft,
                  end: Alignment.centerRight,
                ),
                borderRadius: BorderRadius.circular(AppRadius.xl),
                boxShadow: [
                  BoxShadow(
                    color: AppColors.primary.withValues(alpha: 0.26),
                    blurRadius: 24,
                    offset: const Offset(0, 12),
                  ),
                ],
              ),
              child: FilledButton(
                key: const ValueKey('login-submit-button'),
                onPressed: onLogin,
                style: FilledButton.styleFrom(
                  backgroundColor: Colors.transparent,
                  shadowColor: Colors.transparent,
                  shape: RoundedRectangleBorder(
                    borderRadius: BorderRadius.circular(AppRadius.xl),
                  ),
                ),
                child: Text(
                  '登录 / 注册',
                  style: AppTextStyles.title.copyWith(
                    color: Colors.white,
                    fontSize: 22,
                  ),
                ),
              ),
            ),
          ),
          const SizedBox(height: AppSpacing.lg),
          Text(
            '登录后可同步订阅、收藏、播放历史和推送设置',
            textAlign: TextAlign.center,
            style: AppTextStyles.body,
          ),
        ],
      ),
    );
  }
}
