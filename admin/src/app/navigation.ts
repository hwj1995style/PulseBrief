import {
  BarChart3,
  FileCheck2,
  Files,
  FolderTree,
  Newspaper,
  RadioTower,
  Users
} from 'lucide-react';

export const navigationItems = [
  { key: 'dashboard', label: '仪表盘', icon: BarChart3 },
  { key: 'ingestion', label: '采集任务', icon: RadioTower },
  { key: 'candidates', label: '候选资讯', icon: FileCheck2 },
  { key: 'articles', label: '文章管理', icon: Newspaper },
  { key: 'categories', label: '分类管理', icon: FolderTree },
  { key: 'digests', label: '简报管理', icon: Files },
  { key: 'users', label: '管理员账号', icon: Users, adminOnly: true }
];
