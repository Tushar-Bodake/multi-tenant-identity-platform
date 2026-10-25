'use client';

import React, { useEffect, useState } from 'react';
import Link from 'next/link';
import { useRouter } from 'next/navigation';
import {
  Key,
  Plus,
  Trash2,
  RefreshCw,
  Copy,
  Check,
  AlertTriangle,
  AlertCircle,
  CheckCircle2,
  X,
  Loader2,
  ShieldAlert,
  Globe
} from 'lucide-react';
import DashboardLayout from '@/components/DashboardLayout';
import { useAuth } from '@/context/AuthContext';
import { api, ApplicationDto, ApplicationCreatedDto } from '@/lib/api';

export default function ApplicationsPage() {
  const router = useRouter();
  const { user, activeOrganization, isLoading } = useAuth();

  const [applications, setApplications] = useState<ApplicationDto[]>([]);
  const [loading, setLoading] = useState(true);
  const [errorMessage, setErrorMessage] = useState<string | null>(null);
  const [successMessage, setSuccessMessage] = useState<string | null>(null);

  // Create Modal
  const [isCreateModalOpen, setIsCreateModalOpen] = useState(false);
  const [appName, setAppName] = useState('');
  const [redirectUris, setRedirectUris] = useState('');
  const [creating, setCreating] = useState(false);

  // Secret Revealed Modal (One-time viewing!)
  const [revealedSecret, setRevealedSecret] = useState<ApplicationCreatedDto | null>(null);
  const [copiedSecret, setCopiedSecret] = useState(false);
  const [copiedClientId, setCopiedClientId] = useState(false);

  const canManageApps =
    activeOrganization?.role === 'ORG_OWNER' ||
    activeOrganization?.role === 'ORG_ADMIN' ||
    activeOrganization?.role === 'DEVELOPER';

  const loadApplications = async () => {
    if (!activeOrganization) return;
    setLoading(true);
    setErrorMessage(null);

    try {
      const data = await api.fetch<ApplicationDto[]>(
        `/organizations/${activeOrganization.id}/applications`
      );
      setApplications(data);
    } catch (err: any) {
      setErrorMessage(err.detail || 'Failed to fetch client applications.');
    } finally {
      setLoading(false);
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
      loadApplications();
    }
  }, [activeOrganization, user, isLoading, router]);

  const handleCreateApplication = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!activeOrganization || !appName.trim()) return;

    setCreating(true);
    setErrorMessage(null);

    try {
      const created = await api.fetch<ApplicationCreatedDto>(
        `/organizations/${activeOrganization.id}/applications`,
        {
          method: 'POST',
          body: JSON.stringify({
            name: appName.trim(),
            redirectUris: redirectUris.trim(),
          }),
        }
      );

      setIsCreateModalOpen(false);
      setAppName('');
      setRedirectUris('');
      setRevealedSecret(created);
      await loadApplications();
    } catch (err: any) {
      setErrorMessage(err.detail || 'Failed to register client application.');
    } finally {
      setCreating(false);
    }
  };

  const handleDeleteApplication = async (appId: string, appName: string) => {
    if (!activeOrganization) return;
    if (!confirm(`Are you sure you want to delete application "${appName}"? Active tokens using this client will fail.`)) {
      return;
    }

    try {
      await api.fetch(`/organizations/${activeOrganization.id}/applications/${appId}`, {
        method: 'DELETE',
      });
      setSuccessMessage(`Application "${appName}" deleted.`);
      await loadApplications();
    } catch (err: any) {
      setErrorMessage(err.detail || 'Failed to delete application.');
    }
  };

  const copyToClipboard = (text: string, type: 'secret' | 'client') => {
    navigator.clipboard.writeText(text);
    if (type === 'secret') {
      setCopiedSecret(true);
      setTimeout(() => setCopiedSecret(false), 2000);
    } else {
      setCopiedClientId(true);
      setTimeout(() => setCopiedClientId(false), 2000);
    }
  };

  return (
    <DashboardLayout
      title="Client Applications & API Keys"
      subtitle="Register OAuth 2.0 / OIDC clients and manage tenant-scoped API credentials"
      actions={
        canManageApps ? (
          <button
            type="button"
            onClick={() => setIsCreateModalOpen(true)}
            className="btn-primary text-xs flex items-center gap-1.5"
          >
            <Plus className="w-3.5 h-3.5" />
            Register Application
          </button>
        ) : undefined
      }
    >
      {/* Notifications */}
      {errorMessage && (
        <div className="mb-6 p-4 rounded-xl bg-rose-50 border border-rose-200 text-rose-700 text-xs flex items-center gap-3">
          <AlertCircle className="w-4 h-4 flex-shrink-0" />
          <span>{errorMessage}</span>
        </div>
      )}
      {successMessage && (
        <div className="mb-6 p-4 rounded-xl bg-emerald-50 border border-emerald-200 text-emerald-700 text-xs flex items-center gap-3">
          <CheckCircle2 className="w-4 h-4 flex-shrink-0" />
          <span>{successMessage}</span>
        </div>
      )}

      {/* Applications List */}
      <div className="card-container overflow-hidden p-0">
        <div className="p-4 sm:p-6 border-b border-slate-100 flex items-center justify-between">
          <div>
            <h3 className="text-base font-bold text-slate-900">Registered Applications</h3>
            <p className="text-xs text-slate-500">
              Client identities authorized to request scoped access tokens for this tenant
            </p>
          </div>
          <span className="text-xs font-semibold px-2.5 py-1 rounded-full bg-slate-100 text-slate-600">
            {applications.length} {applications.length === 1 ? 'app' : 'apps'}
          </span>
        </div>

        {loading ? (
          <div className="py-16 text-center">
            <Loader2 className="w-6 h-6 animate-spin text-sky-600 mx-auto mb-2" />
            <p className="text-xs text-slate-500">Loading registered applications...</p>
          </div>
        ) : applications.length === 0 ? (
          <div className="py-16 text-center px-4">
            <Key className="w-10 h-10 text-slate-300 mx-auto mb-3" />
            <h4 className="text-sm font-semibold text-slate-800">No applications registered</h4>
            <p className="text-xs text-slate-500 mt-1 max-w-sm mx-auto">
              Create an application registration to generate client credentials for API integrations.
            </p>
            {canManageApps && (
              <button
                type="button"
                onClick={() => setIsCreateModalOpen(true)}
                className="btn-primary text-xs mt-4 inline-flex items-center gap-1.5"
              >
                <Plus className="w-3.5 h-3.5" />
                Register First Application
              </button>
            )}
          </div>
        ) : (
          <div className="overflow-x-auto">
            <table className="w-full text-left text-xs">
              <thead className="bg-slate-50 text-slate-500 font-semibold border-b border-slate-200">
                <tr>
                  <th className="px-6 py-3">Application Name</th>
                  <th className="px-6 py-3">Client ID</th>
                  <th className="px-6 py-3">Client Secret</th>
                  <th className="px-6 py-3">Redirect URIs</th>
                  <th className="px-6 py-3">Created Date</th>
                  <th className="px-6 py-3 text-right">Actions</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-slate-100">
                {applications.map((app) => (
                  <tr key={app.id} className="hover:bg-slate-50/50 transition">
                    <td className="px-6 py-4 font-semibold text-slate-900">
                      {app.name}
                    </td>
                    <td className="px-6 py-4 font-mono text-slate-600 text-xs">
                      {app.clientId}
                    </td>
                    <td className="px-6 py-4 text-slate-400 font-mono text-xs">
                      •••••••••••••••• (Hashed)
                    </td>
                    <td className="px-6 py-4 text-slate-500 text-xs">
                      {app.redirectUris || (
                        <span className="text-slate-400 italic">None specified</span>
                      )}
                    </td>
                    <td className="px-6 py-4 text-slate-500 text-xs">
                      {new Date(app.createdAt).toLocaleDateString()}
                    </td>
                    <td className="px-6 py-4 text-right">
                      {canManageApps && (
                        <button
                          type="button"
                          onClick={() => handleDeleteApplication(app.id, app.name)}
                          title="Delete application"
                          className="text-slate-400 hover:text-rose-600 p-1.5 rounded hover:bg-rose-50 transition"
                        >
                          <Trash2 className="w-4 h-4" />
                        </button>
                      )}
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}
      </div>

      {/* Register Application Modal */}
      {isCreateModalOpen && (
        <div className="fixed inset-0 z-50 bg-slate-900/40 backdrop-blur-sm flex items-center justify-center p-4">
          <div className="bg-white rounded-2xl max-w-md w-full p-6 shadow-xl border border-slate-200">
            <div className="flex items-center justify-between mb-4 pb-3 border-b border-slate-100">
              <div className="flex items-center gap-2">
                <div className="p-2 bg-sky-50 text-sky-600 rounded-lg">
                  <Key className="w-5 h-5" />
                </div>
                <h3 className="text-base font-bold text-slate-900">Register New Application</h3>
              </div>
              <button
                type="button"
                onClick={() => setIsCreateModalOpen(false)}
                className="text-slate-400 hover:text-slate-600"
              >
                <X className="w-5 h-5" />
              </button>
            </div>

            <form onSubmit={handleCreateApplication} className="space-y-4">
              <div>
                <label className="block text-xs font-semibold text-slate-700 mb-1">
                  Application Name
                </label>
                <input
                  type="text"
                  required
                  value={appName}
                  onChange={(e) => setAppName(e.target.value)}
                  placeholder="e.g., Billing Service API or Web Dashboard"
                  className="input-field"
                />
              </div>

              <div>
                <label className="block text-xs font-semibold text-slate-700 mb-1">
                  Authorized Redirect URIs (Optional)
                </label>
                <input
                  type="text"
                  value={redirectUris}
                  onChange={(e) => setRedirectUris(e.target.value)}
                  placeholder="https://app.example.com/oauth/callback"
                  className="input-field font-mono text-xs"
                />
                <p className="text-[11px] text-slate-400 mt-1">
                  Space- or comma-separated URLs allowed for OAuth 2.0 authorization code flow.
                </p>
              </div>

              <div className="pt-2 flex items-center justify-end gap-3">
                <button
                  type="button"
                  onClick={() => setIsCreateModalOpen(false)}
                  className="btn-secondary text-xs"
                >
                  Cancel
                </button>
                <button
                  type="submit"
                  disabled={creating}
                  className="btn-primary text-xs"
                >
                  {creating ? 'Registering...' : 'Register Application'}
                </button>
              </div>
            </form>
          </div>
        </div>
      )}

      {/* One-Time Secret Revealed Modal */}
      {revealedSecret && (
        <div className="fixed inset-0 z-50 bg-slate-900/60 backdrop-blur-sm flex items-center justify-center p-4">
          <div className="bg-white rounded-2xl max-w-lg w-full p-6 shadow-2xl border border-slate-200 animate-in zoom-in-95 duration-200">
            <div className="flex items-center gap-3 mb-4">
              <div className="p-3 bg-amber-50 text-amber-600 rounded-xl">
                <ShieldAlert className="w-6 h-6" />
              </div>
              <div>
                <h3 className="text-base font-bold text-slate-900">Application Credentials Generated</h3>
                <p className="text-xs text-rose-600 font-semibold">
                  Save your client secret now. It will NEVER be shown again!
                </p>
              </div>
            </div>

            <div className="space-y-4 my-4">
              <div>
                <label className="block text-xs font-semibold text-slate-600 mb-1">
                  Client ID
                </label>
                <div className="flex items-center gap-2">
                  <input
                    type="text"
                    readOnly
                    value={revealedSecret.clientId}
                    className="input-field bg-slate-50 font-mono text-xs text-slate-800"
                  />
                  <button
                    type="button"
                    onClick={() => copyToClipboard(revealedSecret.clientId, 'client')}
                    className="p-2.5 border border-slate-300 rounded-lg hover:bg-slate-50 transition"
                  >
                    {copiedClientId ? <Check className="w-4 h-4 text-emerald-600" /> : <Copy className="w-4 h-4 text-slate-600" />}
                  </button>
                </div>
              </div>

              <div>
                <label className="block text-xs font-semibold text-slate-600 mb-1">
                  Client Secret
                </label>
                <div className="flex items-center gap-2">
                  <input
                    type="text"
                    readOnly
                    value={revealedSecret.clientSecret}
                    className="input-field bg-amber-50/50 border-amber-200 font-mono text-xs text-amber-900"
                  />
                  <button
                    type="button"
                    onClick={() => copyToClipboard(revealedSecret.clientSecret, 'secret')}
                    className="p-2.5 border border-slate-300 rounded-lg hover:bg-slate-50 transition"
                  >
                    {copiedSecret ? <Check className="w-4 h-4 text-emerald-600" /> : <Copy className="w-4 h-4 text-slate-600" />}
                  </button>
                </div>
              </div>
            </div>

            <div className="p-3 bg-slate-50 rounded-lg border border-slate-200 text-xs text-slate-500 mb-6">
              In accordance with Zero-Trust principles, SecureGate stores only a SHA-256 cryptographic digest of your client secret in the database.
            </div>

            <button
              type="button"
              onClick={() => setRevealedSecret(null)}
              className="btn-primary w-full text-xs"
            >
              I Have Securely Saved These Credentials
            </button>
          </div>
        </div>
      )}
    </DashboardLayout>
  );
}
