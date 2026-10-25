'use client';

import React, { useEffect, useState } from 'react';
import Link from 'next/link';
import { useRouter } from 'next/navigation';
import {
  User,
  Mail,
  ShieldCheck,
  Laptop,
  Smartphone,
  Trash2,
  CheckCircle2,
  AlertCircle,
  Clock,
  LogOut,
  RefreshCw,
  Loader2,
  Globe
} from 'lucide-react';
import DashboardLayout from '@/components/DashboardLayout';
import { useAuth } from '@/context/AuthContext';
import { api, UserDto } from '@/lib/api';

interface SessionDto {
  id: string;
  userId: string;
  deviceInfo: string;
  ipAddress: string;
  lastActivityAt: string;
  expiresAt: string;
  isRevoked: boolean;
  createdAt: string;
}

export default function ProfilePage() {
  const router = useRouter();
  const { user, activeOrganization, refreshUser, isLoading } = useAuth();

  const [fullName, setFullName] = useState('');
  const [updatingProfile, setUpdatingProfile] = useState(false);

  const [sessions, setSessions] = useState<SessionDto[]>([]);
  const [loadingSessions, setLoadingSessions] = useState(true);

  const [errorMessage, setErrorMessage] = useState<string | null>(null);
  const [successMessage, setSuccessMessage] = useState<string | null>(null);

  const loadSessions = async () => {
    setLoadingSessions(true);
    try {
      const data = await api.fetch<SessionDto[]>('/users/me/sessions');
      setSessions(data);
    } catch {
      // Sessions
    } finally {
      setLoadingSessions(false);
    }
  };

  useEffect(() => {
    if (!isLoading && !user) {
      router.push('/login');
      return;
    }
    if (user) {
      setFullName(user.fullName || '');
      loadSessions();
    }
  }, [user, isLoading, router]);

  const handleUpdateProfile = async (e: React.FormEvent) => {
    e.preventDefault();
    setUpdatingProfile(true);
    setErrorMessage(null);
    setSuccessMessage(null);

    try {
      await api.fetch('/users/me', {
        method: 'PUT',
        body: JSON.stringify({ fullName: fullName.trim() }),
      });
      setSuccessMessage('Profile name updated successfully.');
      await refreshUser();
    } catch (err: any) {
      setErrorMessage(err.detail || 'Failed to update profile.');
    } finally {
      setUpdatingProfile(false);
    }
  };

  const handleRevokeSession = async (sessionId: string) => {
    setErrorMessage(null);
    setSuccessMessage(null);

    try {
      await api.fetch(`/users/me/sessions/${sessionId}`, { method: 'DELETE' });
      setSuccessMessage('Session revoked.');
      await loadSessions();
    } catch (err: any) {
      setErrorMessage(err.detail || 'Failed to revoke session.');
    }
  };

  const handleRevokeAllSessions = async () => {
    if (!confirm('Are you sure you want to terminate all other active device sessions?')) {
      return;
    }
    setErrorMessage(null);
    setSuccessMessage(null);

    try {
      await api.fetch('/users/me/sessions/revoke-all', { method: 'POST' });
      setSuccessMessage('All other active sessions have been terminated.');
      await loadSessions();
    } catch (err: any) {
      setErrorMessage(err.detail || 'Failed to terminate sessions.');
    }
  };

  return (
    <DashboardLayout
      title="User Profile & Active Sessions"
      subtitle="Manage your identity credentials, profile attributes, and authenticated device sessions"
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

      <div className="grid grid-cols-1 lg:grid-cols-3 gap-8">
        {/* Profile Card */}
        <div className="card-container space-y-6">
          <div className="flex items-center gap-3 pb-4 border-b border-slate-100">
            <div className="w-12 h-12 rounded-full bg-sky-100 text-sky-700 font-bold flex items-center justify-center text-lg">
              {user?.fullName?.charAt(0) || 'U'}
            </div>
            <div>
              <h3 className="text-base font-bold text-slate-900">{user?.fullName}</h3>
              <p className="text-xs text-slate-500 font-mono">{user?.email}</p>
            </div>
          </div>

          <div className="space-y-3 text-xs">
            <div className="flex items-center justify-between">
              <span className="text-slate-500">Email Status</span>
              {user?.emailVerified ? (
                <span className="inline-flex items-center gap-1 text-emerald-600 font-semibold bg-emerald-50 px-2 py-0.5 rounded border border-emerald-200">
                  <CheckCircle2 className="w-3 h-3" /> Verified
                </span>
              ) : (
                <span className="inline-flex items-center gap-1 text-amber-600 font-semibold bg-amber-50 px-2 py-0.5 rounded border border-amber-200">
                  <Clock className="w-3 h-3" /> Pending Verification
                </span>
              )}
            </div>

            <div className="flex items-center justify-between">
              <span className="text-slate-500">Account Status</span>
              <span className="px-2 py-0.5 rounded font-bold bg-slate-100 text-slate-700">
                {user?.status}
              </span>
            </div>

            <div className="flex items-center justify-between">
              <span className="text-slate-500">Active Tenant</span>
              <span className="font-semibold text-slate-800">
                {activeOrganization?.name || 'None Selected'}
              </span>
            </div>
          </div>

          <form onSubmit={handleUpdateProfile} className="pt-4 border-t border-slate-100 space-y-3">
            <h4 className="text-xs font-bold text-slate-800">Update Display Name</h4>
            <input
              type="text"
              required
              value={fullName}
              onChange={(e) => setFullName(e.target.value)}
              className="input-field"
            />
            <button
              type="submit"
              disabled={updatingProfile}
              className="btn-primary w-full text-xs"
            >
              {updatingProfile ? 'Saving...' : 'Save Name'}
            </button>
          </form>
        </div>

        {/* Active Sessions List */}
        <div className="lg:col-span-2 card-container p-0 overflow-hidden">
          <div className="p-4 sm:p-6 border-b border-slate-100 flex items-center justify-between">
            <div>
              <h3 className="text-base font-bold text-slate-900">Active Device Sessions</h3>
              <p className="text-xs text-slate-500">
                All devices currently issued with valid refresh tokens for your account
              </p>
            </div>
            {sessions.length > 1 && (
              <button
                type="button"
                onClick={handleRevokeAllSessions}
                className="btn-secondary text-xs text-rose-600 hover:text-rose-700 border-rose-200 hover:bg-rose-50"
              >
                Revoke Other Sessions
              </button>
            )}
          </div>

          {loadingSessions ? (
            <div className="py-16 text-center text-xs text-slate-400">Loading active sessions...</div>
          ) : sessions.length === 0 ? (
            <div className="py-16 text-center text-xs text-slate-400">No active sessions found.</div>
          ) : (
            <div className="divide-y divide-slate-100">
              {sessions.map((session, idx) => (
                <div key={session.id} className="p-4 sm:px-6 flex items-center justify-between">
                  <div className="flex items-start gap-3">
                    <div className="p-2 bg-slate-100 text-slate-600 rounded-lg mt-0.5">
                      <Laptop className="w-5 h-5" />
                    </div>
                    <div className="space-y-1">
                      <div className="flex items-center gap-2">
                        <span className="text-xs font-semibold text-slate-900 truncate max-w-xs">
                          {session.deviceInfo || 'Standard Web Browser'}
                        </span>
                        {idx === 0 && (
                          <span className="text-[10px] font-bold text-sky-600 bg-sky-50 px-2 py-0.5 rounded border border-sky-200">
                            Current Session
                          </span>
                        )}
                      </div>
                      <p className="text-[11px] text-slate-400 flex items-center gap-2">
                        <span>IP: {session.ipAddress || '127.0.0.1'}</span>
                        <span>•</span>
                        <span>Last active: {new Date(session.lastActivityAt).toLocaleTimeString()}</span>
                      </p>
                    </div>
                  </div>

                  <button
                    type="button"
                    onClick={() => handleRevokeSession(session.id)}
                    title="Terminate session"
                    className="text-slate-400 hover:text-rose-600 p-2 rounded hover:bg-rose-50 transition"
                  >
                    <Trash2 className="w-4 h-4" />
                  </button>
                </div>
              ))}
            </div>
          )}
        </div>
      </div>
    </DashboardLayout>
  );
}
