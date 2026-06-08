import 'package:flutter/cupertino.dart';
import 'package:flutter/material.dart';
import 'package:pulsebrief/shared/theme/app_colors.dart';
import 'package:pulsebrief/shared/theme/app_radius.dart';
import 'package:pulsebrief/shared/theme/app_shadows.dart';
import 'package:pulsebrief/shared/theme/app_spacing.dart';

class PulseBottomNav extends StatelessWidget {
  const PulseBottomNav({
    super.key,
    required this.currentIndex,
    required this.onTap,
  });

  final int currentIndex;
  final ValueChanged<int> onTap;

  @override
  Widget build(BuildContext context) {
    final bottom = MediaQuery.paddingOf(context).bottom;

    return Container(
      height: AppSpacing.bottomNavHeight + bottom,
      padding: EdgeInsets.fromLTRB(10, 10, 10, bottom + 6),
      decoration: const BoxDecoration(
        color: AppColors.surface,
        borderRadius: BorderRadius.vertical(top: Radius.circular(30)),
        boxShadow: AppShadows.nav,
      ),
      child: Row(
        mainAxisAlignment: MainAxisAlignment.spaceAround,
        children: [
          _NavItem(
            label: '首页',
            icon: CupertinoIcons.house,
            selectedIcon: CupertinoIcons.house_fill,
            selected: currentIndex == 0,
            onTap: () => onTap(0),
          ),
          _NavItem(
            label: '分类',
            icon: CupertinoIcons.square_grid_2x2,
            selectedIcon: CupertinoIcons.square_grid_2x2_fill,
            selected: currentIndex == 1,
            onTap: () => onTap(1),
          ),
          _BriefButton(selected: currentIndex == 2, onTap: () => onTap(2)),
          _NavItem(
            label: '我的',
            icon: CupertinoIcons.person,
            selectedIcon: CupertinoIcons.person_fill,
            selected: currentIndex == 3,
            onTap: () => onTap(3),
          ),
        ],
      ),
    );
  }
}

class _NavItem extends StatelessWidget {
  const _NavItem({
    required this.label,
    required this.icon,
    required this.selectedIcon,
    required this.selected,
    required this.onTap,
  });

  final String label;
  final IconData icon;
  final IconData selectedIcon;
  final bool selected;
  final VoidCallback onTap;

  @override
  Widget build(BuildContext context) {
    final color = selected ? AppColors.primary : AppColors.textSecondary;

    return Expanded(
      child: InkWell(
        borderRadius: BorderRadius.circular(AppRadius.lg),
        onTap: onTap,
        child: Column(
          mainAxisAlignment: MainAxisAlignment.center,
          children: [
            Icon(selected ? selectedIcon : icon, color: color, size: 28),
            const SizedBox(height: 5),
            Text(
              label,
              style: TextStyle(
                fontSize: 12,
                fontWeight: selected ? FontWeight.w800 : FontWeight.w500,
                color: color,
              ),
            ),
          ],
        ),
      ),
    );
  }
}

class _BriefButton extends StatelessWidget {
  const _BriefButton({required this.selected, required this.onTap});

  final bool selected;
  final VoidCallback onTap;

  @override
  Widget build(BuildContext context) {
    return Expanded(
      child: InkWell(
        borderRadius: BorderRadius.circular(28),
        onTap: onTap,
        child: Column(
          mainAxisAlignment: MainAxisAlignment.center,
          children: [
            Transform.translate(
              offset: const Offset(0, -8),
              child: Container(
                width: 62,
                height: 62,
                decoration: BoxDecoration(
                  color: AppColors.primary,
                  shape: BoxShape.circle,
                  boxShadow: [
                    BoxShadow(
                      color: AppColors.primary.withValues(alpha: 0.34),
                      blurRadius: 20,
                      offset: const Offset(0, 10),
                    ),
                  ],
                ),
                child: const Icon(
                  Icons.graphic_eq_rounded,
                  color: Colors.white,
                  size: 30,
                ),
              ),
            ),
            const SizedBox(height: 0),
            Text(
              '简报',
              style: TextStyle(
                fontSize: 12,
                fontWeight: selected ? FontWeight.w800 : FontWeight.w500,
                color: selected ? AppColors.primary : AppColors.textSecondary,
              ),
            ),
          ],
        ),
      ),
    );
  }
}
