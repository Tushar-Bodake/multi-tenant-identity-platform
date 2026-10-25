'use client';

import React, { useState } from 'react';
import Link from 'next/link';
import { useRouter } from 'next/navigation';
import { Shield, Building2, Plus, ArrowRight, LogOut, CheckCircle2, AlertCircle, Loader2 } from 'lucide-react';
import { useAuth } from '@/context/AuthContext';
import { api, OrganizationDto } from '@/lib/api';

export default function OrganizationsPage() {
  const router = useRouter();
  const { user, organizations, activeOrganization, switchOrganization, logout, refreshUser } = useAuth();

  const [isModalOpen, setIsModalOpen] = useState(false);
  const [newOrgName, setNewOrgName] = useState('');
  const [creating, setCreating] = useState(false);
  const [switchingId, setSwitchingId] = useState<string | null>(null);
  const [error, setError] = useState<string | null>(null);

  const handleSelectOrg = async (orgId: string) => {
    setSwitchingId(orgId);
    try {
      await switchOrganization(orgId);
    } catch (err: any) {
      setError(err.detail || 'Failed to select organization');
      setSwitchingId(null);
    }
  };

  const handleCreateOrg = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!newOrgName.trim()) return;

    setCreating(true);
    setError(null);

    try {
      const createdOrg = await api.fetch<OrganizationDto>('/organizations', {
        method: 'POST',
        body: JSON.stringify({ name: newOrgName.trim() }),
      });

      await refreshUser();
      setIsModalOpen(false);
      setNewOrgName('');
      await handleSelectOrg(createdOrg.id);
    } catch (err: any) {
      setError(err.detail || 'Failed to create organization');
    } finally {
      setCreating(false);
    }
  };

  const getRoleBadgeClass = (role: string) => {
    switch (role) {
      case 'ORG_OWNER':
        return 'bg-purple-50 text-purple-700 border-purple-200';
      case 'ORG_ADMIN':
        return 'bg-sky-50 text-sky-700 border-sky-200';
      case 'DEVELOPER':
        return 'bg-emerald-50 text-emerald-700 border-emerald-200';
      default:
        return 'bg-slate-50 text-slate-700 border-slate-200';
    }
  };

  return (
    <div className="min-h-screen bg-slate-50 flex flex-col">
      {/* Header */}
      <header className="bg-white border-b border-slate-200 px-6 py-4 flex items-center justify-between">
        <div className="flex items-center gap-3">
          <div className="p-2 bg-sky-600 rounded-lg text-white shadow-sm">
            <Shield className="w-5 h-5" />
          </div>
          <span className="text-xl font-bold tracking-tight text-slate-900">SecureGate</span>
          <span className="text-xs uppercase tracking-wider font-semibold bg-slate-100 text-slate-600 px-2 py-0.5 rounded-full ml-1">
            Tenant Directory
          </span>
        </div>

        <div className="flex items-center gap-4">
          <div className="text-right">
            <p className="text-xs font-semibold text-slate-800">{user?.fullName}</p>
            <p className="text-[11px] text-slate-400">{user?.email}</p>
          </div>
          <button
            onClick={() => logout()}
            className="p-2 text-slate-400 hover:text-slate-600 rounded-lg hover:bg-slate-100"
            title="Sign Out"
          >
            <LogOut className="w-4 h-4" />
          </button>
        </div>
      </header>

      {/* Main Container */}
      <main className="flex-1 max-w-4xl w-full mx-auto p-6 sm:p-10">
        <div className="flex flex-col sm:flex-row sm:items-center sm:justify-between gap-4 mb-8">
          <div>
            <h1 className="text-2xl font-bold tracking-tight text-slate-900">Your Organizations</h1>
            <p className="text-sm text-slate-500 mt-0.5">
              Select an organization workspace to manage users, applications, and security policies.
            </p>
          </div>
          <button
            onClick={() => setIsModalOpen(true)}
            className="btn-primary text-xs flex items-center gap-2 self-start sm:self-auto"
          >
            <Plus className="w-4 h-4" /> Create Organization
          </button>
        </div>

        {error && (
          <div className="mb-6 p-4 rounded-xl bg-rose-50 border border-rose-200 text-rose-700 text-xs flex items-center gap-2">
            <AlertCircle className="w-4 h-4 flex-shrink-0" />
            <span>{error}</span>
          </div>
        )}

        {/* Organizations Grid */}
        <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
          {organizations.map((org) => {
            const isActive = activeOrganization?.id === org.id;
            const isSwitching = switchingId === org.id;

            return (
              <div
                key={org.id}
                className={`card-container transition duration-200 flex flex-col justify-between ${
                  isActive ? 'border-sky-500 ring-2 ring-sky-100' : 'hover:border-slate-300'
                }`}
              >
                <div>
                  <div className="flex items-start justify-between">
                    <div className="flex items-center gap-3">
                      <div className="w-10 h-10 rounded-xl bg-slate-100 border border-slate-200 flex items-center justify-center text-slate-700 font-bold text-base">
                        {org.name.substring(0, 2).toUpperCase()}
                      </div>
                      <div>
                        <h2 className="font-semibold text-slate-900 text-base">{org.name}</h2>
                        <p className="text-xs text-slate-400">slug: {org.slug}</p>
                      </div>
                    </div>
                    {isActive && (
                      <span className="inline-flex items-center gap-1 text-[11px] font-semibold text-sky-700 bg-sky-50 border border-sky-200 px-2 py-0.5 rounded-full">
                        <CheckCircle2 className="w-3 h-3" /> Active
                      </span>
                    )}
                  </div>

                  <div className="mt-4 flex items-center gap-2">
                    <span className={`text-[11px] font-medium border px-2 py-0.5 rounded-md ${getRoleBadgeClass(org.role)}`}>
                      Role: {org.role}
                    </span>
                    <span className="text-[11px] font-medium border bg-emerald-50 text-emerald-700 border-emerald-200 px-2 py-0.5 rounded-md">
                      Status: {org.status}
                    </span>
                  </div>
                </div>

                <div className="mt-6 pt-4 border-t border-slate-100 flex items-center justify-end">
                  <button
                    onClick={() => handleSelectOrg(org.id)}
                    disabled={isSwitching}
                    className="btn-primary text-xs flex items-center gap-1.5"
                  >
                    {isSwitching ? (
                      <>
                        <Loader2 className="w-3.5 h-3.5 animate-spin" /> Switching Context...
                      </>
                    ) : isActive ? (
                      <>
                        Go to Dashboard <ArrowRight className="w-3.5 h-3.5" />
                      </>
                    ) : (
                      <>
                        Enter Workspace <ArrowRight className="w-3.5 h-3.5" />
                      </>
                    )}
                  </button>
                </div>
              </div>
            );
          })}
        </div>
      </main>

      {/* Create Org Modal */}
      {isModalOpen && (
        <div className="fixed inset-0 bg-slate-900/40 backdrop-blur-sm z-50 flex items-center justify-center p-4">
          <div className="bg-white rounded-2xl border border-slate-200 shadow-xl max-w-md w-full p-6">
            <h3 className="text-lg font-bold text-slate-900 mb-1">Create New Organization</h3>
            <p className="text-xs text-slate-500 mb-4">
              Enter a name for the organization. You will automatically become the Organization Owner.
            </p>

            <form onSubmit={handleCreateOrg} className="space-y-4">
              <div>
                <label className="block text-xs font-medium text-slate-700 mb-1">
                  Organization Name
                </label>
                <div className="relative">
                  <Building2 className="w-4 h-4 absolute left-3 top-3 text-slate-400" />
                  <input
                    type="text"
                    required
                    value={newOrgName}
                    onChange={(e) => setNewOrgName(e.target.value)}
                    placeholder="e.g. Acme Innovations"
                    className="input-field pl-9"
                  />
                </div>
              </div>

              <div className="flex justify-end gap-3 pt-2">
                <button
                  type="button"
                  onClick={() => setIsModalOpen(false)}
                  className="btn-secondary text-xs"
                >
                  Cancel
                </button>
                <button
                  type="submit"
                  disabled={creating || !newOrgName.trim()}
                  className="btn-primary text-xs"
                >
                  {creating ? 'Creating...' : 'Create Organization'}
                </button>
              </div>
            </form>
          </div>
        </div>
      )}
    </div>
  );
}
