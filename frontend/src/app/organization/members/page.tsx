'use client';

import React, { useEffect, useState } from 'react';
import Link from 'next/link';
import { useRouter } from 'next/navigation';
import {
  Users,
  UserPlus,
  ShieldAlert,
  Trash2,
  ShieldCheck,
  CheckCircle2,
  AlertCircle,
  Clock,
  KeyRound,
  X,
  Loader2,
  Lock
} from 'lucide-react';
import DashboardLayout from '@/components/DashboardLayout';
import { useAuth } from '@/context/AuthContext';
import { api, MemberDto } from '@/lib/api';

export default function MembersPage() {
  const router = useRouter();
  const { user, activeOrganization, isLoading } = useAuth();

  const [members, setMembers] = useState<MemberDto[]>([]);
  const [loading, setLoading] = useState(true);
  const [errorMessage, setErrorMessage] = useState<string | null>(null);
  const [successMessage, setSuccessMessage] = useState<string | null>(null);

  // Invite modal state
  const [isInviteModalOpen, setIsInviteModalOpen] = useState(false);
  const [inviteEmail, setInviteEmail] = useState('');
  const [inviteRole, setInviteRole] = useState('MEMBER');
  const [inviteSubmitting, setInviteSubmitting] = useState(false);

  // Transfer ownership modal state
  const [isTransferModalOpen, setIsTransferModalOpen] = useState(false);
  const [transferTargetUserId, setTransferTargetUserId] = useState('');
  const [currentPassword, setCurrentPassword] = useState('');
  const [transferSubmitting, setTransferSubmitting] = useState(false);

  const isOwner = activeOrganization?.role === 'ORG_OWNER';
  const isAdmin = activeOrganization?.role === 'ORG_ADMIN' || isOwner;

  const loadMembers = async () => {
    if (!activeOrganization) return;
    setLoading(true);
    setErrorMessage(null);
    try {
      const data = await api.fetch<MemberDto[]>(`/organizations/${activeOrganization.id}/members`);
      setMembers(data);
    } catch (err: any) {
      setErrorMessage(err.detail || 'Failed to fetch organization members.');
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
      loadMembers();
    }
  }, [activeOrganization, user, isLoading, router]);

  const handleInvite = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!activeOrganization || !inviteEmail.trim()) return;

    setInviteSubmitting(true);
    setErrorMessage(null);
    setSuccessMessage(null);

    try {
      await api.fetch(`/organizations/${activeOrganization.id}/invitations`, {
        method: 'POST',
        body: JSON.stringify({
          email: inviteEmail.trim(),
          role: inviteRole,
        }),
      });

      setSuccessMessage(`Invitation dispatched to ${inviteEmail}.`);
      setIsInviteModalOpen(false);
      setInviteEmail('');
      setInviteRole('MEMBER');
    } catch (err: any) {
      setErrorMessage(err.detail || 'Failed to send invitation. Verify your permissions.');
    } finally {
      setInviteSubmitting(false);
    }
  };

  const handleUpdateRole = async (targetUserId: string, newRole: string) => {
    if (!activeOrganization) return;
    setErrorMessage(null);
    setSuccessMessage(null);

    try {
      await api.fetch(`/organizations/${activeOrganization.id}/members/${targetUserId}/role`, {
        method: 'PATCH',
        body: JSON.stringify({ role: newRole }),
      });
      setSuccessMessage('Member role updated successfully.');
      await loadMembers();
    } catch (err: any) {
      setErrorMessage(err.detail || 'Failed to update member role.');
    }
  };

  const handleRemoveMember = async (targetUserId: string, memberName: string) => {
    if (!activeOrganization) return;
    if (!confirm(`Are you sure you want to remove ${memberName} from this organization?`)) {
      return;
    }

    setErrorMessage(null);
    setSuccessMessage(null);

    try {
      await api.fetch(`/organizations/${activeOrganization.id}/members/${targetUserId}`, {
        method: 'DELETE',
      });
      setSuccessMessage(`${memberName} was removed from the organization.`);
      await loadMembers();
    } catch (err: any) {
      setErrorMessage(err.detail || 'Failed to remove member. Protection prevents removing the last owner.');
    }
  };

  const handleTransferOwnership = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!activeOrganization || !transferTargetUserId) return;

    setTransferSubmitting(true);
    setErrorMessage(null);
    setSuccessMessage(null);

    try {
      await api.fetch(`/organizations/${activeOrganization.id}/transfer-ownership`, {
        method: 'POST',
        body: JSON.stringify({
          newOwnerUserId: transferTargetUserId,
          currentPassword,
        }),
      });

      setSuccessMessage('Organization ownership transferred successfully.');
      setIsTransferModalOpen(false);
      setCurrentPassword('');
      await loadMembers();
    } catch (err: any) {
      setErrorMessage(err.detail || 'Failed to transfer ownership. Verify your current password.');
    } finally {
      setTransferSubmitting(false);
    }
  };

  const getRoleBadge = (role: string) => {
    switch (role) {
      case 'ORG_OWNER':
        return 'bg-purple-50 text-purple-700 border-purple-200';
      case 'ORG_ADMIN':
        return 'bg-sky-50 text-sky-700 border-sky-200';
      case 'DEVELOPER':
        return 'bg-indigo-50 text-indigo-700 border-indigo-200';
      default:
        return 'bg-slate-100 text-slate-700 border-slate-200';
    }
  };

  return (
    <DashboardLayout
      title="Team Members & Roles"
      subtitle="Manage organization memberships, role-based access control, and member invitations"
      actions={
        <div className="flex items-center gap-3">
          {isOwner && (
            <button
              type="button"
              onClick={() => setIsTransferModalOpen(true)}
              className="btn-secondary text-xs flex items-center gap-1.5"
            >
              <KeyRound className="w-3.5 h-3.5" />
              Transfer Ownership
            </button>
          )}
          {isAdmin && (
            <button
              type="button"
              onClick={() => setIsInviteModalOpen(true)}
              className="btn-primary text-xs flex items-center gap-1.5"
            >
              <UserPlus className="w-3.5 h-3.5" />
              Invite Member
            </button>
          )}
        </div>
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

      {/* Members Table */}
      <div className="card-container overflow-hidden p-0">
        <div className="p-4 sm:p-6 border-b border-slate-100 flex items-center justify-between">
          <div>
            <h3 className="text-base font-bold text-slate-900">Current Members</h3>
            <p className="text-xs text-slate-500">
              Users with authenticated access to this tenant&apos;s isolated resources
            </p>
          </div>
          <span className="text-xs font-semibold px-2.5 py-1 rounded-full bg-slate-100 text-slate-600">
            {members.length} {members.length === 1 ? 'member' : 'members'}
          </span>
        </div>

        {loading ? (
          <div className="py-16 text-center">
            <Loader2 className="w-6 h-6 animate-spin text-sky-600 mx-auto mb-2" />
            <p className="text-xs text-slate-500">Loading memberships...</p>
          </div>
        ) : (
          <div className="overflow-x-auto">
            <table className="w-full text-left text-xs">
              <thead className="bg-slate-50 text-slate-500 font-semibold border-b border-slate-200">
                <tr>
                  <th className="px-6 py-3">Member</th>
                  <th className="px-6 py-3">Email Address</th>
                  <th className="px-6 py-3">Role</th>
                  <th className="px-6 py-3 text-right">Actions</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-slate-100">
                {members.map((m) => (
                  <tr key={m.userId} className="hover:bg-slate-50/50 transition">
                    <td className="px-6 py-4 font-medium text-slate-900 flex items-center gap-3">
                      <div className="w-8 h-8 rounded-full bg-slate-100 text-slate-700 font-bold flex items-center justify-center text-xs">
                        {m.fullName.charAt(0)}
                      </div>
                      <div>
                        <span>{m.fullName}</span>
                        {m.isCurrentUser && (
                          <span className="ml-2 text-[10px] font-bold text-sky-600 bg-sky-50 px-1.5 py-0.5 rounded border border-sky-200">
                            You
                          </span>
                        )}
                      </div>
                    </td>
                    <td className="px-6 py-4 text-slate-500 font-mono text-xs">
                      {m.email}
                    </td>
                    <td className="px-6 py-4">
                      {isAdmin && !m.isCurrentUser && m.role !== 'ORG_OWNER' ? (
                        <select
                          value={m.role}
                          onChange={(e) => handleUpdateRole(m.userId, e.target.value)}
                          className="text-xs rounded-md border-slate-300 py-1 px-2 font-medium bg-white shadow-sm ring-1 ring-slate-200"
                        >
                          <option value="ORG_ADMIN">ORG_ADMIN</option>
                          <option value="DEVELOPER">DEVELOPER</option>
                          <option value="MEMBER">MEMBER</option>
                        </select>
                      ) : (
                        <span className={`inline-block px-2.5 py-1 rounded-full text-[11px] font-semibold border ${getRoleBadge(m.role)}`}>
                          {m.role}
                        </span>
                      )}
                    </td>
                    <td className="px-6 py-4 text-right">
                      {isAdmin && !m.isCurrentUser && m.role !== 'ORG_OWNER' && (
                        <button
                          type="button"
                          onClick={() => handleRemoveMember(m.userId, m.fullName)}
                          title="Remove member"
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

      {/* Invite Member Modal */}
      {isInviteModalOpen && (
        <div className="fixed inset-0 z-50 bg-slate-900/40 backdrop-blur-sm flex items-center justify-center p-4">
          <div className="bg-white rounded-2xl max-w-md w-full p-6 shadow-xl border border-slate-200 animate-in fade-in duration-200">
            <div className="flex items-center justify-between mb-4 pb-3 border-b border-slate-100">
              <div className="flex items-center gap-2">
                <div className="p-2 bg-sky-50 text-sky-600 rounded-lg">
                  <UserPlus className="w-5 h-5" />
                </div>
                <h3 className="text-base font-bold text-slate-900">Invite New Member</h3>
              </div>
              <button
                type="button"
                onClick={() => setIsInviteModalOpen(false)}
                className="text-slate-400 hover:text-slate-600"
              >
                <X className="w-5 h-5" />
              </button>
            </div>

            <form onSubmit={handleInvite} className="space-y-4">
              <div>
                <label className="block text-xs font-semibold text-slate-700 mb-1">
                  Email Address
                </label>
                <input
                  type="email"
                  required
                  value={inviteEmail}
                  onChange={(e) => setInviteEmail(e.target.value)}
                  placeholder="colleague@company.com"
                  className="input-field"
                />
              </div>

              <div>
                <label className="block text-xs font-semibold text-slate-700 mb-1">
                  Organization Role
                </label>
                <select
                  value={inviteRole}
                  onChange={(e) => setInviteRole(e.target.value)}
                  className="input-field bg-white"
                >
                  <option value="MEMBER">MEMBER — Standard access to permitted tenant resources</option>
                  <option value="DEVELOPER">DEVELOPER — Manage client applications and API keys</option>
                  <option value="ORG_ADMIN">ORG_ADMIN — Manage team members and security settings</option>
                </select>
              </div>

              <div className="pt-2 flex items-center justify-end gap-3">
                <button
                  type="button"
                  onClick={() => setIsInviteModalOpen(false)}
                  className="btn-secondary text-xs"
                >
                  Cancel
                </button>
                <button
                  type="submit"
                  disabled={inviteSubmitting}
                  className="btn-primary text-xs"
                >
                  {inviteSubmitting ? 'Sending Invite...' : 'Send Single-Use Invitation'}
                </button>
              </div>
            </form>
          </div>
        </div>
      )}

      {/* Transfer Ownership Modal */}
      {isTransferModalOpen && (
        <div className="fixed inset-0 z-50 bg-slate-900/40 backdrop-blur-sm flex items-center justify-center p-4">
          <div className="bg-white rounded-2xl max-w-md w-full p-6 shadow-xl border border-slate-200">
            <div className="flex items-center justify-between mb-4 pb-3 border-b border-slate-100">
              <div className="flex items-center gap-2">
                <div className="p-2 bg-amber-50 text-amber-600 rounded-lg">
                  <ShieldAlert className="w-5 h-5" />
                </div>
                <h3 className="text-base font-bold text-slate-900">Transfer Organization Ownership</h3>
              </div>
              <button
                type="button"
                onClick={() => setIsTransferModalOpen(false)}
                className="text-slate-400 hover:text-slate-600"
              >
                <X className="w-5 h-5" />
              </button>
            </div>

            <form onSubmit={handleTransferOwnership} className="space-y-4">
              <p className="text-xs text-slate-500 leading-relaxed">
                Ownership transfer grants complete authority over organization billing, tenant settings, and members.
                You will be demoted to ORG_ADMIN. This action requires reauthentication.
              </p>

              <div>
                <label className="block text-xs font-semibold text-slate-700 mb-1">
                  Select New Owner
                </label>
                <select
                  required
                  value={transferTargetUserId}
                  onChange={(e) => setTransferTargetUserId(e.target.value)}
                  className="input-field bg-white"
                >
                  <option value="">Select a team member...</option>
                  {members
                    .filter((m) => !m.isCurrentUser)
                    .map((m) => (
                      <option key={m.userId} value={m.userId}>
                        {m.fullName} ({m.email})
                      </option>
                    ))}
                </select>
              </div>

              <div>
                <label className="block text-xs font-semibold text-slate-700 mb-1">
                  Current Password (Re-authentication)
                </label>
                <input
                  type="password"
                  required
                  value={currentPassword}
                  onChange={(e) => setCurrentPassword(e.target.value)}
                  placeholder="Enter your current password"
                  className="input-field"
                />
              </div>

              <div className="pt-2 flex items-center justify-end gap-3">
                <button
                  type="button"
                  onClick={() => setIsTransferModalOpen(false)}
                  className="btn-secondary text-xs"
                >
                  Cancel
                </button>
                <button
                  type="submit"
                  disabled={transferSubmitting || !transferTargetUserId}
                  className="inline-flex items-center justify-center rounded-lg bg-rose-600 px-4 py-2.5 text-xs font-semibold text-white shadow-sm hover:bg-rose-500 disabled:opacity-50"
                >
                  {transferSubmitting ? 'Transferring...' : 'Confirm Ownership Transfer'}
                </button>
              </div>
            </form>
          </div>
        </div>
      )}
    </DashboardLayout>
  );
}
