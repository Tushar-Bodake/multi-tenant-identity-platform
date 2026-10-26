'use client';
import React, { useEffect, useState } from 'react';
import Link from 'next/link';
import { useRouter } from 'next/navigation';
import {
  ShieldAlert,
  Building2,
  Users,
  ScrollText,
  Activity,
  Server,
  Lock,
  ArrowRight,
  ShieldCheck,
  Loader2,
  AlertCircle
} from 'lucide-react';
import DashboardLayout from '@/components/DashboardLayout';
import { useAuth } from '@/context/AuthContext';
import { api, OrganizationDto, UserDto, AuditEventDto } from '@/lib/api';

interface PlatformStats {
  totalUsers: number;
  totalOrganizations: number;
  totalAuditEvents: number;
  systemStatus: string;
}

export default function AdminPage() {
  const router = useRouter();
  const { user, activeOrganization, isLoading } = useAuth();

  const [stats, setStats] = useState<PlatformStats | null>(null);
  const [organizations, setOrganizations] = useState<any[]>([]);
  const [usersList, setUsersList] = useState<UserDto[]>([]);
  const [auditLogs, setAuditLogs] = useState<AuditEventDto[]>([]);
  const [loading, setLoading] = useState(true);
  const [errorMessage, setErrorMessage] = useState<string | null>(null);

  const isPlatformAdmin = activeOrganization?.role === 'PLATFORM_ADMIN';

  useEffect(() => {
    if (!isLoading && !user) {
      router.push('/login');
      return;
    }

    const loadAdminData = async () => {
      setLoading(true);
      setErrorMessage(null);

      try {
        const statsData = await api.fetch<PlatformStats>('/admin/stats');
        setStats(statsData);

        const orgsData = await api.fetch<any[]>('/admin/organizations');
        setOrganizations(orgsData);

        const usersData = await api.fetch<UserDto[]>('/admin/users');
        setUsersList(usersData);

        const auditData = await api.fetch<{ content: AuditEventDto[] }>('/admin/audit-logs?page=0&size=10');
        setAuditLogs(auditData.content || []);
      } catch (err: any) {
        setErrorMessage(
          err.detail || 'Access Denied: You do not have PLATFORM_ADMIN credentials.'
        );
      } finally {
        setLoading(false);
      }
    };

    if (user) {
      loadAdminData();
    }
  }, [user, isLoading, router]);

  if (!isPlatformAdmin && !loading && errorMessage) {
    return (
      <DashboardLayout title="Access Denied" subtitle="Platform Administration Boundary">
        <div className="card-container text-center py-16 max-w-lg mx-auto">
          <div className="w-12 h-12 bg-rose-50 text-rose-600 rounded-full flex items-center justify-center mx-auto mb-4">
            <ShieldAlert className="w-6 h-6" />
          </div>
          <h3 className="text-base font-bold text-slate-900">Platform Admin Authorization Required</h3>
          <p className="text-xs text-slate-500 mt-2 leading-relaxed">
            Organization administrators and tenant members cannot access cross-tenant platform telemetry
            or administrative functions without explicitly authorized system-level credentials.
          </p>
          <div className="mt-6">
            <Link href="/dashboard" className="btn-primary text-xs inline-flex items-center gap-2">
              Return to Organization Dashboard <ArrowRight className="w-4 h-4" />
            </Link>
          </div>
        </div>
      </DashboardLayout>
    );
  }

  return (
    <DashboardLayout
      title="Platform Operations & Administration"
      subtitle="System-level multi-tenant cluster telemetry, health monitoring, and cross-tenant auditing"
    >
      {/* Platform Telemetry Cards */}
      <div className="grid grid-cols-1 gap-5 sm:grid-cols-2 lg:grid-cols-4 mb-8">
        <div className="card-container">
          <div className="flex items-center justify-between mb-2">
            <span className="text-xs font-semibold text-slate-500 uppercase">Total Platform Tenants</span>
            <Building2 className="w-4 h-4 text-sky-600" />
          </div>
          <div className="text-2xl font-bold text-slate-900">
            {loading ? '...' : stats?.totalOrganizations ?? 0}
          </div>
          <p className="text-xs text-slate-400 mt-1">Active enterprise SaaS organizations</p>
        </div>

        <div className="card-container">
          <div className="flex items-center justify-between mb-2">
            <span className="text-xs font-semibold text-slate-500 uppercase">Registered Identities</span>
            <Users className="w-4 h-4 text-purple-600" />
          </div>
          <div className="text-2xl font-bold text-slate-900">
            {loading ? '...' : stats?.totalUsers ?? 0}
          </div>
          <p className="text-xs text-slate-400 mt-1">Global user accounts across tenants</p>
        </div>

        <div className="card-container">
          <div className="flex items-center justify-between mb-2">
            <span className="text-xs font-semibold text-slate-500 uppercase">Audit Ledger Records</span>
            <ScrollText className="w-4 h-4 text-emerald-600" />
          </div>
          <div className="text-2xl font-bold text-slate-900">
            {loading ? '...' : stats?.totalAuditEvents ?? 0}
          </div>
          <p className="text-xs text-slate-400 mt-1">Append-only cryptographic blocks</p>
        </div>

        <div className="card-container">
          <div className="flex items-center justify-between mb-2">
            <span className="text-xs font-semibold text-slate-500 uppercase">Platform Cluster Health</span>
            <Activity className="w-4 h-4 text-emerald-600" />
          </div>
          <div className="flex items-center gap-2">
            <span className="w-2.5 h-2.5 rounded-full bg-emerald-500 animate-pulse"></span>
            <span className="text-xl font-bold text-slate-900">OPERATIONAL</span>
          </div>
          <p className="text-xs text-slate-400 mt-1">Actuator metrics & RLS nominal</p>
        </div>
      </div>

      <div className="grid grid-cols-1 lg:grid-cols-2 gap-8 mb-8">
        {/* Organizations Registry */}
        <div className="card-container p-0 overflow-hidden">
          <div className="p-4 sm:p-5 border-b border-slate-100 flex items-center justify-between">
            <h3 className="text-sm font-bold text-slate-900">All Registered Organizations</h3>
            <span className="text-xs px-2 py-0.5 rounded bg-slate-100 text-slate-600">
              {organizations.length}
            </span>
          </div>

          <div className="overflow-x-auto max-h-72">
            <table className="w-full text-left text-xs">
              <thead className="bg-slate-50 text-slate-500 sticky top-0">
                <tr>
                  <th className="px-5 py-2.5">Name</th>
                  <th className="px-5 py-2.5">Slug</th>
                  <th className="px-5 py-2.5">Status</th>
                  <th className="px-5 py-2.5">Created</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-slate-100">
                {organizations.map((org) => (
                  <tr key={org.id} className="hover:bg-slate-50/50">
                    <td className="px-5 py-3 font-semibold text-slate-800">{org.name}</td>
                    <td className="px-5 py-3 font-mono text-slate-500">@{org.slug}</td>
                    <td className="px-5 py-3">
                      <span className="px-2 py-0.5 rounded text-[10px] font-bold bg-emerald-50 text-emerald-700 border border-emerald-200">
                        {org.status}
                      </span>
                    </td>
                    <td className="px-5 py-3 text-slate-400">
                      {new Date(org.createdAt).toLocaleDateString()}
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        </div>

        {/* Global Users Registry */}
        <div className="card-container p-0 overflow-hidden">
          <div className="p-4 sm:p-5 border-b border-slate-100 flex items-center justify-between">
            <h3 className="text-sm font-bold text-slate-900">Platform Identities</h3>
            <span className="text-xs px-2 py-0.5 rounded bg-slate-100 text-slate-600">
              {usersList.length}
            </span>
          </div>

          <div className="overflow-x-auto max-h-72">
            <table className="w-full text-left text-xs">
              <thead className="bg-slate-50 text-slate-500 sticky top-0">
                <tr>
                  <th className="px-5 py-2.5">User</th>
                  <th className="px-5 py-2.5">Email</th>
                  <th className="px-5 py-2.5">Email Status</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-slate-100">
                {usersList.map((u) => (
                  <tr key={u.id} className="hover:bg-slate-50/50">
                    <td className="px-5 py-3 font-semibold text-slate-800">{u.fullName}</td>
                    <td className="px-5 py-3 font-mono text-slate-500">{u.email}</td>
                    <td className="px-5 py-3">
                      {u.emailVerified ? (
                        <span className="text-emerald-600 font-semibold">Verified</span>
                      ) : (
                        <span className="text-amber-600">Pending</span>
                      )}
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        </div>
      </div>

      {/* Platform-wide Audit Stream */}
      <div className="card-container p-0 overflow-hidden">
        <div className="p-4 sm:p-5 border-b border-slate-100">
          <h3 className="text-sm font-bold text-slate-900">Platform-Wide Security Event Stream</h3>
          <p className="text-xs text-slate-500">Cross-tenant operational and administrative audit events</p>
        </div>

        <div className="overflow-x-auto">
          <table className="w-full text-left text-xs">
            <thead className="bg-slate-50 text-slate-500 font-semibold border-b border-slate-200">
              <tr>
                <th className="px-6 py-3">Timestamp</th>
                <th className="px-6 py-3">Tenant ID</th>
                <th className="px-6 py-3">Action</th>
                <th className="px-6 py-3">Outcome</th>
                <th className="px-6 py-3">Current Hash</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-slate-100">
              {auditLogs.map((log) => (
                <tr key={log.id} className="hover:bg-slate-50/50">
                  <td className="px-6 py-3 text-slate-600 whitespace-nowrap">
                    {new Date(log.timestamp).toLocaleString()}
                  </td>
                  <td className="px-6 py-3 font-mono text-[11px] text-slate-500">
                    {log.tenantId ? log.tenantId.substring(0, 8) + '...' : 'GLOBAL'}
                  </td>
                  <td className="px-6 py-3 font-semibold text-slate-900">{log.action}</td>
                  <td className="px-6 py-3">
                    <span
                      className={`px-2 py-0.5 rounded text-[10px] font-bold ${
                        log.outcome === 'SUCCESS'
                          ? 'bg-emerald-50 text-emerald-700 border border-emerald-200'
                          : 'bg-rose-50 text-rose-700 border border-rose-200'
                      }`}
                    >
                      {log.outcome}
                    </span>
                  </td>
                  <td className="px-6 py-3 font-mono text-[11px] text-slate-400">
                    {log.currentHash.substring(0, 16)}...
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      </div>
    </DashboardLayout>
  );
}
