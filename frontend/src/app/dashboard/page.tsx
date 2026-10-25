'use client';

import React, { useEffect, useState } from 'react';
import Link from 'next/link';
import { useRouter } from 'next/navigation';
import {
  Shield,
  Users,
  Key,
  ScrollText,
  Lock,
  ArrowRight,
  CheckCircle2,
  AlertTriangle,
  Building2,
  RefreshCw,
  ExternalLink,
  Cpu,
  Layers,
  Database
} from 'lucide-react';
import DashboardLayout from '@/components/DashboardLayout';
import { useAuth } from '@/context/AuthContext';
import { api, AuditEventDto, AuditVerificationResult, MemberDto, ApplicationDto } from '@/lib/api';

export default function DashboardPage() {
  const router = useRouter();
  const { user, activeOrganization, organizations, isLoading } = useAuth();

  const [membersCount, setMembersCount] = useState<number | null>(null);
  const [appsCount, setAppsCount] = useState<number | null>(null);
  const [auditLogs, setAuditLogs] = useState<AuditEventDto[]>([]);
  const [verificationResult, setVerificationResult] = useState<AuditVerificationResult | null>(null);
  const [loadingStats, setLoadingStats] = useState(true);

  useEffect(() => {
    if (!isLoading && !user) {
      router.push('/login');
      return;
    }

    if (!isLoading && user && !activeOrganization) {
      router.push('/organizations');
      return;
    }

    const loadDashboardData = async () => {
      if (!activeOrganization) return;
      setLoadingStats(true);

      try {
        // Fetch members
        const members = await api.fetch<MemberDto[]>(`/organizations/${activeOrganization.id}/members`);
        setMembersCount(members.length);

        // Fetch applications if permitted
        try {
          const apps = await api.fetch<ApplicationDto[]>(`/organizations/${activeOrganization.id}/applications`);
          setAppsCount(apps.length);
        } catch {
          setAppsCount(0);
        }

        // Fetch recent audit logs
        try {
          const pageRes = await api.fetch<{ content: AuditEventDto[] }>(
            `/organizations/${activeOrganization.id}/audit-logs?page=0&size=5`
          );
          setAuditLogs(pageRes.content || []);

          // Verify audit chain
          const verifyRes = await api.fetch<AuditVerificationResult>(
            `/organizations/${activeOrganization.id}/audit-logs/verify`
          );
          setVerificationResult(verifyRes);
        } catch {
          // If member lacks audit:read
        }
      } catch (err) {
        console.error('Failed to load dashboard data', err);
      } finally {
        setLoadingStats(false);
      }
    };

    if (activeOrganization) {
      loadDashboardData();
    }
  }, [activeOrganization, user, isLoading, router]);

  if (isLoading || !activeOrganization) {
    return (
      <DashboardLayout>
        <div className="py-20 text-center">
          <div className="w-8 h-8 border-4 border-sky-600 border-t-transparent rounded-full animate-spin mx-auto mb-4" />
          <p className="text-sm text-slate-500 font-medium">Resolving tenant context and security posture...</p>
        </div>
      </DashboardLayout>
    );
  }

  return (
    <DashboardLayout
      title={`Dashboard — ${activeOrganization.name}`}
      subtitle={`Tenant ID: ${activeOrganization.id} • Slug: @${activeOrganization.slug}`}
      actions={
        <div className="flex items-center gap-2">
          <Link href="/organization/members" className="btn-primary text-xs flex items-center gap-1.5">
            <Users className="w-3.5 h-3.5" />
            Invite Member
          </Link>
          <Link href="/audit-logs" className="btn-secondary text-xs flex items-center gap-1.5">
            <ScrollText className="w-3.5 h-3.5" />
            Verify Chain
          </Link>
        </div>
      }
    >
      {/* Security Isolation Banner */}
      <div className="mb-8 rounded-xl bg-gradient-to-r from-sky-900 to-indigo-950 p-6 text-white shadow-md relative overflow-hidden">
        <div className="relative z-10 flex flex-col md:flex-row md:items-center justify-between gap-4">
          <div>
            <div className="flex items-center gap-2 mb-1">
              <span className="px-2 py-0.5 rounded text-[11px] font-bold bg-sky-500/20 text-sky-200 border border-sky-400/30">
                ACTIVE TENANT ISOLATION
              </span>
              <span className="text-xs text-sky-200/80">• PostgreSQL Row-Level Security Enforced</span>
            </div>
            <h2 className="text-xl font-bold tracking-tight">
              Organization Context: {activeOrganization.name}
            </h2>
            <p className="mt-1 text-xs text-slate-300 max-w-2xl leading-relaxed">
              Every database query and API operation is cryptographically scoped to this tenant using
              asymmetric RS256 JWT claims and transaction-level PostgreSQL session isolation.
            </p>
          </div>
          <div className="flex items-center gap-3">
            <div className="p-3 bg-white/10 rounded-lg backdrop-blur-sm border border-white/10 text-center">
              <span className="block text-xs text-slate-300">Your Role</span>
              <span className="text-sm font-bold text-sky-300">{activeOrganization.role}</span>
            </div>
          </div>
        </div>
      </div>

      {/* Metric Cards Grid */}
      <div className="grid grid-cols-1 gap-5 sm:grid-cols-2 lg:grid-cols-4 mb-8">
        {/* Members Metric */}
        <div className="card-container flex flex-col justify-between hover:border-slate-300 transition">
          <div>
            <div className="flex items-center justify-between mb-2">
              <span className="text-xs font-semibold text-slate-500 uppercase tracking-wider">
                Organization Members
              </span>
              <div className="p-2 bg-blue-50 text-blue-600 rounded-lg">
                <Users className="w-4 h-4" />
              </div>
            </div>
            <div className="text-2xl font-bold text-slate-900">
              {loadingStats ? '...' : membersCount ?? 0}
            </div>
            <p className="mt-1 text-xs text-slate-500">
              Assigned across Owner, Admin, Developer, Member roles
            </p>
          </div>
          <Link
            href="/organization/members"
            className="mt-4 inline-flex items-center text-xs font-medium text-sky-600 hover:text-sky-700"
          >
            Manage team <ArrowRight className="w-3.5 h-3.5 ml-1" />
          </Link>
        </div>

        {/* Applications Metric */}
        <div className="card-container flex flex-col justify-between hover:border-slate-300 transition">
          <div>
            <div className="flex items-center justify-between mb-2">
              <span className="text-xs font-semibold text-slate-500 uppercase tracking-wider">
                Client Applications
              </span>
              <div className="p-2 bg-purple-50 text-purple-600 rounded-lg">
                <Key className="w-4 h-4" />
              </div>
            </div>
            <div className="text-2xl font-bold text-slate-900">
              {loadingStats ? '...' : appsCount ?? 0}
            </div>
            <p className="mt-1 text-xs text-slate-500">
              OIDC clients & API credentials with secret hashing
            </p>
          </div>
          <Link
            href="/applications"
            className="mt-4 inline-flex items-center text-xs font-medium text-sky-600 hover:text-sky-700"
          >
            Manage credentials <ArrowRight className="w-3.5 h-3.5 ml-1" />
          </Link>
        </div>

        {/* Audit Chain Verifier Metric */}
        <div className="card-container flex flex-col justify-between hover:border-slate-300 transition">
          <div>
            <div className="flex items-center justify-between mb-2">
              <span className="text-xs font-semibold text-slate-500 uppercase tracking-wider">
                Audit Chain Integrity
              </span>
              <div className="p-2 bg-emerald-50 text-emerald-600 rounded-lg">
                <ScrollText className="w-4 h-4" />
              </div>
            </div>
            <div className="flex items-center gap-2">
              <span className="text-2xl font-bold text-slate-900">
                {verificationResult?.valid ? 'VALID' : 'CHECKING'}
              </span>
              {verificationResult?.valid && (
                <CheckCircle2 className="w-5 h-5 text-emerald-600" />
              )}
            </div>
            <p className="mt-1 text-xs text-slate-500">
              {verificationResult?.eventsVerified ?? 0} tamper-evident SHA-256 blocks verified
            </p>
          </div>
          <Link
            href="/audit-logs"
            className="mt-4 inline-flex items-center text-xs font-medium text-sky-600 hover:text-sky-700"
          >
            View audit ledger <ArrowRight className="w-3.5 h-3.5 ml-1" />
          </Link>
        </div>

        {/* Security Policy Metric */}
        <div className="card-container flex flex-col justify-between hover:border-slate-300 transition">
          <div>
            <div className="flex items-center justify-between mb-2">
              <span className="text-xs font-semibold text-slate-500 uppercase tracking-wider">
                Passkeys & Policy
              </span>
              <div className="p-2 bg-amber-50 text-amber-600 rounded-lg">
                <Lock className="w-4 h-4" />
              </div>
            </div>
            <div className="text-2xl font-bold text-slate-900">
              FIDO2 Ready
            </div>
            <p className="mt-1 text-xs text-slate-500">
              WebAuthn hardware tokens and refresh-token rotation
            </p>
          </div>
          <Link
            href="/settings/security"
            className="mt-4 inline-flex items-center text-xs font-medium text-sky-600 hover:text-sky-700"
          >
            Security controls <ArrowRight className="w-3.5 h-3.5 ml-1" />
          </Link>
        </div>
      </div>

      {/* Quick Activity & Security Details */}
      <div className="grid grid-cols-1 lg:grid-cols-3 gap-8">
        {/* Recent Audit Events Stream */}
        <div className="lg:col-span-2 card-container">
          <div className="flex items-center justify-between mb-4 pb-3 border-b border-slate-100">
            <div>
              <h3 className="text-base font-bold text-slate-900">Recent Security Events</h3>
              <p className="text-xs text-slate-500">Live append-only audit trail with previous-hash links</p>
            </div>
            <Link
              href="/audit-logs"
              className="text-xs font-semibold text-sky-600 hover:text-sky-700 flex items-center gap-1"
            >
              Full Log <ArrowRight className="w-3 h-3" />
            </Link>
          </div>

          {auditLogs.length === 0 ? (
            <div className="py-10 text-center text-xs text-slate-400">
              No audit events recorded yet for this organization session.
            </div>
          ) : (
            <div className="divide-y divide-slate-100">
              {auditLogs.map((event) => (
                <div key={event.id} className="py-3 flex items-center justify-between">
                  <div className="space-y-0.5">
                    <div className="flex items-center gap-2">
                      <span className="font-semibold text-xs text-slate-800">
                        {event.action}
                      </span>
                      <span
                        className={`px-1.5 py-0.5 rounded text-[10px] font-bold ${
                          event.outcome === 'SUCCESS'
                            ? 'bg-emerald-50 text-emerald-700 border border-emerald-200'
                            : 'bg-rose-50 text-rose-700 border border-rose-200'
                        }`}
                      >
                        {event.outcome}
                      </span>
                    </div>
                    <p className="text-[11px] text-slate-400 font-mono truncate max-w-sm">
                      Hash: {event.currentHash.substring(0, 16)}... • Prev: {event.prevHash.substring(0, 16)}...
                    </p>
                  </div>
                  <span className="text-[11px] text-slate-400">
                    {new Date(event.timestamp).toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' })}
                  </span>
                </div>
              ))}
            </div>
          )}
        </div>

        {/* Tenant Architecture Highlights */}
        <div className="card-container space-y-4">
          <h3 className="text-base font-bold text-slate-900">Security Architecture</h3>
          <p className="text-xs text-slate-500 leading-relaxed">
            SecureGate implements multi-layered SaaS defense in depth:
          </p>

          <div className="space-y-3">
            <div className="p-3 bg-slate-50 rounded-lg border border-slate-200">
              <div className="flex items-center gap-2 mb-1">
                <Database className="w-4 h-4 text-sky-600" />
                <span className="text-xs font-semibold text-slate-800">PostgreSQL RLS</span>
              </div>
              <p className="text-[11px] text-slate-500">
                Database-kernel row policies ensure cross-tenant queries return zero records, even if application logic were bypassed.
              </p>
            </div>

            <div className="p-3 bg-slate-50 rounded-lg border border-slate-200">
              <div className="flex items-center gap-2 mb-1">
                <Layers className="w-4 h-4 text-purple-600" />
                <span className="text-xs font-semibold text-slate-800">Token Rotation & Reuse Detection</span>
              </div>
              <p className="text-[11px] text-slate-500">
                Opaque refresh tokens rotate on every invocation. Consumed token replay instantly revokes the entire token family.
              </p>
            </div>

            <div className="p-3 bg-slate-50 rounded-lg border border-slate-200">
              <div className="flex items-center gap-2 mb-1">
                <Cpu className="w-4 h-4 text-emerald-600" />
                <span className="text-xs font-semibold text-slate-800">Tamper-Evident Audit Chains</span>
              </div>
              <p className="text-[11px] text-slate-500">
                Audit records contain SHA-256 hashes linked to preceding events. Any modification or deletion invalidates the chain.
              </p>
            </div>
          </div>
        </div>
      </div>
    </DashboardLayout>
  );
}
