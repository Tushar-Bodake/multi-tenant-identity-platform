'use client';

import React, { useEffect, useState } from 'react';
import Link from 'next/link';
import { useRouter } from 'next/navigation';
import {
  ScrollText,
  ShieldCheck,
  ShieldAlert,
  CheckCircle2,
  AlertCircle,
  RefreshCw,
  Search,
  Filter,
  ChevronLeft,
  ChevronRight,
  ChevronDown,
  ChevronUp,
  Cpu,
  Layers,
  Loader2
} from 'lucide-react';
import DashboardLayout from '@/components/DashboardLayout';
import { useAuth } from '@/context/AuthContext';
import { api, AuditEventDto, AuditVerificationResult } from '@/lib/api';

export default function AuditLogsPage() {
  const router = useRouter();
  const { user, activeOrganization, isLoading } = useAuth();

  const [auditLogs, setAuditLogs] = useState<AuditEventDto[]>([]);
  const [totalElements, setTotalElements] = useState(0);
  const [totalPages, setTotalPages] = useState(1);
  const [page, setPage] = useState(0);
  const [size, setSize] = useState(15);
  const [loading, setLoading] = useState(true);

  // Verification state
  const [verificationResult, setVerificationResult] = useState<AuditVerificationResult | null>(null);
  const [verifying, setVerifying] = useState(false);

  // Expanded row metadata toggle
  const [expandedRowId, setExpandedRowId] = useState<string | null>(null);

  // Filter
  const [filterOutcome, setFilterOutcome] = useState<'ALL' | 'SUCCESS' | 'FAILED'>('ALL');
  const [searchQuery, setSearchQuery] = useState('');

  const loadAuditLogs = async (currentPage: number) => {
    if (!activeOrganization) return;
    setLoading(true);

    try {
      const res = await api.fetch<{
        content: AuditEventDto[];
        totalElements: number;
        totalPages: number;
      }>(`/organizations/${activeOrganization.id}/audit-logs?page=${currentPage}&size=${size}`);

      setAuditLogs(res.content || []);
      setTotalElements(res.totalElements || 0);
      setTotalPages(res.totalPages || 1);
    } catch (err: any) {
      console.error('Failed to load audit logs', err);
    } finally {
      setLoading(false);
    }
  };

  const handleVerifyChain = async () => {
    if (!activeOrganization) return;
    setVerifying(true);

    try {
      const result = await api.fetch<AuditVerificationResult>(
        `/organizations/${activeOrganization.id}/audit-logs/verify`
      );
      setVerificationResult(result);
    } catch (err: any) {
      setVerificationResult({
        valid: false,
        eventsVerified: 0,
        errorMessage: err.detail || 'Verification request failed',
      });
    } finally {
      setVerifying(false);
    }
  };

  useEffect(() => {
    if (!isLoading && !user) {
      router.push('/login');
      return;
    }
    if (!isLoading && !activeOrganization) {
      router.push('/organizations');
      return;
    }
    if (activeOrganization) {
      loadAuditLogs(page);
    }
  }, [activeOrganization, user, isLoading, page, router]);

  const filteredLogs = auditLogs.filter((event) => {
    const matchesOutcome =
      filterOutcome === 'ALL' || event.outcome === filterOutcome;
    const matchesQuery =
      searchQuery === '' ||
      event.action.toLowerCase().includes(searchQuery.toLowerCase()) ||
      event.targetType.toLowerCase().includes(searchQuery.toLowerCase()) ||
      event.currentHash.toLowerCase().includes(searchQuery.toLowerCase());
    return matchesOutcome && matchesQuery;
  });

  return (
    <DashboardLayout
      title="Tamper-Evident Audit Ledger"
      subtitle="Cryptographically sealed, append-only security event trail with SHA-256 chain verification"
      actions={
        <button
          type="button"
          onClick={handleVerifyChain}
          disabled={verifying}
          className="btn-primary text-xs flex items-center gap-1.5"
        >
          {verifying ? (
            <Loader2 className="w-3.5 h-3.5 animate-spin" />
          ) : (
            <ShieldCheck className="w-3.5 h-3.5" />
          )}
          {verifying ? 'Verifying Chain...' : 'Verify Cryptographic Chain'}
        </button>
      }
    >
      {/* Verification Status Card */}
      {verificationResult && (
        <div
          className={`mb-6 p-4 rounded-xl border flex items-start gap-4 animate-in fade-in duration-300 ${
            verificationResult.valid
              ? 'bg-emerald-50/70 border-emerald-200 text-emerald-900'
              : 'bg-rose-50/70 border-rose-200 text-rose-900'
          }`}
        >
          <div
            className={`p-2 rounded-lg ${
              verificationResult.valid ? 'bg-emerald-100 text-emerald-700' : 'bg-rose-100 text-rose-700'
            }`}
          >
            {verificationResult.valid ? (
              <CheckCircle2 className="w-6 h-6" />
            ) : (
              <ShieldAlert className="w-6 h-6" />
            )}
          </div>
          <div className="flex-1">
            <h4 className="text-sm font-bold">
              {verificationResult.valid
                ? 'Cryptographic Hash Chain Verified: 100% Intact'
                : 'Warning: Audit Chain Integrity Broken!'}
            </h4>
            <p className="text-xs mt-1 leading-relaxed opacity-90">
              {verificationResult.valid
                ? `Successfully recalculated and checked ${verificationResult.eventsVerified} sequential SHA-256 block hashes from the genesis record. No tampering, event modifications, or broken links detected.`
                : `Integrity failure detected at event index ${verificationResult.brokenIndex} (ID: ${verificationResult.brokenEventId}). Reason: ${verificationResult.errorMessage}`}
            </p>
          </div>
        </div>
      )}

      {/* Filter and Search Bar */}
      <div className="card-container mb-6 p-4 flex flex-col sm:flex-row gap-3 items-center justify-between">
        <div className="relative w-full sm:w-72">
          <Search className="w-4 h-4 absolute left-3 top-2.5 text-slate-400" />
          <input
            type="text"
            value={searchQuery}
            onChange={(e) => setSearchQuery(e.target.value)}
            placeholder="Search action, target, or hash..."
            className="input-field pl-9 py-1.5 text-xs"
          />
        </div>

        <div className="flex items-center gap-2 w-full sm:w-auto justify-end">
          <span className="text-xs font-medium text-slate-500">Outcome:</span>
          <div className="inline-flex rounded-lg border border-slate-200 bg-slate-50 p-0.5 text-xs">
            {(['ALL', 'SUCCESS', 'FAILED'] as const).map((outcome) => (
              <button
                key={outcome}
                type="button"
                onClick={() => setFilterOutcome(outcome)}
                className={`px-3 py-1 rounded-md font-semibold transition ${
                  filterOutcome === outcome
                    ? 'bg-white text-slate-900 shadow-sm'
                    : 'text-slate-500 hover:text-slate-700'
                }`}
              >
                {outcome}
              </button>
            ))}
          </div>
        </div>
      </div>

      {/* Audit Log Table */}
      <div className="card-container overflow-hidden p-0">
        <div className="p-4 sm:p-6 border-b border-slate-100 flex items-center justify-between">
          <div>
            <h3 className="text-base font-bold text-slate-900">Tenant Event Log</h3>
            <p className="text-xs text-slate-500">
              Showing page {page + 1} of {totalPages} ({totalElements} total logged events)
            </p>
          </div>
        </div>

        {loading ? (
          <div className="py-16 text-center">
            <Loader2 className="w-6 h-6 animate-spin text-sky-600 mx-auto mb-2" />
            <p className="text-xs text-slate-500">Fetching audit records...</p>
          </div>
        ) : filteredLogs.length === 0 ? (
          <div className="py-16 text-center text-xs text-slate-400">
            No audit records match the selected filter.
          </div>
        ) : (
          <div className="overflow-x-auto">
            <table className="w-full text-left text-xs">
              <thead className="bg-slate-50 text-slate-500 font-semibold border-b border-slate-200">
                <tr>
                  <th className="px-6 py-3">Timestamp</th>
                  <th className="px-6 py-3">Action</th>
                  <th className="px-6 py-3">Target</th>
                  <th className="px-6 py-3">Outcome</th>
                  <th className="px-6 py-3">SHA-256 Hash</th>
                  <th className="px-6 py-3 text-right">Details</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-slate-100">
                {filteredLogs.map((event) => {
                  const isExpanded = expandedRowId === event.id;
                  return (
                    <React.Fragment key={event.id}>
                      <tr className="hover:bg-slate-50/50 transition">
                        <td className="px-6 py-3.5 text-slate-600 whitespace-nowrap">
                          {new Date(event.timestamp).toLocaleString()}
                        </td>
                        <td className="px-6 py-3.5 font-semibold text-slate-900">
                          {event.action}
                        </td>
                        <td className="px-6 py-3.5 text-slate-500">
                          <span className="font-medium text-slate-700">{event.targetType}</span>
                          {event.targetId && (
                            <span className="block text-[10px] text-slate-400 font-mono truncate max-w-[120px]">
                              {event.targetId}
                            </span>
                          )}
                        </td>
                        <td className="px-6 py-3.5">
                          <span
                            className={`inline-block px-2 py-0.5 rounded text-[10px] font-bold ${
                              event.outcome === 'SUCCESS'
                                ? 'bg-emerald-50 text-emerald-700 border border-emerald-200'
                                : 'bg-rose-50 text-rose-700 border border-rose-200'
                            }`}
                          >
                            {event.outcome}
                          </span>
                        </td>
                        <td className="px-6 py-3.5 font-mono text-[11px] text-slate-500">
                          <span title={event.currentHash}>
                            {event.currentHash.substring(0, 16)}...
                          </span>
                        </td>
                        <td className="px-6 py-3.5 text-right">
                          <button
                            type="button"
                            onClick={() => setExpandedRowId(isExpanded ? null : event.id)}
                            className="text-sky-600 hover:text-sky-700 font-medium inline-flex items-center gap-1"
                          >
                            {isExpanded ? 'Hide' : 'Inspect'}
                            {isExpanded ? (
                              <ChevronUp className="w-3.5 h-3.5" />
                            ) : (
                              <ChevronDown className="w-3.5 h-3.5" />
                            )}
                          </button>
                        </td>
                      </tr>
                      {isExpanded && (
                        <tr className="bg-slate-50/80">
                          <td colSpan={6} className="px-6 py-4">
                            <div className="grid grid-cols-1 md:grid-cols-2 gap-4 text-xs font-mono">
                              <div className="space-y-1">
                                <p className="text-slate-500">
                                  <span className="font-semibold text-slate-700">Event ID:</span>{' '}
                                  {event.id}
                                </p>
                                <p className="text-slate-500">
                                  <span className="font-semibold text-slate-700">Correlation ID:</span>{' '}
                                  {event.correlationId}
                                </p>
                                <p className="text-slate-500">
                                  <span className="font-semibold text-slate-700">Actor ID:</span>{' '}
                                  {event.actorId || 'System / Unauthenticated'}
                                </p>
                                <p className="text-slate-500 truncate" title={event.prevHash}>
                                  <span className="font-semibold text-slate-700">Previous Hash:</span>{' '}
                                  {event.prevHash}
                                </p>
                                <p className="text-slate-500 truncate" title={event.currentHash}>
                                  <span className="font-semibold text-slate-700">Current Hash:</span>{' '}
                                  {event.currentHash}
                                </p>
                              </div>
                              <div>
                                <span className="font-semibold text-slate-700 block mb-1">
                                  Canonical Metadata:
                                </span>
                                <pre className="bg-slate-900 text-slate-100 p-3 rounded-lg text-[11px] overflow-x-auto max-h-36">
                                  {event.metadataJson
                                    ? JSON.stringify(JSON.parse(event.metadataJson), null, 2)
                                    : '{}'}
                                </pre>
                              </div>
                            </div>
                          </td>
                        </tr>
                      )}
                    </React.Fragment>
                  );
                })}
              </tbody>
            </table>
          </div>
        )}

        {/* Pagination Controls */}
        <div className="p-4 border-t border-slate-100 flex items-center justify-between">
          <button
            type="button"
            disabled={page === 0 || loading}
            onClick={() => setPage((p) => Math.max(0, p - 1))}
            className="btn-secondary text-xs flex items-center gap-1 disabled:opacity-40"
          >
            <ChevronLeft className="w-3.5 h-3.5" /> Previous
          </button>
          <span className="text-xs text-slate-500">
            Page {page + 1} of {totalPages}
          </span>
          <button
            type="button"
            disabled={page >= totalPages - 1 || loading}
            onClick={() => setPage((p) => p + 1)}
            className="btn-secondary text-xs flex items-center gap-1 disabled:opacity-40"
          >
            Next <ChevronRight className="w-3.5 h-3.5" />
          </button>
        </div>
      </div>
    </DashboardLayout>
  );
}
