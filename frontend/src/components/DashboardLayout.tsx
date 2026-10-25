'use client';

import React, { useState } from 'react';
import Link from 'next/link';
import { usePathname, useRouter } from 'next/navigation';
import {
  Shield,
  LayoutDashboard,
  Users,
  Key,
  ScrollText,
  Lock,
  UserCheck,
  Building2,
  LogOut,
  ChevronDown,
  Menu,
  X,
  ShieldCheck,
  ExternalLink,
  Laptop
} from 'lucide-react';
import { useAuth } from '@/context/AuthContext';

interface DashboardLayoutProps {
  children: React.ReactNode;
  title?: string;
  subtitle?: string;
  actions?: React.ReactNode;
}

export default function DashboardLayout({
  children,
  title,
  subtitle,
  actions,
}: DashboardLayoutProps) {
  const pathname = usePathname();
  const router = useRouter();
  const { user, activeOrganization, organizations, logout, isLoading } = useAuth();
  const [mobileMenuOpen, setMobileMenuOpen] = useState(false);
  const [userDropdownOpen, setUserDropdownOpen] = useState(false);

  const isPlatformAdmin = activeOrganization?.role === 'PLATFORM_ADMIN';

  const navItems = [
    { label: 'Overview', href: '/dashboard', icon: LayoutDashboard },
    { label: 'Members & Roles', href: '/organization/members', icon: Users },
    { label: 'Applications', href: '/applications', icon: Key },
    { label: 'Audit Logs', href: '/audit-logs', icon: ScrollText },
    { label: 'Security & Passkeys', href: '/settings/security', icon: Lock },
    ...(isPlatformAdmin
      ? [{ label: 'Platform Admin', href: '/admin', icon: ShieldCheck }]
      : []),
  ];

  const handleLogout = async () => {
    try {
      await logout();
      router.push('/login');
    } catch {
      router.push('/login');
    }
  };

  return (
    <div className="min-h-screen bg-slate-50 flex flex-col">
      {/* Top Navigation Bar */}
      <header className="bg-white border-b border-slate-200 sticky top-0 z-40">
        <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
          <div className="flex justify-between h-16 items-center">
            {/* Left: Brand + Active Tenant Selector */}
            <div className="flex items-center gap-6">
              <Link href="/dashboard" className="flex items-center gap-2">
                <div className="p-2 bg-sky-600 rounded-lg text-white shadow-sm">
                  <Shield className="w-5 h-5" />
                </div>
                <span className="text-xl font-bold tracking-tight text-slate-900 hidden sm:inline">
                  SecureGate
                </span>
              </Link>

              {/* Organization Badge / Switcher */}
              {activeOrganization ? (
                <div className="flex items-center gap-2 pl-4 border-l border-slate-200">
                  <Building2 className="w-4 h-4 text-slate-400" />
                  <span className="text-sm font-semibold text-slate-800 max-w-[140px] sm:max-w-xs truncate">
                    {activeOrganization.name}
                  </span>
                  <span className="hidden md:inline-flex items-center px-2 py-0.5 rounded text-xs font-medium bg-slate-100 text-slate-700 border border-slate-200">
                    {activeOrganization.role}
                  </span>
                  <Link
                    href="/organizations"
                    className="text-xs text-sky-600 hover:text-sky-700 font-medium ml-1 flex items-center gap-1"
                  >
                    Switch <ChevronDown className="w-3 h-3" />
                  </Link>
                </div>
              ) : (
                <Link
                  href="/organizations"
                  className="text-xs bg-amber-50 text-amber-700 border border-amber-200 px-2.5 py-1 rounded-md font-medium"
                >
                  Select Organization
                </Link>
              )}
            </div>

            {/* Desktop Navigation Links */}
            <nav className="hidden lg:flex items-center gap-1">
              {navItems.map((item) => {
                const Icon = item.icon;
                const isActive = pathname === item.href;
                return (
                  <Link
                    key={item.href}
                    href={item.href}
                    className={`inline-flex items-center gap-2 px-3 py-2 rounded-lg text-xs font-medium transition ${
                      isActive
                        ? 'bg-sky-50 text-sky-700 font-semibold'
                        : 'text-slate-600 hover:text-slate-900 hover:bg-slate-100'
                    }`}
                  >
                    <Icon className="w-4 h-4" />
                    {item.label}
                  </Link>
                );
              })}
            </nav>

            {/* Right: User Profile & Actions */}
            <div className="flex items-center gap-3">
              <Link
                href="/profile"
                className="hidden sm:flex items-center gap-2 text-xs text-slate-600 hover:text-slate-900 px-2 py-1.5 rounded-lg hover:bg-slate-100 transition"
              >
                <div className="w-7 h-7 rounded-full bg-sky-100 text-sky-700 font-bold flex items-center justify-center text-xs">
                  {user?.fullName?.charAt(0) || 'U'}
                </div>
                <span className="font-medium max-w-[100px] truncate">{user?.fullName || 'Profile'}</span>
              </Link>

              <button
                type="button"
                onClick={handleLogout}
                title="Sign Out"
                className="p-2 text-slate-500 hover:text-rose-600 hover:bg-rose-50 rounded-lg transition"
              >
                <LogOut className="w-4 h-4" />
              </button>

              {/* Mobile menu button */}
              <button
                type="button"
                onClick={() => setMobileMenuOpen(!mobileMenuOpen)}
                className="lg:hidden p-2 text-slate-600 hover:text-slate-900 hover:bg-slate-100 rounded-lg"
              >
                {mobileMenuOpen ? <X className="w-5 h-5" /> : <Menu className="w-5 h-5" />}
              </button>
            </div>
          </div>
        </div>

        {/* Mobile Navigation Drawer */}
        {mobileMenuOpen && (
          <div className="lg:hidden border-t border-slate-200 bg-white px-4 pt-2 pb-4 space-y-1">
            {navItems.map((item) => {
              const Icon = item.icon;
              const isActive = pathname === item.href;
              return (
                <Link
                  key={item.href}
                  href={item.href}
                  onClick={() => setMobileMenuOpen(false)}
                  className={`flex items-center gap-3 px-3 py-2 rounded-lg text-sm font-medium ${
                    isActive
                      ? 'bg-sky-50 text-sky-700 font-semibold'
                      : 'text-slate-700 hover:bg-slate-100'
                  }`}
                >
                  <Icon className="w-4 h-4" />
                  {item.label}
                </Link>
              );
            })}
            <div className="pt-2 border-t border-slate-100">
              <Link
                href="/profile"
                onClick={() => setMobileMenuOpen(false)}
                className="flex items-center gap-3 px-3 py-2 rounded-lg text-sm font-medium text-slate-700 hover:bg-slate-100"
              >
                <UserCheck className="w-4 h-4" />
                Profile & Sessions
              </Link>
              <Link
                href="/organizations"
                onClick={() => setMobileMenuOpen(false)}
                className="flex items-center gap-3 px-3 py-2 rounded-lg text-sm font-medium text-slate-700 hover:bg-slate-100"
              >
                <Building2 className="w-4 h-4" />
                Switch Organization
              </Link>
            </div>
          </div>
        )}
      </header>

      {/* Main Content Header & Body */}
      <main className="flex-1 max-w-7xl w-full mx-auto px-4 sm:px-6 lg:px-8 py-8">
        {(title || actions) && (
          <div className="md:flex md:items-center md:justify-between mb-8 pb-4 border-b border-slate-200">
            <div>
              {title && (
                <h1 className="text-2xl font-bold tracking-tight text-slate-900">
                  {title}
                </h1>
              )}
              {subtitle && (
                <p className="mt-1 text-sm text-slate-500">
                  {subtitle}
                </p>
              )}
            </div>
            {actions && (
              <div className="mt-4 flex items-center gap-3 md:mt-0">
                {actions}
              </div>
            )}
          </div>
        )}

        {children}
      </main>

      {/* Footer */}
      <footer className="border-t border-slate-200 bg-white py-4 mt-auto">
        <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 flex flex-col sm:flex-row items-center justify-between gap-2 text-xs text-slate-500">
          <div className="flex items-center gap-2">
            <span className="font-semibold text-slate-700">SecureGate</span>
            <span>&copy; 2026 Multi-Tenant Identity Platform</span>
          </div>
          <div className="flex items-center gap-4">
            <span className="inline-flex items-center gap-1.5">
              <span className="w-2 h-2 rounded-full bg-emerald-500"></span>
              PostgreSQL RLS Active
            </span>
            <span>Asymmetric RS256 Tokens</span>
            <span>WebAuthn FIDO2</span>
          </div>
        </div>
      </footer>
    </div>
  );
}
